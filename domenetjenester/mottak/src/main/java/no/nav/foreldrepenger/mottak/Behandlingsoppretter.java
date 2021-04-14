package no.nav.foreldrepenger.mottak;

import static java.util.stream.Collectors.toList;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRevurderingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingOpprettingTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.MottattDokumentPersisterer;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;

@Dependent
public class Behandlingsoppretter {

    private BehandlingRepository behandlingRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private BehandlingOpprettingTjeneste behandlingOpprettingTjeneste;
    private MottattDokumentPersisterer mottattDokumentPersisterer;
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    private BehandlingRevurderingRepository revurderingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private SøknadRepository søknadRepository;

    public Behandlingsoppretter() {
        // For CDI
    }

    @Inject
    public Behandlingsoppretter(BehandlingRepositoryProvider behandlingRepositoryProvider,
                                    BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                    BehandlingOpprettingTjeneste behandlingOpprettingTjeneste,
                                    MottattDokumentPersisterer mottattDokumentPersisterer,
                                    MottatteDokumentTjeneste mottatteDokumentTjeneste,
                                    BehandlendeEnhetTjeneste behandlendeEnhetTjeneste) { // NOSONAR
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.behandlingOpprettingTjeneste = behandlingOpprettingTjeneste;
        this.mottattDokumentPersisterer = mottattDokumentPersisterer;
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.mottatteDokumentTjeneste = mottatteDokumentTjeneste;
        this.mottatteDokumentRepository = behandlingRepositoryProvider.getMottatteDokumentRepository();
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
        this.revurderingRepository = behandlingRepositoryProvider.getBehandlingRevurderingRepository();
        this.behandlingsresultatRepository = behandlingRepositoryProvider.getBehandlingsresultatRepository();
        this.søknadRepository = behandlingRepositoryProvider.getSøknadRepository();
    }

    public boolean erKompletthetssjekkPassert(Behandling behandling) {
        return behandlingskontrollTjeneste.erStegPassert(behandling, BehandlingStegType.VURDER_KOMPLETTHET);
    }

    /**
     * Opprett og Oppdater under vil opprette behandling og kopiere grunnlag, men ikke opprette start/fortsett tasks.
     */
    public Behandling opprettFørstegangsbehandling(Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType, Optional<Behandling> tidligereBehandling) {
        if (!tidligereBehandling.map(Behandling::erSaksbehandlingAvsluttet).orElse(true)) {
            throw new IllegalStateException("Utviklerfeil: Prøver opprette ny behandling når det finnes åpen av samme type: " + fagsak.getId());
        }
        return behandlingOpprettingTjeneste.opprettBehandlingUtenHistorikk(fagsak, BehandlingType.FØRSTEGANGSSØKNAD, behandlingÅrsakType);
    }

    public Behandling opprettNyFørstegangsbehandlingMedImOgVedleggFraForrige(Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType, Behandling forrigeBehandling, boolean kopierGrunnlag) {
        var nyFørstegangsbehandling = opprettFørstegangsbehandling(fagsak, behandlingÅrsakType, Optional.ofNullable(forrigeBehandling));
        if (forrigeBehandling != null && kopierGrunnlag) {
            kopierTidligereGrunnlagFraTil(fagsak, forrigeBehandling, nyFørstegangsbehandling);
        }
        opprettInntektsmeldingerFraMottatteDokumentPåNyBehandling(nyFørstegangsbehandling);
        if (forrigeBehandling != null)
            kopierVedlegg(forrigeBehandling, nyFørstegangsbehandling);
        return nyFørstegangsbehandling;
    }

