package no.nav.foreldrepenger.økonomi.ny.tjeneste;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.økonomi.ny.domene.DelytelseId;
import no.nav.foreldrepenger.økonomi.ny.domene.FagsystemId;
import no.nav.foreldrepenger.økonomi.ny.domene.OppdragKjede;
import no.nav.foreldrepenger.økonomi.ny.domene.OppdragKjedeFortsettelse;
import no.nav.foreldrepenger.økonomi.ny.domene.OppdragLinje;
import no.nav.foreldrepenger.økonomi.ny.domene.Periode;
import no.nav.foreldrepenger.økonomi.ny.domene.Sats;
import no.nav.foreldrepenger.økonomi.ny.domene.Ytelse;
import no.nav.foreldrepenger.økonomi.ny.domene.YtelsePeriode;

public class OppdragKjedeFactoryTest {

    LocalDate nå = LocalDate.now();
    Periode p1 = Periode.of(nå, nå.plusDays(5));
    Periode p2 = Periode.of(nå.plusDays(6), nå.plusDays(10));
    Periode p3 = Periode.of(nå.plusDays(11), nå.plusDays(11));

    Periode p2del1 = Periode.of(p2.getFom(), p2.getFom());
    Periode p2del2 = Periode.of(p2.getFom().plusDays(1), p2.getTom());

    Periode mai = Periode.of(nå.withMonth(5).withDayOfMonth(1), nå.withMonth(5).withDayOfMonth(31));

    FagsystemId fagsystemId = FagsystemId.parse("FOO-1");


    @Test
    public void skal_ikke_lage_kjede_når_førstegangsvedtak_er_tomt() {
        OppdragKjede tidligereOppdrag = OppdragKjede.builder().build();
        Ytelse nyYtelse = Ytelse.builder().build();
        OppdragKjedeFactory factory = OppdragKjedeFactory.lagForNyMottaker(fagsystemId);
        OppdragKjedeFortsettelse resultat = factory.lagOppdragskjedeForYtelse(tidligereOppdrag, nyYtelse);
        assertThat(resultat).isNull();
    }

    @Test
    public void skal_kjede_sammen_perioder_i_førstegangsvedtak() {
        OppdragKjede tidligereOppdrag = OppdragKjede.builder().build();
        Ytelse nyYtelse = Ytelse.builder()
            .leggTilPeriode(new YtelsePeriode(p1, Sats.dagsats(1000)))
            .leggTilPeriode(new YtelsePeriode(p2, Sats.dagsats(1100)))
            .leggTilPeriode(new YtelsePeriode(p3, Sats.dagsats(1200)))
            .build();

        OppdragKjedeFactory factory = OppdragKjedeFactory.lagForNyMottaker(fagsystemId);
        OppdragKjedeFortsettelse resultat = factory.lagOppdragskjedeForYtelse(tidligereOppdrag, nyYtelse);

        List<OppdragLinje> linjer = resultat.getOppdragslinjer();
        assertThat(linjer).hasSize(3);
        assertLik(linjer.get(0), p1, Sats.dagsats(1000), fagsystemId, null);
        assertLik(linjer.get(1), p2, Sats.dagsats(1100), fagsystemId, linjer.get(0).getDelytelseId());
        assertLik(linjer.get(2), p3, Sats.dagsats(1200), fagsystemId, linjer.get(1).getDelytelseId());
    }

    @Test
    public void skal_søtte_fullstendig_opphør() {
        OppdragKjede tidligereOppdrag = OppdragKjede.builder()
            .medOppdragslinje(OppdragLinje.builder()
                .medPeriode(p1)
                .medSats(Sats.dagsats(1000))
                .medDelytelseId(DelytelseId.parse("FOO-1-1"))
                .build())
            .medOppdragslinje(OppdragLinje.builder()
                .medPeriode(p2)
                .medSats(Sats.dagsats(2000))
                .medDelytelseId(DelytelseId.parse("FOO-1-2"))
                .medRefDelytelseId(DelytelseId.parse("FOO-1-1"))
                .build())
            .medOppdragslinje(OppdragLinje.builder()
                .medPeriode(p3)
                .medSats(Sats.dagsats(1100))
                .medDelytelseId(DelytelseId.parse("FOO-1-3"))
                .medRefDelytelseId(DelytelseId.parse("FOO-1-2"))
                .build())
            .build();
        Ytelse nyYtelse = Ytelse.builder().build();

        OppdragKjedeFactory factory = OppdragKjedeFactory.lagForEksisterendeMottaker(DelytelseId.parse("FOO-1-3"));
        OppdragKjedeFortsettelse resultat = factory.lagOppdragskjedeForYtelse(tidligereOppdrag, nyYtelse);

        // ved opphør skal siste linje sendes på nytt, med opphørsdato til første dato det skal opphøres fra
        assertThat(resultat.getOppdragslinjer()).hasSize(1);
        assertOpphørslinje(resultat.getOppdragslinjer().get(0), p3, Sats.dagsats(1100), fagsystemId, p1.getFom());
    }

