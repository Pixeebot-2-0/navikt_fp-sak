package no.nav.foreldrepenger.domene.vedtak.intern.svp;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatArbeidsforholdEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatEntitet;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.saldo.MaksDatoUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.saldo.svp.MaksDatoUttakTjenesteImpl;
import no.nav.foreldrepenger.domene.uttak.saldo.StønadskontoSaldoTjeneste;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class SvpFagsakRelasjonAvslutningsdatoOppdatererTest {
    private SvpFagsakRelasjonAvslutningsdatoOppdaterer fagsakRelasjonAvslutningsdatoOppdaterer;

    private MaksDatoUttakTjeneste maksDatoUttakTjeneste;

    @Mock
    private BehandlingRepositoryProvider repositoryProvider;

    @Mock
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;

    @Mock
    private BehandlingRepository behandlingRepository;
    @Mock
    private BehandlingsresultatRepository behandlingsresultatRepository;
    @Mock
    private FamilieHendelseRepository familieHendelseRepository;
    @Mock
    private SvangerskapspengerUttakResultatRepository svpUttakRepository;

    @Mock
    private StønadskontoSaldoTjeneste stønadskontoSaldoTjeneste;

    @Mock
    private UttakInputTjeneste uttakInputTjeneste;

    @Mock
    private ForeldrepengerUttakTjeneste foreldrepengerUttakTjeneste;

    private Fagsak fagsak;
    private Behandling behandling;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        maksDatoUttakTjeneste = new MaksDatoUttakTjenesteImpl(svpUttakRepository);

        repositoryProvider = mock(BehandlingRepositoryProvider.class);
        stønadskontoSaldoTjeneste = spy(stønadskontoSaldoTjeneste);
        when(repositoryProvider.getBehandlingRepository()).thenReturn(behandlingRepository);
        when(repositoryProvider.getBehandlingsresultatRepository()).thenReturn(behandlingsresultatRepository);
        when(repositoryProvider.getFamilieHendelseRepository()).thenReturn(familieHendelseRepository);

        fagsakRelasjonAvslutningsdatoOppdaterer = new SvpFagsakRelasjonAvslutningsdatoOppdaterer(repositoryProvider, stønadskontoSaldoTjeneste,
            uttakInputTjeneste, maksDatoUttakTjeneste, fagsakRelasjonTjeneste, foreldrepengerUttakTjeneste);

        behandling = lagBehandling();
        fagsak = behandling.getFagsak();

        when(uttakInputTjeneste.lagInput(any(Behandling.class))).thenReturn(new UttakInput(BehandlingReferanse.fra(behandling), null, new ForeldrepengerGrunnlag()));
        when(behandlingRepository.hentBehandling(behandling.getId())).thenReturn(behandling);
    }

    private Behandling lagBehandling() {
        ScenarioMorSøkerSvangerskapspenger scenario = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger();
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now().plusDays(40));
        scenario.medBehandlingType(BehandlingType.REVURDERING);
        return scenario.lagMocked();
    }

    @Test
    public void finner_avslutningsdato_fra_sisteuttaksdato(){
        // Arrange
        FagsakRelasjon fagsakRelasjon = mock(FagsakRelasjon.class);
        when(fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(fagsak)).thenReturn(Optional.of(fagsakRelasjon));
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId())).thenReturn(Optional.of(behandling));
        Optional<Behandlingsresultat> behandlingsresultat = lagBehandlingsresultat(behandling, BehandlingResultatType.INNVILGET, KonsekvensForYtelsen.UDEFINERT);
        when(behandlingsresultatRepository.hentHvisEksisterer(behandling.getId()))
            .thenReturn(behandlingsresultat);
        LocalDate sisteUttaksdato = LocalDate.now().plusDays(10);
        when(svpUttakRepository.hentHvisEksisterer(behandling.getId())).thenReturn(lagUttakMedEnGyldigPeriode(LocalDate.now().minusDays(10), sisteUttaksdato, behandlingsresultat.get()));

        // Act
        LocalDate avslutningdato = fagsakRelasjonAvslutningsdatoOppdaterer.finnAvslutningsdato(fagsak.getId(),fagsakRelasjon);

        // Assert
        assertThat(avslutningdato).isEqualTo(sisteUttaksdato.plusDays(1));

    }

    @Test
    public void finner_ingen_sisteuttaksdato_for_avslutning(){
        // Arrange
        FagsakRelasjon fagsakRelasjon = mock(FagsakRelasjon.class);
        when(fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(fagsak)).thenReturn(Optional.of(fagsakRelasjon));
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId())).thenReturn(Optional.of(behandling));
        Optional<Behandlingsresultat> behandlingsresultat = lagBehandlingsresultat(behandling, BehandlingResultatType.INNVILGET, KonsekvensForYtelsen.UDEFINERT);
        when(behandlingsresultatRepository.hentHvisEksisterer(behandling.getId()))
            .thenReturn(behandlingsresultat);
        LocalDate sisteUttaksdato = LocalDate.now().plusDays(10);
        when(svpUttakRepository.hentHvisEksisterer(behandling.getId())).thenReturn(lagUttakMedEnUgyldigPeriode(LocalDate.now().minusDays(10), sisteUttaksdato, behandlingsresultat.get()));

        // Act
        LocalDate avslutningdato = fagsakRelasjonAvslutningsdatoOppdaterer.finnAvslutningsdato(fagsak.getId(),fagsakRelasjon);

        // Assert
        assertThat(avslutningdato).isEqualTo(LocalDate.now().plusDays(1));

    }

    private Optional<Behandlingsresultat> lagBehandlingsresultat(Behandling behandling, BehandlingResultatType behandlingResultatType,
                                                                 KonsekvensForYtelsen konsekvensForYtelsen) {
        return Optional.of(Behandlingsresultat.builder()
            .medBehandlingResultatType(behandlingResultatType)
            .leggTilKonsekvensForYtelsen(konsekvensForYtelsen)
            .buildFor(behandling));
    }

    private Optional<SvangerskapspengerUttakResultatEntitet> lagUttakMedEnGyldigPeriode(LocalDate fom, LocalDate tom, Behandlingsresultat br) {
        SvangerskapspengerUttakResultatPeriodeEntitet periode = new SvangerskapspengerUttakResultatPeriodeEntitet.Builder(fom,tom).medUtbetalingsgrad(BigDecimal.valueOf(100)).medPeriodeResultatType(PeriodeResultatType.INNVILGET).build();
        SvangerskapspengerUttakResultatArbeidsforholdEntitet arbeidsforhold = new SvangerskapspengerUttakResultatArbeidsforholdEntitet.Builder().medPeriode(periode).build();
        return Optional.of(new SvangerskapspengerUttakResultatEntitet.Builder(br).medUttakResultatArbeidsforhold(arbeidsforhold).build());
    }

    private Optional<SvangerskapspengerUttakResultatEntitet> lagUttakMedEnUgyldigPeriode(LocalDate fom, LocalDate tom, Behandlingsresultat br) {
        SvangerskapspengerUttakResultatPeriodeEntitet periode = new SvangerskapspengerUttakResultatPeriodeEntitet.Builder(fom,tom).medUtbetalingsgrad(BigDecimal.valueOf(100)).build();
        SvangerskapspengerUttakResultatArbeidsforholdEntitet arbeidsforhold = new SvangerskapspengerUttakResultatArbeidsforholdEntitet.Builder().medPeriode(periode).build();
        return Optional.of(new SvangerskapspengerUttakResultatEntitet.Builder(br).medUttakResultatArbeidsforhold(arbeidsforhold).build());
    }
}
