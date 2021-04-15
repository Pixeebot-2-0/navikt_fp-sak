package no.nav.foreldrepenger.behandling.revurdering;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkBegrunnelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.saldo.StønadskontoSaldoTjeneste;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;

@ApplicationScoped
public class BerørtBehandlingTjeneste {

    private StønadskontoSaldoTjeneste stønadskontoSaldoTjeneste;
    private HistorikkRepository historikkRepository;
    private ForeldrepengerUttakTjeneste uttakTjeneste;
    private UttakInputTjeneste uttakInputTjeneste;
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;

    @Inject
    public BerørtBehandlingTjeneste(StønadskontoSaldoTjeneste stønadskontoSaldoTjeneste,
                                    BehandlingRepositoryProvider repositoryProvider,
                                    UttakInputTjeneste uttakInputTjeneste,
                                    ForeldrepengerUttakTjeneste uttakTjeneste,
                                    YtelseFordelingTjeneste ytelseFordelingTjeneste) {
        this.stønadskontoSaldoTjeneste = stønadskontoSaldoTjeneste;
        this.historikkRepository = repositoryProvider.getHistorikkRepository();
        this.uttakTjeneste = uttakTjeneste;
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
    }

    BerørtBehandlingTjeneste() {
        // CDI
    }

    /**
     * Finner ut om det skal opprettes en berørt behandling på med forelders sak.
     *
     * @param brukersGjeldendeBehandlingsresultat brukers behandlingsresultat
     * @param behandlingId                        brukers siste vedtatte behandling
     * @param behandlingIdAnnenPart               medforelders siste vedtatte
     *                                            behandling.
     * @return true dersom berørt behandling skal opprettes, ellers false.
     */
    public boolean skalBerørtBehandlingOpprettes(Behandlingsresultat brukersGjeldendeBehandlingsresultat,
                                                 Long behandlingId,
                                                 Long behandlingIdAnnenPart) {
        //Må sjekke konsekvens pga overlapp med samtidig uttak
        if (brukersGjeldendeBehandlingsresultat.isBehandlingHenlagt() || harKonsekvens(
            brukersGjeldendeBehandlingsresultat, KonsekvensForYtelsen.INGEN_ENDRING)) {
            return false;
        }
        var uttakInput = uttakInputTjeneste.lagInput(brukersGjeldendeBehandlingsresultat.getBehandlingId());
        ForeldrepengerGrunnlag foreldrepengerGrunnlag = uttakInput.getYtelsespesifiktGrunnlag();
        if (foreldrepengerGrunnlag.isTapendeBehandling()) {
            return false;
        }

        if (brukersGjeldendeBehandlingsresultat.isEndretStønadskonto()
            || stønadskontoSaldoTjeneste.erNegativSaldoPåNoenKonto(uttakInput)) {
            return true;
        }

        var annenpartsUttak = hentUttak(behandlingIdAnnenPart);
        if (annenpartsUttak.isEmpty()) {
            return false;
        }

        var brukersUttak = hentUttak(behandlingId);
        if (brukersUttak.isEmpty()) {
            return false;
        }

        var endringsdato = ytelseFordelingTjeneste.hentAggregat(behandlingId).getGjeldendeEndringsdato();
        if (harOverlappendeAktivtUttakEtterEndringsdato(brukersUttak.get(), annenpartsUttak.get(), endringsdato)) {
            return true;
        }

        return hullIFellesUttaksplanEtterEndringsdato(brukersUttak.get(), annenpartsUttak.get(), endringsdato);
    }

    private boolean harOverlappendeAktivtUttakEtterEndringsdato(ForeldrepengerUttak brukersUttak,
                                                                ForeldrepengerUttak annenpartsUttak,
                                                                LocalDate endringsdato) {
        //Går ut i fra at brukers uttak alltid er splittet opp (har knekkpunkt) på endringsdato
        var brukersUttakEtterEndringsdato = uttakEtterDato(brukersUttak, endringsdato);
        return harOverlappIAktivtUttak(brukersUttakEtterEndringsdato, annenpartsUttak.getGjeldendePerioder());
    }

