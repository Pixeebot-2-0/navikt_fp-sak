package no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt;

import static no.nav.vedtak.feil.LogLevel.ERROR;
import static no.nav.vedtak.feil.LogLevel.INFO;
import static no.nav.vedtak.feil.LogLevel.WARN;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.mottak.dokumentmottak.SaksbehandlingDokumentmottakTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.FunksjonellFeil;

@ApplicationScoped
public class BehandlingsoppretterApplikasjonTjeneste {

    private BehandlingRepository behandlingRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private SaksbehandlingDokumentmottakTjeneste saksbehandlingDokumentmottakTjeneste;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;

    BehandlingsoppretterApplikasjonTjeneste() {
        // CDI
    }

    @Inject
    public BehandlingsoppretterApplikasjonTjeneste(BehandlingRepositoryProvider behandlingRepositoryProvider,
                                                       SaksbehandlingDokumentmottakTjeneste saksbehandlingDokumentmottakTjeneste,
                                                       BehandlendeEnhetTjeneste behandlendeEnhetTjeneste) {
        Objects.requireNonNull(behandlingRepositoryProvider, "behandlingRepositoryProvider");
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.mottatteDokumentRepository = behandlingRepositoryProvider.getMottatteDokumentRepository();
        this.saksbehandlingDokumentmottakTjeneste = saksbehandlingDokumentmottakTjeneste;
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
    }

    /** Opprett ny behandling. Returner Prosess Task gruppe for å ta den videre. */
    public void opprettNyFørstegangsbehandling(Long fagsakId, Saksnummer saksnummer, boolean erEtterKlageBehandling) {
        List<Behandling> behandlinger = behandlingRepository.hentAbsoluttAlleBehandlingerForSaksnummer(saksnummer);

        if (erLovÅOppretteNyBehandling(behandlinger)) {
            if (erEtterKlageBehandling && !erEtterKlageGyldigValg(fagsakId)) {
                throw BehandlingsoppretterApplikasjonTjenesteFeil.FACTORY.kanIkkeOppretteNyFørstegangsbehandlingEtterKlage(fagsakId).toException();
            }
            BehandlingÅrsakType behandlingÅrsakType = erEtterKlageBehandling ? BehandlingÅrsakType.ETTER_KLAGE : BehandlingÅrsakType.UDEFINERT;
            doOpprettNyBehandlingIgjennomMottak(fagsakId, behandlingÅrsakType);
        } else {
            throw BehandlingsoppretterApplikasjonTjenesteFeil.FACTORY.kanIkkeOppretteNyFørstegangsbehandling(fagsakId).toException();
        }
    }

    private void doOpprettNyBehandlingIgjennomMottak(Long fagsakId, BehandlingÅrsakType behandlingÅrsakType) {
        MottattDokument sisteMottatteSøknad = finnSisteMottatteSøknadPåFagsak(fagsakId);
        if (sisteMottatteSøknad != null) {
            opprettNyBehandlingFraSøknad(behandlingÅrsakType, sisteMottatteSøknad);
        } else {
            opprettNyBehandlingFraInntektsmelding(fagsakId, behandlingÅrsakType);
        }
    }

    private void opprettNyBehandlingFraSøknad(BehandlingÅrsakType behandlingÅrsakType, MottattDokument sisteMottatteSøknad) {
        if (sisteMottatteSøknad.getBehandlingId() != null) {
            if (sisteMottatteSøknad.getPayloadXml() == null) {
                // For å registrere papirsøknad på nytt ....
                var nyMottatt = new MottattDokument.Builder(sisteMottatteSøknad).build();
                saksbehandlingDokumentmottakTjeneste.dokumentAnkommet(nyMottatt, behandlingÅrsakType);
            } else {
                Behandling sisteBehandling = behandlingRepository.hentBehandling(sisteMottatteSøknad.getBehandlingId());
                saksbehandlingDokumentmottakTjeneste.opprettFraTidligereBehandling(sisteMottatteSøknad, sisteBehandling, behandlingÅrsakType);
            }
        } else {
            saksbehandlingDokumentmottakTjeneste.mottaUbehandletSøknad(sisteMottatteSøknad, behandlingÅrsakType);
        }
    }

