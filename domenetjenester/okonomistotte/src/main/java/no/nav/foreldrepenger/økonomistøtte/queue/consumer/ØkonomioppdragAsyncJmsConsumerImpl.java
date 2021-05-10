package no.nav.foreldrepenger.økonomistøtte.queue.consumer;

import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.TextMessage;
import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import javax.xml.stream.XMLStreamException;

import org.xml.sax.SAXException;

import no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.Mmel;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.Oppdrag;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.OppdragSkjemaConstants;
import no.nav.foreldrepenger.xmlutils.JaxbHelper;
import no.nav.foreldrepenger.økonomistøtte.BehandleØkonomioppdragKvittering;
import no.nav.foreldrepenger.økonomistøtte.ØkonomiKvittering;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.integrasjon.jms.InternalQueueConsumer;
import no.nav.vedtak.felles.integrasjon.jms.JmsKonfig;
import no.nav.vedtak.felles.integrasjon.jms.precond.DefaultDatabaseOppePreconditionChecker;
import no.nav.vedtak.felles.integrasjon.jms.precond.PreconditionChecker;

@ApplicationScoped
public class ØkonomioppdragAsyncJmsConsumerImpl extends InternalQueueConsumer implements ØkonomioppdragAsyncJmsConsumer {
    private BehandleØkonomioppdragKvittering behandleØkonomioppdragKvittering;
    private DefaultDatabaseOppePreconditionChecker preconditionChecker;

    public ØkonomioppdragAsyncJmsConsumerImpl() {
    }

    @Inject
    public ØkonomioppdragAsyncJmsConsumerImpl(BehandleØkonomioppdragKvittering behandleØkonomioppdragKvittering,
                                              DefaultDatabaseOppePreconditionChecker preconditionChecker,
                                              @Named("økonomioppdragjmsconsumerkonfig") JmsKonfig konfig) {
        super(konfig);
        this.behandleØkonomioppdragKvittering = behandleØkonomioppdragKvittering;
        this.preconditionChecker = preconditionChecker;
    }

    @Override
    public PreconditionChecker getPreconditionChecker() {
        return preconditionChecker;
    }

    @Override
    public void handle(Message message) throws JMSException {
        log.debug("Mottar melding");
        if (message instanceof TextMessage) {
            handle(((TextMessage) message).getText());
        } else {
            log.warn("Mottok på ikkestøttet message av klasse {}. Kø-elementet ble ignorert", message.getClass());
        }
    }

    @Override
    public void handle(String message) {
        try {
            var kvitteringsmelding = unmarshalOgKorriger(message);
            if (inneholderOppdragslinjer(kvitteringsmelding)) {
                var kvittering = fraKvitteringsmelding(kvitteringsmelding);
                behandleØkonomioppdragKvittering.behandleKvittering(kvittering);
            } else {
                loggKvitteringUtenLinjer(kvitteringsmelding);
            }
        } catch (SAXException | JAXBException e) { // NOSONAR
            throw new TekniskException("FP-595437", "Uventet feil med JAXB ved parsing av melding "
                + "oppdragskjema.oppdrag: " + message, e);
        } catch (XMLStreamException e) { // NOSONAR
            throw new TekniskException("FP-744861", "Feil i parsing av oppdragskjema.oppdrag", e);
        }
    }

    private void loggKvitteringUtenLinjer(Oppdrag kvitteringsmelding) {
        var oppdrag110 = kvitteringsmelding.getOppdrag110();
        var mmel = kvitteringsmelding.getMmel();
        var fagsystemId = oppdrag110.getFagsystemId();
        var fagområde = oppdrag110.getKodeFagomraade();
        var alvorlighetsgrad = mmel.getAlvorlighetsgrad();
        var beskrivendeMelding = mmel.getBeskrMelding();
        var kodeMelding = mmel.getKodeMelding();
        log.warn("""
            Mottok og ignorerte kvitteringsmelding uten oppdragslinjer,
            kan ikke direkte identifisere behandling. Gjelder fagsystemId={}.
            Innhold i kvittering: alvorlighetsgrad={} meldingKode='{}' beskrivendeMelding='{}', fagomraade={}.
            Skal følges opp manuelt
            """, fagsystemId, alvorlighetsgrad, kodeMelding, beskrivendeMelding, fagområde);
    }

    private boolean inneholderOppdragslinjer(Oppdrag kvitteringsmelding) {
        return !kvitteringsmelding.getOppdrag110().getOppdragsLinje150().isEmpty();
    }

    private Oppdrag unmarshalOgKorriger(String message) throws JAXBException, XMLStreamException, SAXException {
        Oppdrag kvitteringsmelding;
        try {
            kvitteringsmelding = JaxbHelper.unmarshalAndValidateXMLWithStAX(OppdragSkjemaConstants.JAXB_CLASS, message,
                OppdragSkjemaConstants.XSD_LOCATION);
        } catch (UnmarshalException e) { // NOSONAR
            var editedMessage = message.replace("<oppdrag ", "<xml_1:oppdrag ")
                .replace("xmlns=", "xmlns:xml_1=")
                .replace("</oppdrag>", "</xml_1:oppdrag>")
                .replace("</ns2:oppdrag>", "</xml_1:oppdrag>");
            kvitteringsmelding = JaxbHelper.unmarshalAndValidateXMLWithStAX(OppdragSkjemaConstants.JAXB_CLASS,
                editedMessage, OppdragSkjemaConstants.XSD_LOCATION);
        }
        return kvitteringsmelding;
    }

    private ØkonomiKvittering fraKvitteringsmelding(Oppdrag melding) {
        var kvittering = new ØkonomiKvittering();
        fraMmel(kvittering, melding.getMmel(), melding.getOppdrag110().getFagsystemId());
        fraOppdragLinje150(kvittering, melding);
        return kvittering;
    }

    private void fraOppdragLinje150(ØkonomiKvittering kvittering, Oppdrag melding) {
        // trenger kun en tilfeldig oppdrag150 for å hente behandlingId
        var oppdragsLinje150 = melding.getOppdrag110().getOppdragsLinje150().get(0);
        kvittering.setBehandlingId(Long.valueOf(oppdragsLinje150.getHenvisning()));
    }

    private void fraMmel(ØkonomiKvittering kvittering, Mmel mmel, String fagsystemId) {
        kvittering.setAlvorlighetsgrad(mmel.getAlvorlighetsgrad());
        kvittering.setMeldingKode(mmel.getKodeMelding());
        kvittering.setBeskrMelding(mmel.getBeskrMelding());
        kvittering.setFagsystemId(Long.parseLong(fagsystemId));
    }

    @Override
    @Resource(mappedName = ØkonomioppdragJmsConsumerKonfig.JNDI_JMS_CONNECTION_FACTORY)
    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        super.setConnectionFactory(connectionFactory);
    }

    @Override
    @Resource(mappedName = ØkonomioppdragJmsConsumerKonfig.JNDI_QUEUE)
    public void setQueue(Queue queue) {
        super.setQueue(queue);
    }
}
