package no.nav.foreldrepenger.web.app.soap.sak.v1;

import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.jws.WebService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.kontrakter.fordel.JournalpostVurderingDto;
import no.nav.foreldrepenger.sikkerhet.abac.AppAbacAttributtType;
import no.nav.foreldrepenger.web.app.soap.sak.tjeneste.OpprettSakOrchestrator;
import no.nav.tjeneste.virksomhet.behandleforeldrepengesak.v1.binding.BehandleForeldrepengesakV1;
import no.nav.tjeneste.virksomhet.behandleforeldrepengesak.v1.binding.OpprettSakSakEksistererAllerede;
import no.nav.tjeneste.virksomhet.behandleforeldrepengesak.v1.binding.OpprettSakSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.behandleforeldrepengesak.v1.binding.OpprettSakUgyldigInput;
import no.nav.tjeneste.virksomhet.behandleforeldrepengesak.v1.feil.UgyldigInput;
import no.nav.tjeneste.virksomhet.behandleforeldrepengesak.v1.meldinger.OpprettSakRequest;
import no.nav.tjeneste.virksomhet.behandleforeldrepengesak.v1.meldinger.OpprettSakResponse;
import no.nav.vedtak.felles.integrasjon.felles.ws.SoapWebService;
import no.nav.vedtak.felles.integrasjon.felles.ws.VLFaultListenerUnntakKonfigurasjon;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.util.env.Cluster;
import no.nav.vedtak.util.env.Environment;

/**
 * Webservice for å opprette sak i VL ved manuelle journalføringsoppgaver.
 */

/* @Transaction
 * HACK (u139158): Transaksjonsgrensen er for denne webservice'en flyttet til javatjenesten OpprettGSakTjeneste
 * Dette er ikke i henhold til standard og kan ikke gjøres uten godkjenning fra sjefsarkitekt.
 * Grunnen for at det er gjort her er for å sikre at de tre kallene går i separate transaksjoner.
 * Se https://jira.adeo.no/browse/PKHUMLE-359 for detaljer.
 */
@Dependent
@WebService(
    wsdlLocation = "wsdl/no/nav/tjeneste/virksomhet/behandleForeldrepengesak/v1/behandleForeldrepengesak.wsdl",
    serviceName = "BehandleForeldrepengesak_v1",
    portName = "BehandleForeldrepengesak_v1Port",
    endpointInterface = "no.nav.tjeneste.virksomhet.behandleforeldrepengesak.v1.binding.BehandleForeldrepengesakV1")
@SoapWebService(endpoint = "/sak/opprettSak/v1", tjenesteBeskrivelseURL = "https://confluence.adeo.no/pages/viewpage.action?pageId=220529015")
public class OpprettSakService implements BehandleForeldrepengesakV1 {

    private static final Logger logger = LoggerFactory.getLogger(OpprettSakService.class);

    private OpprettSakOrchestrator opprettSakOrchestrator;
    private FpfordelRestKlient fordelKlient;
    private boolean isEnvStable;

    public OpprettSakService() {
        // NOSONAR: cdi
    }

    @Inject
    public OpprettSakService(OpprettSakOrchestrator opprettSakOrchestrator,
                             FpfordelRestKlient fordelKlient) {
        this.opprettSakOrchestrator = opprettSakOrchestrator;
        this.fordelKlient = fordelKlient;
        this.isEnvStable = Cluster.PROD_FSS.equals(Environment.current().getCluster());
    }

    OpprettSakService(OpprettSakOrchestrator opprettSakOrchestrator,
                             FpfordelRestKlient fordelKlient,
                             boolean envForTest) {
        this.opprettSakOrchestrator = opprettSakOrchestrator;
        this.fordelKlient = fordelKlient;
        this.isEnvStable = envForTest;
    }

    @Override
    public void ping() {
        logger.debug("ping");
    }

