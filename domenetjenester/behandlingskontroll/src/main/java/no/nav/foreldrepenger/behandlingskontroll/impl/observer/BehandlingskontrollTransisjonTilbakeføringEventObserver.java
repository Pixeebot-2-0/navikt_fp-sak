package no.nav.foreldrepenger.behandlingskontroll.impl.observer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.events.AksjonspunktStatusEvent;
import no.nav.foreldrepenger.behandlingskontroll.events.BehandlingStegOvergangEvent.BehandlingStegTilbakeføringEvent;
import no.nav.foreldrepenger.behandlingskontroll.impl.BehandlingskontrollEventPubliserer;
import no.nav.foreldrepenger.behandlingskontroll.spi.BehandlingskontrollServiceProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKontrollRepository;

/**
 * Håndtere opprydding i Aksjonspunkt og Vilkår ved overhopp framover eller
 * tilbakeføring.
 */
@ApplicationScoped
public class BehandlingskontrollTransisjonTilbakeføringEventObserver {

    private BehandlingskontrollEventPubliserer eventPubliserer = BehandlingskontrollEventPubliserer.NULL_EVENT_PUB;
    private BehandlingskontrollServiceProvider serviceProvider;
    private AksjonspunktKontrollRepository aksjonspunktKontrollRepository;

    @Inject
    public BehandlingskontrollTransisjonTilbakeføringEventObserver(BehandlingskontrollServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
        this.eventPubliserer = serviceProvider.getEventPubliserer();
        this.aksjonspunktKontrollRepository = serviceProvider.getAksjonspunktKontrollRepository();
    }

    protected BehandlingskontrollTransisjonTilbakeføringEventObserver() {
        // for CDI proxy
    }

    public void observerBehandlingSteg(@Observes BehandlingStegTilbakeføringEvent event) {
        var behandlingId = event.getBehandlingId();
        var behandling = serviceProvider.hentBehandling(behandlingId);
        var modell = getModell(behandling);
        guardIngenÅpneAutopunkter(behandling);

        var førsteSteg = event.getFørsteSteg();
        var sisteSteg = event.getSisteSteg();

        var førsteStegStatus = event.getFørsteStegStatus();

        boolean medInngangFørsteSteg = førsteStegStatus.map(BehandlingStegStatus::erVedInngang).orElse(Boolean.TRUE);

        var aksjonspunktDefinisjonerEtterFra = modell.finnAksjonspunktDefinisjonerFraOgMed(førsteSteg, medInngangFørsteSteg);

        var endredeAksjonspunkter = håndterAksjonspunkter(behandling, aksjonspunktDefinisjonerEtterFra, event, førsteSteg, modell,
                medInngangFørsteSteg);

        modell.hvertStegFraOgMedTil(førsteSteg, sisteSteg, true)
                .collect(Collectors.toCollection(ArrayDeque::new))
                .descendingIterator() // stepper bakover
                .forEachRemaining(s -> hoppBakover(s, event, førsteSteg, sisteSteg));

        aksjonspunkterTilbakeført(event.getKontekst(), endredeAksjonspunkter, event.getFraStegType());
    }

    protected void hoppBakover(BehandlingStegModell s, BehandlingStegTilbakeføringEvent event, BehandlingStegType førsteSteg,
            BehandlingStegType sisteSteg) {
        s.getSteg().vedTransisjon(event.getKontekst(), s, BehandlingSteg.TransisjonType.HOPP_OVER_BAKOVER, førsteSteg, sisteSteg);
    }

    private BehandlingModell getModell(Behandling behandling) {
        return serviceProvider.getBehandlingModellRepository().getModell(behandling.getType(), behandling.getFagsakYtelseType());
    }

    private List<Aksjonspunkt> håndterAksjonspunkter(Behandling behandling, Set<AksjonspunktDefinisjon> mellomliggendeAksjonspunkt,
            BehandlingStegTilbakeføringEvent event, BehandlingStegType førsteSteg, BehandlingModell modell,
            boolean tilInngangFørsteSteg) {
        var endredeAksjonspunkter = behandling.getAksjonspunkter().stream()
                .filter(a -> !a.erAutopunkt()) // Autopunkt skal ikke håndteres; skal alltid være lukket ved tilbakehopp
                .filter(a -> mellomliggendeAksjonspunkt.contains(a.getAksjonspunktDefinisjon()))
                .collect(Collectors.toList());

        List<Aksjonspunkt> oppdaterteAksjonspunkt = new ArrayList<>();
        endredeAksjonspunkter.forEach(a -> håndterEndretAksjonspunkt(a, førsteSteg, modell, oppdaterteAksjonspunkt, tilInngangFørsteSteg));

        serviceProvider.getBehandlingRepository().lagre(behandling, event.getKontekst().getSkriveLås());
        return oppdaterteAksjonspunkt;
    }

