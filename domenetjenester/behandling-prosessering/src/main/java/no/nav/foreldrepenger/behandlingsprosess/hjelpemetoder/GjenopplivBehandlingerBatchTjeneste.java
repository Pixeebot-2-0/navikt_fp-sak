package no.nav.foreldrepenger.behandlingsprosess.hjelpemetoder;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.batch.BatchArguments;
import no.nav.foreldrepenger.batch.BatchStatus;
import no.nav.foreldrepenger.batch.BatchTjeneste;
import no.nav.foreldrepenger.behandlingsprosess.dagligejobber.gjenopptak.AutomatiskGjenopptagelseTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskStatus;
import no.nav.vedtak.felles.prosesstask.api.TaskStatus;

/**
 * Batchservice som finner alle behandlinger som ikke er aktive og lager en
 * ditto prosess task for hver. Kriterier for gjenoppliving: Behandlingen er
 * ikke avsluttet/iverksatt og det finnes ikke åpne aksjonspunkt eller
 * autopunkt.
 */
@ApplicationScoped
public class GjenopplivBehandlingerBatchTjeneste implements BatchTjeneste {

    static final String BATCHNAME = "BVL097";
    private static final String EXECUTION_ID_SEPARATOR = "-";

    private AutomatiskGjenopptagelseTjeneste automatiskGjenopptagelseTjeneste;

    @Inject
    public GjenopplivBehandlingerBatchTjeneste(AutomatiskGjenopptagelseTjeneste automatiskGjenopptagelseTjeneste) {
        this.automatiskGjenopptagelseTjeneste = automatiskGjenopptagelseTjeneste;
    }

    @Override
    public String launch(BatchArguments arguments) {
        automatiskGjenopptagelseTjeneste.gjenopplivBehandlinger();
        var executionId = BATCHNAME + EXECUTION_ID_SEPARATOR;
        return executionId;
    }

    @Override
    public BatchStatus status(String executionId) {
        final var gruppe = executionId.substring(executionId.indexOf(EXECUTION_ID_SEPARATOR.charAt(0)) + 1);
        final var taskStatuses = automatiskGjenopptagelseTjeneste.hentStatusForGjenopptaBehandlingGruppe(gruppe);

        BatchStatus res;
        if (isCompleted(taskStatuses)) {
            if (isContainingFailures(taskStatuses)) {
                res = BatchStatus.WARNING;
            } else {
                res = BatchStatus.OK;
            }
        } else {
            res = BatchStatus.RUNNING;
        }
        return res;
    }

    @Override
    public String getBatchName() {
        return BATCHNAME;
    }

    private boolean isContainingFailures(List<TaskStatus> taskStatuses) {
        return taskStatuses.stream().anyMatch(it -> it.getStatus() == ProsessTaskStatus.FEILET);
    }

    private boolean isCompleted(List<TaskStatus> taskStatuses) {
        return taskStatuses.stream().noneMatch(it -> it.getStatus() == ProsessTaskStatus.KLAR);
    }
}
