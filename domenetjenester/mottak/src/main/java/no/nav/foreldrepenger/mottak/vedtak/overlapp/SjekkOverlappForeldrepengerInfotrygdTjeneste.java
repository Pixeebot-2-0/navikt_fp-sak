package no.nav.foreldrepenger.mottak.vedtak.overlapp;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.domene.tid.VirkedagUtil;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.mottak.vedtak.rest.InfotrygdFPGrunnlag;
import no.nav.vedtak.felles.integrasjon.aktør.klient.AktørConsumerMedCache;
import no.nav.vedtak.felles.integrasjon.infotrygd.grunnlag.v1.respons.Grunnlag;
import no.nav.vedtak.felles.integrasjon.infotrygd.grunnlag.v1.respons.Periode;
import no.nav.vedtak.felles.integrasjon.infotrygd.grunnlag.v1.respons.StatusKode;
import no.nav.vedtak.felles.integrasjon.infotrygd.grunnlag.v1.respons.Vedtak;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
public class SjekkOverlappForeldrepengerInfotrygdTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(SjekkOverlappForeldrepengerInfotrygdTjeneste.class);


    private InfotrygdFPGrunnlag tjeneste ;
    private AktørConsumerMedCache aktørConsumer;

    SjekkOverlappForeldrepengerInfotrygdTjeneste() {
        // CDI
    }

    @Inject
    public SjekkOverlappForeldrepengerInfotrygdTjeneste(InfotrygdFPGrunnlag tjeneste, AktørConsumerMedCache aktørConsumer) {
        this.tjeneste = tjeneste;
        this.aktørConsumer = aktørConsumer;
    }

    public boolean harForeldrepengerInfotrygdSomOverlapper(AktørId aktørId, LocalDate vedtakDato) {
        var ident = getFnrFraAktørId(aktørId);
        List<Grunnlag> rest = tjeneste.hentGrunnlag(ident.getIdent(), vedtakDato.minusWeeks(1), vedtakDato.plusYears(3));

        return rest.stream().anyMatch(g -> overlapper(g, vedtakDato));
    }

    private boolean overlapper(Grunnlag grunnlag, LocalDate vedtakDato) {
        LocalDate maxDato = VirkedagUtil.tomVirkedag(finnMaxDatoUtbetaling(grunnlag).orElse(finnMaxDato(grunnlag)));
        if (!maxDato.isBefore(vedtakDato)) {
            LOG.info("Overlapp INFOTRYGD: fødselsdato barn: {} opphørsdato fra INFOTRYGD: {} Startdato ny sak: {}", grunnlag.getFødselsdatoBarn(), maxDato, vedtakDato);
        } else {
            LOG.info("Uten Overlapp INFOTRYGD: fødselsdato barn: {} opphørsdato fra INFOTRYGD: {} Startdato ny sak: {}", grunnlag.getFødselsdatoBarn(), maxDato, vedtakDato);
        }
        return !maxDato.isBefore(vedtakDato);
    }

    private Optional<LocalDate> finnMaxDatoUtbetaling(Grunnlag grunnlag) {
        return grunnlag.getVedtak().stream().filter(this::harUtbetaling).map(Vedtak::getPeriode).map(Periode::getTom).max(Comparator.naturalOrder()).map(VirkedagUtil::tomVirkedag);
    }

    private LocalDate finnMaxDato(Grunnlag grunnlag) {
        if (grunnlag.getOpphørFom() != null) {
            return localDateMinus1Virkedag(grunnlag.getOpphørFom());
        }
        // Ignoreres og gir ikke overlapp
        if (grunnlag.getStatus() == null || StatusKode.UKJENT.equals(grunnlag.getStatus().getKode())) {
            LOG.info("Sjekk overlapp INFOTRYGD: ukjent status");
            return Tid.TIDENES_BEGYNNELSE;
        }
        // Ikke startet eller Løpende - vet ikke om det kan komme flere vedtak - kan ikke se på utbetalt til nå.
        if (Set.of(StatusKode.I, StatusKode.L).contains(grunnlag.getStatus().getKode())) {
            LOG.info("Overlapp INFOTRYGD: status i IT {} gjør at vi ikke vet om det kommer flere utbetalinger ", grunnlag.getStatus());
            return Tid.TIDENES_ENDE;
        }
        // Status Avsluttet, opphørFom ikke satt
        return grunnlag.getPeriode() == null || grunnlag.getPeriode().getTom() == null ? Tid.TIDENES_BEGYNNELSE : grunnlag.getPeriode().getTom();
    }

    private PersonIdent getFnrFraAktørId(AktørId aktørId) {
        return aktørConsumer.hentPersonIdentForAktørId(aktørId.getId()).map(PersonIdent::new).orElseThrow();
    }

    private boolean harUtbetaling(Vedtak v) {
        return v.getUtbetalingsgrad() > 0;
    }

    private LocalDate localDateMinus1Virkedag(LocalDate opphoerFomDato) {
        return VirkedagUtil.tomVirkedag(opphoerFomDato.minusDays(1));
    }
}
