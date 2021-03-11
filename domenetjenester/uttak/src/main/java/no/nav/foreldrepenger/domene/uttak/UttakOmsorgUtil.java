package no.nav.foreldrepenger.domene.uttak;

import static java.lang.Boolean.TRUE;

import java.util.Objects;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;

public final class UttakOmsorgUtil {

    private UttakOmsorgUtil() {
    }

    public static boolean harAleneomsorg(YtelseFordelingAggregat ytelseFordelingAggregat) {
        var perioderAleneOmsorg = ytelseFordelingAggregat.getPerioderAleneOmsorg();
        if (perioderAleneOmsorg.isPresent()) {
            return !perioderAleneOmsorg.get().getPerioder().isEmpty();
        }
        return TRUE.equals(ytelseFordelingAggregat.getOppgittRettighet().getHarAleneomsorgForBarnet());
    }

    public static boolean harAnnenForelderRett(YtelseFordelingAggregat ytelseFordelingAggregat,
                                               Optional<ForeldrepengerUttak> annenpartsGjeldendeUttaksplan) {
        if (annenForelderHarUttakMedUtbetaling(annenpartsGjeldendeUttaksplan)) {
            return true;
        }
        var annenForelderRettAvklaring = ytelseFordelingAggregat.getAnnenForelderRettAvklaring();
        if (annenForelderRettAvklaring.isPresent()) {
            return annenForelderRettAvklaring.get();
        }
        var oppgittRettighet = ytelseFordelingAggregat.getOppgittRettighet();
        Objects.requireNonNull(oppgittRettighet, "oppgittRettighet");
        return oppgittRettighet.getHarAnnenForeldreRett() == null || oppgittRettighet.getHarAnnenForeldreRett();
    }

    public static boolean annenForelderHarUttakMedUtbetaling(Optional<ForeldrepengerUttak> annenpartsGjeldendeUttaksplan) {
        return annenpartsGjeldendeUttaksplan.isPresent() && harUtbetaling(annenpartsGjeldendeUttaksplan.get());
    }

    private static boolean harUtbetaling(ForeldrepengerUttak resultat) {
        return resultat.getGjeldendePerioder().stream().anyMatch(p -> p.harUtbetaling());
    }
}
