package no.nav.foreldrepenger.behandling.steg.avklarfakta.fp;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandling.steg.avklarfakta.AksjonspunktUtlederForVurderArbeidsforhold;
import no.nav.foreldrepenger.behandling.steg.avklarfakta.KontrollerArbeidsforholdSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@BehandlingStegRef(kode = "KOARB")
@BehandlingTypeRef
@FagsakYtelseTypeRef("FP")
@ApplicationScoped
class KontrollerArbeidsforholdStegImpl implements KontrollerArbeidsforholdSteg {

    private BehandlingRepository behandlingRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private AksjonspunktUtlederForVurderArbeidsforhold utleder;

    KontrollerArbeidsforholdStegImpl() {
        // for CDI proxy
    }

    @Inject
    KontrollerArbeidsforholdStegImpl(BehandlingRepository behandlingRepository,
            SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
            AksjonspunktUtlederForVurderArbeidsforhold utleder) {
        this.behandlingRepository = behandlingRepository;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.utleder = utleder;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunkter);
        var aksjonspunktResultat = utleder.utledAksjonspunkterFor(new AksjonspunktUtlederInput(ref));
        return BehandleStegResultat.utførtMedAksjonspunktResultater(aksjonspunktResultat);
    }
}