    @Test
    public void skal_støtte_opphør_fra_spesifikk_dato() {
        OppdragKjede tidligereOppdrag = OppdragKjede.builder()
            .medOppdragslinje(OppdragLinje.builder()
                .medPeriode(p1)
                .medSats(Sats.dagsats(1000))
                .medDelytelseId(DelytelseId.parse("FOO-1-1"))
                .build())
            .medOppdragslinje(OppdragLinje.builder()
                .medPeriode(p2)
                .medSats(Sats.dagsats(2000))
                .medDelytelseId(DelytelseId.parse("FOO-1-2"))
                .medRefDelytelseId(DelytelseId.parse("FOO-1-1"))
                .build())
            .medOppdragslinje(OppdragLinje.builder()
                .medPeriode(p3)
                .medSats(Sats.dagsats(1100))
                .medDelytelseId(DelytelseId.parse("FOO-1-3"))
                .medRefDelytelseId(DelytelseId.parse("FOO-1-2"))
                .build())
            .build();
        Ytelse nyYtelse = Ytelse.builder()
            .leggTilPeriode(new YtelsePeriode(p1, Sats.dagsats(1000)))
            .leggTilPeriode(new YtelsePeriode(p2del1, Sats.dagsats(2000)))
            .build();

        OppdragKjedeFactory factory = OppdragKjedeFactory.lagForEksisterendeMottaker(DelytelseId.parse("FOO-1-3"));
        OppdragKjedeFortsettelse resultat = factory.lagOppdragskjedeForYtelse(tidligereOppdrag, nyYtelse);

        // ved opphør skal siste linje sendes på nytt, med opphørsdato til første dato det skal opphøres fra
        assertThat(resultat.getOppdragslinjer()).hasSize(1);
        assertOpphørslinje(resultat.getOppdragslinjer().get(0), p3, Sats.dagsats(1100), fagsystemId,
            p2del1.getTom().plusDays(1));
    }

    @Test
    @Disabled("Feiler på mandager")
    public void skal_støtte_fotsettelse_etter_opphør() {
        OppdragKjede tidligereOppdrag = OppdragKjede.builder()
            .medOppdragslinje(OppdragLinje.builder()
                .medPeriode(p1)
                .medSats(Sats.dagsats(1000))
                .medDelytelseId(DelytelseId.parse("FOO-1-1"))
                .build())
            .medOppdragslinje(OppdragLinje.builder()
                .medPeriode(p2)
                .medSats(Sats.dagsats(2000))
                .medDelytelseId(DelytelseId.parse("FOO-1-2"))
                .medRefDelytelseId(DelytelseId.parse("FOO-1-1"))
                .build())
            .medOppdragslinje(OppdragLinje.builder()
                .medPeriode(p2)
                .medSats(Sats.dagsats(2000))
                .medDelytelseId(DelytelseId.parse("FOO-1-2"))
                .medOpphørFomDato(p1.getFom())
                .build())
            .build();
        Ytelse nyYtelse = Ytelse.builder().leggTilPeriode(new YtelsePeriode(p2del1, Sats.dagsats(2000))).build();

        OppdragKjedeFactory factory = OppdragKjedeFactory.lagForEksisterendeMottaker(DelytelseId.parse("FOO-1-2"));
        OppdragKjedeFortsettelse resultat = factory.lagOppdragskjedeForYtelse(tidligereOppdrag, nyYtelse);

        // ved opphør skal siste linje sendes på nytt, med opphørsdato til første dato det skal opphøres fra
        assertThat(resultat.getOppdragslinjer()).hasSize(1);
        assertLik(resultat.getOppdragslinjer().get(0), p2del1, Sats.dagsats(2000), fagsystemId,
            DelytelseId.parse("FOO-1-2"));
    }

