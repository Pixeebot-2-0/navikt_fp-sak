package no.nav.foreldrepenger.domene.mappers.fra_kalkulus;

import java.util.Optional;

import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningsgrunnlagDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.FaktaAggregatDto;
import no.nav.folketrygdloven.kalkulator.output.RegelSporingAggregat;
import no.nav.folketrygdloven.kalkulator.steg.besteberegning.BesteberegningMånedGrunnlag;
import no.nav.folketrygdloven.kalkulator.steg.besteberegning.BesteberegningVurderingGrunnlag;
import no.nav.folketrygdloven.kalkulator.steg.besteberegning.Inntekt;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BesteberegningInntektEntitet;
import no.nav.foreldrepenger.domene.modell.BesteberegningMånedsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BesteberegninggrunnlagEntitet;

public final class BesteberegningMapper {

    private BesteberegningMapper() {
    }

    public static BeregningsgrunnlagEntitet mapBeregningsgrunnlagMedBesteberegning(BeregningsgrunnlagDto beregningsgrunnlagFraKalkulus,
                                                                                   Optional<FaktaAggregatDto> faktaAggregat,
                                                                                   Optional<RegelSporingAggregat> regelSporingAggregat,
                                                                                   BesteberegningVurderingGrunnlag besteberegningVurderingGrunnlag) {
        var nyEntitet = KalkulusTilBehandlingslagerMapper.mapBeregningsgrunnlag(beregningsgrunnlagFraKalkulus,
            faktaAggregat, regelSporingAggregat);
        return BeregningsgrunnlagEntitet.Builder.oppdater(nyEntitet)
            .medBesteberegninggrunnlag((BesteberegningMapper.mapBestebergninggrunnlag(besteberegningVurderingGrunnlag)))
            .build();
    }

    private static BesteberegninggrunnlagEntitet mapBestebergninggrunnlag(BesteberegningVurderingGrunnlag besteberegningVurderingGrunnlag) {
        var builder = BesteberegninggrunnlagEntitet.ny();
        besteberegningVurderingGrunnlag.getSeksBesteMåneder()
            .stream()
            .map(BesteberegningMapper::mapBesteberegningMåned)
            .forEach(builder::leggTilMånedsgrunnlag);
        return builder.build();
    }

    private static BesteberegningMånedsgrunnlagEntitet mapBesteberegningMåned(BesteberegningMånedGrunnlag besteberegningMånedGrunnlag) {
        var måned = besteberegningMånedGrunnlag.getMåned();
        var månedBuilder = BesteberegningMånedsgrunnlagEntitet.ny()
            .medPeriode(måned.atDay(1), måned.atEndOfMonth());
        besteberegningMånedGrunnlag.getInntekter()
            .stream()
            .map(BesteberegningMapper::mapBesteberegningInntekt)
            .forEach(månedBuilder::leggTilInntekt);
        return månedBuilder.build();
    }

    private static BesteberegningInntektEntitet mapBesteberegningInntekt(Inntekt inntekt) {
        if (inntekt.getArbeidsgiver() != null) {
            return BesteberegningInntektEntitet.ny()
                .medArbeidsgiver(KalkulusTilIAYMapper.mapArbeidsgiver(inntekt.getArbeidsgiver()))
                .medOpptjeningAktivitetType(inntekt.getOpptjeningAktivitetType()
                    == null ? OpptjeningAktivitetType.ARBEID : OpptjeningAktivitetType.fraKode(
                    inntekt.getOpptjeningAktivitetType().getKode()))
                .medArbeidsforholdRef(KalkulusTilIAYMapper.mapArbeidsforholdRef(inntekt.getArbeidsforholdRef()))
                .medInntekt(inntekt.getInntekt())
                .build();
        }
        return BesteberegningInntektEntitet.ny()
            .medOpptjeningAktivitetType(inntekt.getOpptjeningAktivitetType()
                == null ? OpptjeningAktivitetType.DAGPENGER : OpptjeningAktivitetType.fraKode(
                inntekt.getOpptjeningAktivitetType().getKode()))
            .medInntekt(inntekt.getInntekt())
            .build();
    }
}
