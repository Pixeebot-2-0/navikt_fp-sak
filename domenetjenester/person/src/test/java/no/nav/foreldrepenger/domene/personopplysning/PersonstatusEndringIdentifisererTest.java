package no.nav.foreldrepenger.domene.personopplysning;

import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.*;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class PersonstatusEndringIdentifisererTest {

    private AktørId AKTØRID = AktørId.dummy();

    @Test
    void testPersonstatusUendret() {
        var personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(List.of(PersonstatusType.FOSV));
        var personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(List.of(PersonstatusType.FOSV));
        var differ = new PersonopplysningGrunnlagDiff(AKTØRID, personopplysningGrunnlag1, personopplysningGrunnlag2);

        var erEndret = differ.erPersonstatusEndretForSøkerPeriode(DatoIntervallEntitet.fraOgMed(LocalDate.now()));
        assertThat(erEndret).as("Forventer at personstatus er uendret").isFalse();
    }

    @Test
    void testPersonstatusUendret_flere_statuser() {
        var personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(
                List.of(PersonstatusType.FOSV, PersonstatusType.BOSA));
        var personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(
                List.of(PersonstatusType.FOSV, PersonstatusType.BOSA));
        var differ = new PersonopplysningGrunnlagDiff(AKTØRID, personopplysningGrunnlag1, personopplysningGrunnlag2);

        var erEndret = differ.erPersonstatusEndretForSøkerPeriode(DatoIntervallEntitet.fraOgMed(LocalDate.now()));
        assertThat(erEndret).as("Forventer at personstatus er uendret").isFalse();
    }

    @Test
    void testPersonstatusEndret_ekstra_status_lagt_til() {
        var personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(
                List.of(PersonstatusType.FOSV, PersonstatusType.BOSA));
        var personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(
                List.of(PersonstatusType.FOSV, PersonstatusType.BOSA, PersonstatusType.UREG));
        var differ = new PersonopplysningGrunnlagDiff(AKTØRID, personopplysningGrunnlag1, personopplysningGrunnlag2);

        var erEndret = differ.erPersonstatusEndretForSøkerPeriode(DatoIntervallEntitet.fraOgMed(LocalDate.now()));
        assertThat(erEndret).as("Forventer at endring i personstatus blir detektert.").isTrue();
    }

    @Test
    void testPersonstatusEndret_status_endret_type() {
        var personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(
                List.of(PersonstatusType.FOSV, PersonstatusType.BOSA));
        var personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(
                List.of(PersonstatusType.UREG, PersonstatusType.FOSV));
        var differ = new PersonopplysningGrunnlagDiff(AKTØRID, personopplysningGrunnlag1, personopplysningGrunnlag2);

        var erEndret = differ.erPersonstatusEndretForSøkerPeriode(DatoIntervallEntitet.fraOgMed(LocalDate.now()));
        assertThat(erEndret).as("Forventer at endring i personstatus blir detektert.").isTrue();
    }

    @Test
    void testPersonstatusUendret_men_rekkefølge_i_liste_endret() {
        var personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(
                List.of(PersonstatusType.FOSV, PersonstatusType.BOSA));
        var personopplysningGrunnlag2 = opprettPersonopplysningGrunnlagMotstattRekkefølge(
                personopplysningGrunnlag1.getRegisterVersjon().map(PersonInformasjonEntitet::getPersonstatus).orElse(Collections.emptyList()));
        var differ = new PersonopplysningGrunnlagDiff(AKTØRID, personopplysningGrunnlag1, personopplysningGrunnlag2);

        var erEndret = differ.erPersonstatusEndretForSøkerPeriode(DatoIntervallEntitet.fraOgMed(LocalDate.now()));
        assertThat(erEndret).as("Forventer at endring i rekkefølge ikke skal detektere endring.").isFalse();
    }

    private PersonopplysningGrunnlagEntitet opprettPersonopplysningGrunnlagMotstattRekkefølge(List<PersonstatusEntitet> personstatuser) {
        var builder1 = PersonInformasjonBuilder.oppdater(Optional.empty(), PersonopplysningVersjonType.REGISTRERT);
        builder1.leggTil(builder1.getPersonopplysningBuilder(AKTØRID));
        new LinkedList<>(personstatuser)
                .descendingIterator()
                .forEachRemaining(
                        ps -> builder1.leggTil(builder1.getPersonstatusBuilder(AKTØRID, ps.getPeriode()).medPersonstatus(ps.getPersonstatus())));
        return PersonopplysningGrunnlagBuilder.oppdatere(Optional.empty()).medRegistrertVersjon(builder1).build();
    }

    private PersonopplysningGrunnlagEntitet opprettPersonopplysningGrunnlag(List<PersonstatusType> personstatuser) {
        var builder1 = PersonInformasjonBuilder.oppdater(Optional.empty(), PersonopplysningVersjonType.REGISTRERT);
        builder1.leggTil(builder1.getPersonopplysningBuilder(AKTØRID));
        // Opprett personstatuser med forskjellig fra og med dato. Går 1 mnd tilbake for
        // hver status.
        IntStream.range(0, personstatuser.size())
                .forEach(i -> builder1.leggTil(builder1.getPersonstatusBuilder(AKTØRID, DatoIntervallEntitet.fraOgMed(LocalDate.now().minusMonths(i)))
                        .medPersonstatus(personstatuser.get(i))));
        return PersonopplysningGrunnlagBuilder.oppdatere(Optional.empty()).medRegistrertVersjon(builder1).build();
    }
}
