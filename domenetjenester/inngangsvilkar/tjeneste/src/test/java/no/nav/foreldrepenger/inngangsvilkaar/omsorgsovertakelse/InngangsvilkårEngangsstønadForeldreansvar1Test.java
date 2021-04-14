package no.nav.foreldrepenger.inngangsvilkaar.omsorgsovertakelse;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;

public class InngangsvilkårEngangsstønadForeldreansvar1Test {

    @Test
    public void skal_uavhengig_av_behandling_alltid_opprette_aksjonspunkt_for_manuell_vurdering() {
        var vilkårData = new InngangsvilkårEngangsstønadForeldreansvar1().vurderVilkår(null);

        assertThat(vilkårData.getUtfallType()).isEqualTo(VilkårUtfallType.IKKE_VURDERT);
        assertThat(vilkårData.getApDefinisjoner()).hasSize(1);
        assertThat(vilkårData.getApDefinisjoner().get(0)).isEqualTo(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_FORELDREANSVARSVILKÅRET_2_LEDD);
    }

}
