package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.GraderingAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;

@ExtendWith(MockitoExtension.class)
public class OppgittPeriodeTidligstMottattDatoTjenesteTest {

    @Test
    public void skalFinneTidligstMottattDatoFraOriginalBehandlingHvisMatchendePeriode() {
        var fom = LocalDate.of(2020, 10, 9);
        var tom = LocalDate.of(2020, 11, 9);
        var originalTidligstMottattDato = LocalDate.of(2020, 10, 10);
        var originalBehandlingPerioder = List.of(OppgittPeriodeBuilder.ny()
                .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
                .medPeriode(fom, tom)
                .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
                .medTidligstMottattDato(originalTidligstMottattDato)
                .build());

        var periode = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(fom, tom)
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medMottattDato(LocalDate.now())
            .medTidligstMottattDato(LocalDate.now())
            .build();

        var tlsøknad = OppgittPeriodeTidligstMottattDatoTjeneste.lagSøknadsTimeline(List.of(periode));
        var tlsammenlign = OppgittPeriodeTidligstMottattDatoTjeneste.lagSammenligningTimeline(List.of(periode));
        var tloppdatert = OppgittPeriodeTidligstMottattDatoTjeneste.oppdaterTidligstMottattDato(tlsøknad, tlsammenlign, originalBehandlingPerioder);
        var oppdatert = OppgittPeriodeTidligstMottattDatoTjeneste.tilPerioder(tloppdatert);
        assertThat(oppdatert).hasSize(1);
        var oppdatertperiode = oppdatert.get(0);
        assertThat(oppdatertperiode.getTidligstMottattDato().orElseThrow()).isEqualTo(originalTidligstMottattDato);
        assertThat(oppdatertperiode.getFom()).isEqualTo(periode.getFom());
        assertThat(oppdatertperiode.getTom()).isEqualTo(periode.getTom());
    }

    @Test
    public void skalSplittePeriodeFinneTidligstMottattDatoFraOriginalBehandlingHvisUtvidetPeriode() {
        var fom = LocalDate.of(2020, 10, 9);
        var tom = LocalDate.of(2020, 11, 9);
        var originalTidligstMottattDato = LocalDate.of(2020, 10, 10);
        var originalBehandlingPerioder = List.of(OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(fom, tom)
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medTidligstMottattDato(originalTidligstMottattDato)
            .build());

        var periode = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(fom, tom.plusWeeks(1))
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medMottattDato(LocalDate.now())
            .medTidligstMottattDato(LocalDate.now())
            .build();

        var tlsøknad = OppgittPeriodeTidligstMottattDatoTjeneste.lagSøknadsTimeline(List.of(periode));
        var tlsammenlign = OppgittPeriodeTidligstMottattDatoTjeneste.lagSammenligningTimeline(List.of(periode));
        var tloppdatert = OppgittPeriodeTidligstMottattDatoTjeneste.oppdaterTidligstMottattDato(tlsøknad, tlsammenlign, originalBehandlingPerioder);
        var oppdatert = OppgittPeriodeTidligstMottattDatoTjeneste.tilPerioder(tloppdatert);
        assertThat(oppdatert).hasSize(2);
        assertThat(oppdatert.get(0).getTidligstMottattDato().orElseThrow()).isEqualTo(originalTidligstMottattDato);
        assertThat(oppdatert.get(1).getTidligstMottattDato().orElseThrow()).isEqualTo(LocalDate.now());
        assertThat(oppdatert.get(0).getFom()).isEqualTo(periode.getFom());
        assertThat(oppdatert.get(0).getTom()).isEqualTo(tom);
        assertThat(oppdatert.get(1).getFom()).isEqualTo(tom.plusDays(1));
        assertThat(oppdatert.get(1).getTom()).isEqualTo(periode.getTom());

    }

