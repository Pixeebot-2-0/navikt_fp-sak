package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.domene.mappers.til_kalkulator.BeregningsgrunnlagInputProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.folketrygdloven.kalkulator.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.kalkulator.input.ForeldrepengerGrunnlag;
import no.nav.folketrygdloven.kalkulator.modell.gradering.AktivitetGradering;
import no.nav.folketrygdloven.kalkulus.kodeverk.Dekningsgrad;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.fp.BeregningsgrunnlagInputTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.mappers.til_kalkulator.MapBehandlingRef;
import no.nav.foreldrepenger.domene.output.BeregningsgrunnlagVilkårOgAkjonspunktResultat;
import no.nav.foreldrepenger.domene.prosess.BeregningsgrunnlagKopierOgLagreTjeneste;

@ExtendWith(MockitoExtension.class)
class ForeslåBeregningsgrunnlagStegTest {

    @Mock
    private BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste;
    @Mock
    private BeregningsgrunnlagVilkårOgAkjonspunktResultat beregningsgrunnlagRegelResultat;
    @Mock
    private BehandlingRepository behandlingRepository;
    @Mock
    private BehandlingskontrollKontekst kontekst;
    @Mock
    private Behandling behandling;
    private ForeslåBeregningsgrunnlagSteg steg;
    private final InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    @Mock
    BeregningsgrunnlagInputProvider inputProvider;

    @BeforeEach
    void setUp() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        behandling = scenario.lagMocked();
        var stp = Skjæringstidspunkt.builder()
                .medFørsteUttaksdato(LocalDate.now())
                .medFørsteUttaksdatoGrunnbeløp(LocalDate.now())
                .medSkjæringstidspunktOpptjening(LocalDate.now());
        var ref = BehandlingReferanse.fra(behandling, stp.build());
        var foreldrepengerGrunnlag = new ForeldrepengerGrunnlag(Dekningsgrad.DEKNINGSGRAD_100, false, AktivitetGradering.INGEN_GRADERING);
        var input = new BeregningsgrunnlagInput(MapBehandlingRef.mapRef(ref), null, null, List.of(),
                foreldrepengerGrunnlag);
        var inputTjeneste = mock(BeregningsgrunnlagInputTjeneste.class);
        when(behandlingRepository.hentBehandling(behandling.getId())).thenReturn(behandling);
        when(inputTjeneste.lagInput(behandling.getId())).thenReturn(input);
        when(kontekst.getBehandlingId()).thenReturn(behandling.getId());
        when(beregningsgrunnlagKopierOgLagreTjeneste.foreslåBeregningsgrunnlag(any())).thenReturn(beregningsgrunnlagRegelResultat);

        when(inputProvider.getTjeneste(FagsakYtelseType.FORELDREPENGER)).thenReturn(inputTjeneste);
        steg = new ForeslåBeregningsgrunnlagSteg(behandlingRepository, beregningsgrunnlagKopierOgLagreTjeneste,
                inputProvider);

        iayTjeneste.lagreInntektsmeldinger(behandling.getFagsak().getSaksnummer(), behandling.getId(), List.of());
    }

    @Test
    void stegUtførtUtenAksjonspunkter() {
        // Arrange
        opprettVilkårResultatForBehandling(VilkårResultatType.INNVILGET);

        // Act
        var resultat = steg.utførSteg(kontekst);

        // Assert
        assertThat(resultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(resultat.getAksjonspunktListe()).isEmpty();
    }

    @Test
    void stegUtførtNårRegelResultatInneholderAutopunkt() {
        // Arrange
        opprettVilkårResultatForBehandling(VilkårResultatType.INNVILGET);
        when(beregningsgrunnlagRegelResultat.getAksjonspunkter()).thenReturn(List.of(AksjonspunktResultat.opprettForAksjonspunkt(
            AksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_ARBEIDSTAKER_FRILANS)));

        // Act
        var resultat = steg.utførSteg(kontekst);

        // Assert
        assertThat(resultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(resultat.getAksjonspunktListe()).hasSize(1);
        assertThat(resultat.getAksjonspunktListe().get(0)).isEqualTo(no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_ARBEIDSTAKER_FRILANS);
    }

    private void opprettVilkårResultatForBehandling(VilkårResultatType resultatType) {
        var vilkårResultat = VilkårResultat.builder().medVilkårResultatType(resultatType)
                .buildFor(behandling);
        var behandlingsresultat = Behandlingsresultat.opprettFor(behandling);
        behandlingsresultat.medOppdatertVilkårResultat(vilkårResultat);
    }
}
