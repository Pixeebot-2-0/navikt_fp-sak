package no.nav.foreldrepenger.behandling.steg.avklarfakta.svp;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.*;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.vedtak.konfig.Tid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

class UtledTilretteleggingerMedArbeidsgiverTjenesteTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.of(2019, 8, 1);
    private static final Arbeidsgiver DEFAULT_VIRKSOMHET = Arbeidsgiver.virksomhet("123");
    private static final Arbeidsgiver VIRKSOMHET_2 = Arbeidsgiver.virksomhet("456");
    private static final InternArbeidsforholdRef INTERN_ARBEIDSFORHOLD_REF_1 = InternArbeidsforholdRef.nyRef();
    private static final InternArbeidsforholdRef INTERN_ARBEIDSFORHOLD_REF_2 = InternArbeidsforholdRef.nyRef();
    private static final InternArbeidsforholdRef INTERN_ARBEIDSFORHOLD_REF_3 = InternArbeidsforholdRef.nyRef();

    private Behandling behandling;
    private Skjæringstidspunkt skjæringstidspunkt;

    private InntektArbeidYtelseTjeneste iayTjeneste = Mockito.mock(InntektArbeidYtelseTjeneste.class);
    private InntektsmeldingTjeneste inntektsmeldingTjeneste = Mockito.mock(InntektsmeldingTjeneste.class);
    private UtledTilretteleggingerMedArbeidsgiverTjeneste utledTilretteleggingerMedArbeidsgiverTjeneste = new UtledTilretteleggingerMedArbeidsgiverTjeneste(
            iayTjeneste, inntektsmeldingTjeneste);

    @BeforeEach
    public void oppsett() {
        this.behandling = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger().lagMocked();
        this.skjæringstidspunkt = Skjæringstidspunkt.builder()
                .medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
                .build();
    }

    @Test
    void skal_returne_tom_liste_hvis_ingen_tilrettelegginger_har_arbeidsgiver() {

        // Arrange
        var tilrettelegging = new SvpTilretteleggingEntitet.Builder().build();

        // Act
        var result = utledTilretteleggingerMedArbeidsgiverTjeneste.utled(behandling, skjæringstidspunkt, List.of(tilrettelegging));

        // Assert
        assertThat(result).isEmpty();

    }

    @Test
    void skal_lage_en_tilrettelegging_selv_med_flere_yrkesaktiviteter_hvis_IM_mottatt_på_virksomhet_uten_intern_arbeidsforhold_ref() {

        // Arrange
        var tilrettelegging = lagTilrettelegging(DEFAULT_VIRKSOMHET, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, null);

        when(iayTjeneste.hentGrunnlag(anyLong())).thenReturn(lagGrunnlag(behandling, List.of(
                lagYrkesaktivitet(DEFAULT_VIRKSOMHET, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, InternArbeidsforholdRef.nyRef(),
                        SKJÆRINGSTIDSPUNKT.minusYears(1), Tid.TIDENES_ENDE),
                lagYrkesaktivitet(DEFAULT_VIRKSOMHET, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, InternArbeidsforholdRef.nyRef(),
                        SKJÆRINGSTIDSPUNKT.minusYears(2), Tid.TIDENES_ENDE))));

        when(inntektsmeldingTjeneste.hentInntektsmeldinger(any(), any())).thenReturn(List.of(
                lagInntektsmelding(DEFAULT_VIRKSOMHET, InternArbeidsforholdRef.nullRef())));

        // Act
        var result = utledTilretteleggingerMedArbeidsgiverTjeneste.utled(behandling, skjæringstidspunkt, List.of(tilrettelegging));

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result).anySatisfy(r -> {
            assertThat(r.getInternArbeidsforholdRef()).isEmpty();
            assertThat(r.getArbeidsgiver()).hasValueSatisfying(arbeidsgiver -> assertThat(arbeidsgiver).isEqualTo(DEFAULT_VIRKSOMHET));
        });

    }

    @Test
    void skal_lage_to_tilrettelegginger_for_virksomhet_med_to_yrkesaktiviteter_hvor_IM_er_kommer_på_begge_yrkesaktivitetene() {

        // Arrange
        var tilrettelegginger = List.of(
                lagTilrettelegging(DEFAULT_VIRKSOMHET, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, null));

        var ref_1 = InternArbeidsforholdRef.nyRef();
        var ref_2 = InternArbeidsforholdRef.nyRef();

        when(iayTjeneste.hentGrunnlag(anyLong())).thenReturn(lagGrunnlag(behandling, List.of(
                lagYrkesaktivitet(DEFAULT_VIRKSOMHET, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, ref_1, SKJÆRINGSTIDSPUNKT.minusYears(1), Tid.TIDENES_ENDE),
                lagYrkesaktivitet(DEFAULT_VIRKSOMHET, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, ref_2, SKJÆRINGSTIDSPUNKT.minusYears(2),
                        Tid.TIDENES_ENDE))));

        when(inntektsmeldingTjeneste.hentInntektsmeldinger(any(), any())).thenReturn(List.of(
                lagInntektsmelding(DEFAULT_VIRKSOMHET, ref_1),
                lagInntektsmelding(DEFAULT_VIRKSOMHET, ref_2)));

        // Act
        var result = utledTilretteleggingerMedArbeidsgiverTjeneste.utled(behandling, skjæringstidspunkt, tilrettelegginger);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).anySatisfy(r -> {
            assertThat(r.getInternArbeidsforholdRef()).hasValueSatisfying(ref -> assertThat(ref).isEqualTo(ref_1));
            assertThat(r.getArbeidsgiver()).hasValueSatisfying(arbeidsgiver -> assertThat(arbeidsgiver).isEqualTo(DEFAULT_VIRKSOMHET));
        });
        assertThat(result).anySatisfy(r -> {
            assertThat(r.getInternArbeidsforholdRef()).hasValueSatisfying(ref -> assertThat(ref).isEqualTo(ref_2));
            assertThat(r.getArbeidsgiver()).hasValueSatisfying(arbeidsgiver -> assertThat(arbeidsgiver).isEqualTo(DEFAULT_VIRKSOMHET));
        });

    }

    @Test
    void skal_kun_lage_tilrettelegginger_for_virksomheten_det_er_søkt_for_og_ignorere_resten() {

        // Arrange
        var person = Arbeidsgiver.person(AktørId.dummy());
        var tilrettelegginger = List.of(
                lagTilrettelegging(person, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, null));

        var ref_1 = InternArbeidsforholdRef.nyRef();
        var ref_2 = InternArbeidsforholdRef.nyRef();

        when(iayTjeneste.hentGrunnlag(anyLong())).thenReturn(lagGrunnlag(behandling, List.of(
                lagYrkesaktivitet(DEFAULT_VIRKSOMHET, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, ref_1, SKJÆRINGSTIDSPUNKT.minusYears(1), Tid.TIDENES_ENDE),
                lagYrkesaktivitet(person, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, ref_2, SKJÆRINGSTIDSPUNKT.minusYears(2), Tid.TIDENES_ENDE))));

        when(inntektsmeldingTjeneste.hentInntektsmeldinger(any(), any())).thenReturn(List.of(
                lagInntektsmelding(DEFAULT_VIRKSOMHET, ref_1),
                lagInntektsmelding(person, ref_2)));

        // Act
        var result = utledTilretteleggingerMedArbeidsgiverTjeneste.utled(behandling, skjæringstidspunkt, tilrettelegginger);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result).anySatisfy(r -> {
            assertThat(r.getInternArbeidsforholdRef()).hasValueSatisfying(ref -> assertThat(ref).isEqualTo(ref_2));
            assertThat(r.getArbeidsgiver()).hasValueSatisfying(arbeidsgiver -> assertThat(arbeidsgiver).isEqualTo(person));
        });

    }

    @Test
    void skal_lage_tilrettelegging_for_yrkesaktiviteter_som_inkluderer_eller_tilkommer_etter_stp_men_ikke_før() {

        // Arrange
        var tilrettelegginger = List.of(
                lagTilrettelegging(DEFAULT_VIRKSOMHET, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, null));

        var ref_1 = InternArbeidsforholdRef.nyRef();
        var ref_2 = InternArbeidsforholdRef.nyRef();
        var ref_3 = InternArbeidsforholdRef.nyRef();

        when(iayTjeneste.hentGrunnlag(anyLong())).thenReturn(lagGrunnlag(behandling, List.of(
                lagYrkesaktivitet(DEFAULT_VIRKSOMHET, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, ref_1, SKJÆRINGSTIDSPUNKT.minusYears(1), Tid.TIDENES_ENDE),
                lagYrkesaktivitet(DEFAULT_VIRKSOMHET, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, ref_2, SKJÆRINGSTIDSPUNKT.minusYears(2),
                        SKJÆRINGSTIDSPUNKT.minusDays(1)),
                lagYrkesaktivitet(DEFAULT_VIRKSOMHET, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, ref_3, SKJÆRINGSTIDSPUNKT.plusDays(1), Tid.TIDENES_ENDE))));

        when(inntektsmeldingTjeneste.hentInntektsmeldinger(any(), any())).thenReturn(List.of(
                lagInntektsmelding(DEFAULT_VIRKSOMHET, ref_1),
                lagInntektsmelding(DEFAULT_VIRKSOMHET, ref_2),
                lagInntektsmelding(DEFAULT_VIRKSOMHET, ref_3)));

        // Act
        var result = utledTilretteleggingerMedArbeidsgiverTjeneste.utled(behandling, skjæringstidspunkt, tilrettelegginger);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).anySatisfy(r -> {
            assertThat(r.getInternArbeidsforholdRef()).hasValueSatisfying(ref -> assertThat(ref).isEqualTo(ref_1));
            assertThat(r.getArbeidsgiver()).hasValueSatisfying(arbeidsgiver -> assertThat(arbeidsgiver).isEqualTo(DEFAULT_VIRKSOMHET));
        });
        assertThat(result).anySatisfy(r -> {
            assertThat(r.getInternArbeidsforholdRef()).hasValueSatisfying(ref -> assertThat(ref).isEqualTo(ref_3));
            assertThat(r.getArbeidsgiver()).hasValueSatisfying(arbeidsgiver -> assertThat(arbeidsgiver).isEqualTo(DEFAULT_VIRKSOMHET));
        });

    }

    @Test
    void skal_lage_tilrettelegginger_for_alle_yrkesaktivitetene_når_ingen_IM_er_mottatt() {

        // Arrange
        var tilrettelegginger = List.of(
                lagTilrettelegging(DEFAULT_VIRKSOMHET, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, null));

        var ref_1 = InternArbeidsforholdRef.nyRef();
        var ref_2 = InternArbeidsforholdRef.nyRef();

        when(iayTjeneste.hentGrunnlag(anyLong())).thenReturn(lagGrunnlag(behandling, List.of(
                lagYrkesaktivitet(DEFAULT_VIRKSOMHET, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, ref_1, SKJÆRINGSTIDSPUNKT.minusYears(1), Tid.TIDENES_ENDE),
                lagYrkesaktivitet(DEFAULT_VIRKSOMHET, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, ref_2, SKJÆRINGSTIDSPUNKT.minusYears(2),
                        Tid.TIDENES_ENDE))));

        when(inntektsmeldingTjeneste.hentInntektsmeldinger(any(), any())).thenReturn(List.of());

        // Act
        var result = utledTilretteleggingerMedArbeidsgiverTjeneste.utled(behandling, skjæringstidspunkt, tilrettelegginger);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).anySatisfy(r -> {
            assertThat(r.getInternArbeidsforholdRef()).hasValueSatisfying(ref -> assertThat(ref).isEqualTo(ref_1));
            assertThat(r.getArbeidsgiver()).hasValueSatisfying(arbeidsgiver -> assertThat(arbeidsgiver).isEqualTo(DEFAULT_VIRKSOMHET));
        });
        assertThat(result).anySatisfy(r -> {
            assertThat(r.getInternArbeidsforholdRef()).hasValueSatisfying(ref -> assertThat(ref).isEqualTo(ref_2));
            assertThat(r.getArbeidsgiver()).hasValueSatisfying(arbeidsgiver -> assertThat(arbeidsgiver).isEqualTo(DEFAULT_VIRKSOMHET));
        });

    }

    @Test
    void skal_ikke_lage_tilrettelegging_for_arbeidstyper_som_ikke_kommer_fra_aareg() {

        // Arrange
        var tilrettelegginger = List.of(
                lagTilrettelegging(DEFAULT_VIRKSOMHET, ArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE, null));

        var ref_1 = InternArbeidsforholdRef.nyRef();

        when(iayTjeneste.hentGrunnlag(anyLong())).thenReturn(lagGrunnlag(behandling, List.of(
                lagYrkesaktivitet(DEFAULT_VIRKSOMHET, ArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE, ref_1, SKJÆRINGSTIDSPUNKT.minusYears(1),
                        Tid.TIDENES_ENDE))));

        when(inntektsmeldingTjeneste.hentInntektsmeldinger(any(), any())).thenReturn(List.of(
                lagInntektsmelding(DEFAULT_VIRKSOMHET, ref_1)));

        // Act
        var result = utledTilretteleggingerMedArbeidsgiverTjeneste.utled(behandling, skjæringstidspunkt, tilrettelegginger);

        // Assert
        assertThat(result).isEmpty();

    }

    @Test
    void skal_lage_en_tilrettelegging_for_hver_yrkesaktivitetene_til_alle_tilretteleggingene_det_er_søkt_for() {

        // Arrange
        var virksomhet = Arbeidsgiver.virksomhet("123");
        var person = Arbeidsgiver.person(AktørId.dummy());

        var tilrettelegginger = List.of(
                lagTilrettelegging(virksomhet, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, null),
                lagTilrettelegging(person, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, null));

        var ref_1 = InternArbeidsforholdRef.nyRef();
        var ref_2 = InternArbeidsforholdRef.nyRef();
        var ref_3 = InternArbeidsforholdRef.nyRef();
        var ref_4 = InternArbeidsforholdRef.nyRef();

        when(iayTjeneste.hentGrunnlag(anyLong())).thenReturn(lagGrunnlag(behandling, List.of(
                lagYrkesaktivitet(virksomhet, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, ref_1, SKJÆRINGSTIDSPUNKT.minusYears(1), Tid.TIDENES_ENDE),
                lagYrkesaktivitet(virksomhet, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, ref_2, SKJÆRINGSTIDSPUNKT.minusYears(2), Tid.TIDENES_ENDE),
                lagYrkesaktivitet(person, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, ref_3, SKJÆRINGSTIDSPUNKT.minusYears(1), Tid.TIDENES_ENDE),
                lagYrkesaktivitet(person, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, ref_4, SKJÆRINGSTIDSPUNKT.minusYears(2), Tid.TIDENES_ENDE))));

        when(inntektsmeldingTjeneste.hentInntektsmeldinger(any(), any())).thenReturn(List.of());

        // Act
        var result = utledTilretteleggingerMedArbeidsgiverTjeneste.utled(behandling, skjæringstidspunkt, tilrettelegginger);

        // Assert
        assertThat(result).hasSize(4);
        assertThat(result).anySatisfy(r -> {
            assertThat(r.getInternArbeidsforholdRef()).hasValueSatisfying(ref -> assertThat(ref).isEqualTo(ref_1));
            assertThat(r.getArbeidsgiver()).hasValueSatisfying(arbeidsgiver -> assertThat(arbeidsgiver).isEqualTo(virksomhet));
        });
        assertThat(result).anySatisfy(r -> {
            assertThat(r.getInternArbeidsforholdRef()).hasValueSatisfying(ref -> assertThat(ref).isEqualTo(ref_2));
            assertThat(r.getArbeidsgiver()).hasValueSatisfying(arbeidsgiver -> assertThat(arbeidsgiver).isEqualTo(virksomhet));
        });
        assertThat(result).anySatisfy(r -> {
            assertThat(r.getInternArbeidsforholdRef()).hasValueSatisfying(ref -> assertThat(ref).isEqualTo(ref_3));
            assertThat(r.getArbeidsgiver()).hasValueSatisfying(arbeidsgiver -> assertThat(arbeidsgiver).isEqualTo(person));
        });
        assertThat(result).anySatisfy(r -> {
            assertThat(r.getInternArbeidsforholdRef()).hasValueSatisfying(ref -> assertThat(ref).isEqualTo(ref_4));
            assertThat(r.getArbeidsgiver()).hasValueSatisfying(arbeidsgiver -> assertThat(arbeidsgiver).isEqualTo(person));
        });

    }

    @Test
    void skal_oppdatere_tilrettelegginger_med_nye_arforholdsIder_når_nye_IMer_med_nye_arbeidsforholdsIder_uten_at_det_blir_duplikater() {
        // Arrange
        var tilrettelegginger = List.of(
            lagTilrettelegging(DEFAULT_VIRKSOMHET, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, INTERN_ARBEIDSFORHOLD_REF_1),
            lagTilrettelegging(DEFAULT_VIRKSOMHET, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, INTERN_ARBEIDSFORHOLD_REF_2),
            lagTilrettelegging(VIRKSOMHET_2, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, INTERN_ARBEIDSFORHOLD_REF_3));

        var ref_1 = InternArbeidsforholdRef.nyRef();
        var ref_2 = InternArbeidsforholdRef.nyRef();

        when(iayTjeneste.hentGrunnlag(anyLong())).thenReturn(lagGrunnlag(behandling, List.of(
            lagYrkesaktivitet(DEFAULT_VIRKSOMHET, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, ref_1, SKJÆRINGSTIDSPUNKT.minusYears(1), Tid.TIDENES_ENDE),
            lagYrkesaktivitet(DEFAULT_VIRKSOMHET, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, ref_2, SKJÆRINGSTIDSPUNKT.minusYears(2), Tid.TIDENES_ENDE))));

        when(inntektsmeldingTjeneste.hentInntektsmeldinger(any(), any())).thenReturn(List.of(
            lagInntektsmelding(DEFAULT_VIRKSOMHET, ref_1),
            lagInntektsmelding(DEFAULT_VIRKSOMHET, ref_2),
            lagInntektsmelding(VIRKSOMHET_2, INTERN_ARBEIDSFORHOLD_REF_3)));

        // Act
        var result = utledTilretteleggingerMedArbeidsgiverTjeneste.utled(behandling, skjæringstidspunkt, tilrettelegginger);

        // Assert
        assertThat(result).hasSize(3);
        assertThat(result).anySatisfy(r -> {
            assertThat(r.getInternArbeidsforholdRef()).hasValueSatisfying(ref -> assertThat(ref).isEqualTo(ref_1));
            assertThat(r.getArbeidsgiver()).hasValueSatisfying(arbeidsgiver -> assertThat(arbeidsgiver).isEqualTo(DEFAULT_VIRKSOMHET));
        });
        assertThat(result).anySatisfy(r -> {
            assertThat(r.getInternArbeidsforholdRef()).hasValueSatisfying(ref -> assertThat(ref).isEqualTo(ref_2));
            assertThat(r.getArbeidsgiver()).hasValueSatisfying(arbeidsgiver -> assertThat(arbeidsgiver).isEqualTo(DEFAULT_VIRKSOMHET));
        });
        assertThat(result).anySatisfy(r -> {
            assertThat(r.getInternArbeidsforholdRef()).hasValueSatisfying(ref -> assertThat(ref).isEqualTo(INTERN_ARBEIDSFORHOLD_REF_3));
            assertThat(r.getArbeidsgiver()).hasValueSatisfying(arbeidsgiver -> assertThat(arbeidsgiver).isEqualTo(VIRKSOMHET_2));
        });

    }

    @Test
    void skal_ikke_gjøre_noe_med_tilrettelegginger_hvor_det_ikke_er_endringer_på_im() {

        // Arrange
        var tilrettelegginger = List.of(
            lagTilrettelegging(DEFAULT_VIRKSOMHET, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, INTERN_ARBEIDSFORHOLD_REF_1),
            lagTilrettelegging(DEFAULT_VIRKSOMHET, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, INTERN_ARBEIDSFORHOLD_REF_2),
            lagTilrettelegging(VIRKSOMHET_2, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, INTERN_ARBEIDSFORHOLD_REF_3));


        when(iayTjeneste.hentGrunnlag(anyLong())).thenReturn(lagGrunnlag(behandling, List.of(
            lagYrkesaktivitet(DEFAULT_VIRKSOMHET, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, INTERN_ARBEIDSFORHOLD_REF_1, SKJÆRINGSTIDSPUNKT.minusYears(1), Tid.TIDENES_ENDE),
            lagYrkesaktivitet(DEFAULT_VIRKSOMHET, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, INTERN_ARBEIDSFORHOLD_REF_2, SKJÆRINGSTIDSPUNKT.minusYears(2), Tid.TIDENES_ENDE))));

        when(inntektsmeldingTjeneste.hentInntektsmeldinger(any(), any())).thenReturn(List.of(
            lagInntektsmelding(DEFAULT_VIRKSOMHET, INTERN_ARBEIDSFORHOLD_REF_1),
            lagInntektsmelding(DEFAULT_VIRKSOMHET, INTERN_ARBEIDSFORHOLD_REF_2),
            lagInntektsmelding(VIRKSOMHET_2, INTERN_ARBEIDSFORHOLD_REF_3)));

        // Act
        var result = utledTilretteleggingerMedArbeidsgiverTjeneste.utled(behandling, skjæringstidspunkt, tilrettelegginger);

        // Assert
        assertThat(result).hasSize(3);
        assertThat(result).anySatisfy(r -> {
            assertThat(r.getInternArbeidsforholdRef()).hasValueSatisfying(ref -> assertThat(ref).isEqualTo(INTERN_ARBEIDSFORHOLD_REF_1));
            assertThat(r.getArbeidsgiver()).hasValueSatisfying(arbeidsgiver -> assertThat(arbeidsgiver).isEqualTo(DEFAULT_VIRKSOMHET));
        });
        assertThat(result).anySatisfy(r -> {
            assertThat(r.getInternArbeidsforholdRef()).hasValueSatisfying(ref -> assertThat(ref).isEqualTo(INTERN_ARBEIDSFORHOLD_REF_2));
            assertThat(r.getArbeidsgiver()).hasValueSatisfying(arbeidsgiver -> assertThat(arbeidsgiver).isEqualTo(DEFAULT_VIRKSOMHET));
        });
        assertThat(result).anySatisfy(r -> {
            assertThat(r.getInternArbeidsforholdRef()).hasValueSatisfying(ref -> assertThat(ref).isEqualTo(INTERN_ARBEIDSFORHOLD_REF_3));
            assertThat(r.getArbeidsgiver()).hasValueSatisfying(arbeidsgiver -> assertThat(arbeidsgiver).isEqualTo(VIRKSOMHET_2));
        });

    }

    @Test
    void skal_fjerne_alle_tilrettelegginger_for_en_virksomhet_om_mismatch_med_tilkoblede_inntektsmeldinger() {

        // Arrange
        var tilrettelegginger = List.of(
            lagTilrettelegging(DEFAULT_VIRKSOMHET, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, INTERN_ARBEIDSFORHOLD_REF_1),
            lagTilrettelegging(DEFAULT_VIRKSOMHET, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, INTERN_ARBEIDSFORHOLD_REF_2),
            lagTilrettelegging(DEFAULT_VIRKSOMHET, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, INTERN_ARBEIDSFORHOLD_REF_3));


        when(iayTjeneste.hentGrunnlag(anyLong())).thenReturn(lagGrunnlag(behandling, List.of(
            lagYrkesaktivitet(DEFAULT_VIRKSOMHET, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, INTERN_ARBEIDSFORHOLD_REF_1, SKJÆRINGSTIDSPUNKT.minusYears(1), Tid.TIDENES_ENDE),
            lagYrkesaktivitet(DEFAULT_VIRKSOMHET, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, INTERN_ARBEIDSFORHOLD_REF_2, SKJÆRINGSTIDSPUNKT.minusYears(2), Tid.TIDENES_ENDE),
            lagYrkesaktivitet(DEFAULT_VIRKSOMHET, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, INTERN_ARBEIDSFORHOLD_REF_3, SKJÆRINGSTIDSPUNKT.minusYears(2), Tid.TIDENES_ENDE))));

        when(inntektsmeldingTjeneste.hentInntektsmeldinger(any(), any())).thenReturn(List.of(
            lagInntektsmelding(DEFAULT_VIRKSOMHET, INTERN_ARBEIDSFORHOLD_REF_1),
            lagInntektsmelding(DEFAULT_VIRKSOMHET, INTERN_ARBEIDSFORHOLD_REF_2)));

        // Act
        var result = utledTilretteleggingerMedArbeidsgiverTjeneste.utled(behandling, skjæringstidspunkt, tilrettelegginger);

        // Assert
        assertThat(result).hasSize(3);
        assertThat(result).anySatisfy(r -> {
            assertThat(r.getInternArbeidsforholdRef()).hasValueSatisfying(ref -> assertThat(ref).isEqualTo(INTERN_ARBEIDSFORHOLD_REF_1));
            assertThat(r.getArbeidsgiver()).hasValueSatisfying(arbeidsgiver -> assertThat(arbeidsgiver).isEqualTo(DEFAULT_VIRKSOMHET));
        });
        assertThat(result).anySatisfy(r -> {
            assertThat(r.getInternArbeidsforholdRef()).hasValueSatisfying(ref -> assertThat(ref).isEqualTo(INTERN_ARBEIDSFORHOLD_REF_2));
            assertThat(r.getArbeidsgiver()).hasValueSatisfying(arbeidsgiver -> assertThat(arbeidsgiver).isEqualTo(DEFAULT_VIRKSOMHET));
        });
        assertThat(result).anySatisfy(r -> {
            assertThat(r.getInternArbeidsforholdRef()).hasValueSatisfying(ref -> assertThat(ref).isEqualTo(INTERN_ARBEIDSFORHOLD_REF_3));
            assertThat(r.getArbeidsgiver()).hasValueSatisfying(arbeidsgiver -> assertThat(arbeidsgiver).isEqualTo(DEFAULT_VIRKSOMHET));
        });

    }

    private SvpTilretteleggingEntitet lagTilrettelegging(Arbeidsgiver arbeidsgiver, ArbeidType arbeidType,
                                                         InternArbeidsforholdRef internArbeidsforholdRef) {
        var builder = new SvpTilretteleggingEntitet.Builder()
                .medArbeidType(arbeidType)
                .medArbeidsgiver(arbeidsgiver);
        if (internArbeidsforholdRef != null) {
            builder.medInternArbeidsforholdRef(internArbeidsforholdRef);
        }

        return builder.build();
    }


    private Inntektsmelding lagInntektsmelding(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef ref) {
        return InntektsmeldingBuilder.builder()
                .medArbeidsgiver(arbeidsgiver)
                .medArbeidsforholdId(ref)
                .build();
    }

    private InntektArbeidYtelseGrunnlag lagGrunnlag(Behandling behandling, List<Yrkesaktivitet> yrkesaktiviteter) {
        var aktørArbeidBuilder = InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(Optional.empty())
                .medAktørId(behandling.getAktørId());
        yrkesaktiviteter.forEach(aktørArbeidBuilder::leggTilYrkesaktivitet);
        return InntektArbeidYtelseGrunnlagBuilder.nytt()
                .medData(InntektArbeidYtelseAggregatBuilder
                        .oppdatere(Optional.empty(), VersjonType.REGISTER)
                        .leggTilAktørArbeid(aktørArbeidBuilder))
                .build();
    }

    private Yrkesaktivitet lagYrkesaktivitet(Arbeidsgiver arbeidsgiver, ArbeidType arbeidType, InternArbeidsforholdRef ref, LocalDate fom,
            LocalDate tom) {
        return YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .medArbeidsforholdId(ref)
                .medArbeidsgiver(arbeidsgiver)
                .medArbeidType(arbeidType)
                .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny().medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom)))
                .build();
    }

}
