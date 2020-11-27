package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.historikk.tilfeller;

import static no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.Inntektskategori.ARBEIDSTAKER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDel;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagFelt;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;

import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.dto.FaktaBeregningLagreDto;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.dto.RefusjonskravPrArbeidsgiverVurderingDto;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.historikk.ArbeidsgiverHistorikkinnslag;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.AktivitetStatus;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningRefusjonOverstyringEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningRefusjonOverstyringerEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class VurderRefusjonHistorikkTjenesteTest {
    private static final String NAV_ORGNR = "889640782";
    private static final Arbeidsgiver VIRKSOMHET = Arbeidsgiver.virksomhet(NAV_ORGNR);
    private final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();
    private final Beløp GRUNNBELØP = new Beløp(600000);

    private HistorikkTjenesteAdapter historikkTjenesteAdapter;
    private VurderRefusjonHistorikkTjeneste vurderRefusjonHistorikkTjeneste;
    private final Historikkinnslag historikkinnslag = new Historikkinnslag();

    @BeforeEach
    public void setUp(EntityManager entityManager) {
        historikkinnslag.setType(HistorikkinnslagType.FAKTA_ENDRET);
        DokumentArkivTjeneste dokumentArkivTjeneste = new DokumentArkivTjeneste(null,
            new FagsakRepository(entityManager));
        historikkTjenesteAdapter = new HistorikkTjenesteAdapter(new HistorikkRepository(entityManager), dokumentArkivTjeneste);
        VirksomhetTjeneste virksomhetTjeneste = mock(VirksomhetTjeneste.class);
        when(virksomhetTjeneste.hentOrganisasjon(VIRKSOMHET.getIdentifikator()))
            .thenReturn(new Virksomhet.Builder().medOrgnr(VIRKSOMHET.getOrgnr()).build());
        ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslag = new ArbeidsgiverHistorikkinnslag(new ArbeidsgiverTjeneste(null, virksomhetTjeneste));
        vurderRefusjonHistorikkTjeneste = new VurderRefusjonHistorikkTjeneste(arbeidsgiverHistorikkinnslag);
    }

    @Test
    public void lag_historikk_når_ikkje_gyldig_utvidelse() {
        // Arrange
        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagBeregningsgrunnlag();
        FaktaBeregningLagreDto dto = lagDto(false);

        // Act
        HistorikkInnslagTekstBuilder historikkInnslagTekstBuilder = historikkTjenesteAdapter.tekstBuilder();
        vurderRefusjonHistorikkTjeneste.lagHistorikk(1L, dto, historikkInnslagTekstBuilder, grunnlag.getBeregningsgrunnlag().orElseThrow(), Optional.empty(), InntektArbeidYtelseGrunnlagBuilder.nytt().build());

        // Assert
        assertHistorikk(historikkInnslagTekstBuilder, Boolean.FALSE);
    }

    @Test
    public void oppdater_når_gyldig_utvidelse() {
        // Arrange
        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagBeregningsgrunnlag();
        FaktaBeregningLagreDto dto = lagDto(true);

        // Act
        HistorikkInnslagTekstBuilder historikkInnslagTekstBuilder = historikkTjenesteAdapter.tekstBuilder();
        vurderRefusjonHistorikkTjeneste.lagHistorikk(1L, dto, historikkInnslagTekstBuilder, grunnlag.getBeregningsgrunnlag().orElseThrow(), Optional.empty(), InntektArbeidYtelseGrunnlagBuilder.nytt().build());

        // Assert
        assertHistorikk(historikkInnslagTekstBuilder, Boolean.TRUE);
    }

    @Test
    public void oppdater_når_gyldig_utvidelse_med_forrige_satt_til_false() {
        // Arrange
        BeregningsgrunnlagGrunnlagEntitet forrige = lagBeregningsgrunnlagMedOverstyring(SKJÆRINGSTIDSPUNKT.plusMonths(1));
        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagBeregningsgrunnlag();
        FaktaBeregningLagreDto dto = lagDto(true);

        // Act
        HistorikkInnslagTekstBuilder historikkInnslagTekstBuilder = historikkTjenesteAdapter.tekstBuilder();
        vurderRefusjonHistorikkTjeneste.lagHistorikk(1L, dto, historikkInnslagTekstBuilder, grunnlag.getBeregningsgrunnlag().orElseThrow(), Optional.of(forrige), InntektArbeidYtelseGrunnlagBuilder.nytt().build());

        // Assert
        assertHistorikk(historikkInnslagTekstBuilder, Boolean.TRUE, Boolean.FALSE);
    }

    @Test
    public void oppdater_når_gyldig_utvidelse_med_forrige_satt_til_true() {
        // Arrange
        BeregningsgrunnlagGrunnlagEntitet forrige = lagBeregningsgrunnlagMedOverstyring(SKJÆRINGSTIDSPUNKT);
        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagBeregningsgrunnlag();
        FaktaBeregningLagreDto dto = lagDto(true);

        // Act
        HistorikkInnslagTekstBuilder historikkInnslagTekstBuilder = historikkTjenesteAdapter.tekstBuilder();
        vurderRefusjonHistorikkTjeneste.lagHistorikk(1L, dto, historikkInnslagTekstBuilder, grunnlag.getBeregningsgrunnlag().orElseThrow(), Optional.of(forrige), InntektArbeidYtelseGrunnlagBuilder.nytt().build());

        // Assert
        assertHistorikk(historikkInnslagTekstBuilder, Boolean.TRUE, Boolean.TRUE);
    }

    @Test
    public void oppdater_når_ikkje_gyldig_utvidelse_og_forrige_satt_til_ikkje_gyldig() {
        // Arrange
        BeregningsgrunnlagGrunnlagEntitet forrige = lagBeregningsgrunnlagMedOverstyring(SKJÆRINGSTIDSPUNKT.plusMonths(1));
        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagBeregningsgrunnlag();
        FaktaBeregningLagreDto dto = lagDto(false);

        // Act
        HistorikkInnslagTekstBuilder historikkInnslagTekstBuilder = historikkTjenesteAdapter.tekstBuilder();
        vurderRefusjonHistorikkTjeneste.lagHistorikk(1L, dto, historikkInnslagTekstBuilder, grunnlag.getBeregningsgrunnlag().orElseThrow(), Optional.of(forrige), InntektArbeidYtelseGrunnlagBuilder.nytt().build());

        // Assert
        assertHistorikk(historikkInnslagTekstBuilder, Boolean.FALSE, Boolean.FALSE);
    }

    @Test
    public void oppdater_når_ikkje_gyldig_utvidelse_og_forrige_satt_til_gyldig() {
        // Arrange
        BeregningsgrunnlagGrunnlagEntitet forrige = lagBeregningsgrunnlagMedOverstyring(SKJÆRINGSTIDSPUNKT);
        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagBeregningsgrunnlag();
        FaktaBeregningLagreDto dto = lagDto(false);

        // Act
        HistorikkInnslagTekstBuilder historikkInnslagTekstBuilder = historikkTjenesteAdapter.tekstBuilder();
        vurderRefusjonHistorikkTjeneste.lagHistorikk(1L, dto, historikkInnslagTekstBuilder, grunnlag.getBeregningsgrunnlag().orElseThrow(), Optional.of(forrige), InntektArbeidYtelseGrunnlagBuilder.nytt().build());

        // Assert
        assertHistorikk(historikkInnslagTekstBuilder, Boolean.FALSE, Boolean.TRUE);
    }


    private void assertHistorikk(HistorikkInnslagTekstBuilder historikkInnslagTekstBuilder, Boolean tilVerdi) {
        List<HistorikkinnslagDel> deler = historikkInnslagTekstBuilder.build(historikkinnslag);
        assertThat(deler).hasSize(1);
        HistorikkinnslagDel del = deler.get(0);
        List<HistorikkinnslagFelt> endredeFelt = del.getEndredeFelt();
        assertThat(endredeFelt).hasSize(1);
        assertThat(endredeFelt.get(0).getNavn()).isEqualTo(HistorikkEndretFeltType.NY_REFUSJONSFRIST.getKode());
        assertThat(endredeFelt.get(0).getFraVerdi()).isNull();
        assertThat(endredeFelt.get(0).getTilVerdi()).isEqualTo(tilVerdi.toString());
    }

    private void assertHistorikk(HistorikkInnslagTekstBuilder historikkInnslagTekstBuilder, Boolean tilVerdi, Boolean fraVerdi) {
        List<HistorikkinnslagDel> deler = historikkInnslagTekstBuilder.build(historikkinnslag);
        assertThat(deler).hasSize(1);
        HistorikkinnslagDel del = deler.get(0);
        List<HistorikkinnslagFelt> endredeFelt = del.getEndredeFelt();
        assertThat(endredeFelt).hasSize(1);
        assertThat(endredeFelt.get(0).getNavn()).isEqualTo(HistorikkEndretFeltType.NY_REFUSJONSFRIST.getKode());
        assertThat(endredeFelt.get(0).getFraVerdi()).isEqualTo(fraVerdi.toString());
        assertThat(endredeFelt.get(0).getTilVerdi()).isEqualTo(tilVerdi.toString());
    }

    private FaktaBeregningLagreDto lagDto(boolean skalUtvideGyldighet) {
        FaktaBeregningLagreDto dto = new FaktaBeregningLagreDto(List.of(FaktaOmBeregningTilfelle.VURDER_REFUSJONSKRAV_SOM_HAR_KOMMET_FOR_SENT));

        RefusjonskravPrArbeidsgiverVurderingDto ref1 = new RefusjonskravPrArbeidsgiverVurderingDto();
        ref1.setArbeidsgiverId(VIRKSOMHET.getIdentifikator());
        ref1.setSkalUtvideGyldighet(skalUtvideGyldighet);
        dto.setRefusjonskravGyldighet(List.of(ref1));
        return dto;
    }

    private BeregningsgrunnlagGrunnlagEntitet lagBeregningsgrunnlagMedOverstyring(LocalDate dato) {
        BeregningsgrunnlagEntitet beregningsgrunnlag = BeregningsgrunnlagEntitet.ny()
            .medGrunnbeløp(GRUNNBELØP)
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .leggTilFaktaOmBeregningTilfeller(List.of(FaktaOmBeregningTilfelle.VURDER_REFUSJONSKRAV_SOM_HAR_KOMMET_FOR_SENT))
            .build();
        BeregningsgrunnlagPeriode periode1 = BeregningsgrunnlagPeriode.ny()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusMonths(2).minusDays(1))
            .build(beregningsgrunnlag);
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsgiver(VIRKSOMHET))
            .medInntektskategori(ARBEIDSTAKER)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .build(periode1);
        BeregningRefusjonOverstyringerEntitet overstyring = BeregningRefusjonOverstyringerEntitet.builder().leggTilOverstyring(BeregningRefusjonOverstyringEntitet.builder()
                .medArbeidsgiver(VIRKSOMHET)
                .medFørsteMuligeRefusjonFom(dato).build())
            .build();
        return BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
            .medBeregningsgrunnlag(beregningsgrunnlag)
            .medRefusjonOverstyring(overstyring)
            .build(1L, BeregningsgrunnlagTilstand.KOFAKBER_UT);
    }

    private BeregningsgrunnlagGrunnlagEntitet lagBeregningsgrunnlag() {
        BeregningsgrunnlagEntitet beregningsgrunnlag = BeregningsgrunnlagEntitet.ny()
            .medGrunnbeløp(GRUNNBELØP)
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .leggTilFaktaOmBeregningTilfeller(List.of(FaktaOmBeregningTilfelle.VURDER_REFUSJONSKRAV_SOM_HAR_KOMMET_FOR_SENT))
            .build();
        BeregningsgrunnlagPeriode periode1 = BeregningsgrunnlagPeriode.ny()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusMonths(2).minusDays(1))
            .build(beregningsgrunnlag);
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsgiver(VIRKSOMHET))
            .medInntektskategori(ARBEIDSTAKER)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .build(periode1);
        return BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty()).medBeregningsgrunnlag(beregningsgrunnlag).build(1L, BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER);
    }

}
