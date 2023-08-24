package no.nav.foreldrepenger.behandling.revurdering.satsregulering;

import jakarta.persistence.EntityManager;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSatsType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.SatsReguleringRepository;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.TaskType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static no.nav.foreldrepenger.behandling.revurdering.satsregulering.SatsReguleringUtil.opprettFPMS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ExtendWith(JpaExtension.class)
class MilSivReguleringSaksutvalgTest {

    @Mock
    private ProsessTaskTjeneste taskTjeneste;
    private GrunnbeløpFinnSakerTask tjeneste;

    private long gammelSats;
    private long nySats;
    private LocalDate cutoff;

    @BeforeEach
    public void setUp(EntityManager entityManager) {
        var beregningsresultatRepository = new BeregningsresultatRepository(entityManager);
        nySats = beregningsresultatRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, LocalDate.now()).getVerdi();
        cutoff = beregningsresultatRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, LocalDate.now()).getPeriode().getFomDato();
        gammelSats = beregningsresultatRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, cutoff.minusDays(1)).getVerdi();
        tjeneste = new GrunnbeløpFinnSakerTask(new SatsReguleringRepository(entityManager), beregningsresultatRepository, taskTjeneste);
    }

    @Test
    void skal_ikke_finne_saker_til_revurdering(EntityManager em) {
        opprettFPMS(em, BehandlingStatus.UTREDES, cutoff.plusDays(5), gammelSats, gammelSats * 3); // Har åpen behandling
        opprettFPMS(em, BehandlingStatus.AVSLUTTET, cutoff.minusDays(5), gammelSats, gammelSats * 3); // Uttak før "1/5"

        tjeneste.doTask(SatsReguleringUtil.lagFinnSakerTask("FP", "MS"));

        verifyNoInteractions(taskTjeneste);
    }

    @Test
    void skal_finne_to_saker_til_revurdering(EntityManager em) {
        var kan1 = opprettFPMS(em, BehandlingStatus.AVSLUTTET, cutoff.plusWeeks(2), gammelSats, gammelSats * 3); // Skal finnes
        var kan2 = opprettFPMS(em, BehandlingStatus.AVSLUTTET, cutoff.plusDays(2), gammelSats, gammelSats * 2); // Skal finnes
        var kan3 = opprettFPMS(em, BehandlingStatus.AVSLUTTET, cutoff.plusDays(2), gammelSats, gammelSats * 4); // Over streken på 3G
        var kan4 = opprettFPMS(em, BehandlingStatus.AVSLUTTET, cutoff.minusDays(2), gammelSats, gammelSats * 2); // Uttak før "1/5"
        var kan5 = opprettFPMS(em, BehandlingStatus.AVSLUTTET, cutoff.plusWeeks(2), nySats, gammelSats * 2); // Har allerede ny G

        tjeneste.doTask(SatsReguleringUtil.lagFinnSakerTask("FP", "MS"));

        var captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(taskTjeneste, times(2)).lagre(captor.capture());
        var taskTypeExpected = TaskType.forProsessTask(GrunnbeløpReguleringTask.class);
        assertThat(captor.getValue().taskType()).isEqualTo(taskTypeExpected);

        assertThat(SatsReguleringUtil.finnTaskFor(captor, kan1)).isPresent();
        assertThat(SatsReguleringUtil.finnTaskFor(captor, kan2)).isPresent();
        assertThat(SatsReguleringUtil.finnTaskFor(captor, kan3)).isEmpty();
        assertThat(SatsReguleringUtil.finnTaskFor(captor, kan4)).isEmpty();
        assertThat(SatsReguleringUtil.finnTaskFor(captor, kan5)).isEmpty();
    }

}
