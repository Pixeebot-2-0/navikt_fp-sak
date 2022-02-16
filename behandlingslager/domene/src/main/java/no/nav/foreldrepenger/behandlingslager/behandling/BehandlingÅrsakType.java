package no.nav.foreldrepenger.behandlingslager.behandling;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.behandlingslager.kodeverk.TempAvledeKode;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum BehandlingÅrsakType implements Kodeverdi {

    // MANUELL OPPRETTING - GUI-anvendelse
    RE_FEIL_I_LOVANDVENDELSE("RE-LOV", "Lovanvendelse"),
    RE_FEIL_REGELVERKSFORSTÅELSE("RE-RGLF", "Regelverksforståelse"),
    RE_FEIL_ELLER_ENDRET_FAKTA("RE-FEFAKTA", "Endrede opplysninger"),
    RE_FEIL_PROSESSUELL("RE-PRSSL", "Prosessuell feil"),
    RE_ANNET("RE-ANNET", "Annet"),

    RE_OPPLYSNINGER_OM_MEDLEMSKAP("RE-MDL", "Opplysninger medlemskap"),
    RE_OPPLYSNINGER_OM_OPPTJENING("RE-OPTJ", "Opplysninger opptjening"),
    RE_OPPLYSNINGER_OM_FORDELING("RE-FRDLING", "Opplysninger uttak"),
    RE_OPPLYSNINGER_OM_INNTEKT("RE-INNTK", "Opplysninger inntekt"),
    RE_OPPLYSNINGER_OM_FØDSEL("RE-FØDSEL", "Fødsel"),
    RE_OPPLYSNINGER_OM_DØD("RE-DØD", "Opplysninger død"),
    RE_OPPLYSNINGER_OM_SØKERS_REL("RE-SRTB", "Opplysninger relasjon/barn"),
    RE_OPPLYSNINGER_OM_SØKNAD_FRIST("RE-FRIST", "Opplysninger søknadsfrist"),
    RE_OPPLYSNINGER_OM_BEREGNINGSGRUNNLAG("RE-BER-GRUN", "Opplysninger beregning"),

    // KLAGE - Manuelt opprettet revurdering (obs: årsakene kan også bli satt på en automatisk opprettet revurdering)
    RE_KLAGE_UTEN_END_INNTEKT("RE-KLAG-U-INNTK", "Klage/Anke uendret inntekt"),
    RE_KLAGE_MED_END_INNTEKT("RE-KLAG-M-INNTK", "Klage/Anke endret inntekt"),
    ETTER_KLAGE("ETTER_KLAGE", "Klage/Anke"),

    // Etterkontroll + funksjonell
    RE_MANGLER_FØDSEL("RE-MF", "Mangler fødselsregistrering"),
    RE_MANGLER_FØDSEL_I_PERIODE("RE-MFIP", "Mangler fødselsreg. u26-29"),
    RE_AVVIK_ANTALL_BARN("RE-AVAB", "Avvik antall barn"),

    // Mottak
    RE_ENDRING_FRA_BRUKER("RE-END-FRA-BRUKER", "Søknad"),
    RE_ENDRET_INNTEKTSMELDING("RE-END-INNTEKTSMELD", "Inntektsmelding"),

    // Tekniske behandlinger som skal spesialbehandles i prosessen
    BERØRT_BEHANDLING("BERØRT-BEHANDLING", "Berørt behandling"),
    REBEREGN_FERIEPENGER("REBEREGN-FERIEPENGER", "Omfordel feriepenger"),
    RE_UTSATT_START("RE-UTSATT-START", "Senere startdato"),

    // G-regulering
    RE_SATS_REGULERING("RE-SATS-REGULERING", "Regulering grunnbeløp"),

    //For automatiske informasjonsbrev
    INFOBREV_BEHANDLING("INFOBREV_BEHANDLING", "Informasjonsbrev uttak"),
    INFOBREV_OPPHOLD("INFOBREV_OPPHOLD", "Informasjonsbrev opphold"),
    INFOBREV_PÅMINNELSE("INFOBREV_PÅMINNELSE", "Informasjonsbrev påminnelse"),

    //For å vurdere opphør av ytelse
    OPPHØR_YTELSE_NYTT_BARN("OPPHØR-NYTT-BARN", "Nytt barn/stønadsperiode"),

    // Hendelser
    RE_HENDELSE_FØDSEL("RE-HENDELSE-FØDSEL", "Fødselsmelding"),
    RE_HENDELSE_DØD_FORELDER("RE-HENDELSE-DØD-F", "Forelder død"),
    RE_HENDELSE_DØD_BARN("RE-HENDELSE-DØD-B", "Barn død"),
    RE_HENDELSE_DØDFØDSEL("RE-HENDELSE-DØDFØD", "Dødfødsel"),
    RE_HENDELSE_UTFLYTTING("RE-HENDELSE-UTFLYTTING", "Utflytting"),

    RE_VEDTAK_PLEIEPENGER("RE-VEDTAK-PSB", "Pleiepenger"),


    // UTGÅTT. men ikke slett - BehandlingÅrsak-tabellen er rensket og ikke bruk disse som nye behandlingsårsaker!
    // Det ligger en hel del i historikk-innslag (inntil evt konvertert) og de brukes til vise tekst frontend.
    // OPPLYSNINGER_OM_YTELSER brukes til å lage nye historikkinnslag - de øvrige er historiske

    @Deprecated // Registeroppdatering. 49450 forekomster i HISTORIKKINNSLAG_FELT
    RE_OPPLYSNINGER_OM_YTELSER("RE-YTELSE", "Opplysninger annen ytelse"),

    @Deprecated // Registeroppdatering. 1 forekomst i HISTORIKKINNSLAG_FELT
    RE_REGISTEROPPLYSNING("RE-REGISTEROPPL", "Nye registeropplysninger"),
    @Deprecated // Køing - i tillegg til aksjonspunkt. 532 forekomster i HISTORIKKINNSLAG_FELT
    KØET_BEHANDLING("KØET-BEHANDLING", "Køet behandling"),
    @Deprecated // Infotrygd hendelse-feed. 2 forekomster i HISTORIKKINNSLAG_FELT
    RE_TILSTØTENDE_YTELSE_INNVILGET("RE-TILST-YT-INNVIL", "Annen ytelse innvilget"),
    @Deprecated // Infotrygd hendelse-feed. 2 forekomster i HISTORIKKINNSLAG_FELT
    RE_TILSTØTENDE_YTELSE_OPPHØRT("RE-TILST-YT-OPPH", "Annen ytelse opphørt"),

    // La stå
    UDEFINERT("-", "Ikke definert"),

    ;

    public static final String KODEVERK = "BEHANDLING_AARSAK"; //$NON-NLS-1$

    private static final Set<BehandlingÅrsakType> SPESIELLE_BEHANDLINGER = Set.of(BehandlingÅrsakType.BERØRT_BEHANDLING,
        BehandlingÅrsakType.REBEREGN_FERIEPENGER, BehandlingÅrsakType.RE_UTSATT_START);

    private static final Map<String, BehandlingÅrsakType> KODER = new LinkedHashMap<>();

    @JsonIgnore
    private String navn;

    private String kode;

    BehandlingÅrsakType(String kode) {
        this.kode = kode;
    }

    BehandlingÅrsakType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static BehandlingÅrsakType fraKode(@JsonProperty(value = "kode") Object node) {
        if (node == null) {
            return null;
        }
        var kode = TempAvledeKode.getVerdi(BehandlingÅrsakType.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent BehandlingÅrsakType: " + kode);
        }
        return ad;
    }

    public static Map<String, BehandlingÅrsakType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @JsonProperty
    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @JsonProperty
    @Override
    public String getKode() {
        return kode;
    }

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<BehandlingÅrsakType, String> {
        @Override
        public String convertToDatabaseColumn(BehandlingÅrsakType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public BehandlingÅrsakType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }

    public static Set<BehandlingÅrsakType> årsakerForAutomatiskRevurdering() {
        return Set.of(RE_MANGLER_FØDSEL, RE_MANGLER_FØDSEL_I_PERIODE, RE_AVVIK_ANTALL_BARN);
    }

    public static Set<BehandlingÅrsakType> årsakerForEtterkontroll() {
        return Set.of(RE_MANGLER_FØDSEL, RE_MANGLER_FØDSEL_I_PERIODE, RE_AVVIK_ANTALL_BARN);
    }

    public static Set<BehandlingÅrsakType> årsakerEtterKlageBehandling() {
        return Set.of(ETTER_KLAGE, RE_KLAGE_MED_END_INNTEKT, RE_KLAGE_UTEN_END_INNTEKT);
    }

    public static Set<BehandlingÅrsakType> årsakerRelatertTilDød() {
        return Set.of(RE_OPPLYSNINGER_OM_DØD, RE_HENDELSE_DØD_BARN, RE_HENDELSE_DØD_FORELDER, RE_HENDELSE_DØDFØDSEL);
    }

    public static Set<BehandlingÅrsakType> årsakerForRelatertVedtak() {
        return Set.of(BERØRT_BEHANDLING, REBEREGN_FERIEPENGER);
    }

    public static Set<BehandlingÅrsakType> alleTekniskeÅrsaker() {
        return SPESIELLE_BEHANDLINGER;
    }
}
