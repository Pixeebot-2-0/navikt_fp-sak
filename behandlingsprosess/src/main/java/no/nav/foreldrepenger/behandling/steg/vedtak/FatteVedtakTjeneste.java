package no.nav.foreldrepenger.behandling.steg.vedtak;

import static java.lang.Boolean.TRUE;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.lagretvedtak.LagretVedtak;
import no.nav.foreldrepenger.domene.vedtak.VedtakTjeneste;
import no.nav.foreldrepenger.domene.vedtak.impl.KlageAnkeVedtakTjeneste;
import no.nav.foreldrepenger.domene.vedtak.repo.LagretVedtakRepository;
import no.nav.foreldrepenger.domene.vedtak.xml.FatteVedtakXmlTjeneste;
import no.nav.foreldrepenger.historikk.OppgaveÅrsak;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveForBehandlingSendtTilbakeTask;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.TotrinnTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.Totrinnsvurdering;
import no.nav.vedtak.exception.TekniskException;

@ApplicationScoped
public class FatteVedtakTjeneste {

    public static final String UTVIKLER_FEIL_VEDTAK = "Utvikler-feil: Vedtak kan ikke fattes, behandlingsresultat er ";

    private static final AksjonspunktDefinisjon VENT_MERKNADER = AksjonspunktDefinisjon.AUTO_VENT_ANKE_MERKNADER_FRA_BRUKER;

    private static final Set<BehandlingResultatType> VEDTAKSTILSTANDER_REVURDERING = Set.of(BehandlingResultatType.AVSLÅTT,
            BehandlingResultatType.INNVILGET,
            BehandlingResultatType.OPPHØR, BehandlingResultatType.FORELDREPENGER_ENDRET, BehandlingResultatType.INGEN_ENDRING);
    private static final Set<BehandlingResultatType> VEDTAKSTILSTANDER = Set.of(BehandlingResultatType.AVSLÅTT, BehandlingResultatType.INNVILGET);
    private static final Set<BehandlingResultatType> VEDTAKSTILSTANDER_KLAGE = Set.of(BehandlingResultatType.KLAGE_AVVIST,
            BehandlingResultatType.KLAGE_MEDHOLD,
            BehandlingResultatType.KLAGE_YTELSESVEDTAK_OPPHEVET, BehandlingResultatType.KLAGE_YTELSESVEDTAK_STADFESTET,
            BehandlingResultatType.KLAGE_TILBAKEKREVING_VEDTAK_STADFESTET,
            BehandlingResultatType.HJEMSENDE_UTEN_OPPHEVE);
    private static final Set<BehandlingResultatType> VEDTAKSTILSTANDER_ANKE = Set.of(BehandlingResultatType.ANKE_AVVIST,
            BehandlingResultatType.ANKE_OMGJOER,
            BehandlingResultatType.ANKE_YTELSESVEDTAK_STADFESTET, BehandlingResultatType.ANKE_OPPHEVE_OG_HJEMSENDE,
            BehandlingResultatType.ANKE_HJEMSENDE_UTEN_OPPHEV);
    private static final Set<BehandlingResultatType> VEDTAKSTILSTANDER_INNSYN = Set.of(BehandlingResultatType.INNSYN_AVVIST,
            BehandlingResultatType.INNSYN_DELVIS_INNVILGET, BehandlingResultatType.INNSYN_INNVILGET);

    private static final Map<BehandlingType, Set<BehandlingResultatType>> LOVLIGE_RESULTAT = Map.ofEntries(
            Map.entry(BehandlingType.ANKE, VEDTAKSTILSTANDER_ANKE),
            Map.entry(BehandlingType.KLAGE, VEDTAKSTILSTANDER_KLAGE),
            Map.entry(BehandlingType.INNSYN, VEDTAKSTILSTANDER_INNSYN),
            Map.entry(BehandlingType.FØRSTEGANGSSØKNAD, VEDTAKSTILSTANDER),
            Map.entry(BehandlingType.REVURDERING, VEDTAKSTILSTANDER_REVURDERING));

    private LagretVedtakRepository lagretVedtakRepository;
    private FatteVedtakXmlTjeneste vedtakXmlTjeneste;
    private VedtakTjeneste vedtakTjeneste;
    private OppgaveTjeneste oppgaveTjeneste;
    private TotrinnTjeneste totrinnTjeneste;
    private BehandlingVedtakTjeneste behandlingVedtakTjeneste;
    private KlageAnkeVedtakTjeneste klageAnkeVedtakTjeneste;

    FatteVedtakTjeneste() {
        // for CDI proxy
    }

    @Inject
    public FatteVedtakTjeneste(LagretVedtakRepository vedtakRepository,
            KlageAnkeVedtakTjeneste klageAnkeVedtakTjeneste,
            FatteVedtakXmlTjeneste vedtakXmlTjeneste,
            VedtakTjeneste vedtakTjeneste,
            OppgaveTjeneste oppgaveTjeneste,
            TotrinnTjeneste totrinnTjeneste,
            BehandlingVedtakTjeneste behandlingVedtakTjeneste) {
        this.lagretVedtakRepository = vedtakRepository;
        this.klageAnkeVedtakTjeneste = klageAnkeVedtakTjeneste;
        this.vedtakXmlTjeneste = vedtakXmlTjeneste;
        this.vedtakTjeneste = vedtakTjeneste;
        this.oppgaveTjeneste = oppgaveTjeneste;
        this.totrinnTjeneste = totrinnTjeneste;
        this.behandlingVedtakTjeneste = behandlingVedtakTjeneste;
    }

