package no.nav.foreldrepenger.domene.rest.historikk.tilfeller;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;
import no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.domene.rest.FaktaOmBeregningTilfelleRef;
import no.nav.foreldrepenger.domene.rest.dto.FaktaBeregningLagreDto;
import no.nav.foreldrepenger.domene.rest.historikk.InntektHistorikkTjeneste;
import no.nav.foreldrepenger.domene.rest.historikk.MapTilLønnsendring;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
@FaktaOmBeregningTilfelleRef(FaktaOmBeregningTilfelle.FASTSETT_MAANEDSINNTEKT_FL)
public class FastsettInntektFLHistorikkTjeneste extends FaktaOmBeregningHistorikkTjeneste {

    private InntektHistorikkTjeneste inntektHistorikkTjeneste;

    public FastsettInntektFLHistorikkTjeneste() {
        // For CDI
    }

    @Inject
    public FastsettInntektFLHistorikkTjeneste(InntektHistorikkTjeneste inntektHistorikkTjeneste) {
        this.inntektHistorikkTjeneste = inntektHistorikkTjeneste;
    }

    @Override
    public void lagHistorikk(Long behandlingId, FaktaBeregningLagreDto dto, HistorikkInnslagTekstBuilder tekstBuilder, BeregningsgrunnlagEntitet nyttBeregningsgrunnlag, Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        var lønnsendring = MapTilLønnsendring.mapTilLønnsendring(
            AktivitetStatus.FRILANSER,
            dto.getFastsettMaanedsinntektFL().getMaanedsinntekt(),
            nyttBeregningsgrunnlag,
            forrigeGrunnlag.flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag));
        inntektHistorikkTjeneste.lagHistorikk(tekstBuilder, List.of(lønnsendring), iayGrunnlag);
    }
}
