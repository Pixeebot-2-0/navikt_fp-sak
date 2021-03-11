package no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;

public class OppgittPeriodeUtilTest {

    @Test
    public void sorterEtterFom() {
        var periode1 = OppgittPeriodeBuilder.ny()
            .medPeriode(LocalDate.of(2018, 6, 11), LocalDate.of(2018, 6, 17))
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .build();
        var periode2 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(LocalDate.of(2018, 6, 4), LocalDate.of(2018, 6, 10))
            .build();

        var perioder = OppgittPeriodeUtil.sorterEtterFom(List.of(periode1, periode2));

        assertThat(perioder.get(0)).isEqualTo(periode2);
        assertThat(perioder.get(1)).isEqualTo(periode1);
    }

    @Test
    public void første_søkte_dato_kan_være_overføringsperiode() {

        var førstePeriodeOverføring = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medÅrsak(OverføringÅrsak.SYKDOM_ANNEN_FORELDER)
            .medPeriode(LocalDate.of(2018, 6, 11), LocalDate.of(2018, 6, 17))
            .build();

        var andrePeriodeUttaksperiode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medPeriode(LocalDate.of(2018, 6, 18), LocalDate.of(2018, 6, 28))
            .build();

        var førsteSøkteUttaksdato = OppgittPeriodeUtil.finnFørsteSøkteUttaksdato(List.of(førstePeriodeOverføring, andrePeriodeUttaksperiode));

        assertThat(førsteSøkteUttaksdato.get()).isEqualTo(førstePeriodeOverføring.getFom());
    }

    @Test
    public void første_søkte_dato_kan_være_vanlig_uttaksperiode() {

        var førstePeriodeUttaksperiode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(LocalDate.of(2018, 6, 11), LocalDate.of(2018, 6, 17))
            .build();

        var andrePeriodeOverføring = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medÅrsak(OverføringÅrsak.SYKDOM_ANNEN_FORELDER)
            .medPeriode(LocalDate.of(2018, 6, 18), LocalDate.of(2018, 6, 28))
            .build();

        var førsteSøkteUttaksdato = OppgittPeriodeUtil.finnFørsteSøkteUttaksdato(List.of(førstePeriodeUttaksperiode, andrePeriodeOverføring));

        assertThat(førsteSøkteUttaksdato.get()).isEqualTo(førstePeriodeUttaksperiode.getFom());
    }

    @Test
    public void første_søkte_dato_kan_være_utsettelseperioder() {

        var førstePeriodeUtsettelse = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medÅrsak(UtsettelseÅrsak.FERIE)
            .medPeriode(LocalDate.of(2018, 6, 11), LocalDate.of(2018, 6, 17))
            .build();

        var andrePeriodeUttaksperiode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medPeriode(LocalDate.of(2018, 6, 18), LocalDate.of(2018, 6, 28))
            .build();

        var førsteSøkteUttaksdato = OppgittPeriodeUtil.finnFørsteSøkteUttaksdato(List.of(førstePeriodeUtsettelse, andrePeriodeUttaksperiode));

        assertThat(førsteSøkteUttaksdato.get()).isEqualTo(førstePeriodeUtsettelse.getFom());
    }

    @Test
    public void første_søkte_dato_skal_ikke_være_oppholdsperioder() {

        var førstePeriodeOpphold = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medÅrsak(OppholdÅrsak.MØDREKVOTE_ANNEN_FORELDER)
            .medPeriode(LocalDate.of(2018, 6, 11), LocalDate.of(2018, 6, 17))
            .build();

        var andrePeriodeUttaksperiode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medPeriode(LocalDate.of(2018, 6, 18), LocalDate.of(2018, 6, 28))
            .build();

        var førsteSøkteUttaksdato = OppgittPeriodeUtil.finnFørsteSøkteUttaksdato(List.of(førstePeriodeOpphold, andrePeriodeUttaksperiode));

        assertThat(førsteSøkteUttaksdato.get()).isEqualTo(andrePeriodeUttaksperiode.getFom());
    }
}
