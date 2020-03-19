package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.aksjonspunkt;

import java.util.ArrayList;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.InnvilgetÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakUtsettelseType;
import no.nav.foreldrepenger.domene.uttak.KodeMapper;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.UttakResultatPeriode;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.UttakResultatPeriodeAktivitet;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.UttakResultatPerioder;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.ArbeidsgiverLagreDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.UttakResultatPeriodeAktivitetLagreDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.UttakResultatPeriodeLagreDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.overstyring.EndreUttakUtil;
import no.nav.fpsak.tidsserie.LocalDateInterval;

public final class UttakPerioderMapper {

    private UttakPerioderMapper() {
    }

    public static UttakResultatPerioder map(List<UttakResultatPeriodeLagreDto> dtoPerioder, UttakResultatPerioderEntitet gjeldeneperioder) {
        List<UttakResultatPeriode> perioder = new ArrayList<>();
        for (UttakResultatPeriodeLagreDto dtoPeriode : dtoPerioder) {
            perioder.add(map(dtoPeriode, gjeldeneperioder));
        }
        return new UttakResultatPerioder(perioder);
    }

    private static UttakResultatPeriode map(UttakResultatPeriodeLagreDto dtoPeriode,
                                            UttakResultatPerioderEntitet gjeldeneperioder) {
        LocalDateInterval periodeInterval = new LocalDateInterval(dtoPeriode.getFom(), dtoPeriode.getTom());
        List<UttakResultatPeriodeAktivitet> aktiviteter = new ArrayList<>();
        for (UttakResultatPeriodeAktivitetLagreDto nyAktivitet : dtoPeriode.getAktiviteter()) {
            UttakResultatPeriodeAktivitetEntitet matchendeGjeldendeAktivitet = EndreUttakUtil.finnGjeldendeAktivitetFor(gjeldeneperioder,
                periodeInterval, nyAktivitet.getArbeidsforholdId(), identifikator(nyAktivitet.getArbeidsgiver()), nyAktivitet.getUttakArbeidType());
            aktiviteter.add(map(nyAktivitet, matchendeGjeldendeAktivitet));

        }

        return new UttakResultatPeriode.Builder()
            .medTidsperiode(new LocalDateInterval(dtoPeriode.getFom(), dtoPeriode.getTom()))
            .medType(dtoPeriode.getPeriodeResultatType())
            .medÅrsak(mapInnvilgetÅrsak(EndreUttakUtil.finnGjeldendePeriodeFor(gjeldeneperioder, new LocalDateInterval(dtoPeriode.getFom(), dtoPeriode.getTom())), dtoPeriode))
            .medBegrunnelse(dtoPeriode.getBegrunnelse())
            .medSamtidigUttak(dtoPeriode.isSamtidigUttak())
            .medSamtidigUttaksprosent(dtoPeriode.getSamtidigUttaksprosent())
            .medFlerbarnsdager(dtoPeriode.isFlerbarnsdager())
            .medGraderingInnvilget(dtoPeriode.isGraderingInnvilget())
            .medGraderingAvslåttÅrsak(dtoPeriode.getGraderingAvslagÅrsak())
            .medUtsettelseType(EndreUttakUtil.finnGjeldendePeriodeFor(gjeldeneperioder, new LocalDateInterval(dtoPeriode.getFom(), dtoPeriode.getTom())).getUtsettelseType())
            .medOppholdÅrsak(dtoPeriode.getOppholdÅrsak())
            .medAktiviteter(aktiviteter)
            .build();
    }

    private static String identifikator(ArbeidsgiverLagreDto arbeidsgiver) {
        if (arbeidsgiver == null) {
            return null;
        }
        if (arbeidsgiver.erVirksomhet()) {
            return arbeidsgiver.getIdentifikator();
        }
        return arbeidsgiver.getAktørId().getId();
    }

    private static PeriodeResultatÅrsak mapInnvilgetÅrsak(UttakResultatPeriodeEntitet periodeEntitet, UttakResultatPeriodeLagreDto nyPeriode) {
        if (PeriodeResultatÅrsak.UKJENT.equals(nyPeriode.getPeriodeResultatÅrsak())) {
            if (!erOppholdsPeriode(nyPeriode) && PeriodeResultatType.INNVILGET.equals(nyPeriode.getPeriodeResultatType())) {
                return toUtsettelseårsaktype(periodeEntitet.getUtsettelseType());
            }
        }
        return nyPeriode.getPeriodeResultatÅrsak();
    }

    private static InnvilgetÅrsak toUtsettelseårsaktype(UttakUtsettelseType årsakType) {
        return innvilgetUtsettelseÅrsakMapper()
            .map(årsakType)
            .orElse(InnvilgetÅrsak.UTTAK_OPPFYLT);
    }

    private static KodeMapper<UttakUtsettelseType, InnvilgetÅrsak> innvilgetUtsettelseÅrsakMapper() {
        return KodeMapper
            .medMapping(UttakUtsettelseType.ARBEID, InnvilgetÅrsak.UTSETTELSE_GYLDIG_PGA_100_PROSENT_ARBEID)
            .medMapping(UttakUtsettelseType.FERIE, InnvilgetÅrsak.UTSETTELSE_GYLDIG_PGA_FERIE)
            .medMapping(UttakUtsettelseType.SYKDOM_SKADE, InnvilgetÅrsak.UTSETTELSE_GYLDIG_PGA_SYKDOM)
            .medMapping(UttakUtsettelseType.SØKER_INNLAGT, InnvilgetÅrsak.UTSETTELSE_GYLDIG_PGA_INNLEGGELSE)
            .medMapping(UttakUtsettelseType.BARN_INNLAGT, InnvilgetÅrsak.UTSETTELSE_GYLDIG_PGA_BARN_INNLAGT)
            .build();
    }

    private static UttakResultatPeriodeAktivitet map(UttakResultatPeriodeAktivitetLagreDto dto,
                                                     UttakResultatPeriodeAktivitetEntitet matchendeGjeldendeAktivitet) {
        return new UttakResultatPeriodeAktivitet.Builder()
            .medUtbetalingsgrad(dto.getUtbetalingsgrad())
            .medArbeidsprosent(matchendeGjeldendeAktivitet.getArbeidsprosent())
            .medTrekkonto(dto.getStønadskontoType())
            .medArbeidsforholdId(matchendeGjeldendeAktivitet.getArbeidsforholdId())
            .medArbeidsgiver(matchendeGjeldendeAktivitet.getUttakAktivitet().getArbeidsgiver().orElse(null))
            .medUttakArbeidType(matchendeGjeldendeAktivitet.getUttakArbeidType())
            .medTrekkdager(dto.getTrekkdagerDesimaler())
            .build();
    }

    private static boolean erOppholdsPeriode(UttakResultatPeriodeLagreDto uttakResultatPeriode) {
        return !OppholdÅrsak.UDEFINERT.equals(uttakResultatPeriode.getOppholdÅrsak());
    }
}
