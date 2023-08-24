package no.nav.foreldrepenger.familiehendelse.kontrollerfakta.omsorgsovertakelse;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.FarSøkerType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerEngangsstønad;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AVKLAR_VILKÅR_FOR_OMSORGSOVERTAKELSE;
import static no.nav.foreldrepenger.behandlingslager.behandling.søknad.FarSøkerType.*;
import static org.assertj.core.api.Assertions.assertThat;

class AksjonspunktUtlederForOmsorgsovertakelseTest {

    private static final LocalDate FØDSELSDATO_BARN = LocalDate.of(2017, Month.JANUARY, 1);
    private AksjonspunktUtlederForOmsorgsovertakelse aksjonspunktUtleder;

    @Test
    void skal_utledede_aksjonspunkt_basert_på_fakta_om_engangsstønad_til_far() {
        var overtattOmsorg = aksjonspunktForFakta(OVERTATT_OMSORG);
        assertThat(overtattOmsorg).hasSize(1);
        assertThat(overtattOmsorg.get(0).getAksjonspunktDefinisjon()).isEqualTo(AVKLAR_VILKÅR_FOR_OMSORGSOVERTAKELSE);

        var andreForelderDød = aksjonspunktForFakta(ANDRE_FORELDER_DØD);
        assertThat(andreForelderDød).hasSize(1);
        assertThat(andreForelderDød.get(0).getAksjonspunktDefinisjon()).isEqualTo(AVKLAR_VILKÅR_FOR_OMSORGSOVERTAKELSE);

        assertThat(aksjonspunktForFakta(ADOPTERER_ALENE)).isEmpty();
    }

    private List<AksjonspunktResultat> aksjonspunktForFakta(FarSøkerType farSøkerType) {
        var behandling = byggBehandling(farSøkerType);
        return aksjonspunktUtleder.utledAksjonspunkterFor(new AksjonspunktUtlederInput(BehandlingReferanse.fra(behandling)));
    }

    private Behandling byggBehandling(FarSøkerType farSøkerType) {

        var farSøkerAdopsjonScenario = ScenarioFarSøkerEngangsstønad.forAdopsjon();

        farSøkerAdopsjonScenario.medSøknad().medFarSøkerType(farSøkerType);
        farSøkerAdopsjonScenario.medSøknadHendelse().medFødselsDato(FØDSELSDATO_BARN);
        if (farSøkerType.equals(FarSøkerType.ADOPTERER_ALENE)) {
            farSøkerAdopsjonScenario.medSøknadHendelse()
                .medAdopsjon(farSøkerAdopsjonScenario.medSøknadHendelse().getAdopsjonBuilder().medAdoptererAlene(true));
        }

        var behandling = farSøkerAdopsjonScenario.lagMocked();
        var repositoryProvider = farSøkerAdopsjonScenario.mockBehandlingRepositoryProvider();
        var familieHendelseTjeneste = new FamilieHendelseTjeneste(null, repositoryProvider.getFamilieHendelseRepository());
        aksjonspunktUtleder = new AksjonspunktUtlederForOmsorgsovertakelse(familieHendelseTjeneste);
        return behandling;
    }

}
