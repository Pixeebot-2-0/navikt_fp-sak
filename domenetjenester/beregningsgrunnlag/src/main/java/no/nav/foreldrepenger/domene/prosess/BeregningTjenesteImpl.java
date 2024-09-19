package no.nav.foreldrepenger.domene.prosess;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.gui.BeregningsgrunnlagDto;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OverstyringAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.domene.aksjonspunkt.OppdaterBeregningsgrunnlagResultat;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlag;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.output.BeregningsgrunnlagVilkårOgAkjonspunktResultat;
import no.nav.foreldrepenger.konfig.Environment;

@ApplicationScoped
public class BeregningTjenesteImpl implements BeregningTjeneste {
    private BeregningFPSAK fpsakBeregner;
    private BeregningKalkulus kalkulusBeregner;
    private boolean skalKalleKalkulus;

    BeregningTjenesteImpl() {
        // CDI
    }

    @Inject
    public BeregningTjenesteImpl(BeregningFPSAK fpsakBeregner,
                                 BeregningKalkulus kalkulusBeregner) {
        this.fpsakBeregner = fpsakBeregner;
        this.kalkulusBeregner = kalkulusBeregner;
        this.skalKalleKalkulus = false;
    }

    @Override
    public Optional<BeregningsgrunnlagGrunnlag> hent(BehandlingReferanse referanse) {
        if (skalKalleKalkulus) {
            return kalkulusBeregner.hent(referanse);
        } else {
            return fpsakBeregner.hent(referanse);
        }
    }

    @Override
    public Optional<BeregningsgrunnlagDto> hentGuiDto(BehandlingReferanse referanse) {
        if (skalKalleKalkulus) {
            return kalkulusBeregner.hentGUIDto(referanse);
        } else {
            return fpsakBeregner.hentGUIDto(referanse);
        }
    }

    @Override
    public BeregningsgrunnlagVilkårOgAkjonspunktResultat beregn(BehandlingReferanse referanse, BehandlingStegType stegType) {
        if (skalKalleKalkulus) {
            return kalkulusBeregner.beregn(referanse, stegType);
        } else {
            return fpsakBeregner.beregn(referanse, stegType);
        }
    }

    @Override
    public void lagre(BeregningsgrunnlagGrunnlag beregningsgrunnlagGrunnlag, BehandlingReferanse referanse) {
        throw new IllegalStateException("Skal kun kalles i test, bruk heller #beregn");
    }

    @Override
    public void kopier(BehandlingReferanse revurdering, BehandlingReferanse originalbehandling, BeregningsgrunnlagTilstand tilstand) {
        if (skalKalleKalkulus) {
            kalkulusBeregner.kopier(revurdering, originalbehandling, tilstand);
        } else {
            fpsakBeregner.kopier(revurdering, originalbehandling, tilstand);
        }

    }

    @Override
    public Optional<OppdaterBeregningsgrunnlagResultat> oppdaterBeregning(BekreftetAksjonspunktDto oppdatering, BehandlingReferanse referanse) {
        if (skalKalleKalkulus) {
            return kalkulusBeregner.oppdaterBeregning(oppdatering, referanse);
        } else {
            return fpsakBeregner.oppdaterBeregning(oppdatering, referanse);
        }
    }

    @Override
    public Optional<OppdaterBeregningsgrunnlagResultat> overstyrBeregning(OverstyringAksjonspunktDto overstyring, BehandlingReferanse referanse) {
        if (skalKalleKalkulus) {
            return kalkulusBeregner.overstyrBeregning(overstyring, referanse);
        } else {
            return fpsakBeregner.overstyrBeregning(overstyring, referanse);
        }
    }

    @Override
    public void avslutt(BehandlingReferanse referanse) {
        if (skalKalleKalkulus) {
            kalkulusBeregner.avslutt(referanse);
        } else {
            fpsakBeregner.avslutt(referanse);
        }

    }

}
