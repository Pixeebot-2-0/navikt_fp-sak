package no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.FamilieYtelseType;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.wrapper.TilkjentYtelse;

public class TilkjentYtelseMapperTest {

    @Test
    public void skal_mappe_beregningsresultat_uten_perioder() {
        BeregningsresultatEntitet utenPeriode = BeregningsresultatEntitet.builder().medRegelInput("foo").medRegelSporing("bar").build();
        TilkjentYtelse resultat = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL).map(utenPeriode);
        assertThat(resultat.getEndringsdato()).isEmpty();
        assertThat(resultat.getTilkjenteFeriepenger()).isEmpty();
        assertThat(resultat.getTilkjentYtelsePerioder()).isEmpty();
    }
}
