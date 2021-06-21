package no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLås;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLåsRepository;
import no.nav.foreldrepenger.historikk.OppgaveÅrsak;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveVurderKonsekvensTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

public class OpprettOppgaveVurderKonsekvensTaskTest {

    private static final long FAGSAK_ID = 2L;
    private OppgaveTjeneste oppgaveTjeneste;
    private OpprettOppgaveVurderKonsekvensTask opprettOppgaveVurderKonsekvensTask;
    private BehandlingRepositoryProvider repositoryProvider;
    private FagsakLåsRepository låsRepository;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;

    @BeforeEach
    public void before() {
        oppgaveTjeneste = mock(OppgaveTjeneste.class);
        repositoryProvider = mock(BehandlingRepositoryProvider.class);
        låsRepository = mock(FagsakLåsRepository.class);
        behandlendeEnhetTjeneste = mock(BehandlendeEnhetTjeneste.class);

        when(repositoryProvider.getFagsakLåsRepository()).thenReturn(låsRepository);
        when(låsRepository.taLås(anyLong())).thenReturn(mock(FagsakLås.class));
        when(behandlendeEnhetTjeneste.gyldigEnhetNfpNk(any())).thenReturn(true);

        opprettOppgaveVurderKonsekvensTask = new OpprettOppgaveVurderKonsekvensTask(oppgaveTjeneste, behandlendeEnhetTjeneste);
    }

    @Test
    public void skal_opprette_oppgave_for_å_vurdere_konsekvens_basert_på_fagsakId() {
        // Arrange
        var prosessTaskData = new ProsessTaskData(OpprettOppgaveVurderKonsekvensTask.TASKTYPE);
        prosessTaskData.setFagsakId(FAGSAK_ID);
        prosessTaskData.setProperty(OpprettOppgaveVurderKonsekvensTask.KEY_BESKRIVELSE, OpprettOppgaveVurderKonsekvensTask.STANDARD_BESKRIVELSE);
        prosessTaskData.setProperty(OpprettOppgaveVurderKonsekvensTask.KEY_PRIORITET, OpprettOppgaveVurderKonsekvensTask.PRIORITET_NORM);
        var fagsakIdCaptor = ArgumentCaptor.forClass(Long.class);
        var årsakCaptor = ArgumentCaptor.forClass(OppgaveÅrsak.class);
        var beskrivelseCaptor = ArgumentCaptor.forClass(String.class);

        // Act
        opprettOppgaveVurderKonsekvensTask.doTask(prosessTaskData);

        // Assert
        verify(oppgaveTjeneste).opprettMedPrioritetOgBeskrivelseBasertPåFagsakId(fagsakIdCaptor.capture(), årsakCaptor.capture(), any(),
                beskrivelseCaptor.capture(), Mockito.eq(false));
        assertThat(fagsakIdCaptor.getValue()).isEqualTo(FAGSAK_ID);
        assertThat(årsakCaptor.getValue()).isEqualTo(OppgaveÅrsak.VURDER_KONS_FOR_YTELSE);
        assertThat(beskrivelseCaptor.getValue()).isEqualTo(OpprettOppgaveVurderKonsekvensTask.STANDARD_BESKRIVELSE);
    }
}