    private List<ForeldrepengerUttakPeriode> uttakEtterDato(ForeldrepengerUttak uttak, LocalDate dato) {
        return uttak.getGjeldendePerioder()
            .stream()
            .filter(periode -> !periode.getTidsperiode().getFomDato().isBefore(dato))
            .collect(Collectors.toList());
    }

    private boolean harOverlappIAktivtUttak(List<ForeldrepengerUttakPeriode> brukersUttak,
                                            List<ForeldrepengerUttakPeriode> annenpartsUttak) {
        return brukersUttak.stream().anyMatch(periode -> overlappendePeriode(periode, annenpartsUttak).isPresent());
    }

    private Optional<ForeldrepengerUttakPeriode> overlappendePeriode(ForeldrepengerUttakPeriode periode,
                                                                     List<ForeldrepengerUttakPeriode> perioder) {
        return perioder.stream()
            .filter(periode2 -> periode.getTidsperiode().overlaps(periode2.getTidsperiode()) && periode.harAktivtUttak()
                && periode2.harAktivtUttak())
            .findFirst();
    }

    private boolean hullIFellesUttaksplanEtterEndringsdato(ForeldrepengerUttak brukersUttak,
                                                           ForeldrepengerUttak annenpartsUttak,
                                                           LocalDate endringsdato) {
        var brukersUttakEtterEndringsdato = uttakEtterDato(brukersUttak, endringsdato);
        return brukersUttakEtterEndringsdato.stream().anyMatch(p -> lagerHullIFellespUttaksplan(p, annenpartsUttak));
    }

    private boolean lagerHullIFellespUttaksplan(ForeldrepengerUttakPeriode periode,
                                                ForeldrepengerUttak annenpartsUttak) {
        if (periode.harAktivtUttak()) {
            return false;
        }
        if (!harUttakEtterDato(periode.getTom(), annenpartsUttak)) {
            return false;
        }
        var annenpartsOverlappendePeriode = overlappendePeriode(periode, annenpartsUttak.getGjeldendePerioder());
        return annenpartsOverlappendePeriode.isEmpty() || !annenpartsOverlappendePeriode.get().harAktivtUttak();
    }

    private boolean harUttakEtterDato(LocalDate dato, ForeldrepengerUttak uttak) {
        return uttak.getGjeldendePerioder().stream().anyMatch(p -> p.getTom().isAfter(dato));
    }

    private Optional<ForeldrepengerUttak> hentUttak(Long behandling) {
        return uttakTjeneste.hentUttakHvisEksisterer(behandling);
    }

    public boolean harKonsekvens(Behandlingsresultat behandlingsresultat, KonsekvensForYtelsen konsekvens) {
        return behandlingsresultat.getKonsekvenserForYtelsen().contains(konsekvens);
    }

    public void opprettHistorikkinnslagOmRevurdering(Behandling behandling,
                                                     HistorikkBegrunnelseType historikkBegrunnelseType) {
        opprettHistorikkinnslag(behandling, historikkBegrunnelseType);
    }

    public void opprettHistorikkinnslagOmRevurdering(Behandling behandling, BehandlingÅrsakType behandlingÅrsakType) {
        opprettHistorikkinnslag(behandling, behandlingÅrsakType);
    }

    private void opprettHistorikkinnslag(Behandling behandling, Kodeverdi begrunnelse) {
        var revurderingsInnslag = new Historikkinnslag();
        revurderingsInnslag.setBehandling(behandling);
        var historikkinnslagType = HistorikkinnslagType.REVURD_OPPR;
        revurderingsInnslag.setType(historikkinnslagType);
        revurderingsInnslag.setAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        var historiebygger = new HistorikkInnslagTekstBuilder().medHendelse(historikkinnslagType);
        historiebygger.medBegrunnelse(begrunnelse);

        historiebygger.build(revurderingsInnslag);

        historikkRepository.lagre(revurderingsInnslag);
    }
}
