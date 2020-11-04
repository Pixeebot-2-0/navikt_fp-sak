package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;

@CdiDbAwareTest
public class UttakPeriodegrenseDtoTjenesteTest {

    @Inject
    private UttakPeriodegrenseDtoTjeneste tjeneste;

    @Test
    public void finnerSøknadsfristForPeriodeStartDato() {
        LocalDate periodeStart = LocalDate.of(2018, 1, 31);
        LocalDate forventetSøknadsfrist = LocalDate.of(2018, 4, 30);

        LocalDate søknadsfrist = tjeneste.finnSøknadsfristForPeriodeMedStart(periodeStart);
        assertThat(søknadsfrist).isEqualTo(forventetSøknadsfrist);

        periodeStart = LocalDate.of(2018, 1, 31);
        søknadsfrist = tjeneste.finnSøknadsfristForPeriodeMedStart(periodeStart);
        assertThat(søknadsfrist).isEqualTo(forventetSøknadsfrist);
    }

}
