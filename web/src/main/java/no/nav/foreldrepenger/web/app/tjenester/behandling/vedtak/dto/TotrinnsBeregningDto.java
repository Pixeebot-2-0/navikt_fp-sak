package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.dto;

import no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle;

import java.util.List;


public class TotrinnsBeregningDto {

    private boolean fastsattVarigEndringNaering;

    private List<FaktaOmBeregningTilfelle> faktaOmBeregningTilfeller;

    public void setFastsattVarigEndringNaering(boolean fastsattVarigEndringNaering) {
        this.fastsattVarigEndringNaering = fastsattVarigEndringNaering;
    }

    public boolean isFastsattVarigEndringNaering() {
        return fastsattVarigEndringNaering;
    }

    public List<FaktaOmBeregningTilfelle> getFaktaOmBeregningTilfeller() {
        return faktaOmBeregningTilfeller;
    }

    public void setFaktaOmBeregningTilfeller(List<FaktaOmBeregningTilfelle> faktaOmBeregningTilfeller) {
        this.faktaOmBeregningTilfeller = faktaOmBeregningTilfeller;
    }
}
