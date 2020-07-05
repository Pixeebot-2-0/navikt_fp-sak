package no.nav.foreldrepenger.behandling.steg.registrersøknad;

import static java.util.Collections.singletonList;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.REGISTRER_PAPIRSØKNAD_ENGANGSSTØNAD;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.REGISTRER_PAPIRSØKNAD_FORELDREPENGER;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.REGISTRER_PAPIRSØKNAD_SVANGERSKAPSPENGER;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.REGISTRER_PAPIR_ENDRINGSØKNAD_FORELDREPENGER;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.VENT_PÅ_SØKNAD;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.steg.iverksettevedtak.HenleggBehandlingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentKategori;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.kompletthet.KompletthetResultat;
import no.nav.foreldrepenger.kompletthet.Kompletthetsjekker;

@BehandlingStegRef(kode = "REGSØK")
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class RegistrerSøknadSteg implements BehandlingSteg {
    private static final Period VENT_PÅ_SØKNAD_PERIODE = Period.parse("P4W");
    private BehandlingRepository behandlingRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private HenleggBehandlingTjeneste henleggBehandlingTjeneste;

    RegistrerSøknadSteg() {
        // for CDI proxy
    }

    @Inject
    public RegistrerSøknadSteg(BehandlingRepositoryProvider behandlingRepositoryProvider,
                               HenleggBehandlingTjeneste henleggBehandlingTjeneste) {

        this.mottatteDokumentRepository = behandlingRepositoryProvider.getMottatteDokumentRepository();
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.henleggBehandlingTjeneste = henleggBehandlingTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        List<MottattDokument> mottatteDokumenterBehandling = mottatteDokumentRepository.hentMottatteDokument(kontekst.getBehandlingId());
        List<MottattDokument> alleDokumentSak = mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(kontekst.getFagsakId());
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());

        if (alleDokumentSak.isEmpty()) {
            // Behandlingen er startet uten noe dokument, f.eks. gjennom en forretningshendselse eller behov for utgående brev
            return resultatVedIngenMottatteDokument(behandling);
        }

        BehandlingReferanse ref = BehandlingReferanse.fra(behandling);

        Kompletthetsjekker kompletthetsjekker = BehandlingTypeRef.Lookup.find(Kompletthetsjekker.class, behandling.getFagsakYtelseType(), behandling.getType()).orElseThrow();
        KompletthetResultat søknadMottatt = kompletthetsjekker.vurderSøknadMottatt(ref);
        if (!søknadMottatt.erOppfylt()) {
            return evaluerSøknadMottattUoppfylt(behandling, søknadMottatt, VENT_PÅ_SØKNAD);
        }

        // OBS Dokumentmottak kan kopierere vedlegg fra tidligere behandlinger og disse er "nyere" enn søknad som trigger ny 1gang/revurdering
        MottattDokument nyesteSøknad = nyesteSøknad(mottatteDokumenterBehandling);
        if (nyesteSøknad == null && BehandlingType.FØRSTEGANGSSØKNAD.equals(behandling.getType())) {
            // Må dekke tilfellet der det tidligere papirsøknad er avslått på opplysningsplikt og det kommer ikke-søknad
            nyesteSøknad = nyesteSøknad(alleDokumentSak);
        }
        if (nyesteSøknad == null) {
            // Behandlingen er startet uten noe dokument, f.eks. gjennom en forretningshendselse
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        return resultatVedNySøknad(behandling, nyesteSøknad);
    }

    private BehandleStegResultat resultatVedNySøknad(Behandling behandling, MottattDokument nyesteSøknad) {
        if (!nyesteSøknad.erUstrukturertDokument()) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }
        FagsakYtelseType ytelseType = behandling.getFagsak().getYtelseType();
        if (FagsakYtelseType.ENGANGSTØNAD.equals(ytelseType) && erUstrukturertEngangsstønadSøknad(nyesteSøknad)) {
            return BehandleStegResultat.utførtMedAksjonspunkter(singletonList(REGISTRER_PAPIRSØKNAD_ENGANGSSTØNAD));
        }

        if (FagsakYtelseType.FORELDREPENGER.equals(ytelseType) && erUstrukturertEndringSøknad(nyesteSøknad)) {
            return BehandleStegResultat.utførtMedAksjonspunkter(singletonList(REGISTRER_PAPIR_ENDRINGSØKNAD_FORELDREPENGER));
        }

        if (FagsakYtelseType.FORELDREPENGER.equals(ytelseType) && erUstrukturertForeldrepengerSøknad(nyesteSøknad)) {
            //Hvis vi mottar en førstegangssøknad på papir etter første vedtak, så skal det tolkes som en endringssøknad
            if (behandling.erRevurdering()) {
                return BehandleStegResultat.utførtMedAksjonspunkter(singletonList(REGISTRER_PAPIR_ENDRINGSØKNAD_FORELDREPENGER));
            }
            return BehandleStegResultat.utførtMedAksjonspunkter(singletonList(REGISTRER_PAPIRSØKNAD_FORELDREPENGER));
        }

        if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(ytelseType) && erUstrukturertSvangerskapspengerSøknad(nyesteSøknad)) {
            return BehandleStegResultat.utførtMedAksjonspunkter(singletonList(REGISTRER_PAPIRSØKNAD_SVANGERSKAPSPENGER));
        }

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private MottattDokument nyesteSøknad(List<MottattDokument> mottatteDokumenter) {
        return mottatteDokumenter.stream()
            .filter(MottattDokument::erSøknadsDokument)
            .max(Comparator.comparing(MottattDokument::getOpprettetTidspunkt)) // Sist mottatte dokument
            .orElse(null);
    }

    private BehandleStegResultat resultatVedIngenMottatteDokument(Behandling behandling) {
        if (behandling.harBehandlingÅrsak(BehandlingÅrsakType.INFOBREV_BEHANDLING) || behandling.harBehandlingÅrsak(BehandlingÅrsakType.INFOBREV_OPPHOLD)) {
            KompletthetResultat kResultat = KompletthetResultat.ikkeOppfylt(LocalDate.now().plus(VENT_PÅ_SØKNAD_PERIODE).atStartOfDay(), Venteårsak.VENT_SØKNAD_SENDT_INFORMASJONSBREV);
            return evaluerSøknadMottattUoppfylt(behandling, kResultat, VENT_PÅ_SØKNAD);
        }
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private boolean erUstrukturertEndringSøknad(MottattDokument dokument) {
        DokumentTypeId dokumentTypeId = dokument.getDokumentType();
        return dokument.getPayloadXml() == null && dokumentTypeId != null && dokumentTypeId.erEndringsSøknadType();
    }

    private boolean erUstrukturertEngangsstønadSøknad(MottattDokument dokument) {
        DokumentTypeId dokumentTypeId = dokument.getDokumentType();
        return dokument.getPayloadXml() == null && (DokumentTypeId.SØKNAD_ENGANGSSTØNAD_ADOPSJON.equals(dokumentTypeId)
            || DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL.equals(dokumentTypeId) || DokumentKategori.SØKNAD.equals(dokument.getDokumentKategori()));
    }

    private boolean erUstrukturertForeldrepengerSøknad(MottattDokument dokument) {
        DokumentTypeId dokumentTypeId = dokument.getDokumentType();
        return dokument.getPayloadXml() == null &&
            (DokumentTypeId.SØKNAD_FORELDREPENGER_ADOPSJON.equals(dokumentTypeId)
                || DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL.equals(dokumentTypeId) || DokumentKategori.SØKNAD.equals(dokument.getDokumentKategori()));
    }

    private boolean erUstrukturertSvangerskapspengerSøknad(MottattDokument dokument) {
        DokumentTypeId dokumentTypeId = dokument.getDokumentType();
        return dokument.getPayloadXml() == null &&
            (DokumentTypeId.SØKNAD_SVANGERSKAPSPENGER.equals(dokumentTypeId) || DokumentKategori.SØKNAD.equals(dokument.getDokumentKategori()));
    }

    private boolean henleggBehandling(Behandling behandling) {
        return behandling.getAksjonspunkter().stream().anyMatch(a -> a.getAksjonspunktDefinisjon().equals(AksjonspunktDefinisjon.VENT_PÅ_SØKNAD) && a.getFristTid().isBefore(LocalDateTime.now()));
    }

    private BehandleStegResultat evaluerSøknadMottattUoppfylt(Behandling behandling, KompletthetResultat kompletthetResultat, AksjonspunktDefinisjon apDef) {
        if (henleggBehandling(behandling)) {
            henleggBehandlingTjeneste.lagHistorikkInnslagForHenleggelseFraSteg(behandling.getId(), BehandlingResultatType.HENLAGT_SØKNAD_MANGLER, null);
            return BehandleStegResultat.henlagtBehandling();
        }
        // TODO: Bestill brev (Venter på PK-50295)
        Venteårsak venteårsak = kompletthetResultat.getVenteårsak() != null ? kompletthetResultat.getVenteårsak() : Venteårsak.AVV_DOK;

        return BehandleStegResultat.utførtMedAksjonspunktResultater(Collections.singletonList(AksjonspunktResultat.opprettForAksjonspunktMedFrist(apDef, venteårsak, kompletthetResultat.getVentefrist())));
    }
}
