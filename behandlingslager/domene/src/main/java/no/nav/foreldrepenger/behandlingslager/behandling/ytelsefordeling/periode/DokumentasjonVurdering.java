package no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;


public enum DokumentasjonVurdering implements Kodeverdi {

    ///Både utsettelse
    SYKDOM_SØKER_GODKJENT("SYKDOM_SØKER_GODKJENT", "Søker er syk"),
    SYKDOM_SØKER_IKKE_GODKJENT("SYKDOM_SØKER_IKKE_GODKJENT", "Søker er ikke syk"),
    INNLEGGELSE_SØKER_GODKJENT("INNLEGGELSE_SØKER_GODKJENT", "Søker er innlagt"),
    INNLEGGELSE_SØKER_IKKE_GODKJENT("INNLEGGELSE_SØKER_IKKE_GODKJENT", "Søker er ikke innlag"),
    INNLEGGELSE_BARN_GODKJENT("INNLEGGELSE_BARN_GODKJENT", "Barn er innlagt"),
    INNLEGGELSE_BARN_IKKE_GODKJENT("INNLEGGELSE_BARN_IKKE_GODKJENT", "Barn er ikke innlagt"),
    HV_OVELSE_GODKJENT("HV_OVELSE_GODKJENT", "Søker er på hv øvelse"),
    HV_OVELSE_IKKE_GODKJENT("HV_OVELSE_IKKE_GODKJENT", "Søker er ikke på hv øvelse"),
    NAV_TILTAK_GODKJENT("NAV_TILTAK_GODKJENT", "Søker er i tiltak i regi nav NAV"),
    NAV_TILTAK_IKKE_GODKJENT("NAV_TILTAK_IKKE_GODKJENT", "Søker er ikke i tiltak i regi nav NAV"),

    //Aktivitetskrav
    MORS_AKTIVITET_GODKJENT("MORS_AKTIVITET_GODKJENT", "Mors aktivitet er godkjent"),
    MORS_AKTIVITET_IKKE_GODKJENT("MORS_AKTIVITET_IKKE_GODKJENT", "Mors aktivitet er ikke godkjent"),
    MORS_AKTIVITET_IKKE_DOKUMENTERT("MORS_AKTIVITET_IKKE_DOKUMENTERT", "Mors aktivitet er ikke dokumentert"),

    //Tidlig oppstart
    TIDLIG_OPPSTART_FEDREKVOTE_GODKJENT("TIDLIG_OPPSTART_FEDREKVOTE_GODKJENT", "Far kan starte tidlig fedrekvote, pga mor er syk eller innlagt"),
    TIDLIG_OPPSTART_FEDREKVOTE_IKKE_GODKJENT("TIDLIG_OPPSTART_FEDREKVOTE_IKKE_GODKJENT", "Far kan ikke starte tidlig fedrekvotet"),

    //Overføring
    INNLEGGELSE_ANNEN_FORELDER_GODKJENT("INNLEGGELSE_ANNEN_FORELDER_GODKJENT", "Annen forelder er innlagt"),
    INNLEGGELSE_ANNEN_FORELDER_IKKE_GODKJENT("INNLEGGELSE_ANNEN_FORELDER_IKKE_GODKJENT", "Annen forelder er ikke innlagt"),
    SYKDOM_ANNEN_FORELDER_GODKJENT("SYKDOM_ANNEN_FORELDER_GODKJENT", "Annen forelder er syk"),
    SYKDOM_ANNEN_FORELDER_IKKE_GODKJENT("SYKDOM_ANNEN_FORELDER_IKKE_GODKJENT", "Annen forelder er ikke syk"),
    ALENEOMSORG_GODKJENT("ALENEOMSORG_GODKJENT", "Søker har aleneomsorg"),
    ALENEOMSORG_IKKE_GODKJENT("ALENEOMSORG_IKKE_GODKJENT", "Søker har ikke aleneomsorg"),
    BARE_SØKER_RETT_GODKJENT("BARE_SØKER_RETT_GODKJENT", "Bare søker har rett"),
    BARE_SØKER_RETT_IKKE_GODKJENT("BARE_SØKER_RETT_IKKE_GODKJENT", "Søker er ikke neste som har rett"),
    ;

    private static final Map<String, DokumentasjonVurdering> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "DOKUMENTASJON_VURDERING";

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private String navn;
    @JsonValue
    private String kode;

    DokumentasjonVurdering(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static Map<String, DokumentasjonVurdering> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @Override
    public String getKode() {
        return kode;
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<DokumentasjonVurdering, String> {
        @Override
        public String convertToDatabaseColumn(DokumentasjonVurdering attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public DokumentasjonVurdering convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

        private static DokumentasjonVurdering fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent Dokumentasjonsvurdering: " + kode);
            }
            return ad;
        }
    }
}