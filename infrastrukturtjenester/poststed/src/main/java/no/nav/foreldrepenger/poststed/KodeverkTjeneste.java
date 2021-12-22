package no.nav.foreldrepenger.poststed;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.UriBuilder;
import javax.xml.datatype.XMLGregorianCalendar;

import no.nav.foreldrepenger.domene.tid.SimpleLocalDateInterval;
import no.nav.tjeneste.virksomhet.kodeverk.v2.HentKodeverkHentKodeverkKodeverkIkkeFunnet;
import no.nav.tjeneste.virksomhet.kodeverk.v2.informasjon.EnkeltKodeverk;
import no.nav.tjeneste.virksomhet.kodeverk.v2.informasjon.IdentifiserbarEntitet;
import no.nav.tjeneste.virksomhet.kodeverk.v2.informasjon.Kode;
import no.nav.tjeneste.virksomhet.kodeverk.v2.informasjon.Kodeverkselement;
import no.nav.tjeneste.virksomhet.kodeverk.v2.informasjon.Periode;
import no.nav.tjeneste.virksomhet.kodeverk.v2.informasjon.Term;
import no.nav.tjeneste.virksomhet.kodeverk.v2.meldinger.FinnKodeverkListeRequest;
import no.nav.tjeneste.virksomhet.kodeverk.v2.meldinger.FinnKodeverkListeResponse;
import no.nav.tjeneste.virksomhet.kodeverk.v2.meldinger.HentKodeverkRequest;
import no.nav.tjeneste.virksomhet.kodeverk.v2.meldinger.HentKodeverkResponse;
import no.nav.vedtak.exception.IntegrasjonException;
import no.nav.vedtak.felles.integrasjon.kodeverk.KodeverkConsumer;
import no.nav.vedtak.felles.integrasjon.rest.OidcRestClient;

@ApplicationScoped
public class KodeverkTjeneste {

    private static final String KODEVERK_URL = "http://kodeverk.org/";
    private static final String BOKMÅL = "nb";

    private KodeverkConsumer kodeverkConsumer;
    private OidcRestClient restClient;


    private static final String NORSK_BOKMÅL = "nb";

    KodeverkTjeneste() {
        // for CDI proxy
    }

    @Inject
    public KodeverkTjeneste(KodeverkConsumer kodeverkConsumer, OidcRestClient restClient) {
        this.kodeverkConsumer = kodeverkConsumer;
        this.restClient = restClient;
    }

    public Optional<KodeverkInfo> hentGjeldendeKodeverk(String kodeverk) {
        var request = new FinnKodeverkListeRequest();

        var response = kodeverkConsumer.finnKodeverkListe(request);
        if (response != null) {
            return Optional.ofNullable(oversettFraKodeverkListe(response, kodeverk));
        }
        return Optional.empty();
    }

    public Map<String, KodeverkBetydning> hentKodeverkBetydninger(String kodeverk) {
        var request = UriBuilder.fromUri(KODEVERK_URL)
            .path("/api/v1/kodeverk").path(kodeverk).path("/koder/betydninger")
            .queryParam("spraak", BOKMÅL)
            .build();
        var response = restClient.get(request, KodeverkBetydninger.class);

        Map<String, KodeverkBetydning> resultatMap = new LinkedHashMap<>();
        if (response != null) {
            response.betydninger().entrySet().forEach(entry -> {
                var ferskest = entry.getValue().stream().max(Comparator.comparing(KodeInnslag::gyldigFra));
                ferskest.ifPresent(f -> resultatMap.put(entry.getKey(), new KodeverkBetydning(f.gyldigFra(), f.gyldigTil(), f.beskrivelser().get(BOKMÅL).term())));
            });
        }
        return resultatMap;

    }

    private static KodeverkInfo oversettFraKodeverkListe(FinnKodeverkListeResponse response, String kodeverk) {
        return response.getKodeverkListe().stream()
                .filter(k -> kodeverk.equalsIgnoreCase(k.getNavn()))
                .map(k -> new KodeverkInfo.Builder()
                        .medNavn(k.getNavn())
                        .medVersjon(k.getVersjonsnummer())
                        .medVersjonDato(convertToLocalDate(k.getVersjoneringsdato()))
                        .build())
                .findFirst().orElse(null);
    }

