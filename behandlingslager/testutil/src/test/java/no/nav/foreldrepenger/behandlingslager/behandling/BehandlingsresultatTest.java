package no.nav.foreldrepenger.behandlingslager.behandling;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.fagsak.FagsakBuilder;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareTest;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class BehandlingsresultatTest extends EntityManagerAwareTest {


    private BehandlingRepository behandlingRepository;

    @BeforeEach
    void setUp() {
        behandlingRepository = new BehandlingRepository(getEntityManager());
    }

    @Test
    public void skal_opprette_ny_behandlingsresultat() {
        Behandlingsresultat.Builder behandlingsresultatBuilder = Behandlingsresultat.builderForInngangsvilkår();
        Behandlingsresultat behandlingsresultat = behandlingsresultatBuilder.build();

        assertThat(behandlingsresultat).isNotNull();
        assertThat(behandlingsresultat.getVilkårResultat()).isNotNull();
    }

    @Test
    public void skal_opprette_ny_behandlingsresultat_og_lagre_med_ikke_fastsatt_vilkårresultat() {
        var behandling = opprettBehandling();

        Behandlingsresultat.Builder behandlingsresultatBuilder = Behandlingsresultat.builderForInngangsvilkår();
        Behandlingsresultat behandlingsresultat = behandlingsresultatBuilder.buildFor(behandling);

        assertThat(behandling.getBehandlingsresultat()).isEqualTo(behandlingsresultat);
        assertThat(behandlingsresultat.getBehandlingId()).isNotNull();
        assertThat(behandlingsresultat.getVilkårResultat().getVilkårResultatType()).isEqualTo(VilkårResultatType.IKKE_FASTSATT);

        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandlingsresultat.getVilkårResultat(), lås);

        Long id = behandling.getId();
        assertThat(id).isNotNull();

        Behandling lagretBehandling = behandlingRepository.hentBehandling(behandling.getId());
        assertThat(lagretBehandling).isEqualTo(behandling);
        assertThat(lagretBehandling.getBehandlingsresultat()).isEqualTo(behandlingsresultat);
    }

    private Behandling opprettBehandling() {
        var fagsak = FagsakBuilder.nyEngangstønadForMor().build();
        new FagsakRepository(getEntityManager()).opprettNy(fagsak);
        var behandling = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingRepository.lagre(behandling, new BehandlingLåsRepository(getEntityManager()).taLås(behandling.getId()));
        return behandling;
    }
}
