package no.nav.foreldrepenger.ytelse.beregning;


import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.ytelse.beregning.adapter.MapInputFraVLTilRegelGrunnlag;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatRegelmodell;
import no.nav.fpsak.nare.json.JsonOutput;
import no.nav.fpsak.nare.json.NareJsonException;

@ApplicationScoped
public class BeregnYtelseTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(BeregnYtelseTjeneste.class);

    private Instance<BeregnFeriepengerTjeneste> beregnFeriepengerTjeneste;
    private MapInputFraVLTilRegelGrunnlag mapBeregningsresultatFraVLTilRegel;

    public BeregnYtelseTjeneste() {
        // CDI
    }

    @Inject
    public BeregnYtelseTjeneste(@Any Instance<BeregnFeriepengerTjeneste> beregnFeriepengerTjeneste,
                                MapInputFraVLTilRegelGrunnlag mapBeregningsresultatFraVLTilRegel) {
        this.beregnFeriepengerTjeneste = beregnFeriepengerTjeneste;
        this.mapBeregningsresultatFraVLTilRegel = mapBeregningsresultatFraVLTilRegel;
    }

    public BeregningsresultatEntitet beregnYtelse(BehandlingReferanse referanse) {
        // Map til regelmodell
        var regelmodell = mapBeregningsresultatFraVLTilRegel.mapFra(referanse);

        // Verifiser input til regel
        if (andelerIBeregningMåLiggeIUttak(referanse)) {
            BeregningsresultatInputVerifiserer.verifiserAlleAndelerIBeregningErIUttak(regelmodell);
        } else {
            BeregningsresultatInputVerifiserer.verifiserAndelerIUttakLiggerIBeregning(regelmodell);
        }

        // Kalle regeltjeneste
        var beregningsresultat = FastsettBeregningsresultatTjeneste.fastsettBeregningsresultat(regelmodell);

        // Beregn feriepenger
        var feriepengerTjeneste = FagsakYtelseTypeRef.Lookup.find(beregnFeriepengerTjeneste, referanse.fagsakYtelseType()).orElseThrow();
        feriepengerTjeneste.beregnFeriepenger(referanse, beregningsresultat);

        // Verifiser beregningsresultat
        try {
            BeregningsresultatOutputVerifiserer.verifiserOutput(beregningsresultat);
        } catch (Exception e) {
            log(regelmodell);
            throw new RuntimeException("Validering av beregningsresultat feilet", e);
        }

        return beregningsresultat;
    }

    private void log(BeregningsresultatRegelmodell grunnlag) {
        try {
            String maskertRegelinput = JsonOutput.asJson(grunnlag).replaceAll("\\d{13}|\\d{11}|\\d{9}", "*");
            LOG.info("Regelinput til beregning av tilkjent ytelse {}", maskertRegelinput);
        } catch (NareJsonException jsonProcessingException) {
            LOG.warn("Feil ved logging av regelinput", jsonProcessingException);
        }
    }

    private boolean andelerIBeregningMåLiggeIUttak(BehandlingReferanse ref) {
        return ref.fagsakYtelseType().equals(FagsakYtelseType.FORELDREPENGER);
    }
}
