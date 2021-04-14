package no.nav.foreldrepenger.domene.tid;

import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.temporal.ChronoUnit.DAYS;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.chrono.ChronoLocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import no.nav.vedtak.konfig.Tid;

/**
 * Basis klasse for modellere et dato interval.
 */
public abstract class AbstractLocalDateInterval implements Comparable<AbstractLocalDateInterval>, Serializable {

    private static final LocalDate TIDENES_BEGYNNELSE = Tid.TIDENES_BEGYNNELSE;
    public static final LocalDate TIDENES_ENDE = Tid.TIDENES_ENDE;

    static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public abstract LocalDate getFomDato();

    public abstract LocalDate getTomDato();

    protected abstract AbstractLocalDateInterval lagNyPeriode(LocalDate fomDato, LocalDate tomDato);

    protected static LocalDate finnTomDato(LocalDate fom, int antallArbeidsdager) {
        if (antallArbeidsdager < 1) {
            throw new IllegalArgumentException("Antall arbeidsdager må være 1 eller større.");
        }
        var tom = fom;
        var antallArbeidsdagerTmp = antallArbeidsdager;

        while (antallArbeidsdagerTmp > 0) {
            if (antallArbeidsdagerTmp > antallArbeidsdager) {
                throw new IllegalArgumentException("Antall arbeidsdager beregnes feil.");
            }
            if (erArbeidsdag(tom)) {
                antallArbeidsdagerTmp--;
            }
            if (antallArbeidsdagerTmp > 0) {
                tom = tom.plusDays(1);
            }
        }
        return tom;
    }

    protected static LocalDate finnFomDato(LocalDate tom, int antallArbeidsdager) {
        if (antallArbeidsdager < 1) {
            throw new IllegalArgumentException("Antall arbeidsdager må være 1 eller større.");
        }
        var fom = tom;
        var antallArbeidsdagerTmp = antallArbeidsdager;

        while (antallArbeidsdagerTmp > 0) {
            if (antallArbeidsdagerTmp > antallArbeidsdager) {
                throw new IllegalArgumentException("Antall arbeidsdager beregnes feil.");
            }
            if (erArbeidsdag(fom)) {
                antallArbeidsdagerTmp--;
            }
            if (antallArbeidsdagerTmp > 0) {
                fom = fom.minusDays(1);
            }
        }
        return fom;
    }

    public boolean erFørEllerLikPeriodeslutt(ChronoLocalDate dato) {
        return getTomDato() == null || getTomDato().isAfter(dato) || getTomDato().isEqual(dato);
    }

    public boolean erEtterEllerLikPeriodestart(ChronoLocalDate dato) {
        return getFomDato().isBefore(dato) || getFomDato().isEqual(dato);
    }

    public boolean inkluderer(ChronoLocalDate dato) {
        return erEtterEllerLikPeriodestart(dato) && erFørEllerLikPeriodeslutt(dato);
    }

    public static LocalDate forrigeArbeidsdag(LocalDate dato) {
        if (dato == TIDENES_BEGYNNELSE || dato == TIDENES_ENDE) return dato;

        return switch (dato.getDayOfWeek()) {
            case SATURDAY -> dato.minusDays(1);
            case SUNDAY -> dato.minusDays(2);
            default -> dato;
        };
    }

    public static LocalDate nesteArbeidsdag(LocalDate dato) {
        if (dato == TIDENES_BEGYNNELSE || dato == TIDENES_ENDE) return dato;
        return switch (dato.getDayOfWeek()) {
            case SATURDAY -> dato.plusDays(2);
            case SUNDAY -> dato.plusDays(1);
            default -> dato;
        };
    }

    public long antallDager() {
        return DAYS.between(getFomDato(), getTomDato());
    }

    public boolean overlapper(AbstractLocalDateInterval other) {
        var fomBeforeOrEqual = this.getFomDato().isBefore(other.getTomDato()) || this.getFomDato().isEqual(other.getTomDato());
        var tomAfterOrEqual = this.getTomDato().isAfter(other.getFomDato()) || this.getTomDato().isEqual(other.getFomDato());
        var overlapper = fomBeforeOrEqual && tomAfterOrEqual;
        return overlapper;
    }

    public int antallArbeidsdager() {
        if (getTomDato().isEqual(TIDENES_ENDE)) {
            throw new IllegalStateException("Både fra og med og til og med dato må være satt for å regne ut arbeidsdager.");
        }
        return arbeidsdager().size();
    }

