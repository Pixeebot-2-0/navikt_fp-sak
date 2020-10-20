package no.nav.foreldrepenger.behandlingslager.behandling.vedtak;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandlingslager.behandling.BasicBehandlingBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareTest;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class OverlappVedtakRepositoryTest extends EntityManagerAwareTest {

    private OverlappVedtakRepository overlappVedtakRepository;

    private BasicBehandlingBuilder behandlingBuilder;

    @BeforeEach
    void setUp() {
        var entityManager = getEntityManager();
        overlappVedtakRepository = new OverlappVedtakRepository(entityManager);
        behandlingBuilder = new BasicBehandlingBuilder(entityManager);
    }

    @Test
    public void lagre() {
        // Arrange
        Behandling behandling = behandlingBuilder.opprettOgLagreFørstegangssøknad(FagsakYtelseType.FORELDREPENGER);
        ÅpenDatoIntervallEntitet periodeVL = ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2019, 1, 1),
            LocalDate.of(2019, 5, 1));
        String ytelseInfotrygd = "BS";
        OverlappVedtak.Builder builder = OverlappVedtak.builder()
            .medSaksnummer(behandling.getFagsak().getSaksnummer())
            .medBehandlingId(behandling.getId())
            .medPeriode(periodeVL)
            .medHendelse("TEST")
            .medUtbetalingsprosent(100L)
            .medFagsystem(Fagsystem.INFOTRYGD.getKode())
            .medYtelse(ytelseInfotrygd);

        // Act
        overlappVedtakRepository.lagre(builder);

        // Assert
        OverlappVedtak hentet = overlappVedtakRepository.hentForSaksnummer(behandling.getFagsak().getSaksnummer()).get(0);
        assertThat(hentet.getBehandlingId()).isEqualTo(behandling.getId());
        assertThat(hentet.getSaksnummer()).isEqualTo(behandling.getFagsak().getSaksnummer());
        assertThat(hentet.getPeriode()).isEqualTo(periodeVL);
        assertThat(hentet.getYtelse()).isEqualTo(ytelseInfotrygd);

    }
}