    @Test
    public void skalFinneTidligstMottattDatoFraOriginalBehandlingHvisKrympetPeriode() {
        var fom = LocalDate.of(2020, 10, 9);
        var tom = LocalDate.of(2020, 11, 9);
        var originalTidligstMottattDato = LocalDate.of(2020, 10, 10);
        var originalBehandlingPerioder = List.of(OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(fom, tom)
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medTidligstMottattDato(originalTidligstMottattDato)
            .build());

        var periode = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(fom, tom.minusWeeks(1))
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medMottattDato(LocalDate.now())
            .medTidligstMottattDato(LocalDate.now())
            .build();

        var tlsøknad = OppgittPeriodeTidligstMottattDatoTjeneste.lagSøknadsTimeline(List.of(periode));
        var tlsammenlign = OppgittPeriodeTidligstMottattDatoTjeneste.lagSammenligningTimeline(List.of(periode));
        var tloppdatert = OppgittPeriodeTidligstMottattDatoTjeneste.oppdaterTidligstMottattDato(tlsøknad, tlsammenlign, originalBehandlingPerioder);
        var oppdatert = OppgittPeriodeTidligstMottattDatoTjeneste.tilPerioder(tloppdatert);
        assertThat(oppdatert).hasSize(1);
        var oppdatertperiode = oppdatert.get(0);
        assertThat(oppdatertperiode.getTidligstMottattDato().orElseThrow()).isEqualTo(originalTidligstMottattDato);
        assertThat(oppdatertperiode.getFom()).isEqualTo(periode.getFom());
        assertThat(oppdatertperiode.getTom()).isEqualTo(periode.getTom());
    }

    @Test
    public void skalSplittePeriodeFinneTidligstMottattDatoFraOriginalBehandlingHvisHullIPeriode() {
        var fom = LocalDate.of(2020, 10, 9);
        var tom = LocalDate.of(2020, 11, 9);
        var originalTidligstMottattDato = LocalDate.of(2020, 10, 10);
        var originalBehandlingPerioder = List.of(OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(fom, tom)
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medTidligstMottattDato(originalTidligstMottattDato)
            .build());

        var periode1 = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(fom, tom.minusWeeks(2))
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medMottattDato(LocalDate.now())
            .medTidligstMottattDato(LocalDate.now())
            .build();
        var periode2 = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(tom.minusWeeks(1).plusDays(1), tom.plusWeeks(1))
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medMottattDato(LocalDate.now())
            .medTidligstMottattDato(LocalDate.now())
            .build();

        var tlsøknad = OppgittPeriodeTidligstMottattDatoTjeneste.lagSøknadsTimeline(List.of(periode1, periode2));
        var tlsammenlign = OppgittPeriodeTidligstMottattDatoTjeneste.lagSammenligningTimeline(List.of(periode1, periode2));
        var tloppdatert = OppgittPeriodeTidligstMottattDatoTjeneste.oppdaterTidligstMottattDato(tlsøknad, tlsammenlign, originalBehandlingPerioder);
        var oppdatert = OppgittPeriodeTidligstMottattDatoTjeneste.tilPerioder(tloppdatert);
        assertThat(oppdatert).hasSize(3);
        assertThat(oppdatert.get(0).getTidligstMottattDato().orElseThrow()).isEqualTo(originalTidligstMottattDato);
        assertThat(oppdatert.get(1).getTidligstMottattDato().orElseThrow()).isEqualTo(originalTidligstMottattDato);
        assertThat(oppdatert.get(2).getTidligstMottattDato().orElseThrow()).isEqualTo(LocalDate.now());
        assertThat(oppdatert.get(0).getFom()).isEqualTo(periode1.getFom());
        assertThat(oppdatert.get(0).getTom()).isEqualTo(periode1.getTom());
        assertThat(oppdatert.get(1).getFom()).isEqualTo(tom.minusWeeks(1).plusDays(1));
        assertThat(oppdatert.get(1).getTom()).isEqualTo(tom);
        assertThat(oppdatert.get(2).getFom()).isEqualTo(tom.plusDays(1));
        assertThat(oppdatert.get(2).getTom()).isEqualTo(periode2.getTom());
    }

