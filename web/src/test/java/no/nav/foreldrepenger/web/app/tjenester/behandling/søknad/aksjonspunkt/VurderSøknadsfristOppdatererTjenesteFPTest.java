package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.Søknadsfrister;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
import no.nav.foreldrepenger.behandlingslager.uttak.UttaksperiodegrenseRepository;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;

@CdiDbAwareTest
public class VurderSøknadsfristOppdatererTjenesteFPTest {

    @Inject
    @FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
    private VurderSøknadsfristOppdatererTjenesteFP tjeneste;

    @Inject
    private UttaksperiodegrenseRepository uttaksperiodegrenseRepository;

    @Inject
    private BehandlingsresultatRepository behandlingsresultatRepository;

    @Inject
    private SøknadRepository søknadRepository;

    @Inject
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;

    @Test
    public void skal_oppdatere_behandlingsresultet_med_uttaksperiodegrense(EntityManager em) {
        // Arrange
        var nyMottattDato = LocalDate.of(2018, 1, 15);
        var førsteLovligeUttaksdag = LocalDate.of(2017, 10, 1);
        var dto = new VurderSøknadsfristDto("Begrunnelse", true);
        dto.setAnsesMottattDato(nyMottattDato);
        var behandling = byggBehandlingMedYf(em);

        // Act
        tjeneste.oppdater(dto, aksjonspunktParam(behandling, dto));

        // Assert
        var uttaksperiodegrense = uttaksperiodegrenseRepository.hent(behandling.getId());
        assertThat(uttaksperiodegrense.getErAktivt()).isTrue();
        assertThat(uttaksperiodegrense.getMottattDato()).isEqualTo(nyMottattDato);
        assertThat(Søknadsfrister.tidligsteDatoDagytelse(uttaksperiodegrense.getMottattDato())).isEqualTo(førsteLovligeUttaksdag);
    }

    private AksjonspunktOppdaterParameter aksjonspunktParam(Behandling behandling, BekreftetAksjonspunktDto dto) {
        var ap = behandling.getAksjonspunktFor(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_SØKNADSFRIST);
        return new AksjonspunktOppdaterParameter(behandling, ap, dto);
    }

    @Test
    public void skal_oppdatere_behandlingsresultat_med_eksisterende_uttaksperiodegrense(EntityManager em) {
        // Arrange
        var gammelMottatDato = LocalDate.of(2018, 3, 15);

        var behandling = byggBehandlingMedYf(em);
        var gammelUttaksperiodegrense = new Uttaksperiodegrense(gammelMottatDato);
        uttaksperiodegrenseRepository.lagre(behandling.getId(), gammelUttaksperiodegrense);

        var nyMottattDato = LocalDate.of(2018, 2, 28);
        var førsteLovligeUttaksdag = LocalDate.of(2017, 11, 1);
        var dto = new VurderSøknadsfristDto("Begrunnelse", true);
        dto.setAnsesMottattDato(nyMottattDato);

        // Act
        tjeneste.oppdater(dto, aksjonspunktParam(behandling, dto));

        // Assert
        var uttaksperiodegrense = uttaksperiodegrenseRepository.hent(behandling.getId());
        assertThat(uttaksperiodegrense.getErAktivt()).isTrue();
        assertThat(uttaksperiodegrense.getMottattDato()).isEqualTo(nyMottattDato);
        assertThat(Søknadsfrister.tidligsteDatoDagytelse(uttaksperiodegrense.getMottattDato())).isEqualTo(førsteLovligeUttaksdag);
    }

    @Test
    public void skal_oppdatere_mottatt_dato_i_oppgitte_perioder(EntityManager em) {
        var nyMottattDato = LocalDate.of(2018, 1, 15);
        var dto = new VurderSøknadsfristDto("begrunnelse", true);
        dto.setAnsesMottattDato(nyMottattDato);

        var behandling = byggBehandlingMedYf(em);
        tjeneste.oppdater(dto, aksjonspunktParam(behandling, dto));

        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregat(behandling.getId());
        var justertFordelingSortert = ytelseFordelingAggregat.getJustertFordeling().orElseThrow().getOppgittePerioder().stream()
                .sorted(Comparator.comparing(OppgittPeriodeEntitet::getFom))
                .collect(Collectors.toList());
        // Skal ikke oppdatere vedtaksperioder
        assertThat(justertFordelingSortert.get(0).getMottattDato()).isNotEqualTo(nyMottattDato);
        assertThat(justertFordelingSortert.get(1).getMottattDato()).isEqualTo(nyMottattDato);
    }

    @Test
    public void lagrerMottattDatoFraSøknadVedEndringFraGyldigGrunnTilIkkeGyldigGrunn(EntityManager em) {
        var behandling = byggBehandlingMedYf(em);
        var mottattDatoSøknad = LocalDate.of(2019, 1, 1);
        var søknad = new SøknadEntitet.Builder()
                .medMottattDato(mottattDatoSøknad)
                .medSøknadsdato(mottattDatoSøknad)
                .build();
        søknadRepository.lagreOgFlush(behandling, søknad);

        var dto = new VurderSøknadsfristDto("bg", false);
        dto.setAnsesMottattDato(mottattDatoSøknad.plusYears(1));

        tjeneste.oppdater(dto, aksjonspunktParam(behandling, dto));

        var uttaksperiodegrense = uttaksperiodegrenseRepository.hent(behandling.getId());

        assertThat(uttaksperiodegrense.getMottattDato()).isEqualTo(mottattDatoSøknad);
    }

    private Behandling byggBehandlingMedYf(EntityManager em) {
        var mødrekvote = OppgittPeriodeBuilder.ny()
                .medPeriodeKilde(FordelingPeriodeKilde.TIDLIGERE_VEDTAK)
                .medMottattDato(LocalDate.of(2020, 1, 1))
                .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
                .medPeriode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 2, 2))
                .build();
        var fellesperiode = OppgittPeriodeBuilder.ny()
                .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
                .medMottattDato(LocalDate.of(2020, 1, 1))
                .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
                .medPeriode(LocalDate.of(2020, 2, 3), LocalDate.of(2020, 3, 3))
                .build();
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medOpprinneligEndringsdato(mødrekvote.getFom()).build())
                .medJustertFordeling(new OppgittFordelingEntitet(List.of(mødrekvote, fellesperiode), true))
                .leggTilAksjonspunkt(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_SØKNADSFRIST, BehandlingStegType.SØKNADSFRIST_FORELDREPENGER)
                .medBehandlingsresultat(new Behandlingsresultat.Builder())
                .lagre(new BehandlingRepositoryProvider(em));
        var søknad = new SøknadEntitet.Builder()
                .medSøknadsdato(mødrekvote.getFom())
                .medMottattDato(mødrekvote.getFom())
                .build();
        søknadRepository.lagreOgFlush(behandling, søknad);
        var uttaksperiodegrense = new Uttaksperiodegrense(mødrekvote.getFom());
        uttaksperiodegrenseRepository.lagre(behandling.getId(), uttaksperiodegrense);
        return behandling;
    }

}
