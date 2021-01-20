package no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.opphør;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.økonomi.økonomistøtte.Oppdragsmottaker;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.KlassekodeUtleder;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.TidligereOppdragTjeneste;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.fp.OppdragInput;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.wrapper.TilkjentYtelseAndel;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.wrapper.TilkjentYtelsePeriode;

class VurderOpphørForYtelse {

    private static final Logger log = LoggerFactory.getLogger(VurderOpphørForYtelse.class);

    private VurderOpphørForYtelse() {
    }

    static boolean vurder(OppdragInput behandlingInfo, Oppdragslinje150 sisteOppdr150ForMottaker, Oppdragsmottaker mottaker) {
        log.info("Vurder for endringsdato: {}, siste linje150: {} og mottaker: {}", behandlingInfo.getEndringsdato(), sisteOppdr150ForMottaker.getDelytelseId() + " " + sisteOppdr150ForMottaker.getKodeStatusLinje(), mottaker.getIdMaskert());
        List<TilkjentYtelsePeriode> forrigeTilkjentYtelsePeriodeListe = behandlingInfo.getForrigeTilkjentYtelsePerioder();
        Optional<LocalDate> endringsdatoOpt = behandlingInfo.getEndringsdato();
        if (forrigeTilkjentYtelsePeriodeListe.isEmpty()) {
            return false;
        }
        if (endringsdatoOpt.isEmpty()) {
            return behandlingInfo.gjelderOpphør();
        }
        if (mottaker.erStatusOpphør()) {
            return true;
        }
        LocalDate endringsdato = endringsdatoOpt.get();
        var resultat = vurderOpphør(sisteOppdr150ForMottaker, mottaker, forrigeTilkjentYtelsePeriodeListe, endringsdato);
        log.info("Vurderingsresultat: {}", resultat);
        return resultat;
    }

    private static boolean vurderOpphør(Oppdragslinje150 sisteOppdr150ForMottaker, Oppdragsmottaker mottaker,
                                        List<TilkjentYtelsePeriode> forrigeTilkjentYtelsePeriodeListe, LocalDate endringsdato) {
        if (sisteOppdr150ForMottaker.gjelderOpphør()) {
            List<TilkjentYtelseAndel> forrigeTilkjentYtelseAndelListe = filtrerForKlassekode(
                TidligereOppdragTjeneste.finnAndelerIOppdragPerioder(mottaker, forrigeTilkjentYtelsePeriodeListe),
                sisteOppdr150ForMottaker.getKodeKlassifik());

            if (forrigeTilkjentYtelseAndelListe.isEmpty()) {
                return false;
            }
            return endringsdato.isBefore(sisteOppdr150ForMottaker.getDatoStatusFom());
        }
        return !endringsdato.isAfter(sisteOppdr150ForMottaker.getDatoVedtakTom());
    }

    private static List<TilkjentYtelseAndel> filtrerForKlassekode(List<TilkjentYtelseAndel> finnAndelerIOppdragPerioder, String kodeKlassifik) {
        return finnAndelerIOppdragPerioder.stream()
            .filter(andel -> KlassekodeUtleder.utled(andel).equals(kodeKlassifik))
            .collect(Collectors.toList());
    }
}
