package no.nav.foreldrepenger.behandlingsprosess.dagligejobber.infobrev;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakEgenskapRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.egenskaper.UtlandMarkering;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@Dependent
@ProsessTask("oppgavebehandling.migrer.utland")
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
class MigrerUtlandMerkingTask implements ProsessTaskHandler {

    public static final String HENDELSE_TYPE = "hendelseType";

    private final FagsakEgenskapRepository fagsakEgenskapRepository;
    private final InformasjonssakRepository informasjonssakRepository;

    @Inject
    public MigrerUtlandMerkingTask(FagsakEgenskapRepository fagsakEgenskapRepository,
                                   InformasjonssakRepository informasjonssakRepository) {
        this.fagsakEgenskapRepository = fagsakEgenskapRepository;
        this.informasjonssakRepository = informasjonssakRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        informasjonssakRepository.finnBehandlingerMedUtlandsMerking()
            .forEach(udv -> fagsakEgenskapRepository.lagreEgenskapUtenHistorikk(udv.fagsakId(), UtlandMarkering.valueOf(udv.kode())));
    }

}