    @Override
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.CREATE, ressurs = BeskyttetRessursResourceAttributt.FAGSAK)
    public OpprettSakResponse opprettSak(
        @TilpassetAbacAttributt(supplierClass = AbacDataSupplier.class) OpprettSakRequest opprettSakRequest)
        throws OpprettSakSakEksistererAllerede, OpprettSakSikkerhetsbegrensning, OpprettSakUgyldigInput {

        if (opprettSakRequest.getSakspart().getAktoerId() == null) {
            UgyldigInput faultInfo = lagUgyldigInput("AktørId", null);
            throw new OpprettSakUgyldigInput(faultInfo.getFeilmelding(), faultInfo);
        }
        AktørId aktørId = new AktørId(opprettSakRequest.getSakspart().getAktoerId());
        BehandlingTema behandlingTema = hentBehandlingstema(opprettSakRequest.getBehandlingstema().getValue());
        JournalpostId journalpostId = new JournalpostId(opprettSakRequest.getJournalpostId());
        validerJournalpostId(journalpostId, behandlingTema, aktørId);

        Saksnummer saksnummer = opprettSakOrchestrator.opprettSak(journalpostId, behandlingTema, aktørId);

        return lagResponse(saksnummer);
    }

    private void validerJournalpostId(JournalpostId journalpostId, BehandlingTema behandlingTema, AktørId aktørId) throws OpprettSakUgyldigInput {
        if (!isEnvStable)
            return;
        JournalpostVurderingDto vurdering = fordelKlient.utledYtelestypeFor(journalpostId);
        var btVurdering = BehandlingTema.finnForKodeverkEiersKode(vurdering.getBehandlingstemaOffisiellKode());
        var btOppgitt = BehandlingTema.fraFagsakHendelse(behandlingTema.getFagsakYtelseType(), null);
        logger.info("FPSAK vurdering FPFORDEL ytelsedok {} vs ytelseoppgitt {}", btVurdering, btOppgitt);
        if (btVurdering.equals(btOppgitt) && (vurdering.getErFørstegangssøknad() || vurdering.getErInntektsmelding())) {
            if (vurdering.getErInntektsmelding() && BehandlingTema.FORELDREPENGER.equals(btVurdering) && opprettSakOrchestrator.harAktivSak(aktørId, behandlingTema)) {
                UgyldigInput faultInfo = lagUgyldigInput("Journalpost", "Inntektsmelding når det finnes åpen Foreldrepengesak");
                throw new OpprettSakUgyldigInput(faultInfo.getFeilmelding(), faultInfo);
            } else {
                return;
            }
        }
        if (BehandlingTema.UDEFINERT.equals(btVurdering) || btVurdering.equals(btOppgitt)) {
            throw OpprettSakServiceFeil.FACTORY.ikkeStøttetDokumentType().toException();
        } else {
            throw OpprettSakServiceFeil.FACTORY.inkonsistensTemaVsDokument().toException();
        }
    }

    private BehandlingTema hentBehandlingstema(String behandlingstemaOffisiellKode) throws OpprettSakUgyldigInput {
        BehandlingTema behandlingTema = BehandlingTema.finnForKodeverkEiersKode(behandlingstemaOffisiellKode);
        if (BehandlingTema.UDEFINERT.equals(behandlingTema)) {
            UgyldigInput faultInfo = lagUgyldigInput("Behandlingstema", behandlingstemaOffisiellKode);
            throw new OpprettSakUgyldigInput(faultInfo.getFeilmelding(), faultInfo);
        }
        if (FagsakYtelseType.UDEFINERT.equals(behandlingTema.getFagsakYtelseType())) {
            UgyldigInput faultInfo = lagUgyldigInput("Behandlingstema", behandlingstemaOffisiellKode);
            throw new OpprettSakUgyldigInput(faultInfo.getFeilmelding(), faultInfo);
        }
        return behandlingTema;
    }

    private UgyldigInput lagUgyldigInput(String feltnavn, String value) {
        UgyldigInput faultInfo = new UgyldigInput();
        faultInfo.setFeilmelding(feltnavn + " med verdi " + (value != null ? value : "") + " er ugyldig input");
        faultInfo.setFeilaarsak("Ugyldig input");
        return faultInfo;
    }

    private OpprettSakResponse lagResponse(Saksnummer saksnummer) {
        OpprettSakResponse response = new OpprettSakResponse();
        response.setSakId(saksnummer.getVerdi());
        return response;
    }

    @ApplicationScoped
    public static class Unntak extends VLFaultListenerUnntakKonfigurasjon {
        public Unntak() {
            super(OpprettSakUgyldigInput.class);
        }
    }

    public static class AbacDataSupplier implements Function<Object, AbacDataAttributter> {

        @Override
        public AbacDataAttributter apply(Object obj) {
            OpprettSakRequest req = (OpprettSakRequest) obj;
            AbacDataAttributter dataAttributter = AbacDataAttributter.opprett();
            if (req.getSakspart() != null) {
                dataAttributter = dataAttributter.leggTil(AppAbacAttributtType.AKTØR_ID, req.getSakspart().getAktoerId());
            }
            return dataAttributter;
        }
    }
}
