package no.nav.foreldrepenger.domene.rest;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.kalkulator.avklaringsbehov.*;
import no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.*;
import no.nav.folketrygdloven.kalkulator.avklaringsbehov.fordeling.FordelBeregningsgrunnlagDto;
import no.nav.folketrygdloven.kalkulator.avklaringsbehov.refusjon.VurderRefusjonBeregningsgrunnlagHåndterer;
import no.nav.folketrygdloven.kalkulator.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.kalkulus.kodeverk.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.domene.input.KalkulatorHåndteringInputTjeneste;
import no.nav.foreldrepenger.domene.prosess.HentOgLagreBeregningsgrunnlagTjeneste;

import java.util.List;

/**
 * Fasadetjeneste for håndtering av aksjonspunkt
 */
@ApplicationScoped
public class BeregningHåndterer {

    private HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    private BeregningFaktaOgOverstyringHåndterer beregningFaktaOgOverstyringHåndterer;
    private KalkulatorHåndteringInputTjeneste kalkulatorHåndteringInputTjeneste;

    public BeregningHåndterer() {
        // CDI
    }

    @Inject
    public BeregningHåndterer(HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                              BeregningFaktaOgOverstyringHåndterer beregningFaktaOgOverstyringHåndterer,
                              KalkulatorHåndteringInputTjeneste kalkulatorHåndteringInputTjeneste) {
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
        this.beregningFaktaOgOverstyringHåndterer = beregningFaktaOgOverstyringHåndterer;
        this.kalkulatorHåndteringInputTjeneste = kalkulatorHåndteringInputTjeneste;
    }

    public void håndterAvklarAktiviteter(BeregningsgrunnlagInput input, AvklarteAktiviteterDto dto) {
        var håndterBeregningsgrunnlagInput = kalkulatorHåndteringInputTjeneste.lagInput(
            input.getKoblingReferanse().getKoblingId(), input, AksjonspunktDefinisjon.AVKLAR_AKTIVITETER);
        var resultatFraKalkulus = AvklarAktiviteterHåndterer.håndter(dto, håndterBeregningsgrunnlagInput);
        beregningsgrunnlagTjeneste.lagre(getBehandlingId(input), resultatFraKalkulus);
    }

    public void håndterFastsettBGTidsbegrensetArbeidsforhold(BeregningsgrunnlagInput input,
                                                             FastsettBGTidsbegrensetArbeidsforholdDto dto) {
        var håndterBeregningsgrunnlagInput = kalkulatorHåndteringInputTjeneste.lagInput(
            input.getKoblingReferanse().getKoblingId(), input,
            AksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_TIDSBEGRENSET_ARBEIDSFORHOLD);
        var resultatFraKalkulus = FastsettBGTidsbegrensetArbeidsforholdHåndterer.håndter(håndterBeregningsgrunnlagInput,
            dto);
        beregningsgrunnlagTjeneste.lagre(getBehandlingId(input), resultatFraKalkulus);
    }

    public void håndterFastsettBeregningsgrunnlagATFL(BeregningsgrunnlagInput input,
                                                      FastsettBeregningsgrunnlagATFLDto dto) {
        var håndterBeregningsgrunnlagInput = kalkulatorHåndteringInputTjeneste.lagInput(
            input.getKoblingReferanse().getKoblingId(), input,
            AksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_ARBEIDSTAKER_FRILANS);
        var resultatFraKalkulus = FastsettBeregningsgrunnlagATFLHåndterer.håndter(håndterBeregningsgrunnlagInput, dto);
        beregningsgrunnlagTjeneste.lagre(getBehandlingId(input), resultatFraKalkulus);
    }

    public void håndterBeregningAktivitetOverstyring(BeregningsgrunnlagInput input,
                                                     List<BeregningsaktivitetLagreDto> overstyrAktiviteter) {
        var håndterBeregningsgrunnlagInput = kalkulatorHåndteringInputTjeneste.lagInput(
            input.getKoblingReferanse().getKoblingId(), input,
            AksjonspunktDefinisjon.OVERSTYRING_AV_BEREGNINGSAKTIVITETER);
        var grunnlag = AvklarAktiviteterHåndterer.håndterOverstyring(overstyrAktiviteter,
            håndterBeregningsgrunnlagInput);
        beregningsgrunnlagTjeneste.lagre(getBehandlingId(input), grunnlag);
    }

