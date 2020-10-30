package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.FaktaUttakDto;

public class FaktaUttakToTrinnsTjenesteTest extends EntityManagerAwareTest {

    private BehandlingRepositoryProvider behandlingRepositoryProvider;
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;

    @BeforeEach
    void setUp() {
        var entityManager = getEntityManager();
        behandlingRepositoryProvider = new BehandlingRepositoryProvider(entityManager);
        ytelseFordelingTjeneste = new YtelseFordelingTjeneste(new YtelsesFordelingRepository(entityManager));
    }

    @Test
    public void skal_sette_totrinns_ved_endring_fakta_uttak() {
        //Scenario med avklar fakta uttak
        ScenarioMorSøkerForeldrepenger scenario = AvklarFaktaTestUtil.opprettScenarioMorSøkerForeldrepenger();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_FAKTA_UTTAK_KONTROLLER_SØKNADSPERIODER,
            BehandlingStegType.VURDER_UTTAK);
        scenario.lagre(behandlingRepositoryProvider);
        // Behandling
        Behandling behandling = AvklarFaktaTestUtil.opprettBehandling(scenario);

        // dto
        FaktaUttakDto dto = AvklarFaktaTestUtil.opprettDtoAvklarFaktaUttakDto();

        boolean totrinn = new FaktaUttakToTrinnsTjeneste(ytelseFordelingTjeneste).oppdaterTotrinnskontrollVedEndringerFaktaUttak(dto);
        //assert
        assertThat(behandling.harAksjonspunktMedType(AksjonspunktDefinisjon.AVKLAR_FAKTA_UTTAK_KONTROLLER_SØKNADSPERIODER)).isTrue();
        assertThat(totrinn).isTrue();

    }

}
