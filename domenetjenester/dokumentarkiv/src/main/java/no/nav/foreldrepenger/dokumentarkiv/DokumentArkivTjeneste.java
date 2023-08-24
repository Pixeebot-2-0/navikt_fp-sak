package no.nav.foreldrepenger.dokumentarkiv;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.VariantFormat;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.saf.*;
import no.nav.vedtak.felles.integrasjon.saf.HentDokumentQuery;
import no.nav.vedtak.felles.integrasjon.saf.Saf;
import no.nav.vedtak.util.LRUCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpHeaders;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static jakarta.ws.rs.core.HttpHeaders.CONTENT_DISPOSITION;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;

@ApplicationScoped
public class DokumentArkivTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(DokumentArkivTjeneste.class);

    private static final VariantFormat VARIANT_FORMAT_ARKIV = VariantFormat.ARKIV;
    private static final Set<Journalstatus> EKSKLUDER_STATUS = Set.of(Journalstatus.UTGAAR);

    private static final long CACHE_ELEMENT_LIVE_TIME_MS = TimeUnit.MILLISECONDS.convert(30, TimeUnit.SECONDS);
    private static final LRUCache<String, List<ArkivJournalPost>> SAK_JOURNAL_CACHE = new LRUCache<>(500, CACHE_ELEMENT_LIVE_TIME_MS);

    static final String DEFAULT_CONTENT_DISPOSITION_SAF = "filename=innhold.pdf";
    static final String DEFAULT_CONTENT_TYPE_SAF = "application/pdf";
    static final String FP_DOK_TYPE = "fp_innholdtype";

    private Saf safKlient;


    DokumentArkivTjeneste() {
        // for CDI proxy
    }

    @Inject
    public DokumentArkivTjeneste(Saf safTjeneste) {
        this.safKlient = safTjeneste;
    }

    public DokumentRespons hentDokument(JournalpostId journalpostId, String dokumentId) {
        var query = new HentDokumentQuery(journalpostId.getVerdi(), dokumentId, VARIANT_FORMAT_ARKIV.getOffisiellKode());
        var httpResponse = safKlient.hentDokumentResponse(query);
        return tilDokumentRespons(httpResponse.body(), httpResponse.headers());
    }

    static DokumentRespons tilDokumentRespons(byte[] innhold, HttpHeaders headers) {
        return new DokumentRespons(innhold,
                headers.firstValue(CONTENT_TYPE).orElse(DEFAULT_CONTENT_TYPE_SAF),
                headers.firstValue(CONTENT_DISPOSITION).orElse(DEFAULT_CONTENT_DISPOSITION_SAF));
    }

    public String hentStrukturertDokument(JournalpostId journalpostId, String dokumentId) {
        var query = new HentDokumentQuery(journalpostId.getVerdi(), dokumentId, VariantFormat.ORIGINAL.getOffisiellKode());
        return new String(safKlient.hentDokument(query));
    }

    public List<ArkivJournalPost> hentAlleDokumenterForVisning(Saksnummer saksnummer) {
        return hentAlleJournalposterForSak(saksnummer).stream()
            .map(this::kopiMedKunArkivdokument)
            .filter(Objects::nonNull)
            .toList();
    }

    private ArkivJournalPost kopiMedKunArkivdokument(ArkivJournalPost journalPost) {
        var hoved = Optional.ofNullable(journalPost.getHovedDokument()).filter(this::erDokumentArkiv);
        var andre = journalPost.getAndreDokument().stream()
            .filter(this::erDokumentArkiv)
            .toList();
        if (hoved.isEmpty() && andre.isEmpty()) return null;

        return ArkivJournalPost.Builder.ny()
            .medJournalpostId(journalPost.getJournalpostId())
            .medBeskrivelse(journalPost.getBeskrivelse())
            .medKommunikasjonsretning(journalPost.getKommunikasjonsretning())
            .medHoveddokument(hoved.orElse(null))
            .medAndreDokument(andre)
            .medTidspunkt(journalPost.getTidspunkt())
            .build();
    }

    private boolean erDokumentArkiv(ArkivDokument arkivDokument) {
        return arkivDokument.getTilgjengeligSom().contains(VARIANT_FORMAT_ARKIV);
    }

    public List<ArkivJournalPost> hentAlleJournalposterForSak(Saksnummer saksnummer) {
        var cached = SAK_JOURNAL_CACHE.get(saksnummer.getVerdi());
        if (cached != null && !cached.isEmpty()) {
            SAK_JOURNAL_CACHE.put(saksnummer.getVerdi(), cached);
            return cached;
        }
        var query = new DokumentoversiktFagsakQueryRequest();
        query.setFagsak(new FagsakInput(saksnummer.getVerdi(), Fagsystem.FPSAK.getOffisiellKode()));
        query.setFoerste(1000);

        var projection = new DokumentoversiktResponseProjection()
            .journalposter(standardJournalpostProjection());

        var resultat = safKlient.dokumentoversiktFagsak(query, projection);

        var journalposter = resultat.getJournalposter().stream()
            .filter(j -> j.getJournalstatus() == null || !EKSKLUDER_STATUS.contains(j.getJournalstatus()))
            .map(this::mapTilArkivJournalPost)
            .toList();

        SAK_JOURNAL_CACHE.put(saksnummer.getVerdi(), journalposter);
        return journalposter;
    }

    public Optional<ArkivJournalPost> hentJournalpostForSak(JournalpostId journalpostId) {
        var query = new JournalpostQueryRequest();
        query.setJournalpostId(journalpostId.getVerdi());

        var projection = standardJournalpostProjection();

        var resultat = safKlient.hentJournalpostInfo(query, projection);

        return Optional.ofNullable(resultat).map(this::mapTilArkivJournalPost);
    }

    public List<ArkivDokumentUtgående> hentAlleUtgåendeJournalposterForSak(Saksnummer saksnummer) {
        var query = new DokumentoversiktFagsakQueryRequest();
        query.setFagsak(new FagsakInput(saksnummer.getVerdi(), Fagsystem.FPSAK.getOffisiellKode()));
        query.setFoerste(1000);

        var projection = new DokumentoversiktResponseProjection()
            .journalposter(utgåendeProjection());

        var resultat = safKlient.dokumentoversiktFagsak(query, projection);

        return resultat.getJournalposter().stream()
            .filter(j -> j.getJournalstatus() == null || !EKSKLUDER_STATUS.contains(j.getJournalstatus()))
            .filter(j -> Journalposttype.U.equals(j.getJournalposttype()))
            .map(DokumentArkivTjeneste::mapTilArkivDokumentUtgående)
            .flatMap(Collection::stream)
            .toList();
    }

    public Optional<ArkivDokumentUtgående> hentUtgåendeJournalpostForSak(JournalpostId journalpostId) {
        var query = new JournalpostQueryRequest();
        query.setJournalpostId(journalpostId.getVerdi());

        var resultat = safKlient.hentJournalpostInfo(query, utgåendeProjection());

        return Optional.ofNullable(resultat)
            .map(DokumentArkivTjeneste::mapTilArkivDokumentUtgående).orElse(List.of()).stream().findFirst();
    }

    private static JournalpostResponseProjection utgåendeProjection() {
        return new JournalpostResponseProjection()
            .journalpostId()
            .tittel()
            .dokumenter(new DokumentInfoResponseProjection()
                .dokumentInfoId()
                .dokumentvarianter(new DokumentvariantResponseProjection().variantformat()));
    }

    private static List<ArkivDokumentUtgående> mapTilArkivDokumentUtgående(Journalpost journalpost) {
        return Optional.ofNullable(journalpost.getDokumenter()).orElse(List.of()).stream()
            .filter(d -> d.getDokumentvarianter().stream().filter(Objects::nonNull).anyMatch(v -> Variantformat.ARKIV.equals(v.getVariantformat())))
            .map(d -> mapTilArkivDokumentUtgående(journalpost, d))
            .toList();
    }

    private static ArkivDokumentUtgående mapTilArkivDokumentUtgående(Journalpost journalpost, DokumentInfo dokumentInfo) {
        return new ArkivDokumentUtgående(journalpost.getTittel(), new JournalpostId(journalpost.getJournalpostId()), dokumentInfo.getDokumentInfoId());
    }

    public Set<DokumentTypeId> hentDokumentTypeIdForSak(Saksnummer saksnummer, LocalDate mottattEtterDato) {
        Set<DokumentTypeId> dokumenttyper = new HashSet<>();
        if (LocalDate.MIN.equals(mottattEtterDato)) {
            dokumenttyper.addAll(hentAlleJournalposterForSak(saksnummer).stream()
                .filter(ajp -> Kommunikasjonsretning.INN.equals(ajp.getKommunikasjonsretning()))
                .flatMap(jp -> ekstraherJournalpostDTID(jp).stream())
                .collect(Collectors.toSet()));
        } else {
            dokumenttyper.addAll(hentAlleJournalposterForSak(saksnummer).stream()
                .filter(ajp -> Kommunikasjonsretning.INN.equals(ajp.getKommunikasjonsretning()))
                .filter(jpost -> jpost.getTidspunkt() != null && jpost.getTidspunkt().isAfter(mottattEtterDato.atStartOfDay()))
                .flatMap(jp -> ekstraherJournalpostDTID(jp).stream())
                .collect(Collectors.toSet()));
        }
        dokumenttyper.addAll(DokumentTypeId.ekvivalenter(dokumenttyper));
        return dokumenttyper;
    }

    void emptyCache(String key) {
        SAK_JOURNAL_CACHE.remove(key);
    }

    private Set<DokumentTypeId> ekstraherJournalpostDTID(ArkivJournalPost jpost) {
        Set<DokumentTypeId> alle = new HashSet<>();
        dokumentTypeFraTittel(jpost.getBeskrivelse()).ifPresent(alle::add);
        alle.addAll(ekstraherDokumentDTID(jpost.getHovedDokument()));
        jpost.getAndreDokument().forEach(dok -> alle.addAll(ekstraherDokumentDTID(dok)));
        return alle;
    }

    private Set<DokumentTypeId> ekstraherDokumentDTID(ArkivDokument dokument) {
        return Optional.ofNullable(dokument).map(ArkivDokument::getAlleDokumenttyper).orElse(Set.of());
    }

    private JournalpostResponseProjection standardJournalpostProjection() {
        return new JournalpostResponseProjection()
            .journalpostId()
            .journalposttype()
            .tittel()
            .journalstatus()
            .datoOpprettet()
            .tilleggsopplysninger(new TilleggsopplysningResponseProjection().nokkel().verdi())
            .dokumenter(new DokumentInfoResponseProjection()
                .dokumentInfoId()
                .tittel()
                .brevkode()
                .dokumentvarianter(new DokumentvariantResponseProjection().variantformat())
                .logiskeVedlegg(new LogiskVedleggResponseProjection().tittel()));
    }

    private ArkivJournalPost mapTilArkivJournalPost(Journalpost journalpost) {

        var dokumenter = journalpost.getDokumenter().stream()
            .map(this::mapTilArkivDokument)
            .toList();

        var doktypeFraTilleggsopplysning = Optional.ofNullable(journalpost.getTilleggsopplysninger()).orElse(List.of()).stream()
            .filter(to -> FP_DOK_TYPE.equals(to.getNokkel()))
            .map(to -> DokumentTypeId.finnForKodeverkEiersKode(to.getVerdi()))
            .collect(Collectors.toSet());
        var doktypeFraDokumenter = dokumenter.stream().map(ArkivDokument::getDokumentType).collect(Collectors.toSet());
        var alleTyper = new HashSet<>(doktypeFraDokumenter);
        alleTyper.addAll(doktypeFraTilleggsopplysning);
        if (!doktypeFraTilleggsopplysning.isEmpty() && !doktypeFraDokumenter.containsAll(doktypeFraTilleggsopplysning)) {
            LOG.info("DokArkivTjenest ulike dokumenttyper fra dokument {} fra tilleggsopplysning {}", doktypeFraDokumenter, doktypeFraTilleggsopplysning);
        } else if (doktypeFraTilleggsopplysning.isEmpty()) {
            LOG.info("DokArkivTjenest journalpost {} uten tilleggsopplysninger", journalpost.getJournalpostId());
        }
        var hoveddokumentType = utledHovedDokumentType(alleTyper);
        var hoveddokument = dokumenter.stream().filter(d -> hoveddokumentType.equals(d.getDokumentType())).findFirst();

        var builder = ArkivJournalPost.Builder.ny()
            .medJournalpostId(new JournalpostId(journalpost.getJournalpostId()))
            .medBeskrivelse(journalpost.getTittel())
            .medTidspunkt(journalpost.getDatoOpprettet() == null ? null : LocalDateTime.ofInstant(journalpost.getDatoOpprettet().toInstant(), ZoneId.systemDefault()))
            .medKommunikasjonsretning(Kommunikasjonsretning.fromKommunikasjonsretningCode(journalpost.getJournalposttype().name()));
        hoveddokument.ifPresent(builder::medHoveddokument);
        dokumenter.stream()
            .filter(d -> hoveddokument.map(hd -> !hd.getDokumentId().equals(d.getDokumentId())).orElse(true))
            .forEach(builder::leggTillVedlegg);
        return builder.build();
    }

    private ArkivDokument mapTilArkivDokument(DokumentInfo dokumentInfo) {
        var alleDokumenttyper = utledDokumentType(dokumentInfo);
        var varianter = dokumentInfo.getDokumentvarianter().stream()
            .filter(Objects::nonNull)
            .map(Dokumentvariant::getVariantformat)
            .map(Variantformat::name)
            .map(VariantFormat::finnForKodeverkEiersKode)
            .collect(Collectors.toSet());

        return ArkivDokument.Builder.ny()
            .medDokumentId(dokumentInfo.getDokumentInfoId())
            .medTittel(dokumentInfo.getTittel())
            .medVariantFormater(varianter)
            .medAlleDokumenttyper(alleDokumenttyper)
            .medDokumentTypeId(utledHovedDokumentType(alleDokumenttyper))
            .build();
    }

    private Set<DokumentTypeId> utledDokumentType(DokumentInfo dokumentInfo) {
        Set<NAVSkjema> allebrevkoder = new HashSet<>();
        allebrevkoder.add(NAVSkjema.fraTermNavn(dokumentInfo.getTittel()));
        dokumentInfo.getLogiskeVedlegg().forEach(v -> allebrevkoder.add(NAVSkjema.fraTermNavn(v.getTittel())));
        Optional.ofNullable(dokumentInfo.getBrevkode()).map(NAVSkjema::fraOffisiellKode).ifPresent(allebrevkoder::add);

        Set<DokumentTypeId> alletyper = new HashSet<>();
        alletyper.add(DokumentTypeId.finnForKodeverkEiersNavn(dokumentInfo.getTittel()));
        dokumentInfo.getLogiskeVedlegg().forEach(v -> alletyper.add(DokumentTypeId.finnForKodeverkEiersNavn(v.getTittel())));
        allebrevkoder.stream().filter(b -> !NAVSkjema.UDEFINERT.equals(b)).forEach(b -> alletyper.add(MapNAVSkjemaDokumentTypeId.mapBrevkode(b)));
        return alletyper;
    }

    private static DokumentTypeId utledHovedDokumentType(Set<DokumentTypeId> alleTyper) {
        int lavestrank = alleTyper.stream()
            .map(MapNAVSkjemaDokumentTypeId::dokumentTypeRank)
            .min(Comparator.naturalOrder()).orElse(MapNAVSkjemaDokumentTypeId.UDEF_RANK);
        if (lavestrank == MapNAVSkjemaDokumentTypeId.GEN_RANK) {
            return alleTyper.stream()
                .filter(t -> MapNAVSkjemaDokumentTypeId.dokumentTypeRank(t) == MapNAVSkjemaDokumentTypeId.GEN_RANK)
                .findFirst().orElse(DokumentTypeId.UDEFINERT);
        }
        return MapNAVSkjemaDokumentTypeId.dokumentTypeFromRank(lavestrank);
    }

    private Optional<DokumentTypeId> dokumentTypeFraTittel(String tittel) {
        return Optional.ofNullable(tittel).map(DokumentTypeId::finnForKodeverkEiersNavn);
    }
}