    public void håndterBeregningsgrunnlagOverstyring(BeregningsgrunnlagInput input,
                                                     OverstyrBeregningsgrunnlagDto overstyrBeregningsgrunnlagDto) {
        var håndterBeregningsgrunnlagInput = kalkulatorHåndteringInputTjeneste.lagInput(
            input.getKoblingReferanse().getKoblingId(), input,
            AksjonspunktDefinisjon.OVERSTYRING_AV_BEREGNINGSGRUNNLAG);
        var resultat = beregningFaktaOgOverstyringHåndterer.håndterMedOverstyring(håndterBeregningsgrunnlagInput,
            overstyrBeregningsgrunnlagDto);
        beregningsgrunnlagTjeneste.lagre(getBehandlingId(input), resultat);
    }

    public void håndterFastsettBruttoForSNNyIArbeidslivet(BeregningsgrunnlagInput input,
                                                          FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto dto) {
        var håndterBeregningsgrunnlagInput = kalkulatorHåndteringInputTjeneste.lagInput(
            input.getKoblingReferanse().getKoblingId(), input,
            AksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_FOR_SN_NY_I_ARBEIDSLIVET);
        var resultat = FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetHåndterer.oppdater(
            håndterBeregningsgrunnlagInput, dto);
        beregningsgrunnlagTjeneste.lagre(getBehandlingId(input), resultat);
    }

    public void håndterFordelBeregningsgrunnlag(BeregningsgrunnlagInput input, FordelBeregningsgrunnlagDto dto) {
        var håndterBeregningsgrunnlagInput = kalkulatorHåndteringInputTjeneste.lagInput(
            input.getKoblingReferanse().getKoblingId(), input,
            AksjonspunktDefinisjon.FORDEL_BEREGNINGSGRUNNLAG);
        var resultat = FordelBeregningsgrunnlagHåndterer.håndter(dto, håndterBeregningsgrunnlagInput);
        beregningsgrunnlagTjeneste.lagre(getBehandlingId(input), resultat);
    }

    public void håndterVurderRefusjonBeregningsgrunnlag(BeregningsgrunnlagInput input,
                                                        VurderRefusjonBeregningsgrunnlagDto dto) {
        var håndterBeregningsgrunnlagInput = kalkulatorHåndteringInputTjeneste.lagInput(
            input.getKoblingReferanse().getKoblingId(), input, AksjonspunktDefinisjon.VURDER_REFUSJON_BERGRUNN);
        var resultat = VurderRefusjonBeregningsgrunnlagHåndterer.håndter(dto, håndterBeregningsgrunnlagInput);
        beregningsgrunnlagTjeneste.lagre(getBehandlingId(input), resultat);
    }


    public void håndterVurderFaktaOmBeregning(BeregningsgrunnlagInput input, FaktaBeregningLagreDto dto) {
        var håndterBeregningsgrunnlagInput = kalkulatorHåndteringInputTjeneste.lagInput(
            input.getKoblingReferanse().getKoblingId(), input,
            AksjonspunktDefinisjon.VURDER_FAKTA_FOR_ATFL_SN);
        var resultat = beregningFaktaOgOverstyringHåndterer.håndter(håndterBeregningsgrunnlagInput, dto);
        beregningsgrunnlagTjeneste.lagre(getBehandlingId(input), resultat);
    }

    public void håndterVurderVarigEndretNyoppstartetSN(BeregningsgrunnlagInput input,
                                                       Integer bruttoBeregningsgrunnlag) {
        var håndterBeregningsgrunnlagInput = kalkulatorHåndteringInputTjeneste.lagInput(
            input.getKoblingReferanse().getKoblingId(), input,
            AksjonspunktDefinisjon.VURDER_VARIG_ENDRET_ELLER_NYOPPSTARTET_NÆRING_SELVSTENDIG_NÆRINGSDRIVENDE);
        var resultat = VurderVarigEndretEllerNyoppstartetHåndterer.håndter(håndterBeregningsgrunnlagInput, bruttoBeregningsgrunnlag, AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE);
        beregningsgrunnlagTjeneste.lagre(getBehandlingId(input), resultat);
    }

    private Long getBehandlingId(BeregningsgrunnlagInput input) {
        return input.getKoblingReferanse().getKoblingId();
    }

}
