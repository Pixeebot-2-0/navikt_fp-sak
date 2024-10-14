package no.nav.foreldrepenger.behandling.revurdering;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingRevurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapVilkårPeriodeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

@ApplicationScoped
public class RevurderingTjenesteFelles {

    private static final Logger LOG = LoggerFactory.getLogger(RevurderingTjenesteFelles.class);

    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private BehandlingRevurderingTjeneste behandlingRevurderingTjeneste;
    private FagsakRevurdering fagsakRevurdering;
    private MedlemskapVilkårPeriodeRepository medlemskapVilkårPeriodeRepository;
    private OpptjeningRepository opptjeningRepository;
    private RevurderingHistorikk revurderingHistorikk;

    public RevurderingTjenesteFelles() {
        // for CDI proxy
    }

    @Inject
    public RevurderingTjenesteFelles(BehandlingRepositoryProvider repositoryProvider, BehandlingRevurderingTjeneste behandlingRevurderingTjeneste) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.behandlingRevurderingTjeneste = behandlingRevurderingTjeneste;
        this.fagsakRevurdering = new FagsakRevurdering(repositoryProvider.getBehandlingRepository());
        this.medlemskapVilkårPeriodeRepository = repositoryProvider.getMedlemskapVilkårPeriodeRepository();
        this.opptjeningRepository = repositoryProvider.getOpptjeningRepository();
        this.revurderingHistorikk = new RevurderingHistorikk(repositoryProvider.getHistorikkRepository());
    }

    public Behandling opprettRevurderingsbehandling(BehandlingÅrsakType revurderingsÅrsak, Behandling opprinneligBehandling,
                                                    boolean manueltOpprettet, OrganisasjonsEnhet enhet, String opprettetAv) {
        var behandlingType = BehandlingType.REVURDERING;
        var revurderingÅrsak = BehandlingÅrsak.builder(revurderingsÅrsak)
                .medOriginalBehandlingId(opprinneligBehandling.getId())
                .medManueltOpprettet(manueltOpprettet);
        if (BehandlingÅrsakType.BERØRT_BEHANDLING.equals(revurderingsÅrsak)
            && opprinneligBehandling.getFagsakYtelseType().equals(FagsakYtelseType.FORELDREPENGER)) {
            var basis = behandlingRevurderingTjeneste.finnSisteVedtatteIkkeHenlagteBehandlingForMedforelder(opprinneligBehandling.getFagsak())
                    .orElseThrow(() -> new IllegalStateException(
                            "Berørt behandling må ha en tilhørende avlsuttet behandling for medforelder - skal ikke skje"));
            var behandlingsresultat = behandlingsresultatRepository.hent(basis.getId());
            LOG.info("Revurderingtjeneste oppretter berørt pga id {} med resultat {}", basis.getId(),
                    behandlingsresultat.getBehandlingResultatType().getKode());
        }
        var revurdering = Behandling.fraTidligereBehandling(opprinneligBehandling, behandlingType)
                .medBehandlendeEnhet(enhet)
                .medBehandlingstidFrist(LocalDate.now().plusWeeks(behandlingType.getBehandlingstidFristUker()))
                .medBehandlingÅrsak(revurderingÅrsak).build();
        if (manueltOpprettet && opprettetAv != null) {
            revurdering.setAnsvarligSaksbehandler(opprettetAv);
        }
        return revurdering;
    }

    public void opprettHistorikkInnslagForNyRevurdering(Behandling revurdering, BehandlingÅrsakType revurderingsÅrsak, boolean manueltOpprettet) {
        revurderingHistorikk.opprettHistorikkinnslagOmRevurdering(revurdering, revurderingsÅrsak, manueltOpprettet);
    }

    public OppgittFordelingEntitet kopierOppgittFordelingFraForrigeBehandling(OppgittFordelingEntitet forrigeBehandlingFordeling) {
        var kopiertFordeling = forrigeBehandlingFordeling.getPerioder().stream()
                .map(periode -> OppgittPeriodeBuilder.fraEksisterende(periode).build())
                .toList();
        return new OppgittFordelingEntitet(kopiertFordeling, forrigeBehandlingFordeling.getErAnnenForelderInformert(),
            forrigeBehandlingFordeling.ønskerJustertVedFødsel());
    }

    public Boolean kanRevurderingOpprettes(Fagsak fagsak) {
        return fagsakRevurdering.kanRevurderingOpprettes(fagsak);
    }

    public void kopierVilkårsresultat(Behandling origBehandling, Behandling revurdering, BehandlingskontrollKontekst kontekst) {
        kopierVilkårsresultat(origBehandling, revurdering, kontekst, Set.of());
    }

    public void kopierVilkårsresultat(Behandling origBehandling, Behandling revurdering, BehandlingskontrollKontekst kontekst, Set<VilkårType> nullstilles) {
        var origVilkårResultat = behandlingsresultatRepository.hent(origBehandling.getId()).getVilkårResultat();
        Objects.requireNonNull(origVilkårResultat, "Vilkårsresultat må være satt på revurderingens originale behandling");

        var vilkårBuilder = VilkårResultat.builder();
        origVilkårResultat.getVilkårene().stream()
            .filter(v -> v.getVilkårType().erInngangsvilkår())
            .forEach(vilkår -> vilkårBuilder.kopierVilkårFraAnnenBehandling(vilkår, true, nullstilles.contains(vilkår.getVilkårType())));
        var vilkårResultat = vilkårBuilder.buildFor(revurdering);
        behandlingRepository.lagre(vilkårResultat, kontekst.getSkriveLås());
        behandlingRepository.lagre(revurdering, kontekst.getSkriveLås());

        // MedlemskapsvilkårPerioder er tilknyttet vilkårresultat, ikke behandling
        medlemskapVilkårPeriodeRepository.kopierGrunnlagFraEksisterendeBehandling(origBehandling, revurdering);

        // Kan være at førstegangsbehandling ble avslått før den har kommet til
        // opptjening.
        if (opptjeningRepository.finnOpptjening(origBehandling.getId()).isPresent()) {
            opptjeningRepository.kopierGrunnlagFraEksisterendeBehandling(origBehandling, revurdering);
        }
    }
}
