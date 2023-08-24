package no.nav.foreldrepenger.mottak.publiserer.observer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.events.MottattDokumentPersistertEvent;
import no.nav.foreldrepenger.mottak.publiserer.task.PubliserPersistertDokumentHendelseTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@ApplicationScoped
public class MottattDokumentPersistertEventObserver {

    private ProsessTaskTjeneste taskRepository;

    public MottattDokumentPersistertEventObserver() {
    }

    @Inject
    public MottattDokumentPersistertEventObserver(ProsessTaskTjeneste taskRepository) {
        this.taskRepository = taskRepository;
    }

    public void observerMottattDokumentPersistert(@Observes MottattDokumentPersistertEvent event) {
        if (DokumentTypeId.INNTEKTSMELDING.equals(event.getMottattDokument().getDokumentType())) {
            var taskData = ProsessTaskData.forProsessTask(PubliserPersistertDokumentHendelseTask.class);
            taskData.setBehandling(event.getFagsakId(), event.getBehandlingId(), event.getAktørId().getId());
            taskData.setProperty(PubliserPersistertDokumentHendelseTask.MOTTATT_DOKUMENT_ID_KEY, event.getMottattDokument().getId().toString());
            taskData.setCallIdFraEksisterende();
            taskRepository.lagre(taskData);
        }
    }
}