    public Behandling opprettRevurdering(Fagsak fagsak, BehandlingÅrsakType revurderingsÅrsak) {
        var revurderingTjeneste = FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, fagsak.getYtelseType()).orElseThrow();
        return revurderingTjeneste.opprettAutomatiskRevurdering(fagsak, revurderingsÅrsak, behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(fagsak));
    }

    public Behandling opprettRevurderingMultiÅrsak(Fagsak fagsak, List<BehandlingÅrsakType> revurderingsÅrsaker) {
        var revurderingTjeneste = FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, fagsak.getYtelseType()).orElseThrow();
        return revurderingTjeneste.opprettAutomatiskRevurderingMultiÅrsak(fagsak, revurderingsÅrsaker, behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(fagsak));
    }

    public Behandling opprettManuellRevurdering(Fagsak fagsak, BehandlingÅrsakType revurderingsÅrsak) {
        var revurderingTjeneste = FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, fagsak.getYtelseType()).orElseThrow();
        return revurderingTjeneste.opprettManuellRevurdering(fagsak, revurderingsÅrsak, behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(fagsak));
    }

    public Behandling oppdaterBehandlingViaHenleggelse(Behandling sisteYtelseBehandling) {
        var årsakstype = sisteYtelseBehandling.getBehandlingÅrsaker().stream()
            .map(BehandlingÅrsak::getBehandlingÅrsakType)
            .findFirst().orElse(BehandlingÅrsakType.UDEFINERT);
        return oppdaterBehandlingViaHenleggelse(sisteYtelseBehandling, årsakstype);
    }

    public Behandling oppdaterBehandlingViaHenleggelse(Behandling sisteYtelseBehandling, BehandlingÅrsakType revurderingsÅrsak) {
        // Ifm køhåndtering - kun relevant for Foreldrepenger. REGSØK har relevant logikk for FØRSTEGANG.
        // Må håndtere revurderinger med åpent aksjonspunkt: Kopier med siste papirsøknad hvis finnes så AP reutledes i REGSØK
        var uregistrertPapirSøknadFP = sisteYtelseBehandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.REGISTRER_PAPIR_ENDRINGSØKNAD_FORELDREPENGER);
        henleggBehandling(sisteYtelseBehandling);
        if (BehandlingType.FØRSTEGANGSSØKNAD.equals(sisteYtelseBehandling.getType())) {
            return opprettNyFørstegangsbehandlingMedImOgVedleggFraForrige(sisteYtelseBehandling.getFagsak(), revurderingsÅrsak, sisteYtelseBehandling,false);
        }
        var revurdering = opprettRevurdering(sisteYtelseBehandling.getFagsak(), revurderingsÅrsak);

        if (uregistrertPapirSøknadFP) {
            kopierPapirsøknadVedBehov(sisteYtelseBehandling, revurdering);
        }
        opprettInntektsmeldingerFraMottatteDokumentPåNyBehandling(revurdering);
        kopierVedlegg(sisteYtelseBehandling, revurdering);

        // Kopier behandlingsårsaker fra forrige behandling
        var forrigeÅrsaker = sisteYtelseBehandling.getBehandlingÅrsaker().stream()
            .map(BehandlingÅrsak::getBehandlingÅrsakType)
            .filter(bat -> !revurderingsÅrsak.equals(bat))
            .collect(Collectors.toList());
        if (!forrigeÅrsaker.isEmpty()) {
            var årsakBuilder = BehandlingÅrsak.builder(forrigeÅrsaker);
            revurdering.getOriginalBehandlingId().ifPresent(årsakBuilder::medOriginalBehandlingId);
            årsakBuilder.medManueltOpprettet(sisteYtelseBehandling.erManueltOpprettet()).buildFor(revurdering);
        }

        var nyKontekst = behandlingskontrollTjeneste.initBehandlingskontroll(revurdering);
        behandlingRepository.lagre(revurdering, nyKontekst.getSkriveLås());

        return revurdering;
    }

    public void henleggBehandling(Behandling behandling) {
        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling.getId());
        behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtførtForHenleggelse(behandling, kontekst);
        behandlingskontrollTjeneste.henleggBehandling(kontekst, BehandlingResultatType.MERGET_OG_HENLAGT);
    }

    public void opprettInntektsmeldingerFraMottatteDokumentPåNyBehandling(Behandling nyBehandling) {
            hentAlleInntektsmeldingdokumenter(nyBehandling.getFagsakId()).stream()
                .sorted(MottattDokumentSorterer.sorterMottattDokument())
                .forEach(mottattDokument ->
                    mottattDokumentPersisterer.persisterDokumentinnhold(mottattDokument, nyBehandling));

    }

    private void kopierPapirsøknadVedBehov(Behandling opprinneligBehandling, Behandling nyBehandling) {
        var søknad = mottatteDokumentTjeneste.hentMottatteDokumentFagsak(opprinneligBehandling.getFagsakId()).stream()
            .filter(MottattDokument::erSøknadsDokument)
            .max(Comparator.comparing(MottattDokument::getOpprettetTidspunkt))
            .filter(MottattDokument::erUstrukturertDokument);

        søknad.ifPresent(s -> {
            var dokument = new MottattDokument.Builder(s)
                .medBehandlingId(nyBehandling.getId())
                .build();
            mottatteDokumentRepository.lagre(dokument);
        });
    }

    private void kopierVedlegg(Behandling opprinneligBehandling, Behandling nyBehandling) {
        var vedlegg = mottatteDokumentTjeneste.hentMottatteDokumentVedlegg(opprinneligBehandling.getId());

        if (!vedlegg.isEmpty()) {
            vedlegg.forEach(vedlegget -> {
                var dokument = new MottattDokument.Builder(vedlegget)
                    .medBehandlingId(nyBehandling.getId())
                    .build();
                mottatteDokumentRepository.lagre(dokument);
            });
        }
    }

    private List<MottattDokument> hentAlleInntektsmeldingdokumenter(Long fagsakId) {
        return mottatteDokumentTjeneste.hentMottatteDokumentFagsak(fagsakId).stream()
            .filter(dok -> DokumentTypeId.INNTEKTSMELDING.equals(dok.getDokumentType()))
            .collect(toList());
    }


    public void settSomKøet(Behandling nyKøetBehandling) {
        behandlingskontrollTjeneste.settBehandlingPåVent(nyKøetBehandling, AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING, null, null, Venteårsak.VENT_ÅPEN_BEHANDLING);
    }

    public boolean harBehandlingsresultatOpphørt(Behandling behandling) {
        var behandlingsresultat = behandlingsresultatRepository.hentHvisEksisterer(behandling.getId());
        return behandlingsresultat.map(Behandlingsresultat::isBehandlingsresultatOpphørt).orElse(false);
    }

    public boolean erAvslåttBehandling(Behandling behandling) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandling.getId()).map(Behandlingsresultat::isBehandlingsresultatAvslått).orElse(false);
    }

    private void kopierTidligereGrunnlagFraTil(Fagsak fagsak, Behandling behandlingMedSøknad, Behandling nyBehandling) {
        var søknad = søknadRepository.hentSøknad(behandlingMedSøknad.getId());
        if (søknad != null) {
            søknadRepository.lagreOgFlush(nyBehandling, søknad);
        }
        var revurderingTjeneste = FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, fagsak.getYtelseType()).orElseThrow();
        revurderingTjeneste.kopierAlleGrunnlagFraTidligereBehandling(behandlingMedSøknad, nyBehandling);
    }

    public Behandling opprettNyFørstegangsbehandlingFraTidligereSøknad(Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType, Behandling behandlingMedSøknad) {
        var sisteYtelsesbehandling = revurderingRepository.hentSisteYtelsesbehandling(fagsak.getId());
        var harÅpenBehandling = !sisteYtelsesbehandling.map(Behandling::erSaksbehandlingAvsluttet).orElse(Boolean.TRUE);
        var behandling = harÅpenBehandling ? oppdaterBehandlingViaHenleggelse(sisteYtelsesbehandling.get(), behandlingÅrsakType)
            : opprettFørstegangsbehandling(fagsak, behandlingÅrsakType, Optional.of(behandlingMedSøknad));

        kopierTidligereGrunnlagFraTil(fagsak, behandlingMedSøknad, behandling);
        return behandling;
    }

    public boolean erBehandlingOgFørstegangsbehandlingHenlagt(Fagsak fagsak) {
        var behandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId());
        var behandlingsresultat = behandling.flatMap(b -> behandlingsresultatRepository.hentHvisEksisterer(b.getId()));
        if (behandlingsresultat.isPresent() && erHenlagt(behandlingsresultat.get())) {
            var førstegangsbehandlingBehandlingsresultat = hentFørstegangsbehandlingsresultat(fagsak);
            return førstegangsbehandlingBehandlingsresultat.map(br -> erHenlagt(br)).orElse(false);
        }
        return false;
    }

    private Optional<Behandlingsresultat> hentFørstegangsbehandlingsresultat(Fagsak fagsak) {
        var førstegangsbehandling = behandlingRepository.hentSisteBehandlingAvBehandlingTypeForFagsakId(
            fagsak.getId(), BehandlingType.FØRSTEGANGSSØKNAD);
        return førstegangsbehandling.flatMap(b -> behandlingsresultatRepository.hentHvisEksisterer(b.getId()));
    }

    private boolean erHenlagt(Behandlingsresultat br) {
        //Sjekker andre koder enn Behandlingsresultat.isBehandlingHenlagt()
        return BehandlingResultatType.getHenleggelseskoderForSøknad().contains(br.getBehandlingResultatType());
    }
}
