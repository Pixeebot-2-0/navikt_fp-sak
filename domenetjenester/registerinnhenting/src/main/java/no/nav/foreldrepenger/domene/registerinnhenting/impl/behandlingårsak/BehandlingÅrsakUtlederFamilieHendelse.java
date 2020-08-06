package no.nav.foreldrepenger.domene.registerinnhenting.impl.behandlingårsak;

import java.util.Collections;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.GrunnlagRef;

@ApplicationScoped
@GrunnlagRef(value="FamilieHendelseGrunnlag")
class BehandlingÅrsakUtlederFamilieHendelse implements BehandlingÅrsakUtleder {

    @Inject
    public BehandlingÅrsakUtlederFamilieHendelse() {
        //For CDI
    }

    @Override
    public Set<EndringResultatType> utledEndringsResultat(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2) {
        return Collections.singleton(EndringResultatType.REGISTEROPPLYSNING);
    }
}