    private void opprettNyBehandlingFraInntektsmelding(Long fagsakId, BehandlingÅrsakType behandlingÅrsakType) {
        MottattDokument sisteMottatteInntektsmelding = finnSisteMottatteInntektsmeldingPåFagsak(fagsakId);
        Optional<Behandling> sisteBehandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsakId);
        if (sisteMottatteInntektsmelding != null && sisteBehandling.isPresent()) {
            saksbehandlingDokumentmottakTjeneste.opprettFraTidligereBehandling(sisteMottatteInntektsmelding, sisteBehandling.get(), behandlingÅrsakType);
        } else {
            throw BehandlingsoppretterApplikasjonTjenesteFeil.FACTORY.ingenSøknadEllerImÅOppretteNyFørstegangsbehandlingPå(fagsakId).toException();
        }
    }

    public void henleggÅpenFørstegangsbehandlingOgOpprettNy(Long fagsakId, Saksnummer saksnummer) {
        List<Behandling> behandlinger = behandlingRepository.hentAbsoluttAlleBehandlingerForSaksnummer(saksnummer);

        if (harMinstEnÅpenFørstegangsbehandling(behandlinger)) {
            doOpprettNyBehandlingIgjennomMottak(fagsakId, BehandlingÅrsakType.UDEFINERT);
        } else {
            throw BehandlingsoppretterApplikasjonTjenesteFeil.FACTORY.kanIkkeHenleggeÅpenBehandlingOgOppretteNyFørstegangsbehandling(fagsakId).toException();
        }
    }

    public Behandling opprettRevurdering(Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {
        RevurderingTjeneste revurderingTjeneste = FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, fagsak.getYtelseType()).orElseThrow();
        Boolean kanRevurderingOpprettes = revurderingTjeneste.kanRevurderingOpprettes(fagsak);
        if (!kanRevurderingOpprettes) {
            throw BehandlingsoppretterApplikasjonTjenesteFeil.FACTORY.kanIkkeOppretteRevurdering(fagsak.getSaksnummer()).toException();
        }

        OrganisasjonsEnhet enhet = behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(fagsak);
        return revurderingTjeneste.opprettManuellRevurdering(fagsak, behandlingÅrsakType, enhet);
    }

    private MottattDokument finnSisteMottatteSøknadPåFagsak(Long fagsakId) {
        return mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(fagsakId).stream()
            .filter(md -> md.getDokumentType().erSøknadType())
            // Søknader lagret fra utfylte papirsøknader skal ikke hentes, altså hvis de ikke er elektronisk
            // registert og har payLoadXml
            .filter(md -> md.getElektroniskRegistrert() || md.getPayloadXml() == null)
            .max(Comparator.comparing(MottattDokument::getMottattDato).thenComparing(MottattDokument::getOpprettetTidspunkt))
            .orElse(null);
    }

    private MottattDokument finnSisteMottatteInntektsmeldingPåFagsak(Long fagsakId) {
        return mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(fagsakId).stream()
            .filter(md -> md.getDokumentType().erInntektsmelding())
            .max(Comparator.comparing(MottattDokument::getMottattDato).thenComparing(MottattDokument::getOpprettetTidspunkt))
            .orElse(null);
    }

    private boolean erLovÅOppretteNyBehandling(List<Behandling> behandlinger) {
        boolean ingenApenYtelsesBeh = behandlinger.stream()
            .noneMatch(b -> (b.getType().equals(BehandlingType.REVURDERING) && !b.erAvsluttet())
                || (b.getType().equals(BehandlingType.FØRSTEGANGSSØKNAD) && !b.erAvsluttet()));

        boolean minstEnAvsluttet = behandlinger.stream().anyMatch(Behandling::erAvsluttet);

        return ingenApenYtelsesBeh && minstEnAvsluttet;
    }

    private boolean harMinstEnÅpenFørstegangsbehandling(List<Behandling> behandlinger) {
        return behandlinger.stream()
            .anyMatch(b -> (b.getType().equals(BehandlingType.FØRSTEGANGSSØKNAD) && !b.erAvsluttet()));
    }

    private boolean erEtterKlageGyldigValg(Long fagsakId) {
        Behandling klage = behandlingRepository.hentSisteBehandlingAvBehandlingTypeForFagsakId(fagsakId, BehandlingType.KLAGE).orElse(null);

        // Vurdere å differensiere på KlageVurderingResultat (er tilstede) eller henlagt (resultat ikke tilstede)
        return klage != null && klage.erAvsluttet();
    }

    interface BehandlingsoppretterApplikasjonTjenesteFeil extends DeklarerteFeil {
        BehandlingsoppretterApplikasjonTjenesteFeil FACTORY = FeilFactory.create(BehandlingsoppretterApplikasjonTjenesteFeil.class); // NOSONAR

        @FunksjonellFeil(feilkode = "FP-663487", feilmelding = "Fagsak med saksnummer %s oppfyller ikke kravene for revurdering", løsningsforslag = "", logLevel = INFO)
        Feil kanIkkeOppretteRevurdering(Saksnummer saksnummer);

        @FunksjonellFeil(feilkode = "FP-909861", feilmelding = "Det eksisterer allerede en åpen ytelsesbehandling eller det eksisterer ingen avsluttede behandlinger for fagsakId %s", løsningsforslag = "", logLevel = ERROR)
        Feil kanIkkeOppretteNyFørstegangsbehandling(Long fagsakId);

        @FunksjonellFeil(feilkode = "FP-102451", feilmelding = "Det finnes ikke en åpen ytelsesbehandling som kan henlegges for fagsakId %s", løsningsforslag = "", logLevel = WARN)
        Feil kanIkkeHenleggeÅpenBehandlingOgOppretteNyFørstegangsbehandling(Long fagsakId);

        @FunksjonellFeil(feilkode = "FP-909862", feilmelding = "Det eksisterer ingen avsluttede klagebehandlinger for fagsakId %s", løsningsforslag = "", logLevel = ERROR)
        Feil kanIkkeOppretteNyFørstegangsbehandlingEtterKlage(Long fagsakId);

        @FunksjonellFeil(feilkode = "FP-287882", feilmelding = "FagsakId %s har ingen mottatte søknader eller inntektsmeldinger som kan brukes til å opprette ny førstegangsbehandling", løsningsforslag = "", logLevel = ERROR)
        Feil ingenSøknadEllerImÅOppretteNyFørstegangsbehandlingPå(Long fagsakId);

        @FunksjonellFeil(feilkode = "FP-066870", feilmelding = "Det eksisterer ingen avsluttede behandlinger for fagsakId %s", løsningsforslag = "", logLevel = INFO)
        Feil kanIkkeOppretteKlage(Long fagsakId);

        @FunksjonellFeil(feilkode = "FP-780304", feilmelding = "Det eksisterer ingen avsluttede behandlinger for fagsakId %s", løsningsforslag = "", logLevel = INFO)
        Feil kanIkkeOppretteAnke(Long fagsakId);
    }
}
