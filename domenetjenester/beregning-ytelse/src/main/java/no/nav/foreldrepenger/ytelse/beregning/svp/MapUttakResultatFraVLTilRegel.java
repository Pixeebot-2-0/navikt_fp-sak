package no.nav.foreldrepenger.ytelse.beregning.svp;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.ArbeidsforholdIkkeOppfyltÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatArbeidsforholdEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.ytelse.beregning.adapter.ArbeidsforholdMapper;
import no.nav.foreldrepenger.ytelse.beregning.adapter.MapUttakArbeidTypeTilAktivitetStatus;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.UttakAktivitet;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.UttakResultat;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.UttakResultatPeriode;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Arbeidsforhold;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

@ApplicationScoped
public class MapUttakResultatFraVLTilRegel {

    @Inject
    public MapUttakResultatFraVLTilRegel() {
    }

    public UttakResultat mapFra(SvangerskapspengerUttakResultatEntitet uttakResultat, UttakInput input) {
        var timelines = mapUttakResultatTilTimeline(uttakResultat, input);
        if (timelines.isEmpty()) {
            return new UttakResultat(Collections.emptyList());
        }
        var uttakTimeline = slåSammenUttakTimelines(timelines);
        var perioder = mapTimelineTilRegelmodell(uttakTimeline);
        return new UttakResultat(perioder);
    }

    private List<LocalDateTimeline<List<UttakAktivitet>>> mapUttakResultatTilTimeline(SvangerskapspengerUttakResultatEntitet uttakResultat, UttakInput input) {
        List<LocalDateTimeline<List<UttakAktivitet>>> timelines = new ArrayList<>();
        for (var arbeidsforhold : uttakResultat.getUttaksResultatArbeidsforhold()) {
            if (ArbeidsforholdIkkeOppfyltÅrsak.INGEN.equals(arbeidsforhold.getArbeidsforholdIkkeOppfyltÅrsak())) { //Fortsetter bare om arbeidsforhold er godkjent
                List<LocalDateSegment<List<UttakAktivitet>>> segments = new ArrayList<>();
                for (var periode : arbeidsforhold.getPerioder()) {
                    LocalDateSegment<List<UttakAktivitet>> segment = mapUttakResultatTilSegment(input, arbeidsforhold, periode);
                    segments.add(segment);
                }
                timelines.add(new LocalDateTimeline<>(segments));
            }
        }
        return timelines;
    }

    private LocalDateSegment<List<UttakAktivitet>> mapUttakResultatTilSegment(UttakInput input, SvangerskapspengerUttakResultatArbeidsforholdEntitet arbeidsforhold, SvangerskapspengerUttakResultatPeriodeEntitet periode) {
        LocalDateInterval interval = new LocalDateInterval(periode.getFom(), periode.getTom());
        var uttakAktivitet = mapAktivitet(input, arbeidsforhold, periode);
        var uttakAktivitetListe = List.of(uttakAktivitet);
        return new LocalDateSegment<>(interval, uttakAktivitetListe);
    }

    private LocalDateTimeline<List<UttakAktivitet>> slåSammenUttakTimelines(List<LocalDateTimeline<List<UttakAktivitet>>> timelines) {
        return timelines.stream()
            .reduce((a, b) -> a.union(b, getSegmentCombiner()))
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Mangler uttaksperioder. Skal ikke skje."));
    }

    private LocalDateSegmentCombinator<List<UttakAktivitet>, List<UttakAktivitet>, List<UttakAktivitet>> getSegmentCombiner() {
        return (dateInterval, segmentA, segmentB) -> {
            if (segmentA == null) {
                return segmentB;
            }
            if (segmentB == null) {
                return segmentA;
            }
            return new LocalDateSegment<>(dateInterval, Stream.concat(segmentA.getValue().stream(), segmentB.getValue().stream()).collect(Collectors.toList()));
        };
    }

    private List<UttakResultatPeriode> mapTimelineTilRegelmodell(LocalDateTimeline<List<UttakAktivitet>> uttakTimeline) {
        return uttakTimeline.getLocalDateIntervals()
            .stream()
            .map(uttakTimeline::getSegment)
            .map(segment -> new UttakResultatPeriode(segment.getFom(), segment.getTom(), segment.getValue(), false))
            .collect(Collectors.toList());
    }

    private UttakAktivitet mapAktivitet(UttakInput input, SvangerskapspengerUttakResultatArbeidsforholdEntitet uttakArbeidsforhold, SvangerskapspengerUttakResultatPeriodeEntitet periode) {
        var utbetalingsgrad = periode.getUtbetalingsgrad().harUtbetaling() ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
        var stillingsprosent = mapStillingsprosent(input, uttakArbeidsforhold);
        var totalStillingsprosent = finnTotalStillingsprosentHosAG(input, uttakArbeidsforhold);
        var arbeidsforhold = mapArbeidsforhold(uttakArbeidsforhold);
        var aktivitetStatus = MapUttakArbeidTypeTilAktivitetStatus.map(uttakArbeidsforhold.getUttakArbeidType());

        return new UttakAktivitet(stillingsprosent, null, utbetalingsgrad, arbeidsforhold, aktivitetStatus, false, totalStillingsprosent);
    }

    protected BigDecimal finnTotalStillingsprosentHosAG(UttakInput input, SvangerskapspengerUttakResultatArbeidsforholdEntitet arbeidsforhold) {
        if (!arbeidsforhold.getUttakArbeidType().equals(UttakArbeidType.ORDINÆRT_ARBEID)) {
            return BigDecimal.valueOf(100);
        }
        var identifikator = arbeidsforhold.getArbeidsgiver();
        var fom = arbeidsforhold.getPerioder().get(0).getFom();
        return input.getYrkesaktiviteter().finnStillingsprosentOrdinærtArbeid(identifikator, InternArbeidsforholdRef.nullRef(), fom);
    }

    private BigDecimal mapStillingsprosent(UttakInput input, SvangerskapspengerUttakResultatArbeidsforholdEntitet arbeidsforhold) {
        if (arbeidsforhold.getUttakArbeidType().equals(UttakArbeidType.ORDINÆRT_ARBEID)) {
            return finnStillingsprosent(input, arbeidsforhold);
        }
        return BigDecimal.valueOf(100L);
    }

    protected BigDecimal finnStillingsprosent(UttakInput input, SvangerskapspengerUttakResultatArbeidsforholdEntitet arbeidsforhold) {
        var identifikator = arbeidsforhold.getArbeidsgiver();
        var referanse = arbeidsforhold.getArbeidsforholdRef();
        var fom = arbeidsforhold.getPerioder().get(0).getFom();
        return input.getYrkesaktiviteter().finnStillingsprosentOrdinærtArbeid(identifikator, referanse, fom);
    }

    private Arbeidsforhold mapArbeidsforhold(SvangerskapspengerUttakResultatArbeidsforholdEntitet uttakArbeidsforhold) {
        return ArbeidsforholdMapper.mapArbeidsforholdFraUttakAktivitet(Optional.ofNullable(uttakArbeidsforhold.getArbeidsgiver()),
            uttakArbeidsforhold.getArbeidsforholdRef(), uttakArbeidsforhold.getUttakArbeidType());
    }
}