    @Test
    public void skalSplitteSamtBrukeDatoFraInnvilgetGradertPeriodeMenIkkeForNyPeriode() {
        var fom = LocalDate.of(2022, 10, 10);
        var tom = LocalDate.of(2022, 11, 9);
        var originalTidligstMottattDato = LocalDate.of(2020, 10, 10);

        var arbeidsgiver = Arbeidsgiver.virksomhet(OrgNummer.KUNSTIG_ORG);
        var originalBehandlingPerioder = List.of(OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(fom, tom)
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medGraderingAktivitetType(GraderingAktivitetType.ARBEID)
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidsprosent(BigDecimal.TEN)
            .medTidligstMottattDato(originalTidligstMottattDato)
            .build());

        // Utvide eksisterende uttak i begge ender
        var søknad = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(fom.minusWeeks(1), tom.plusWeeks(1))
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medGraderingAktivitetType(GraderingAktivitetType.ARBEID)
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidsprosent(BigDecimal.TEN)
            .medMottattDato(LocalDate.now())
            .medTidligstMottattDato(LocalDate.now())
            .build();

        var tlsøknad = OppgittPeriodeTidligstMottattDatoTjeneste.lagSøknadsTimeline(List.of(søknad));
        var tlsammenlign = OppgittPeriodeTidligstMottattDatoTjeneste.lagSammenligningTimeline(List.of(søknad));
        var tloppdatert = OppgittPeriodeTidligstMottattDatoTjeneste.oppdaterTidligstMottattDato(tlsøknad, tlsammenlign, originalBehandlingPerioder);
        var oppdatert = OppgittPeriodeTidligstMottattDatoTjeneste.tilPerioder(tloppdatert).stream()
            .sorted(Comparator.comparing(OppgittPeriodeEntitet::getFom)).toList();
        assertThat(oppdatert).hasSize(3);
        assertThat(oppdatert.get(0).getTidligstMottattDato().orElseThrow()).isEqualTo(LocalDate.now());
        assertThat(oppdatert.get(1).getTidligstMottattDato().orElseThrow()).isEqualTo(originalTidligstMottattDato);
        assertThat(oppdatert.get(2).getTidligstMottattDato().orElseThrow()).isEqualTo(LocalDate.now());
        assertThat(oppdatert.get(0).getFom()).isEqualTo(søknad.getFom());
        assertThat(oppdatert.get(0).getTom()).isEqualTo(fom.minusDays(1));
        assertThat(oppdatert.get(1).getFom()).isEqualTo(fom);
        assertThat(oppdatert.get(1).getTom()).isEqualTo(tom);
        assertThat(oppdatert.get(2).getFom()).isEqualTo(tom.plusDays(1));
        assertThat(oppdatert.get(2).getTom()).isEqualTo(tom.plusWeeks(1));
    }


    @Test
    public void skalBrukeDatoFraInnvilgetGradertPeriodeMenIkkeForNyPeriode() {
        var fom = LocalDate.of(2022, 10, 10);
        var tom = LocalDate.of(2022, 11, 9);
        var originalTidligstMottattDato = LocalDate.of(2020, 10, 10);

        var arbeidsgiver = Arbeidsgiver.virksomhet(OrgNummer.KUNSTIG_ORG);
        var originalBehandlingPerioder = List.of(OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(fom, tom)
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medGraderingAktivitetType(GraderingAktivitetType.ARBEID)
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidsprosent(BigDecimal.TEN)
            .medTidligstMottattDato(originalTidligstMottattDato)
            .build());

        // Lik eksisterende vedtak
        var søknad1 = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(fom, tom)
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medGraderingAktivitetType(GraderingAktivitetType.ARBEID)
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidsprosent(BigDecimal.TEN)
            .medMottattDato(LocalDate.now())
            .medTidligstMottattDato(LocalDate.now())
            .build();
        // Ny periode
        var søknad2 = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(tom.plusDays(1), tom.plusWeeks(4))
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medMottattDato(LocalDate.now())
            .medTidligstMottattDato(LocalDate.now())
            .build();

        var tlsøknad = OppgittPeriodeTidligstMottattDatoTjeneste.lagSøknadsTimeline(List.of(søknad1, søknad2));
        var tlsammenlign = OppgittPeriodeTidligstMottattDatoTjeneste.lagSammenligningTimeline(List.of(søknad1, søknad2));
        var tloppdatert = OppgittPeriodeTidligstMottattDatoTjeneste.oppdaterTidligstMottattDato(tlsøknad, tlsammenlign, originalBehandlingPerioder);
        var oppdatert = OppgittPeriodeTidligstMottattDatoTjeneste.tilPerioder(tloppdatert)
            .stream().sorted(Comparator.comparing(OppgittPeriodeEntitet::getFom)).toList();
        assertThat(oppdatert).hasSize(2);
        assertThat(oppdatert.get(0).getTidligstMottattDato().orElseThrow()).isEqualTo(originalTidligstMottattDato);
        assertThat(oppdatert.get(1).getTidligstMottattDato().orElseThrow()).isEqualTo(LocalDate.now());
        assertThat(oppdatert.get(0).getFom()).isEqualTo(søknad1.getFom());
        assertThat(oppdatert.get(0).getTom()).isEqualTo(søknad1.getTom());
        assertThat(oppdatert.get(1).getFom()).isEqualTo(søknad2.getFom());
        assertThat(oppdatert.get(1).getTom()).isEqualTo(søknad2.getTom());
    }