    private void guardIngenÅpneAutopunkter(Behandling behandling) {
        var autopunkt = behandling.getAksjonspunkter().stream()
                .filter(Aksjonspunkt::erAutopunkt)
                .filter(Aksjonspunkt::erÅpentAksjonspunkt)
                .findFirst();

        if (autopunkt.isPresent()) {
            throw new IllegalStateException(
                    "Utvikler-feil: Tilbakehopp ikke tillatt for autopunkt '" +
                            autopunkt.get().getAksjonspunktDefinisjon().getNavn() + "'");
        }
    }

    private void aksjonspunkterTilbakeført(BehandlingskontrollKontekst kontekst, List<Aksjonspunkt> aksjonspunkter,
            BehandlingStegType behandlingStegType) {
        if (!aksjonspunkter.isEmpty()) {
            eventPubliserer.fireEvent(new AksjonspunktStatusEvent(kontekst, aksjonspunkter, behandlingStegType));
        }
    }

    private void håndterEndretAksjonspunkt(Aksjonspunkt a, BehandlingStegType førsteSteg, BehandlingModell modell,
            List<Aksjonspunkt> oppdaterteAksjonspunkt, boolean tilInngangFørsteSteg) {
        if (!a.erAvbrutt() && skalAvbryte(a, førsteSteg, modell, tilInngangFørsteSteg)) {
            aksjonspunktKontrollRepository.setTilAvbrutt(a);
            oppdaterteAksjonspunkt.add(a);
        } else if (!a.erÅpentAksjonspunkt() && skalReåpne(a, førsteSteg, modell)) {
            aksjonspunktKontrollRepository.setReåpnet(a);
            oppdaterteAksjonspunkt.add(a);
        }
    }

    /**
     * Ved tilbakeføring skal følgende reåpnes: - Påfølgende aksjonspunkt som er
     * OVERSTYRING
     */
    private boolean skalReåpne(Aksjonspunkt a, BehandlingStegType førsteSteg, BehandlingModell modell) {
        var måTidligstLøsesISteg = modell.finnTidligsteStegFor(a.getAksjonspunktDefinisjon())
                .getBehandlingStegType();
        var måLøsesIEllerEtterFørsteSteg = !modell.erStegAFørStegB(måTidligstLøsesISteg, førsteSteg);
        return a.erManueltOpprettet() && måLøsesIEllerEtterFørsteSteg;
    }

    /**
     * Ved tilbakeføring skal alle påfølgende åpne aksjonspunkt (som IKKE ER
     * OVERSTYRING) som identifiseres i eller senere steg Avbrytes. De som er UTFØRT
     * bilr stående og må evt reutledes - obs en del avklarte AP reutledes ikke.
     */
    private boolean skalAvbryte(Aksjonspunkt a, BehandlingStegType førsteSteg, BehandlingModell modell, boolean tilInngangFørsteSteg) {
        var erFunnetIFørsteStegEllerSenere = !modell.erStegAFørStegB(a.getBehandlingStegFunnet(), førsteSteg);
        var erManueltOpprettet = a.erManueltOpprettet();
        var erOpprettetIFørsteSteg = erOpprettetIFørsteSteg(a, førsteSteg);
        var hensyntaÅpneOpprettetIFørste = erOpprettetIFørsteSteg && tilInngangFørsteSteg && a.erÅpentAksjonspunkt();
        var avbryt = !erManueltOpprettet && erFunnetIFørsteStegEllerSenere && (hensyntaÅpneOpprettetIFørste || !erOpprettetIFørsteSteg);
        return avbryt;
    }

    private boolean erOpprettetIFørsteSteg(Aksjonspunkt ap, BehandlingStegType førsteSteg) {
        return Objects.equals(førsteSteg, ap.getBehandlingStegFunnet());
    }

}
