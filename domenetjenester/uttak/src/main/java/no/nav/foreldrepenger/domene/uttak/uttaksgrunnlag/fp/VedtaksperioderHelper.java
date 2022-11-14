package no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.GraderingAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.Årsak;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.SamtidigUttaksprosent;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeSøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakUtsettelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.uttak.UttakEnumMapper;

public class VedtaksperioderHelper {

    private VedtaksperioderHelper() {
        //
    }

    public static List<OppgittPeriodeEntitet> opprettOppgittePerioder(UttakResultatEntitet uttakResultatFraForrigeBehandling,
                                                                      List<OppgittPeriodeEntitet> søknadsperioder,
                                                                      LocalDate endringsdato,
                                                                      boolean kreverSammenhengendeUttak) {
        var førsteSøknadsdato = OppgittPeriodeUtil.finnFørsteSøknadsdato(søknadsperioder);
        var vedtaksperioder = lagVedtaksperioder(uttakResultatFraForrigeBehandling, endringsdato, førsteSøknadsdato, kreverSammenhengendeUttak);

        List<OppgittPeriodeEntitet> søknadOgVedtaksperioder = new ArrayList<>();
        søknadsperioder.forEach(op -> søknadOgVedtaksperioder.add(OppgittPeriodeBuilder.fraEksisterende(op).build()));
        søknadOgVedtaksperioder.addAll(vedtaksperioder);
        return OppgittPeriodeUtil.sorterEtterFom(søknadOgVedtaksperioder);
    }

    private static List<OppgittPeriodeEntitet> lagVedtaksperioder(UttakResultatEntitet uttakResultat, LocalDate endringsdato,
                                                                  Optional<LocalDate> førsteSøknadsdato, boolean kreverSammenhengendeUttak) {
        return uttakResultat.getGjeldendePerioder()
            .getPerioder()
            .stream()
            .filter(p -> !p.getTom().isBefore(endringsdato))
            .filter(p -> kreverSammenhengendeUttak || !avslåttIngenTrekkdager(p))
            .filter(p -> !avslåttPgaAvTaptPeriodeTilAnnenpart(p))
            .filter(p -> filtrerFørsteSøknadsdato(p, førsteSøknadsdato))
            .filter(VedtaksperioderHelper::erPeriodeFraSøknad)
            .map(VedtaksperioderHelper::konverter)
            .flatMap(p -> klipp(p, endringsdato, førsteSøknadsdato))
            .toList();
    }


    private static boolean filtrerFørsteSøknadsdato(UttakResultatPeriodeEntitet periode, Optional<LocalDate> førsteSøknadsdatoOptional) {
        if (førsteSøknadsdatoOptional.isPresent()) {
            var førsteSøknadsdato = førsteSøknadsdatoOptional.get();
            //Perioder som starter på eller etter første søknadsdato skal filtreres bort
            return !periode.getFom().equals(førsteSøknadsdato) && !periode.getFom().isAfter(førsteSøknadsdato);
        }
        return true;
    }

    public static boolean avslåttPgaAvTaptPeriodeTilAnnenpart(UttakResultatPeriodeEntitet periode) {
        return PeriodeResultatÅrsak.årsakerTilAvslagPgaAnnenpart().contains(periode.getResultatÅrsak())
            && PeriodeResultatType.AVSLÅTT.equals(periode.getResultatType()) && periode.getAktiviteter()
            .stream()
            .allMatch(aktivitet -> aktivitet.getTrekkdager().equals(Trekkdager.ZERO));
    }

    public static boolean avslåttIngenTrekkdager(UttakResultatPeriodeEntitet periode) {
        return PeriodeResultatType.AVSLÅTT.equals(periode.getResultatType()) &&
            periode.getAktiviteter().stream().allMatch(aktivitet -> aktivitet.getTrekkdager().equals(Trekkdager.ZERO));
    }

    private static boolean erPeriodeFraSøknad(UttakResultatPeriodeEntitet periode) {
        return periode.getPeriodeSøknad().isPresent();
    }