    public Map<String, KodeverkKode> hentKodeverk(String kodeverkNavn, String kodeverkVersjon) {
        var request = new HentKodeverkRequest();
        request.setNavn(kodeverkNavn);
        request.setVersjonsnummer(kodeverkVersjon);

        Map<String, KodeverkKode> kodeverkKodeMap = Collections.emptyMap();
        try {
            var response = kodeverkConsumer.hentKodeverk(request);
            if (response != null) {
                kodeverkKodeMap = oversettFraHentKodeverkResponse(response);
            }
        } catch (HentKodeverkHentKodeverkKodeverkIkkeFunnet ex) {
            throw new IntegrasjonException("FP-868814", "Kodeverk ikke funnet " + kodeverkNavn + " - " + kodeverkVersjon);
        }
        return kodeverkKodeMap;
    }

    private Map<String, KodeverkKode> oversettFraHentKodeverkResponse(HentKodeverkResponse response) {
        if (response.getKodeverk() instanceof EnkeltKodeverk) {
            return ((EnkeltKodeverk) response.getKodeverk()).getKode().stream()
                    .map(KodeverkTjeneste::oversettFraKode)
                    .collect(Collectors.toMap(KodeverkKode::getKode, kodeverkKode -> kodeverkKode));
        }
        throw new IntegrasjonException("FP-402871", "Kodeverktype ikke støttet: "
            + response.getKodeverk().getClass().getSimpleName());
    }

    private static KodeverkKode oversettFraKode(Kode kode) {
        var gyldighetsperiode = finnGyldighetsperiode(kode.getGyldighetsperiode());
        var term = finnTerm(kode.getTerm());
        return new KodeverkKode.Builder()
                .medKode(kode.getNavn())
                .medNavn(term.orElse(null))
                .medGyldigFom(gyldighetsperiode.map(SimpleLocalDateInterval::getFomDato).orElse(null))
                .medGyldigTom(gyldighetsperiode.map(SimpleLocalDateInterval::getTomDato).orElse(null))
                .build();
    }

    /**
     * Finner term navnet med nyeste gyldighetsdato og angitt språk (default norsk
     * bokmål).
     */
    private static Optional<String> finnTerm(List<Term> termList) {
        Comparator<Kodeverkselement> vedGyldigFom = (e1, e2) -> e1.getGyldighetsperiode().get(0).getFom()
                .compare(e2.getGyldighetsperiode().get(0).getFom());
        var språk = NORSK_BOKMÅL;
        return termList.stream()
                .filter(term -> term.getSpraak().compareToIgnoreCase(språk) == 0)
                .max(vedGyldigFom)
                .map(IdentifiserbarEntitet::getNavn);
    }

    /**
     * Finner nyeste gyldighetsperiode ut fra fom dato.
     */
    private static Optional<SimpleLocalDateInterval> finnGyldighetsperiode(List<Periode> periodeList) {
        Comparator<Periode> vedGyldigFom = (p1, p2) -> p1.getFom().compare(p2.getFom());
        var periodeOptional = periodeList.stream().max(vedGyldigFom);
        return periodeOptional.map(periode -> SimpleLocalDateInterval.fraOgMedTomNotNull(convertToLocalDate(periode.getFom()),
                convertToLocalDate(periode.getTom())));
    }

    private static LocalDate convertToLocalDate(XMLGregorianCalendar xmlGregorianCalendar) {
        if (xmlGregorianCalendar == null) {
            return null;
        }
        return xmlGregorianCalendar.toGregorianCalendar().toZonedDateTime().toLocalDate();
    }

    private static record TermTekst(String term) {}
    private static record KodeInnslag(LocalDate gyldigFra, LocalDate gyldigTil, Map<String, TermTekst> beskrivelser) {}
    private static record KodeverkBetydninger(Map<String, List<KodeInnslag>> betydninger) {}

    public static record KodeverkBetydning(LocalDate gyldigFra, LocalDate gyldigTil, String term) {}
}