    @Test
    public void skalBrukeDatoFraInnvilgetPeriodeMenIkkeForNyPeriode() {
        var fom = LocalDate.of(2022, 10, 10);
        var tom = LocalDate.of(2022, 11, 9);
        var originalTidligstMottattDato = LocalDate.of(2020, 10, 10);

        var originalBehandlingPerioder = List.of(OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(fom, tom)
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medTidligstMottattDato(originalTidligstMottattDato)
            .build());

        var søknad1 = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(fom, tom.minusWeeks(1))
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medMottattDato(LocalDate.now())
            .medTidligstMottattDato(LocalDate.now())
            .build();
        var søknad2 = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(tom.plusWeeks(1), tom.plusWeeks(2))
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medMottattDato(LocalDate.now())
            .medTidligstMottattDato(LocalDate.now())
            .build();

        var tlsøknad = OppgittPeriodeTidligstMottattDatoTjeneste.lagSøknadsTimeline(List.of(søknad1, søknad2));
        var tlsammenlign = OppgittPeriodeTidligstMottattDatoTjeneste.lagSammenligningTimeline(List.of(søknad1, søknad2));
        var tloppdatert = OppgittPeriodeTidligstMottattDatoTjeneste.oppdaterTidligstMottattDato(tlsøknad, tlsammenlign, originalBehandlingPerioder);
        var oppdatert = OppgittPeriodeTidligstMottattDatoTjeneste.tilPerioder(tloppdatert)
            .stream().sorted(Comparator.comparing(OppgittPeriodeEntitet::getFom)).toList();
        assertThat(oppdatert).hasSize(2);
        assertThat(oppdatert.get(0).getTidligstMottattDato().orElseThrow()).isEqualTo(originalTidligstMottattDato);
        assertThat(oppdatert.get(1).getTidligstMottattDato().orElseThrow()).isEqualTo(LocalDate.now());
        assertThat(oppdatert.get(0).getFom()).isEqualTo(søknad1.getFom());
        assertThat(oppdatert.get(0).getTom()).isEqualTo(søknad1.getTom());
        assertThat(oppdatert.get(1).getFom()).isEqualTo(søknad2.getFom());
        assertThat(oppdatert.get(1).getTom()).isEqualTo(søknad2.getTom());
    }


