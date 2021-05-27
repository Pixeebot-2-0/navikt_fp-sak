package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.fødsel;

import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.evaluation.RuleReasonRef;
import no.nav.fpsak.nare.evaluation.RuleReasonRefImpl;
import no.nav.fpsak.nare.specification.LeafSpecification;

public class SjekkErDetForTidligeForAtFødselBurdeHaInntruffet extends LeafSpecification<FødselsvilkårGrunnlag> {

    static final String ID = SjekkErDetForTidligeForAtFødselBurdeHaInntruffet.class.getSimpleName();

    static final RuleReasonRef FØDSEL_BURDE_HA_INNTRUFFET = new RuleReasonRefImpl("1026", "Fødsel ikke funnet i folkeregisteret");

    public SjekkErDetForTidligeForAtFødselBurdeHaInntruffet() {
        super(ID);
    }

    @Override
    public Evaluation evaluate(FødselsvilkårGrunnlag grunnlag) {
        if (grunnlag.behandlingsdato() == null) {
            throw new IllegalArgumentException("Mangler behandlingsdato i :" + grunnlag);
        }
        // Tidligere regel har sjekket om det er registrert barn
        if (!grunnlag.erSøktOmTermin()) {
            // Det er søkt på fødsel og det er ikke registrert barn i Folkeregisteret
            // Det er allerede ventet en periode etter angitt fødselsdato
            return nei(FØDSEL_BURDE_HA_INNTRUFFET);
        }
        if (grunnlag.erFødselRegistreringFristUtløpt()){
            // Det er søkt på termin og det er ikke registrert eller bekreftet barn.
            return nei(FØDSEL_BURDE_HA_INNTRUFFET);
        }
        return ja();
    }

}
