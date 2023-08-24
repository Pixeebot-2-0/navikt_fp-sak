package no.nav.foreldrepenger.domene.vedtak.batch;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandlingskontroll.events.BehandlingStatusEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.domene.vedtak.OppdaterFagsakStatusTjeneste;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Observerer og propagerer / håndterer events internt i Behandlingskontroll
 */
@ApplicationScoped
public class FagsakStatusEventObserver {

    private static final Logger LOG = LoggerFactory.getLogger(FagsakStatusEventObserver.class);

    private OppdaterFagsakStatusTjeneste oppdaterFagsakStatusTjeneste;
    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;

    FagsakStatusEventObserver() {
        // For CDI
    }

    @Inject
    public FagsakStatusEventObserver(OppdaterFagsakStatusTjeneste oppdaterFagsakStatusTjeneste,
                                     BehandlingRepository behandlingRepository,
                                     FagsakRepository fagsakRepository) {
        this.oppdaterFagsakStatusTjeneste = oppdaterFagsakStatusTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.fagsakRepository = fagsakRepository;
    }

    public void observerBehandlingOpprettetEvent(@Observes BehandlingStatusEvent.BehandlingOpprettetEvent event) {
        LOG.debug("Oppdaterer status på Fagsak etter endring i behandling {}", event.getBehandlingId());
        var fagsak = fagsakRepository.finnEksaktFagsak(event.getFagsakId());
        oppdaterFagsakStatusTjeneste.oppdaterFagsakNårBehandlingOpprettet(fagsak, event.getBehandlingId(), event.getNyStatus());
    }

    public void observerBehandlingAvsluttetEvent(@Observes BehandlingStatusEvent.BehandlingAvsluttetEvent event) {
        LOG.debug("Oppdaterer status på Fagsak etter endring i behandling {}", event.getBehandlingId());
        var behandling = behandlingRepository.hentBehandling(event.getBehandlingId());
        oppdaterFagsakStatusTjeneste.oppdaterFagsakNårBehandlingAvsluttet(behandling, event.getNyStatus());
    }
}