    @Test
    public void leggerInnUtsettelseOgSenereUttak() {
        var fom = LocalDate.of(2022, 10, 10);
        var tom = LocalDate.of(2022, 11, 9);
        var utsattFom = LocalDate.of(2022, 11, 1);
        var senereUttakFom = LocalDate.of(2022, 12, 1);
        var originalTidligstMottattDato = LocalDate.of(2020, 10, 10);

        var originalBehandlingPerioder = List.of(OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(fom, tom)
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medTidligstMottattDato(originalTidligstMottattDato)
            .build());

        // Lik eksisterende vedtak
        var søknad0 = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(fom, utsattFom.minusDays(1))
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medMottattDato(LocalDate.now())
            .medTidligstMottattDato(LocalDate.now())
            .build();
        var søknad1 = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medÅrsak(UtsettelseÅrsak.FRI)
            .medPeriode(utsattFom, senereUttakFom.minusDays(1))
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medMottattDato(LocalDate.now())
            .medTidligstMottattDato(LocalDate.now())
            .build();
        // Ny periode
        var søknad2 = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(senereUttakFom, senereUttakFom.plusDays(23))
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medMottattDato(LocalDate.now())
            .medTidligstMottattDato(LocalDate.now())
            .build();

        var tlsøknad = OppgittPeriodeTidligstMottattDatoTjeneste.lagSøknadsTimeline(List.of(søknad1, søknad2, søknad0));
        var tlsammenlign = OppgittPeriodeTidligstMottattDatoTjeneste.lagSammenligningTimeline(List.of(søknad1, søknad2, søknad0));
        var tloppdatert = OppgittPeriodeTidligstMottattDatoTjeneste.oppdaterTidligstMottattDato(tlsøknad, tlsammenlign, originalBehandlingPerioder);
        var oppdatert = OppgittPeriodeTidligstMottattDatoTjeneste.tilPerioder(tloppdatert)
            .stream().sorted(Comparator.comparing(OppgittPeriodeEntitet::getFom)).toList();
        assertThat(oppdatert).hasSize(3);
        assertThat(oppdatert.get(0).getTidligstMottattDato().orElseThrow()).isEqualTo(originalTidligstMottattDato);
        assertThat(oppdatert.get(1).getTidligstMottattDato().orElseThrow()).isEqualTo(LocalDate.now());
        assertThat(oppdatert.get(2).getTidligstMottattDato().orElseThrow()).isEqualTo(LocalDate.now());
        assertThat(oppdatert.get(0).getFom()).isEqualTo(søknad0.getFom());
        assertThat(oppdatert.get(0).getTom()).isEqualTo(søknad0.getTom());
        assertThat(oppdatert.get(1).getFom()).isEqualTo(søknad1.getFom());
        assertThat(oppdatert.get(1).getTom()).isEqualTo(søknad1.getTom());
        assertThat(oppdatert.get(2).getFom()).isEqualTo(søknad2.getFom());
        assertThat(oppdatert.get(2).getTom()).isEqualTo(søknad2.getTom());
    }

    @Test
    public void utviderPeriodeUtenUttakOgLeggerTilSenereUttak() {
        var fom0 = LocalDate.of(2022, 10, 4);
        var fom = LocalDate.of(2022, 10, 10);
        var tom = LocalDate.of(2022, 11, 9);
        var originalTidligstMottattDato0 = LocalDate.of(2020, 10, 10);
        var originalTidligstMottattDato1 = LocalDate.of(2021, 10, 10);

        var originalBehandlingPerioder = List.of(OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(fom, tom)
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medTidligstMottattDato(originalTidligstMottattDato1)
            .build(), OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(fom0, fom0.plusDays(1))
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medTidligstMottattDato(originalTidligstMottattDato0)
            .build());

        // Utvider oppholdet mellom uttaket med et par dager
        var søknad0 = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(fom0, fom0.plusDays(1))
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medMottattDato(LocalDate.now())
            .medTidligstMottattDato(LocalDate.now())
            .build();
        var søknad1 = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(fom.plusDays(3), tom.plusWeeks(2))
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medMottattDato(LocalDate.now())
            .medTidligstMottattDato(LocalDate.now())
            .build();

        var tlsøknad = OppgittPeriodeTidligstMottattDatoTjeneste.lagSøknadsTimeline(List.of(søknad1, søknad0));
        var tlsammenlign = OppgittPeriodeTidligstMottattDatoTjeneste.lagSammenligningTimeline(List.of(søknad1, søknad0));
        var tloppdatert = OppgittPeriodeTidligstMottattDatoTjeneste.oppdaterTidligstMottattDato(tlsøknad, tlsammenlign, originalBehandlingPerioder);
        var oppdatert = OppgittPeriodeTidligstMottattDatoTjeneste.tilPerioder(tloppdatert)
            .stream().sorted(Comparator.comparing(OppgittPeriodeEntitet::getFom)).toList();
        assertThat(oppdatert).hasSize(3);
        assertThat(oppdatert.get(0).getTidligstMottattDato().orElseThrow()).isEqualTo(originalTidligstMottattDato0);
        assertThat(oppdatert.get(1).getTidligstMottattDato().orElseThrow()).isEqualTo(originalTidligstMottattDato1);
        assertThat(oppdatert.get(2).getTidligstMottattDato().orElseThrow()).isEqualTo(LocalDate.now());
        assertThat(oppdatert.get(0).getFom()).isEqualTo(søknad0.getFom());
        assertThat(oppdatert.get(0).getTom()).isEqualTo(søknad0.getTom());
        assertThat(oppdatert.get(1).getFom()).isEqualTo(søknad1.getFom());
        assertThat(oppdatert.get(1).getTom()).isEqualTo(tom);
        assertThat(oppdatert.get(2).getFom()).isEqualTo(tom.plusDays(1));
        assertThat(oppdatert.get(2).getTom()).isEqualTo(søknad1.getTom());
    }

