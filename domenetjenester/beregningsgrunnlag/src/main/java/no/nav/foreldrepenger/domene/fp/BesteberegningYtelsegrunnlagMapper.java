package no.nav.foreldrepenger.domene.fp;

import no.nav.folketrygdloven.kalkulator.steg.besteberegning.Ytelseandel;
import no.nav.folketrygdloven.kalkulator.steg.besteberegning.Ytelsegrunnlag;
import no.nav.folketrygdloven.kalkulator.steg.besteberegning.Ytelseperiode;
import no.nav.folketrygdloven.kalkulator.tid.Intervall;
import no.nav.folketrygdloven.kalkulus.kodeverk.AktivitetStatus;
import no.nav.folketrygdloven.kalkulus.kodeverk.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.domene.iay.modell.Ytelse;
import no.nav.foreldrepenger.domene.iay.modell.YtelseFilter;
import no.nav.foreldrepenger.domene.iay.modell.YtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.Arbeidskategori;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.RelatertYtelseTilstand;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class BesteberegningYtelsegrunnlagMapper {
    private static final List<RelatertYtelseType> FPSAK_YTELSER = Arrays.asList(RelatertYtelseType.FORELDREPENGER,
        RelatertYtelseType.SVANGERSKAPSPENGER);

    private BesteberegningYtelsegrunnlagMapper() {
        // Skjuler default konstruktør
    }

    public static List<Saksnummer> saksnummerSomMåHentesFraFpsak(DatoIntervallEntitet periodeYtelserKanVæreRelevantForBB, YtelseFilter ytelseFilter) {
        Collection<Ytelse> ytelserFraFpsak = ytelseFilter.filter(y -> FPSAK_YTELSER.contains(y.getRelatertYtelseType()))
            .filter(y -> y.getKilde().equals(Fagsystem.FPSAK))
            .filter(y -> y.getPeriode().overlapper(periodeYtelserKanVæreRelevantForBB))
            .getFiltrertYtelser();
        return ytelserFraFpsak.stream().map(Ytelse::getSaksnummer).collect(Collectors.toList());
    }

    public static Optional<Ytelsegrunnlag> mapSykepengerTilYtelegrunnlag(DatoIntervallEntitet periodeYtelserKanVæreRelevantForBB, YtelseFilter ytelseFilter) {
        Collection<Ytelse> sykepengegrunnlag = ytelseFilter.filter(y -> y.getRelatertYtelseType().equals(RelatertYtelseType.SYKEPENGER))
            .filter(y -> y.getPeriode().overlapper(periodeYtelserKanVæreRelevantForBB))
            .getFiltrertYtelser();
        List<Ytelseperiode> sykepengeperioder = sykepengegrunnlag.stream()
            .map(BesteberegningYtelsegrunnlagMapper::mapTilYtelsegrunnlag)
            .flatMap(Optional::stream)
            .collect(Collectors.toList());
        return sykepengeperioder.isEmpty()
            ? Optional.empty()
            : Optional.of(new Ytelsegrunnlag(FagsakYtelseType.SYKEPENGER, sykepengeperioder));
    }

    public static Optional<Ytelsegrunnlag> mapFpsakYtelseTilYtelsegrunnlag(BeregningsresultatEntitet resultat,
                                                                           no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType ytelseType) {
        List<Ytelseperiode> ytelseperioder = resultat.getBeregningsresultatPerioder().stream()
            .filter(periode -> periode.getDagsats() > 0)
            .map(BesteberegningYtelsegrunnlagMapper::mapPeriode)
            .collect(Collectors.toList());
        return ytelseperioder.isEmpty()
            ? Optional.empty()
            : Optional.of(new Ytelsegrunnlag(FagsakYtelseType.fraKode(ytelseType.getKode()), ytelseperioder));
    }

    private static Ytelseperiode mapPeriode(BeregningsresultatPeriode periode) {
        List<Ytelseandel> andeler = periode.getBeregningsresultatAndelList().stream()
            .map(BesteberegningYtelsegrunnlagMapper::mapAndel)
            .collect(Collectors.toList());
        return new Ytelseperiode(Intervall.fraOgMedTilOgMed(periode.getBeregningsresultatPeriodeFom(), periode.getBeregningsresultatPeriodeTom()), andeler);
    }

    private static Ytelseandel mapAndel(BeregningsresultatAndel a) {
        return new Ytelseandel(AktivitetStatus.fraKode(a.getAktivitetStatus().getKode()), Long.valueOf(a.getDagsats()));
    }


    private static Optional<Ytelseperiode> mapTilYtelsegrunnlag(Ytelse sp) {
        Optional<Arbeidskategori> arbeidskategori = sp.getYtelseGrunnlag()
            .flatMap(YtelseGrunnlag::getArbeidskategori);
        if (arbeidskategori.isEmpty() || harUgyldigTilstandForBesteberegning(arbeidskategori.get(), sp.getStatus())) {
            return Optional.empty();
        }
        Ytelseandel andel = new Ytelseandel(no.nav.folketrygdloven.kalkulus.kodeverk.Arbeidskategori.fraKode(arbeidskategori.get().getKode()),
            null);
        return Optional.of(new Ytelseperiode(Intervall.fraOgMedTilOgMed(sp.getPeriode().getFomDato(), sp.getPeriode().getTomDato()),
            Collections.singletonList(andel)));
    }

    private static boolean harUgyldigTilstandForBesteberegning(Arbeidskategori kategori, RelatertYtelseTilstand sp) {
        return Arbeidskategori.UGYLDIG.equals(kategori) && RelatertYtelseTilstand.ÅPEN.equals(sp);
    }
}
