package no.nav.foreldrepenger.økonomistøtte.dagytelse.oppdrag110;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeEndring;
import no.nav.foreldrepenger.økonomistøtte.Oppdragsmottaker;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.FinnMottakerInfoITilkjentYtelse;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.OppdragskontrollConstants;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.fp.OppdragInput;

class ØkonomiKodeEndringUtleder {

    private ØkonomiKodeEndringUtleder() {
        // skjul default constructor
    }

    static KodeEndring finnKodeEndring(OppdragInput behandlingInfo, Oppdragsmottaker mottaker, boolean erNyMottakerIEndring) {
        return mottaker.erBruker()
            ? finnKodeEndringForBruker(behandlingInfo, mottaker, erNyMottakerIEndring)
            : finnKodeEndringForArbeidsgiver(erNyMottakerIEndring);
    }

    private static KodeEndring finnKodeEndringForBruker(OppdragInput behandlingInfo, Oppdragsmottaker mottaker, boolean erNyMottakerIEndring) {
        if (erNyMottakerIEndring) {
            return OppdragskontrollConstants.KODE_ENDRING_NY;
        }
        if (mottaker.erStatusOpphør()) {
            return OppdragskontrollConstants.KODE_ENDRING_UENDRET;
        }
        return mottaker.erStatusEndret() && FinnMottakerInfoITilkjentYtelse.erBrukerMottakerIForrigeTilkjentYtelse(behandlingInfo)
            ? OppdragskontrollConstants.KODE_ENDRING_ENDRET
            : OppdragskontrollConstants.KODE_ENDRING_UENDRET;
    }

    private static KodeEndring finnKodeEndringForArbeidsgiver(boolean erNyMottakerIEndring) {
        return erNyMottakerIEndring
            ? OppdragskontrollConstants.KODE_ENDRING_NY
            : OppdragskontrollConstants.KODE_ENDRING_UENDRET;
    }
}