    @Test
    public void skal_støtte_endring_fra_spesifikk_dato_inne_i_periode_fra_tilkjent_ytelse() {
        OppdragKjede tidligereOppdrag = OppdragKjede.builder()
            .medOppdragslinje(OppdragLinje.builder()
                .medPeriode(p1)
                .medSats(Sats.dagsats(1000))
                .medDelytelseId(DelytelseId.parse("FOO-1-1"))
                .build())
            .medOppdragslinje(OppdragLinje.builder()
                .medPeriode(p2del1)
                .medSats(Sats.dagsats(1500))
                .medDelytelseId(DelytelseId.parse("FOO-1-2"))
                .medRefDelytelseId(DelytelseId.parse("FOO-1-1"))
                .build())
            .medOppdragslinje(OppdragLinje.builder()
                .medPeriode(p2del2)
                .medSats(Sats.dagsats(2000))
                .medDelytelseId(DelytelseId.parse("FOO-1-3"))
                .medRefDelytelseId(DelytelseId.parse("FOO-1-2"))
                .build())
            .medOppdragslinje(OppdragLinje.builder()
                .medPeriode(p3)
                .medSats(Sats.dagsats(1100))
                .medDelytelseId(DelytelseId.parse("FOO-1-4"))
                .medRefDelytelseId(DelytelseId.parse("FOO-1-3"))
                .build())
            .build();
        Ytelse nyYtelse = Ytelse.builder()
            .leggTilPeriode(new YtelsePeriode(p1, Sats.dagsats(1000)))
            .leggTilPeriode(new YtelsePeriode(p2, Sats.dagsats(1500)))
            .build();

        OppdragKjedeFactory factory = OppdragKjedeFactory.lagForEksisterendeMottaker(DelytelseId.parse("FOO-1-4"));
        OppdragKjedeFortsettelse resultat = factory.lagOppdragskjedeForYtelse(tidligereOppdrag, nyYtelse);

        // ved opphør skal siste linje sendes på nytt, med opphørsdato til første dato det skal opphøres fra
        List<OppdragLinje> linjer = resultat.getOppdragslinjer();
        assertThat(linjer).hasSize(2);
        assertOpphørslinje(linjer.get(0), p3, Sats.dagsats(1100), fagsystemId, p2.getFom().plusDays(1));
        assertLik(linjer.get(1), p2del2, Sats.dagsats(1500), fagsystemId, linjer.get(0).getDelytelseId());
    }

    @Test
    public void skal_opphøre_fra_starten_og_sende_alle_perioder_når_det_legges_til_en_tidligere_periode() {
        OppdragKjede eksisterendeKjede = OppdragKjede.builder()
            .medOppdragslinje(OppdragLinje.builder()
                .medDelytelseId(DelytelseId.parse("FOO-1-1"))
                .medPeriode(p2)
                .medSats(Sats.dag7(1000))
                .build())
            .build();

        Ytelse nyttVedtak = Ytelse.builder()
            .leggTilPeriode(new YtelsePeriode(p1, Sats.dag7(1000)))
            .leggTilPeriode(new YtelsePeriode(p2, Sats.dag7(1000)))
            .build();

        OppdragKjedeFactory factory = OppdragKjedeFactory.lagForEksisterendeMottaker(DelytelseId.parse("FOO-1-1"));
        List<OppdragLinje> linjer = factory.lagOppdragskjedeForYtelse(eksisterendeKjede, nyttVedtak)
            .getOppdragslinjer();
        assertThat(linjer).hasSize(3);
        LocalDate opphørsdato = p2.getFom();
        assertOpphørslinje(linjer.get(0), p2, Sats.dag7(1000), fagsystemId, opphørsdato);
        assertLik(linjer.get(1), p1, Sats.dag7(1000), fagsystemId, linjer.get(0).getDelytelseId());
        assertLik(linjer.get(2), p2, Sats.dag7(1000), fagsystemId, linjer.get(1).getDelytelseId());
    }

    @Test
    public void skal_søtte_fullstendig_opphør_av_feriepenger() {
        OppdragKjede tidligereOppdrag = OppdragKjede.builder()
            .medOppdragslinje(OppdragLinje.builder()
                .medPeriode(mai)
                .medSats(Sats.engang(1000))
                .medDelytelseId(DelytelseId.parse("FOO-1-1"))
                .build())
            .build();
        Ytelse nyYtelse = Ytelse.builder().build();

        OppdragKjedeFactory factory = OppdragKjedeFactory.lagForEksisterendeMottaker(DelytelseId.parse("FOO-1-1"));
        OppdragKjedeFortsettelse resultat = factory.lagOppdragskjedeForFeriepenger(tidligereOppdrag, nyYtelse);

        // ved opphør skal siste linje sendes på nytt, med opphørsdato til første dato det skal opphøres fra
        assertThat(resultat.getOppdragslinjer()).hasSize(1);
        assertOpphørslinje(resultat.getOppdragslinjer().get(0), mai, Sats.engang(1000), fagsystemId, mai.getFom());
    }

