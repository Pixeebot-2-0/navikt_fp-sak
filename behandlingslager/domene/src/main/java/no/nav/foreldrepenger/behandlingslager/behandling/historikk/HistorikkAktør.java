package no.nav.foreldrepenger.behandlingslager.behandling.historikk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum HistorikkAktør implements Kodeverdi {

    BESLUTTER("BESL", "Beslutter"),
    SAKSBEHANDLER("SBH", "Saksbehandler"),
    SØKER("SOKER", "Søker"),
    ARBEIDSGIVER("ARBEIDSGIVER", "Arbeidsgiver"),
    VEDTAKSLØSNINGEN("VL", "Vedtaksløsningen"),
    UDEFINERT("-", "Ikke definert"),
    ;

    private static final Map<String, HistorikkAktør> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "HISTORIKK_AKTOER";

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private final String navn;

    @JsonValue
    private final String kode;

    HistorikkAktør(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static Map<String, HistorikkAktør> kodeMap() {
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
    public static class KodeverdiConverter implements AttributeConverter<HistorikkAktør, String> {
        @Override
        public String convertToDatabaseColumn(HistorikkAktør attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public HistorikkAktør convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

        private static HistorikkAktør fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent HistorikkAktør: " + kode);
            }
            return ad;
        }
    }
}
