package no.nav.foreldrepenger.familiehendelse.dødsfall;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningVersjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningGrunnlagDiff;
import no.nav.foreldrepenger.domene.typer.AktørId;

public class BarnetsDødsdatoEndringIdentifisererTest {
    private AktørId AKTØRID_SØKER = AktørId.dummy();
    private AktørId AKTØRID_BARN = AktørId.dummy();

    @Test
    public void testBarnLever() {
        final LocalDate dødsdato = null;
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(dødsdato, true);
        PersonopplysningGrunnlagEntitet personopplysningGrunnlagOrginal = opprettPersonopplysningGrunnlag(dødsdato, true);
        PersonopplysningGrunnlagDiff differ = new PersonopplysningGrunnlagDiff(AKTØRID_SØKER, personopplysningGrunnlag1, personopplysningGrunnlagOrginal);

        boolean erEndret = differ.erBarnDødsdatoEndret();
        assertThat(erEndret).as("Forventer at informsjon om barnets død er uendret").isFalse();
    }

    @Test
    public void test_nytt_barn_i_tps_som_ikke_var_registrert_i_TPS_orginalt() {
        final LocalDate dødsdato = null;
        PersonopplysningGrunnlagEntitet personopplysningGrunnlagOrginal = opprettPersonopplysningGrunnlag(dødsdato, false);
        PersonopplysningGrunnlagEntitet personopplysningGrunnlagNy = opprettPersonopplysningGrunnlag(dødsdato, true);
        PersonopplysningGrunnlagDiff differ = new PersonopplysningGrunnlagDiff(AKTØRID_SØKER, personopplysningGrunnlagNy, personopplysningGrunnlagOrginal);

        boolean erEndret = differ.erBarnDødsdatoEndret();
        assertThat(erEndret).as("Forventer at informsjon om barnets død er uendret").isFalse();
    }

    @Test
    public void testDødsdatoUendret() {
        final LocalDate dødsdato = LocalDate.now().minusDays(10);
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(dødsdato, true);
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(dødsdato, true);
        PersonopplysningGrunnlagDiff differ = new PersonopplysningGrunnlagDiff(AKTØRID_SØKER, personopplysningGrunnlag1, personopplysningGrunnlag2);

        boolean erEndret = differ.erBarnDødsdatoEndret();
        assertThat(erEndret).as("Forventer at informsjon om barnets død er uendret").isFalse();
    }

    @Test
    public void testBarnDør() {
        final LocalDate dødsdato = LocalDate.now().minusDays(10);
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(null, true);
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(dødsdato, true);
        PersonopplysningGrunnlagDiff differ = new PersonopplysningGrunnlagDiff(AKTØRID_SØKER, personopplysningGrunnlag1, personopplysningGrunnlag2);

        boolean erEndret = differ.erBarnDødsdatoEndret();
        assertThat(erEndret).as("Forventer at endring om barnets død blir detektert.").isTrue();
    }

    @Test
    public void testDødsdatoEndret() {
        final LocalDate dødsdato = LocalDate.now().minusDays(10);
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(dødsdato.minusDays(1), true);
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(dødsdato, true);
        PersonopplysningGrunnlagDiff differ = new PersonopplysningGrunnlagDiff(AKTØRID_SØKER, personopplysningGrunnlag1, personopplysningGrunnlag2);

        boolean erEndret = differ.erBarnDødsdatoEndret();
        assertThat(erEndret).as("Forventer at endring om barnets død blir detektert.").isTrue();
    }

    @Test
    public void skal_detektere_dødsdato_selv_om_registeropplysninger_ikke_finnes_på_originalt_grunnlag() {
        // Arrange
        final LocalDate dødsdato = LocalDate.now().minusDays(10);
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag1 = opprettTomtPersonopplysningGrunnlag();
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(dødsdato, true);

        // Act
        PersonopplysningGrunnlagDiff differ = new PersonopplysningGrunnlagDiff(AKTØRID_SØKER, personopplysningGrunnlag1, personopplysningGrunnlag2);
        boolean erEndret = differ.erBarnDødsdatoEndret();

        // Assert
        assertThat(erEndret).as("Forventer at barnets død blir detektert selv om det ikke finnes registeropplysninger på originalt grunnlag.").isTrue();
    }

    private PersonopplysningGrunnlagEntitet opprettPersonopplysningGrunnlag(LocalDate dødsdatoBarn, boolean registrerMedBarn) {
        final PersonInformasjonBuilder builder = PersonInformasjonBuilder.oppdater(Optional.empty(), PersonopplysningVersjonType.REGISTRERT);
        builder.leggTil(builder.getPersonopplysningBuilder(AKTØRID_SØKER).medFødselsdato(LocalDate.now().minusYears(30)));
        if (registrerMedBarn) {
            builder.leggTil(builder.getPersonopplysningBuilder(AKTØRID_BARN).medFødselsdato(LocalDate.now().minusMonths(1)).medDødsdato(dødsdatoBarn));
            builder.leggTil(builder.getRelasjonBuilder(AKTØRID_SØKER, AKTØRID_BARN, RelasjonsRolleType.BARN));
            builder.leggTil(builder.getRelasjonBuilder(AKTØRID_BARN, AKTØRID_SØKER, RelasjonsRolleType.MORA));
        }
        return PersonopplysningGrunnlagBuilder.oppdatere(Optional.empty()).medRegistrertVersjon(builder).medOppgittAnnenPart(new OppgittAnnenPartBuilder().build()).build();
    }

    private PersonopplysningGrunnlagEntitet opprettTomtPersonopplysningGrunnlag() {
        return PersonopplysningGrunnlagBuilder.oppdatere(Optional.empty()).build();
    }
}