    public BehandleStegResultat fattVedtak(BehandlingskontrollKontekst kontekst, Behandling behandling) {
        verifiserBehandlingsresultat(behandling);

        if (behandling.isToTrinnsBehandling()) {
            var totrinnaksjonspunktvurderinger = totrinnTjeneste.hentTotrinnaksjonspunktvurderinger(behandling);
            if (sendesTilbakeTilSaksbehandler(totrinnaksjonspunktvurderinger)) {
                oppgaveTjeneste.avsluttOppgaveOgStartTask(behandling, OppgaveÅrsak.GODKJENNE_VEDTAK,
                        OpprettOppgaveForBehandlingSendtTilbakeTask.TASKTYPE);
                var aksjonspunktDefinisjoner = totrinnaksjonspunktvurderinger.stream()
                        .filter(a -> !TRUE.equals(a.isGodkjent()))
                        .map(Totrinnsvurdering::getAksjonspunktDefinisjon).collect(Collectors.toList());

                return BehandleStegResultat.tilbakeførtMedAksjonspunkter(aksjonspunktDefinisjoner);
            }
            // Dette er spesialbehandling av Klage og Anke hos KA.
            // Hvis KAs medunderskriver(belsutter) godkjenner behandlingen må den fremdeles
            // tilbake til saksbehandler
            // så han/hun kan ferdigstille behandlingen derfor tilbakeføres behandlingen med
            // entrinn så vi ikke kommer hit igjen.
            if (klageAnkeVedtakTjeneste.erVurdertVedKlageinstans(behandling)
                    && skalTilbakeTilForeslåVedtakForKlageinstans(behandling)) {
                behandling.nullstillToTrinnsBehandling();
                // Spesialbehandling for KA også i FORVEDSTEG - hvis medunderskriver har sagt OK
                // gir den V_U_2trinn
                return BehandleStegResultat.tilbakeførtForeslåVedtak();
            }
            oppgaveTjeneste.opprettTaskAvsluttOppgave(behandling);
        } else {
            vedtakTjeneste.lagHistorikkinnslagFattVedtak(behandling);
        }

        behandlingVedtakTjeneste.opprettBehandlingVedtak(kontekst, behandling);

        opprettLagretVedtak(behandling);

        if (klageAnkeVedtakTjeneste.skalOversendesTrygdretten(behandling) && !behandling.harAksjonspunktMedType(VENT_MERKNADER)) {
            var apVent = AksjonspunktResultat.opprettForAksjonspunktMedFrist(VENT_MERKNADER, Venteårsak.ANKE_VENTER_PAA_MERKNADER_FRA_BRUKER,
                    LocalDateTime.now().plus(Objects.requireNonNull(VENT_MERKNADER.getFristPeriod())));
            return BehandleStegResultat.utførtMedAksjonspunktResultater(List.of(apVent));
        }

        // Ingen nye aksjonspunkt herfra
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private boolean skalTilbakeTilForeslåVedtakForKlageinstans(Behandling behandling) {
        var apForeslåMedTT = behandling.getAksjonspunktFor(AksjonspunktDefinisjon.FORESLÅ_VEDTAK.getKode()).orElse(null);
        var apForeslåUtenTT = behandling.getAksjonspunktFor(AksjonspunktDefinisjon.VEDTAK_UTEN_TOTRINNSKONTROLL.getKode()).orElse(null);
        if ((apForeslåMedTT == null) || (apForeslåMedTT.getEndretTidspunkt() == null)) {
            return false;
        }
        if ((apForeslåUtenTT == null) || (apForeslåUtenTT.getEndretTidspunkt() == null)) {
            return true;
        }
        return apForeslåMedTT.getEndretTidspunkt().isAfter(apForeslåUtenTT.getEndretTidspunkt());
    }

    private boolean sendesTilbakeTilSaksbehandler(Collection<Totrinnsvurdering> medTotrinnskontroll) {
        return medTotrinnskontroll.stream()
                .anyMatch(a -> !TRUE.equals(a.isGodkjent()));
    }

    private void verifiserBehandlingsresultat(Behandling behandling) { // NOSONAR dette er bare enkel verifisering og har ikke høy complexity
        var behandlingsresultat = behandlingVedtakTjeneste.getBehandlingsresultat(behandling.getId());
        if ((behandlingsresultat == null) || !LOVLIGE_RESULTAT.get(behandling.getType()).contains(behandlingsresultat.getBehandlingResultatType())) {
            var exString = UTVIKLER_FEIL_VEDTAK + (behandlingsresultat == null ? "null" : behandlingsresultat.getBehandlingResultatType().getNavn());
            throw new IllegalStateException(exString);
        }
    }

    private void opprettLagretVedtak(Behandling behandling) {
        if (BehandlingType.INNSYN.equals(behandling.getType())) {
            return;
        }
        if (!erKlarForVedtak(behandling)) {
            var msg = String.format("Vedtak-XML kan ikke utarbeides for behandling %s i tilstand %s", behandling.getId(),
                behandling.getStatus().getKode());
            throw new TekniskException("FP-142918", msg);
        }
        var lagretVedtak = LagretVedtak.builder()
                .medBehandlingId(behandling.getId())
                .medFagsakId(behandling.getFagsakId())
                .medXmlClob(vedtakXmlTjeneste.opprettVedtakXml(behandling.getId()))
                .build();
        lagretVedtakRepository.lagre(lagretVedtak);
    }

    private boolean erKlarForVedtak(Behandling behandling) {
        return BehandlingType.KLAGE.equals(behandling.getType()) || BehandlingType.ANKE.equals(behandling.getType())
                || BehandlingStatus.FATTER_VEDTAK.equals(behandling.getStatus());
    }

}