    @Test
    public void skal_ikke_bruke_opphørslinje_men_bare_overskrive_forrige_periode_ved_endring_i_feriepenger() {
        OppdragKjede tidligereOppdrag = OppdragKjede.builder()
            .medOppdragslinje(OppdragLinje.builder()
                .medPeriode(mai)
                .medSats(Sats.engang(1000))
                .medDelytelseId(DelytelseId.parse("FOO-1-1"))
                .build())
            .build();
        Ytelse nyYtelse = Ytelse.builder().leggTilPeriode(new YtelsePeriode(mai, Sats.engang(1001))).build();

        OppdragKjedeFactory factory = OppdragKjedeFactory.lagForEksisterendeMottaker(DelytelseId.parse("FOO-1-1"));
        OppdragKjedeFortsettelse resultat = factory.lagOppdragskjedeForFeriepenger(tidligereOppdrag, nyYtelse);

        // ved opphør skal siste linje sendes på nytt, med opphørsdato til første dato det skal opphøres fra
        assertThat(resultat.getOppdragslinjer()).hasSize(1);
        assertLik(resultat.getOppdragslinjer().get(0), mai, Sats.engang(1001), fagsystemId,
            DelytelseId.parse("FOO-1-1"));
    }

    @Test
    public void skal_støtte_å_splitte_periode() {
        LocalDate dag1 = LocalDate.now();
        LocalDate dag2 = dag1.plusDays(1);
        LocalDate dag3 = dag1.plusDays(2);
        Periode helePeriode = Periode.of(dag1, dag3);

        OppdragKjede tidligereOppdrag = OppdragKjede.builder()
            .medOppdragslinje(OppdragLinje.builder()
                .medPeriode(helePeriode)
                .medSats(Sats.dag7(1000))
                .medDelytelseId(DelytelseId.parse("FOO-1-1"))
                .build())
            .build();
        Ytelse nyYtelse = Ytelse.builder()
            .leggTilPeriode(new YtelsePeriode(Periode.of(dag1, dag1), Sats.dag7(1000)))
            .leggTilPeriode(new YtelsePeriode(Periode.of(dag3, dag3), Sats.dag7(1000)))
            .build();

        OppdragKjedeFactory factory = OppdragKjedeFactory.lagForEksisterendeMottaker(DelytelseId.parse("FOO-1-1"));
        OppdragKjedeFortsettelse resultat = factory.lagOppdragskjedeForYtelse(tidligereOppdrag, nyYtelse);
        assertThat(resultat.getEndringsdato()).isEqualTo(dag2);
        List<OppdragLinje> linjer = resultat.getOppdragslinjer();
        assertThat(linjer.get(0).erOpphørslinje()).isTrue();
        assertThat(linjer.get(0).getPeriode()).isEqualTo(helePeriode);
        assertThat(linjer.get(0).getOpphørFomDato()).isEqualTo(dag2);
        assertThat(linjer.get(0).getSats()).isEqualTo(Sats.dag7(1000));
        assertThat(linjer.get(0).getDelytelseId()).isEqualTo(DelytelseId.parse("FOO-1-1"));

        assertThat(linjer.get(1).erOpphørslinje()).isFalse();
        assertThat(linjer.get(1).getPeriode()).isEqualTo(Periode.of(dag3, dag3));
        assertThat(linjer.get(1).getSats()).isEqualTo(Sats.dag7(1000));
        assertThat(linjer.get(1).getDelytelseId()).isEqualTo(DelytelseId.parse("FOO-1-2"));
        assertThat(linjer.get(1).getRefDelytelseId()).isEqualTo(DelytelseId.parse("FOO-1-1"));
    }

    private static Periode p(String tekst) {
        String[] deler = tekst.split("-");
        DateTimeFormatter pattern = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        return Periode.of(LocalDate.parse(deler[0], pattern), LocalDate.parse(deler[1], pattern));
    }


    public void assertLik(OppdragLinje linje,
                          Periode p,
                          Sats sats,
                          FagsystemId fagsystemId,
                          DelytelseId refDelytelseId) {
        assertThat(linje.getPeriode()).isEqualTo(p);
        assertThat(linje.getSats()).isEqualTo(sats);
        assertThat(linje.getUtbetalingsgrad()).isNull();
        assertThat(linje.getDelytelseId().getFagsystemId()).isEqualTo(fagsystemId);
        assertThat(linje.getRefDelytelseId()).isEqualTo(refDelytelseId);
        assertThat(linje.getOpphørFomDato()).isNull();
    }

    public void assertOpphørslinje(OppdragLinje linje,
                                   Periode p,
                                   Sats sats,
                                   FagsystemId fagsystemId,
                                   LocalDate opphørsdato) {
        assertThat(linje.getPeriode()).isEqualTo(p);
        assertThat(linje.getSats()).isEqualTo(sats);
        assertThat(linje.getUtbetalingsgrad()).isNull();
        assertThat(linje.getDelytelseId().getFagsystemId()).isEqualTo(fagsystemId);
        assertThat(linje.getRefDelytelseId()).isNull();
        assertThat(linje.getOpphørFomDato()).isEqualTo(opphørsdato);
    }

}
