package no.nav.foreldrepenger.domene.uttak;

import static no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType.AVSLÅTT;
import static no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType.INNVILGET;
import static no.nav.foreldrepenger.behandlingslager.uttak.fp.InnvilgetÅrsak.UTTAK_OPPFYLT;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.IkkeOppfyltÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class OpphørUttakTjenesteTest {

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private UttakRepositoryProvider repositoryProvider = new UttakRepositoryProvider(repoRule.getEntityManager());

    private Behandling revurdering;
    private Behandling originalBehandling;
    private OpphørUttakTjeneste opphørUttakTjeneste = new OpphørUttakTjeneste(repositoryProvider);

    private Behandling opprettOriginalBehandling() {
        Behandlingsresultat.Builder originalResultat = Behandlingsresultat.builderForInngangsvilkår()
            .medBehandlingResultatType(BehandlingResultatType.INNVILGET);

        var scenario = ScenarioMorSøkerForeldrepenger
            .forFødsel()
            .medVilkårResultatType(VilkårResultatType.INNVILGET);
        scenario.medBehandlingsresultat(originalResultat);
        return scenario.lagre(repositoryProvider);
    }

    @Before
    public void oppsett() {
        originalBehandling = opprettOriginalBehandling();
        var behandlingsresultat = Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.OPPHØR);
        revurdering = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medOriginalBehandling(originalBehandling, BehandlingÅrsakType.RE_HENDELSE_FØDSEL)
            .medBehandlingType(BehandlingType.REVURDERING)
            .medBehandlingsresultat(behandlingsresultat)
            .lagre(repositoryProvider);
    }

    @Test
    public void skal_kun_komme_når_behandlingsresultatet_er_av_typen_opphør() {
        LocalDate skjæringstidspunkt = LocalDate.now();
        lagreSkjæringstidspunkt(revurdering, skjæringstidspunkt);
        var ref = BehandlingReferanse.fra(revurdering, skjæringstidspunkt);

        Optional<LocalDate> kallPåOpphørTjenesteForOpphørBehandling = opphørUttakTjeneste.getOpphørsdato(ref, getBehandlingsresultat(revurdering.getId()));
        assertThat(kallPåOpphørTjenesteForOpphørBehandling).isNotEmpty();

        Optional<LocalDate> kallPåOpphørTjenesteForInnvilgetBehandling = opphørUttakTjeneste.getOpphørsdato(ref, getBehandlingsresultat(originalBehandling.getId()));
        assertThat(kallPåOpphørTjenesteForInnvilgetBehandling).isEmpty();
    }

    private Behandlingsresultat getBehandlingsresultat(Long behandlingId) {
        return repositoryProvider.getBehandlingsresultatRepository().hent(behandlingId);
    }

    @Test
    public void skal_returnere_fom_dato_til_tidligste_opphørte_periode_etter_seneste_innvilgede_periode() {
        // arrange
        LocalDate skjæringstidspunkt = LocalDate.now();
        lagreSkjæringstidspunkt(revurdering, skjæringstidspunkt);
        var ref = BehandlingReferanse.fra(revurdering, skjæringstidspunkt);
        var opphørsÅrsaker = IkkeOppfyltÅrsak.opphørsAvslagÅrsaker().iterator();
        new MockUttakResultatBuilder(skjæringstidspunkt.plusDays(10))
            .medInnvilgetPeriode(UTTAK_OPPFYLT, 10)
            .medInnvilgetPeriode(UTTAK_OPPFYLT, 10)
            .medAvslåttPeriode(opphørsÅrsaker.next(), 10)
            .medInnvilgetPeriode(UTTAK_OPPFYLT, 10)
            .medAvslåttPeriode(opphørsÅrsaker.next(), 10)
            .medAvslåttPeriode(opphørsÅrsaker.next(), 10)
            .buildFor(revurdering.getId());
        // act
        Optional<LocalDate> opphørsdato = opphørUttakTjeneste.getOpphørsdato(ref, getBehandlingsresultat(revurdering.getId()));
        // assert
        assertThat(opphørsdato.orElseThrow()).isEqualTo(skjæringstidspunkt.plusDays(54));
    }

    @Test
    public void skal_bruke_skjæringstidspunkt_hvis_alle_perioder_har_fått_opphør_eller_avslagsårsak() {
        // arrange
        LocalDate skjæringstidspunkt = lagreSkjæringstidspunkt(revurdering, LocalDate.now());
        var ref = BehandlingReferanse.fra(revurdering, skjæringstidspunkt);
        var opphørsÅrsaker = IkkeOppfyltÅrsak.opphørsAvslagÅrsaker().iterator();
        new MockUttakResultatBuilder(skjæringstidspunkt.plusDays(7))
            .medAvslåttPeriode(opphørsÅrsaker.next(), 14).medAvslåttPeriode(opphørsÅrsaker.next(), 61)
            .medAvslåttPeriode(opphørsÅrsaker.next(), 14).medAvslåttPeriode(opphørsÅrsaker.next(), 62)
            .buildFor(revurdering.getId());
        // act
        Optional<LocalDate> opphørsdato = opphørUttakTjeneste.getOpphørsdato(ref, getBehandlingsresultat(revurdering.getId()));
        // assert
        assertThat(opphørsdato.get()).isEqualTo(skjæringstidspunkt);
    }

    private LocalDate lagreSkjæringstidspunkt(Behandling behandling, LocalDate skjæringstidspunkt) {
        repositoryProvider.getYtelsesFordelingRepository().lagre(behandling.getId(), new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(skjæringstidspunkt).build());
        return skjæringstidspunkt;
    }


    private class MockUttakResultatBuilder {
        private UttakResultatPerioderEntitet uttakResultatPerioder;
        private LocalDate fom;

        MockUttakResultatBuilder(LocalDate fom) {
            this.fom = fom;
            uttakResultatPerioder = new UttakResultatPerioderEntitet();
        }

        MockUttakResultatBuilder medInnvilgetPeriode(PeriodeResultatÅrsak innvilgetårsak, int varighetIDager) {
            return this.medPeriode(INNVILGET, innvilgetårsak, varighetIDager);
        }

        MockUttakResultatBuilder medAvslåttPeriode(PeriodeResultatÅrsak opphørEllerAvslagsårsak, int varighetIDager) {
            return this.medPeriode(AVSLÅTT, opphørEllerAvslagsårsak, varighetIDager);
        }

        private MockUttakResultatBuilder medPeriode(PeriodeResultatType type, PeriodeResultatÅrsak innvilgetårsak, int varighetIDager) {
            leggTilPeriode(varighetIDager, type, innvilgetårsak);
            oppdaterFom(varighetIDager);
            return this;
        }

        void buildFor(Long behandlingId) {
            repositoryProvider.getFpUttakRepository().lagreOpprinneligUttakResultatPerioder(behandlingId, uttakResultatPerioder);
        }

        private void oppdaterFom(int varighetDager) {
            fom = fom.plusDays(varighetDager + 1);
        }

        private void leggTilPeriode(int varighetDager, PeriodeResultatType resultatType, PeriodeResultatÅrsak årsak) {
            UttakResultatPeriodeEntitet periode = new UttakResultatPeriodeEntitet.Builder(fom, fom.plusDays(varighetDager))
                .medResultatType(resultatType, årsak).build();
            uttakResultatPerioder.leggTilPeriode(periode);
        }
    }
}
