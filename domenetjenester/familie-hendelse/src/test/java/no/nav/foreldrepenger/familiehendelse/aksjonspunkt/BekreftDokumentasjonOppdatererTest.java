package no.nav.foreldrepenger.familiehendelse.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarnEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDel;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagFelt;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.FarSøkerType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.BekreftDokumentertDatoAksjonspunktDto;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktRegisterinnhentingTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.es.RegisterInnhentingIntervall;
import no.nav.foreldrepenger.skjæringstidspunkt.es.SkjæringstidspunktTjenesteImpl;

public class BekreftDokumentasjonOppdatererTest extends EntityManagerAwareTest {

    private final HistorikkInnslagTekstBuilder tekstBuilder = new HistorikkInnslagTekstBuilder();
    private final DateTimeFormatter formatterer = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private BehandlingRepositoryProvider repositoryProvider;
    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private SkjæringstidspunktRegisterinnhentingTjeneste skjæringstidspunktTjeneste;

    @BeforeEach
    void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        familieHendelseTjeneste = new FamilieHendelseTjeneste(null,
            repositoryProvider.getFamilieHendelseRepository());
        skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider,
            new RegisterInnhentingIntervall(Period.of(1, 0, 0), Period.of(0, 6, 0)));
    }

    @Test
    public void skal_generere_historikkinnslag_ved_avklaring_av_dokumentert_adopsjonsdato() {
        // Arrange
        LocalDate opprinneligOvertakelsesdato = LocalDate.now();
        LocalDate bekreftetOvertakelsesdato = opprinneligOvertakelsesdato.plusDays(1);
        LocalDate opprinneligFødselsdato = LocalDate.now().plusDays(30);
        LocalDate bekreftetFødselsdato = opprinneligFødselsdato.plusDays(1);

        // Behandling
        ScenarioFarSøkerEngangsstønad scenario = ScenarioFarSøkerEngangsstønad.forAdopsjon();
        scenario.medSøknad().medFarSøkerType(FarSøkerType.ADOPTERER_ALENE);
        scenario.medSøknadHendelse()
            .medAdopsjon(scenario.medSøknadHendelse().getAdopsjonBuilder()
                .medOmsorgsovertakelseDato(opprinneligOvertakelsesdato))
            .leggTilBarn(new UidentifisertBarnEntitet(opprinneligFødselsdato));
        scenario.medBekreftetHendelse().medAdopsjon(scenario.medBekreftetHendelse().getAdopsjonBuilder()
            .medOmsorgsovertakelseDato(opprinneligOvertakelsesdato)).leggTilBarn(opprinneligFødselsdato);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_ADOPSJONSDOKUMENTAJON, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        scenario.lagre(repositoryProvider);

        Behandling behandling = scenario.getBehandling();

        // Dto
        Map<Integer, LocalDate> bekreftedeFødselsdatoer = new HashMap<>();
        bekreftedeFødselsdatoer.put(1, bekreftetFødselsdato);
        BekreftDokumentertDatoAksjonspunktDto dto = new BekreftDokumentertDatoAksjonspunktDto("begrunnelse", bekreftetOvertakelsesdato,
            bekreftedeFødselsdatoer);
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getKode());
        // Act
        new BekreftDokumentasjonOppdaterer(lagMockHistory(), familieHendelseTjeneste, skjæringstidspunktTjeneste).oppdater(dto,
            new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto));
        Historikkinnslag historikkinnslag = new Historikkinnslag();
        historikkinnslag.setType(HistorikkinnslagType.FAKTA_ENDRET);
        List<HistorikkinnslagDel> historikkInnslag = tekstBuilder.build(historikkinnslag);

        // Assert
        assertThat(historikkInnslag).hasSize(1);
        HistorikkinnslagDel del = historikkInnslag.get(0);
        List<HistorikkinnslagFelt> feltList = del.getEndredeFelt();
        assertThat(feltList).hasSize(2);
        assertFelt(del, HistorikkEndretFeltType.OMSORGSOVERTAKELSESDATO, opprinneligOvertakelsesdato.format(formatterer),
            bekreftetOvertakelsesdato.format(formatterer));
        assertFelt(del, HistorikkEndretFeltType.FODSELSDATO, opprinneligFødselsdato.format(formatterer), bekreftetFødselsdato.format(formatterer));
    }

    private void assertFelt(HistorikkinnslagDel historikkinnslagDel, HistorikkEndretFeltType historikkEndretFeltType, String fraVerdi, String tilVerdi) {
        Optional<HistorikkinnslagFelt> feltOpt = historikkinnslagDel.getEndretFelt(historikkEndretFeltType);
        String feltNavn = historikkEndretFeltType.getKode();
        assertThat(feltOpt).as("endretFelt[" + feltNavn + "]").hasValueSatisfying(felt -> {
            assertThat(felt.getNavn()).as(feltNavn + ".navn").isEqualTo(feltNavn);
            assertThat(felt.getFraVerdi()).as(feltNavn + ".fraVerdi").isEqualTo(fraVerdi);
            assertThat(felt.getTilVerdi()).as(feltNavn + ".tilVerdi").isEqualTo(tilVerdi);
        });
    }

    private HistorikkTjenesteAdapter lagMockHistory() {
        HistorikkTjenesteAdapter mockHistory = mock(HistorikkTjenesteAdapter.class);
        when(mockHistory.tekstBuilder()).thenReturn(tekstBuilder);
        return mockHistory;
    }

}
