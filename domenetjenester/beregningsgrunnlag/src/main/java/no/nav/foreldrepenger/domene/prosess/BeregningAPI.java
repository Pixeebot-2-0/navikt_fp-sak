package no.nav.foreldrepenger.domene.prosess;

import java.util.Optional;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlag;

public interface BeregningAPI {

    Optional<BeregningsgrunnlagGrunnlag> hent(BehandlingReferanse referanse);

    void beregn(BehandlingReferanse behandlingReferanse, BehandlingStegType stegType);

}
