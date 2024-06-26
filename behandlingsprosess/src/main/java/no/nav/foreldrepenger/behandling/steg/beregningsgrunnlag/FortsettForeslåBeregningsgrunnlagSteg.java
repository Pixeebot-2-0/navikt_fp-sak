package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;

import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.mappers.til_kalkulator.BeregningsgrunnlagInputFelles;
import no.nav.foreldrepenger.domene.mappers.til_kalkulator.BeregningsgrunnlagInputProvider;
import no.nav.foreldrepenger.domene.prosess.BeregningsgrunnlagKopierOgLagreTjeneste;

@FagsakYtelseTypeRef
@BehandlingStegRef(BehandlingStegType.FORTSETT_FORESLÅ_BEREGNINGSGRUNNLAG)
@BehandlingTypeRef
@ApplicationScoped
public class FortsettForeslåBeregningsgrunnlagSteg implements BeregningsgrunnlagSteg {
    private BehandlingRepository behandlingRepository;
    private BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste;
    private BeregningsgrunnlagInputProvider beregningsgrunnlagInputProvider;

    protected FortsettForeslåBeregningsgrunnlagSteg() {
        // for CDI proxy
    }

    @Inject
    public FortsettForeslåBeregningsgrunnlagSteg(BehandlingRepository behandlingRepository,
                                                 BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste,
                                                 BeregningsgrunnlagInputProvider inputTjenesteProvider) {
        this.behandlingRepository = behandlingRepository;
        this.beregningsgrunnlagKopierOgLagreTjeneste = beregningsgrunnlagKopierOgLagreTjeneste;
        this.beregningsgrunnlagInputProvider = Objects.requireNonNull(inputTjenesteProvider, "inputTjenesteProvider");
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var ref = BehandlingReferanse.fra(behandling);
        var input = getInputTjeneste(ref.fagsakYtelseType()).lagInput(ref.behandlingId());
        var resultat = beregningsgrunnlagKopierOgLagreTjeneste.fortsettForeslåBeregningsgrunnlag(input);
        return BehandleStegResultat.utførtMedAksjonspunktResultater(resultat.getAksjonspunkter());

    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg,
            BehandlingStegType fraSteg) {
        if (tilSteg.equals(BehandlingStegType.FORTSETT_FORESLÅ_BEREGNINGSGRUNNLAG)) {
            beregningsgrunnlagKopierOgLagreTjeneste.getRyddBeregningsgrunnlag(kontekst).ryddFortsettForeslåBeregningsgrunnlagVedTilbakeføring();
        }
    }

    private BeregningsgrunnlagInputFelles getInputTjeneste(FagsakYtelseType ytelseType) {
        return beregningsgrunnlagInputProvider.getTjeneste(ytelseType);
    }
}
