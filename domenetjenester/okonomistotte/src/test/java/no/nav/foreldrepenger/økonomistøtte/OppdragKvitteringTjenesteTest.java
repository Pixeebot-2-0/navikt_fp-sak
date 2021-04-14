package no.nav.foreldrepenger.økonomistøtte;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.OppdragKvittering;

public class OppdragKvitteringTjenesteTest {

    @Test
    public void harPositivKvittering_ja_hvis_00() {
        // Arrange
        var oppdrag110 = lagOppdrag110("00");

        // Act
        var resultat = OppdragKvitteringTjeneste.harPositivKvittering(oppdrag110);

        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    public void harPositivKvittering_ja_hvis_04() {
        // Arrange
        var oppdrag110 = lagOppdrag110("04");

        // Act
        var resultat = OppdragKvitteringTjeneste.harPositivKvittering(oppdrag110);

        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    public void harPositivKvittering_nei_hvis_08() {
        // Arrange
        var oppdrag110 = lagOppdrag110("08");

        // Act
        var resultat = OppdragKvitteringTjeneste.harPositivKvittering(oppdrag110);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    public void harPositivKvittering_nei_hvis_12() {
        // Arrange
        var oppdrag110 = lagOppdrag110("12");

        // Act
        var resultat = OppdragKvitteringTjeneste.harPositivKvittering(oppdrag110);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    public void harPositivKvittering_nei_hvis_ingen_kvittering() {
        // Arrange
        var oppdrag110 = lagOppdrag110(null);

        // Act
        var resultat = OppdragKvitteringTjeneste.harPositivKvittering(oppdrag110);

        // Assert
        assertThat(resultat).isFalse();
    }

    private Oppdrag110 lagOppdrag110(String alvorlighetsgrad) {
        var oppdragskontroll = OppdragTestDataHelper.buildOppdragskontroll();
        var oppdrag110 = OppdragTestDataHelper.buildOppdrag110ES(oppdragskontroll, 123L);
        if (alvorlighetsgrad != null) {
            lagOppdragKvittering(oppdrag110, alvorlighetsgrad);
        }
        return oppdrag110;
    }

    private OppdragKvittering lagOppdragKvittering(Oppdrag110 oppdrag110, String alvorlighetsgrad) {
        return OppdragKvittering.builder()
            .medOppdrag110(oppdrag110)
            .medAlvorlighetsgrad(alvorlighetsgrad)
            .build();
    }
}