    public int maksAntallArbeidsdager() {
        if (getTomDato().isEqual(TIDENES_ENDE)) {
            throw new IllegalStateException("Både fra og med og til og med dato må være satt for å regne ut arbeidsdager.");
        }

        var månedsstart = getFomDato().minusDays(getFomDato().getDayOfMonth() - 1L);
        var månedsslutt = getTomDato().minusDays(getTomDato().getDayOfMonth() - 1L).plusDays(getTomDato().lengthOfMonth() - 1L);
        return listArbeidsdager(månedsstart, månedsslutt).size();
    }

    public List<LocalDate> arbeidsdager() {
        return listArbeidsdager(getFomDato(), getTomDato());
    }

    private static List<LocalDate> listArbeidsdager(LocalDate fomDato, LocalDate tomDato) { // NOSONAR
        List<LocalDate> arbeidsdager = new ArrayList<>();
        var dato = fomDato;
        while (!dato.isAfter(tomDato)) {
            if (erArbeidsdag(dato)) {
                arbeidsdager.add(dato);
            }
            dato = dato.plusDays(1L);
        }
        return arbeidsdager;
    }

    protected static boolean erArbeidsdag(LocalDate dato) {
        return !dato.getDayOfWeek().equals(SATURDAY) && !dato.getDayOfWeek().equals(SUNDAY); // NOSONAR
    }

    public boolean grenserTil(AbstractLocalDateInterval periode2) {
        return getTomDato().equals(periode2.getFomDato().minusDays(1)) || periode2.getTomDato().equals(getFomDato().minusDays(1));
    }

    public List<AbstractLocalDateInterval> splittVedMånedsgrenser() {
        List<AbstractLocalDateInterval> perioder = new ArrayList<>();

        var dato = getFomDato().minusDays(getFomDato().getDayOfMonth() - 1L);
        var periodeFomDato = getFomDato();

        while (dato.isBefore(getTomDato())) {
            var dagerIMåned = dato.lengthOfMonth();
            var sisteDagIMåneden = dato.plusDays(dagerIMåned - 1L);
            var harMånedsslutt = inkluderer(sisteDagIMåneden);
            if (harMånedsslutt) {
                perioder.add(lagNyPeriode(periodeFomDato, sisteDagIMåneden));
                dato = sisteDagIMåneden.plusDays(1);
                periodeFomDato = dato;
            } else {
                perioder.add(lagNyPeriode(periodeFomDato, getTomDato()));
                dato = getTomDato();
            }
        }

        return perioder;
    }

    public AbstractLocalDateInterval avgrensTilArbeidsdager() {
        var nyFomDato = nesteArbeidsdag(getFomDato());
        var nyTomDato = forrigeArbeidsdag(getTomDato());
        if (nyFomDato.equals(getFomDato()) && nyTomDato.equals(getTomDato())) {
            return this;
        }
        return lagNyPeriode(nyFomDato, nyTomDato);
    }

    @Override
    public int compareTo(AbstractLocalDateInterval periode) {
        return getFomDato().compareTo(periode.getFomDato());
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof AbstractLocalDateInterval)) {
            return false;
        }
        var annen = (AbstractLocalDateInterval) object;
        return likFom(annen) && likTom(annen);
    }

    private boolean likFom(AbstractLocalDateInterval annen) {
        var likFom = Objects.equals(this.getFomDato(), annen.getFomDato());
        if (this.getFomDato() == null || annen.getFomDato() == null) {
            return likFom;
        }
        return likFom
                || Objects.equals(nesteArbeidsdag(this.getFomDato()), nesteArbeidsdag(annen.getFomDato()));
    }

    private boolean likTom(AbstractLocalDateInterval annen) {
        var likTom = Objects.equals(getTomDato(), annen.getTomDato());
        if (this.getTomDato() == null || annen.getTomDato() == null) {
            return likTom;
        }
        return likTom
                || Objects.equals(forrigeArbeidsdag(this.getTomDato()), forrigeArbeidsdag(annen.getTomDato()));
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFomDato(), getTomDato());
    }

    @Override
    public String toString() {
        return String.format("Periode: %s - %s", getFomDato().format(FORMATTER), getTomDato().format(FORMATTER));
    }
}
