package no.nav.foreldrepenger.økonomistøtte.ny.util;

import java.util.Arrays;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik;

public class ØkonomiKodeKlassifikSortering {

    private static final List<String> SUFFIX_SORTERING = Arrays.asList(
        "ATORD", "ATAL", "ATFRI", "ATSJO",
        "SND-OP", "SNDDM-OP", "SNDJB-OP", "SNDFI",
        "REFAG-IOP",
        "FER", "FER-IOP",
        "ENFOD-OP", "ENAD-OP"
    );

    public static int getSorteringsplassering(KodeKlassifik kodeKlassifik) {
        for (var i = 0; i < SUFFIX_SORTERING.size(); i++) {
            if (kodeKlassifik.getKode().endsWith(SUFFIX_SORTERING.get(i))) {
                return i;
            }
        }
        throw new IllegalArgumentException("Ikke-definert sorteringsplassering for " + kodeKlassifik);
    }
}
