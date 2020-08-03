package no.nav.foreldrepenger.ytelse.beregning;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepenger;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepengerPrÅr;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.ytelse.beregning.fp.BeregnFeriepenger;
import no.nav.vedtak.felles.testutilities.db.Repository;

public class BeregnFeriepengerTjenesteTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT_MOR = LocalDate.of(2018, 12, 1);
    private static final LocalDate SKJÆRINGSTIDSPUNKT_FAR = SKJÆRINGSTIDSPUNKT_MOR.plusWeeks(6);
    private static final LocalDate SISTE_DAG_FAR = SKJÆRINGSTIDSPUNKT_FAR.plusWeeks(4);
    private static final int DAGSATS = 123;
    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private final EntityManager entityManager = repoRule.getEntityManager();
    private final Repository repository = repoRule.getRepository();
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(entityManager);
    private BeregningsresultatRepository beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();

    private BeregnFeriepengerTjeneste tjeneste;

    @Before
    public void setUp() {
        tjeneste = new BeregnFeriepenger(repositoryProvider, 60);
    }

    @Test
    public void skalBeregneFeriepenger() {
        Behandling farsBehandling = lagBehandlingFar();
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medDefaultOppgittDekningsgrad();
        Behandling morsBehandling = scenario.lagre(repositoryProvider);
        repositoryProvider.getFagsakRelasjonRepository().opprettRelasjon(morsBehandling.getFagsak(), Dekningsgrad._100);
        repositoryProvider.getFagsakRelasjonRepository().kobleFagsaker(morsBehandling.getFagsak(), farsBehandling.getFagsak(), morsBehandling);
        BeregningsresultatEntitet morsBeregningsresultatFP = lagBeregningsresultatFP(SKJÆRINGSTIDSPUNKT_MOR, SKJÆRINGSTIDSPUNKT_FAR, Inntektskategori.ARBEIDSTAKER);

        // Act
        tjeneste.beregnFeriepenger(morsBehandling, morsBeregningsresultatFP);

        // Assert
        assertThat(morsBeregningsresultatFP.getBeregningsresultatFeriepenger()).hasValueSatisfying(this::assertBeregningsresultatFeriepenger);
    }

    @Test
    public void skalIkkeBeregneFeriepenger() {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medDefaultOppgittDekningsgrad();
        Behandling morsBehandling = scenario.lagre(repositoryProvider);
        repositoryProvider.getFagsakRelasjonRepository().opprettRelasjon(morsBehandling.getFagsak(), Dekningsgrad._100);
        BeregningsresultatEntitet morsBeregningsresultatFP = lagBeregningsresultatFP(SKJÆRINGSTIDSPUNKT_MOR, SKJÆRINGSTIDSPUNKT_MOR.plusMonths(6), Inntektskategori.ARBEIDSTAKER_UTEN_FERIEPENGER);

        // Act
        tjeneste.beregnFeriepenger(morsBehandling, morsBeregningsresultatFP);

        //Assert
        assertThat(morsBeregningsresultatFP.getBeregningsresultatFeriepenger()).hasValueSatisfying(resultat -> {
            assertThat(resultat.getBeregningsresultatFeriepengerPrÅrListe()).isEmpty();
            assertThat(resultat.getFeriepengerPeriodeFom()).isNull();
            assertThat(resultat.getFeriepengerPeriodeTom()).isNull();
            assertThat(resultat.getFeriepengerRegelInput()).isNotNull();
            assertThat(resultat.getFeriepengerRegelSporing()).isNotNull();
        });
    }

    private void assertBeregningsresultatFeriepenger(BeregningsresultatFeriepenger feriepenger) {
        assertThat(feriepenger.getFeriepengerPeriodeFom()).as("FeriepengerPeriodeFom").isEqualTo(SKJÆRINGSTIDSPUNKT_MOR);
        assertThat(feriepenger.getFeriepengerPeriodeTom()).as("FeriepengerPeriodeTom").isEqualTo(SISTE_DAG_FAR);
        List<BeregningsresultatFeriepengerPrÅr> beregningsresultatFeriepengerPrÅrListe = feriepenger.getBeregningsresultatFeriepengerPrÅrListe();
        assertThat(beregningsresultatFeriepengerPrÅrListe).as("beregningsresultatFeriepengerPrÅrListe").hasSize(2);
        BeregningsresultatFeriepengerPrÅr prÅr1 = beregningsresultatFeriepengerPrÅrListe.get(0);
        assertThat(prÅr1.getOpptjeningsår()).as("prÅr1.opptjeningsår").isEqualTo(LocalDate.of(2018, 12, 31));
        assertThat(prÅr1.getÅrsbeløp().getVerdi()).as("prÅr1.årsbeløp").isEqualTo(BigDecimal.valueOf(263)); // DAGSATS * 21 * 0.102
        BeregningsresultatAndel andelÅr1 = prÅr1.getBeregningsresultatAndel();
        assertThat(andelÅr1).isNotNull();
        assertThat(andelÅr1.getBeregningsresultatFeriepengerPrÅrListe()).hasSize(2);
        BeregningsresultatFeriepengerPrÅr prÅr2 = beregningsresultatFeriepengerPrÅrListe.get(1);
        assertThat(prÅr2.getOpptjeningsår()).as("prÅr2.opptjeningsår").isEqualTo(LocalDate.of(2019, 12, 31));
        assertThat(prÅr2.getÅrsbeløp().getVerdi()).as("prÅr2.årsbeløp").isEqualTo(BigDecimal.valueOf(113)); // DAGSATS * 9 * 0.102
        BeregningsresultatAndel andelÅr2 = prÅr2.getBeregningsresultatAndel();
        assertThat(andelÅr2).isNotNull();
        assertThat(andelÅr2.getBeregningsresultatFeriepengerPrÅrListe()).hasSize(2);
    }

    private Behandling lagBehandlingFar() {
        ScenarioFarSøkerForeldrepenger scenarioAnnenPart = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medVilkårResultatType(VilkårResultatType.INNVILGET);
        scenarioAnnenPart.medBehandlingVedtak()
            .medVedtakstidspunkt(LocalDateTime.now())
            .medVedtakResultatType(VedtakResultatType.INNVILGET);
        Behandling farsBehandling = scenarioAnnenPart.lagre(repositoryProvider);
        Behandlingsresultat behandlingsresultat = repositoryProvider.getBehandlingsresultatRepository().hent(farsBehandling.getId());
        Behandlingsresultat.builderEndreEksisterende(behandlingsresultat).medBehandlingResultatType(BehandlingResultatType.INNVILGET);
        repository.lagre(behandlingsresultat);
        farsBehandling.avsluttBehandling();
        repository.lagre(farsBehandling);

        BeregningsresultatEntitet farsBeregningsresultatFP = lagBeregningsresultatFP(SKJÆRINGSTIDSPUNKT_FAR, SISTE_DAG_FAR, Inntektskategori.ARBEIDSTAKER);

        beregningsresultatRepository.lagre(farsBehandling, farsBeregningsresultatFP);
        return farsBehandling;
    }

    private BeregningsresultatEntitet lagBeregningsresultatFP(LocalDate periodeFom, LocalDate periodeTom, Inntektskategori inntektskategori) {
        BeregningsresultatEntitet beregningsresultat = BeregningsresultatEntitet.builder().medRegelInput("input").medRegelSporing("sporing").build();
        BeregningsresultatPeriode beregningsresultatPeriode = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(periodeFom, periodeTom)
            .build(beregningsresultat);
        BeregningsresultatAndel.builder()
            .medInntektskategori(inntektskategori)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medDagsats(DAGSATS)
            .medDagsatsFraBg(DAGSATS)
            .medBrukerErMottaker(true)
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medStillingsprosent(BigDecimal.valueOf(100))
            .build(beregningsresultatPeriode);
        return beregningsresultat;
    }
}
