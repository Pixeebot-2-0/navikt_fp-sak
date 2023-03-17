package no.nav.foreldrepenger.behandling.steg.mottatteopplysninger.svp;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandling.steg.mottatteopplysninger.RegistrerFagsakEgenskaper;
import no.nav.foreldrepenger.behandling.steg.mottatteopplysninger.TilknyttFagsakSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

@BehandlingStegRef(BehandlingStegType.INNHENT_SØKNADOPP)
@BehandlingTypeRef(BehandlingType.FØRSTEGANGSSØKNAD)
@FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER)
@ApplicationScoped
public class TilknyttFagsakStegImpl implements TilknyttFagsakSteg {

    private BehandlingRepository behandlingRepository;
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private RegistrerFagsakEgenskaper registrerFagsakEgenskaper;

    TilknyttFagsakStegImpl() {
        // for CDI proxy
    }

    @Inject
    public TilknyttFagsakStegImpl(BehandlingRepository behandlingRepository,
                                  FagsakRelasjonTjeneste fagsakRelasjonTjeneste,
                                  RegistrerFagsakEgenskaper registrerFagsakEgenskaper) {
        this.behandlingRepository = behandlingRepository;
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.registrerFagsakEgenskaper = registrerFagsakEgenskaper;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        if (fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(behandling.getFagsak()).isEmpty()) {
            fagsakRelasjonTjeneste.opprettRelasjon(behandling.getFagsak(), Dekningsgrad._100);
        }
        registrerFagsakEgenskaper.registrerFagsakEgenskaper(behandling, false);
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }
}