    @Test
    public void prod_overstyrt_uttak_utsettelse_med_konto() {
        var fom = LocalDate.of(2022, 1, 3);
        var fom0 = LocalDate.of(2022, 1, 10);
        var tom = LocalDate.of(2022, 6, 10);
        var fom2 = LocalDate.of(2022, 6, 13);
        var tom2 = LocalDate.of(2022, 6, 17);
        var originalTidligstMottattDato = LocalDate.of(2021, 10, 10);

        var originalBehandlingPerioder = List.of(OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(fom, fom0.minusDays(1))
            .medÅrsak(UtsettelseÅrsak.ARBEID)
            .medMorsAktivitet(MorsAktivitet.UTDANNING)
            .medTidligstMottattDato(originalTidligstMottattDato)
            .build(), OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(fom0, tom)
            .medÅrsak(UtsettelseÅrsak.ARBEID)
            .medMorsAktivitet(MorsAktivitet.UTDANNING)
            .medTidligstMottattDato(originalTidligstMottattDato)
            .build(), OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(fom2, tom2)
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medMorsAktivitet(MorsAktivitet.UTDANNING)
            .medTidligstMottattDato(originalTidligstMottattDato)
            .build());

        var søknad0 = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(fom, tom)
            .medPeriodeType(UttakPeriodeType.UDEFINERT)
            .medÅrsak(UtsettelseÅrsak.ARBEID)
            .medMorsAktivitet(MorsAktivitet.UTDANNING)
            .medMottattDato(LocalDate.now())
            .medTidligstMottattDato(LocalDate.now())
            .build();
        var søknad1 = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(fom2, tom2)
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medMorsAktivitet(MorsAktivitet.UTDANNING)
            .medMottattDato(LocalDate.now())
            .medTidligstMottattDato(LocalDate.now())
            .build();
        var søknad2 = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(LocalDate.of(2022, 6, 20), LocalDate.of(2022, 8, 19))
            .medPeriodeType(UttakPeriodeType.UDEFINERT)
            .medÅrsak(UtsettelseÅrsak.ARBEID)
            .medMorsAktivitet(MorsAktivitet.UTDANNING)
            .medMottattDato(LocalDate.now())
            .medTidligstMottattDato(LocalDate.now())
            .build();

        var tlsøknad = OppgittPeriodeTidligstMottattDatoTjeneste.lagSøknadsTimeline(List.of(søknad0, søknad1, søknad2));
        var tlsammenlign = OppgittPeriodeTidligstMottattDatoTjeneste.lagSammenligningTimeline(List.of(søknad0, søknad1, søknad2));
        var tloppdatert = OppgittPeriodeTidligstMottattDatoTjeneste.oppdaterTidligstMottattDato(tlsøknad, tlsammenlign, originalBehandlingPerioder);
        var oppdatert = OppgittPeriodeTidligstMottattDatoTjeneste.tilPerioder(tloppdatert)
            .stream().sorted(Comparator.comparing(OppgittPeriodeEntitet::getFom)).toList();
        assertThat(oppdatert).hasSize(3);
        assertThat(oppdatert.get(0).getTidligstMottattDato().orElseThrow()).isEqualTo(originalTidligstMottattDato);
        assertThat(oppdatert.get(1).getTidligstMottattDato().orElseThrow()).isEqualTo(originalTidligstMottattDato);
        assertThat(oppdatert.get(2).getTidligstMottattDato().orElseThrow()).isEqualTo(LocalDate.now());
        assertThat(oppdatert.get(0).getFom()).isEqualTo(søknad0.getFom());
        assertThat(oppdatert.get(0).getTom()).isEqualTo(søknad0.getTom());
        assertThat(oppdatert.get(1).getFom()).isEqualTo(søknad1.getFom());
        assertThat(oppdatert.get(1).getTom()).isEqualTo(søknad1.getTom());
        assertThat(oppdatert.get(2).getFom()).isEqualTo(søknad2.getFom());
        assertThat(oppdatert.get(2).getTom()).isEqualTo(søknad2.getTom());
    }

}