    private static Stream<OppgittPeriodeEntitet> klipp(OppgittPeriodeEntitet op,
                                                LocalDate endringsdato,
                                                Optional<LocalDate> førsteSøknadsdato) {
        Objects.requireNonNull(endringsdato);

        var opb = OppgittPeriodeBuilder.fraEksisterende(op);
        var fom = op.getFom();
        var tom = op.getTom();
        if (endringsdato.isAfter(fom)) {
            fom = endringsdato;
        }
        if (førsteSøknadsdato.isPresent() && (førsteSøknadsdato.get().isBefore(tom) || førsteSøknadsdato.get()
            .isEqual(tom))) {
            tom = førsteSøknadsdato.get().minusDays(1);
        }
        if (!fom.isAfter(tom)) {
            return Stream.of(opb.medPeriode(fom, tom).build());
        }
        return Stream.empty();
    }

    static OppgittPeriodeEntitet konverter(UttakResultatPeriodeEntitet up) {
        var samtidigUttaksprosent = finnSamtidigUttaksprosent(up).orElse(null);
        var builder = OppgittPeriodeBuilder.ny()
            .medPeriode(up.getTidsperiode().getFomDato(), up.getTidsperiode().getTomDato())
            .medPeriodeType(finnPeriodetype(up))
            .medSamtidigUttak(SamtidigUttaksprosent.erSamtidigUttak(samtidigUttaksprosent))
            .medSamtidigUttaksprosent(samtidigUttaksprosent)
            .medFlerbarnsdager(up.isFlerbarnsdager())
            .medGraderingAktivitetType(finnGradertAktivitetType(up))
            .medPeriodeKilde(FordelingPeriodeKilde.TIDLIGERE_VEDTAK);

        finnMorsAktivitet(up).ifPresent(builder::medMorsAktivitet);
        finnDokumentasjonVurdering(up).ifPresent(builder::medDokumentasjonVurdering);
        finnGraderingArbeidsprosent(up).ifPresent(builder::medArbeidsprosent);
        finnUtsettelsesÅrsak(up).ifPresent(builder::medÅrsak);
        finnGradertArbeidsgiver(up).ifPresent(builder::medArbeidsgiver);
        finnOppholdsÅrsak(up).ifPresent(builder::medÅrsak);
        finnOverføringÅrsak(up).ifPresent(builder::medÅrsak);
        builder.medMottattDato(up.getPeriodeSøknad().orElseThrow().getMottattDato());
        builder.medTidligstMottattDato(up.getPeriodeSøknad().orElseThrow().getTidligstMottattDato().orElse(null));

        return builder.build();
    }

    static Optional<Årsak> finnOverføringÅrsak(UttakResultatPeriodeEntitet up) {
        if (up.isOverføring()) {
            return Optional.of(up.getOverføringÅrsak());
        }
        return Optional.empty();
    }

    static Optional<SamtidigUttaksprosent> finnSamtidigUttaksprosent(UttakResultatPeriodeEntitet up) {
        return Optional.ofNullable(up.getSamtidigUttaksprosent())
            .or(() -> up.getPeriodeSøknad().map(UttakResultatPeriodeSøknadEntitet::getSamtidigUttaksprosent));
    }

    static Optional<MorsAktivitet> finnMorsAktivitet(UttakResultatPeriodeEntitet up) {
        return up.getPeriodeSøknad().map(UttakResultatPeriodeSøknadEntitet::getMorsAktivitet);
    }

    static Optional<DokumentasjonVurdering> finnDokumentasjonVurdering(UttakResultatPeriodeEntitet up) {
        return up.getPeriodeSøknad().map(UttakResultatPeriodeSøknadEntitet::getDokumentasjonVurdering);
    }

    static Optional<Arbeidsgiver> finnGradertArbeidsgiver(UttakResultatPeriodeEntitet up) {
        return up.getAktiviteter().stream()
            .filter(UttakResultatPeriodeAktivitetEntitet::isSøktGradering)
            .findFirst()
            .map(UttakResultatPeriodeAktivitetEntitet::getUttakAktivitet)
            .flatMap(UttakAktivitetEntitet::getArbeidsgiver);
    }

