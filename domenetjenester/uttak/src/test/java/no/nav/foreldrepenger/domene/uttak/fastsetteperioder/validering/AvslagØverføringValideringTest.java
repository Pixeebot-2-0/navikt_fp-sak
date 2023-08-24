package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.validering;


import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakAktivitet;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriodeAktivitet;
import no.nav.vedtak.exception.TekniskException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AvslagØverføringValideringTest {

    // Hyppig case fra prod at saksbehandler avslår overføring av fedrekvote, men likevel trekker dager fra fedrekvoten
    // Her skal det helst trekkes mødrekvote/fellesperiode
    @Test
    void skalIkkeKunneTrekkeDagerFraSøktKvote() {
        var validering = new AvslagØverføringValidering();

        var periode = avslåttOverføring(StønadskontoType.FEDREKVOTE, UttakPeriodeType.FEDREKVOTE);
        var perioder = List.of(periode);
        assertThrows(TekniskException.class, () -> validering.utfør(perioder));
    }

    @Test
    void skalKunneTrekkeDagerFraAnnenKvoteEnnSøktKvote() {
        var validering = new AvslagØverføringValidering();

        var periode = avslåttOverføring(StønadskontoType.MØDREKVOTE, UttakPeriodeType.FEDREKVOTE);
        assertDoesNotThrow(() -> validering.utfør(List.of(periode)));
    }

    @Test
    void skalKunneTrekkeDagerFraSøktKvoteHvisInnvilgetOverføring() {
        var validering = new AvslagØverføringValidering();

        var periode = innvilgetOverføring(StønadskontoType.FEDREKVOTE, UttakPeriodeType.FEDREKVOTE);
        assertDoesNotThrow(() -> validering.utfør(List.of(periode)));
    }

    private ForeldrepengerUttakPeriode avslåttOverføring(StønadskontoType kontoDetTrekkesFra,
                                                         UttakPeriodeType søktKonto) {
        return overføring(kontoDetTrekkesFra, søktKonto, PeriodeResultatÅrsak.DEN_ANDRE_PART_INNLEGGELSE_IKKE_OPPFYLT);
    }

    private ForeldrepengerUttakPeriode innvilgetOverføring(StønadskontoType kontoDetTrekkesFra,
                                                           UttakPeriodeType søktKonto) {
        return overføring(kontoDetTrekkesFra, søktKonto, PeriodeResultatÅrsak.OVERFØRING_ANNEN_PART_INNLAGT);
    }

    private ForeldrepengerUttakPeriode overføring(StønadskontoType kontoDetTrekkesFra,
                                                  UttakPeriodeType søktKonto,
                                                  PeriodeResultatÅrsak resultatÅrsak) {
        var aktivitet = aktivitetMedTrekkdager(kontoDetTrekkesFra);
        return new ForeldrepengerUttakPeriode.Builder().medTidsperiode(LocalDate.now().minusWeeks(1),
            LocalDate.now().plusWeeks(3))
            .medOverføringÅrsak(OverføringÅrsak.INSTITUSJONSOPPHOLD_ANNEN_FORELDER)
            .medResultatÅrsak(resultatÅrsak)
            .medResultatType(resultatÅrsak != null && PeriodeResultatÅrsak.UtfallType.INNVILGET.equals(resultatÅrsak.getUtfallType()) ? PeriodeResultatType.INNVILGET : PeriodeResultatType.AVSLÅTT)
            .medAktiviteter(List.of(aktivitet))
            .medSøktKonto(søktKonto)
            .build();
    }

    private ForeldrepengerUttakPeriodeAktivitet aktivitetMedTrekkdager(StønadskontoType stønadskontoType) {
        return new ForeldrepengerUttakPeriodeAktivitet.Builder().medUtbetalingsgrad(Utbetalingsgrad.ZERO)
            .medTrekkdager(new Trekkdager(10))
            .medTrekkonto(stønadskontoType)
            .medAktivitet(new ForeldrepengerUttakAktivitet(UttakArbeidType.FRILANS))
            .medArbeidsprosent(BigDecimal.ZERO)
            .build();
    }

}