    static GraderingAktivitetType finnGradertAktivitetType(UttakResultatPeriodeEntitet up) {
        return GraderingAktivitetType.from(erArbeidstaker(up), erFrilans(up), erSelvstendig(up));
    }

    static boolean erArbeidstaker(UttakResultatPeriodeEntitet up) {
        return up.getAktiviteter()
            .stream()
            .filter(UttakResultatPeriodeAktivitetEntitet::isSøktGradering)
            .anyMatch(a -> UttakArbeidType.ORDINÆRT_ARBEID.equals(a.getUttakArbeidType()));
    }

    static boolean erFrilans(UttakResultatPeriodeEntitet up) {
        return up.getAktiviteter()
            .stream()
            .filter(UttakResultatPeriodeAktivitetEntitet::isSøktGradering)
            .anyMatch(a -> UttakArbeidType.FRILANS.equals(a.getUttakArbeidType()));
    }

    static boolean erSelvstendig(UttakResultatPeriodeEntitet up) {
        return up.getAktiviteter()
            .stream()
            .filter(UttakResultatPeriodeAktivitetEntitet::isSøktGradering)
            .anyMatch(a -> UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE.equals(a.getUttakArbeidType()));
    }

    static UttakPeriodeType finnPeriodetype(UttakResultatPeriodeEntitet uttakResultatPeriode) {
        //Oppholdsperiode har ingen aktiviteter
        if (uttakResultatPeriode.isOpphold()) {
            return UttakPeriodeType.ANNET;
        }
        var harTrekkdager = uttakResultatPeriode.getAktiviteter().stream()
            .map(UttakResultatPeriodeAktivitetEntitet::getTrekkdager)
            .anyMatch(Trekkdager::merEnn0);
        // Utsettelse kommer normalt uten konto, mens det ofte er satt i overstyrt UR, selv uten trekkdager
        if (uttakResultatPeriode.isUtsettelse() && !harTrekkdager) {
            return UttakPeriodeType.UDEFINERT;
        }
        var stønadskontoType = uttakResultatPeriode.getAktiviteter()
            .stream()
            .max(Comparator.comparing(aktivitet -> aktivitet.getTrekkdager().decimalValue()))
            .map(UttakResultatPeriodeAktivitetEntitet::getTrekkonto);
        if (stønadskontoType.isPresent()) {
            return UttakEnumMapper.mapTilYf(stønadskontoType.get());
        }
        throw new IllegalStateException("Uttaksperiode mangler stønadskonto");
    }

    static Optional<UtsettelseÅrsak> finnUtsettelsesÅrsak(UttakResultatPeriodeEntitet uttakResultatPeriode) {
        if (erInnvilgetUtsettelse(uttakResultatPeriode)) {
            return UttakEnumMapper.mapTilYf(uttakResultatPeriode.getUtsettelseType());
        }
        return Optional.empty();
    }

    static boolean erInnvilgetUtsettelse(UttakResultatPeriodeEntitet uttakResultatPeriode) {
        var utsettelseType = uttakResultatPeriode.getUtsettelseType();
        if (utsettelseType != null && !UttakUtsettelseType.UDEFINERT.equals(utsettelseType)) {
            return uttakResultatPeriode.getAktiviteter()
                .stream()
                .noneMatch(a -> a.getUtbetalingsgrad().harUtbetaling());
        }
        return false;
    }

    static Optional<OppholdÅrsak> finnOppholdsÅrsak(UttakResultatPeriodeEntitet uttakResultatPeriode) {
        if (uttakResultatPeriode.isOpphold()) {
            return Optional.of(uttakResultatPeriode.getOppholdÅrsak());
        }
        return Optional.empty();
    }

    static Optional<BigDecimal> finnGraderingArbeidsprosent(UttakResultatPeriodeEntitet up) {
        if (up.getPeriodeSøknad().map(UttakResultatPeriodeSøknadEntitet::getGraderingArbeidsprosent).isEmpty()) {
            return Optional.empty();
        }
        for (var akt : up.getAktiviteter()) {
            if (akt.isSøktGradering()) {
                return Optional.ofNullable(up.getPeriodeSøknad().get().getGraderingArbeidsprosent());
            }
        }
        return Optional.empty();
    }
}
