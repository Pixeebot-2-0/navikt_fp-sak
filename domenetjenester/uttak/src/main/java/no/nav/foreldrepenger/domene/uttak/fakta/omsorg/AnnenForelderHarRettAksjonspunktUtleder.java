package no.nav.foreldrepenger.domene.uttak.fakta.omsorg;

import static java.lang.Boolean.TRUE;
import static no.nav.foreldrepenger.domene.uttak.UttakOmsorgUtil.annenForelderHarUttakMedUtbetaling;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.PersonopplysningerForUttak;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.fakta.FaktaUttakAksjonspunktUtleder;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;

/**
 * Aksjonspunkt for Avklar Annen forelder har rett
 */
@FagsakYtelseTypeRef("FP")
@ApplicationScoped
public class AnnenForelderHarRettAksjonspunktUtleder implements FaktaUttakAksjonspunktUtleder {

    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private PersonopplysningerForUttak personopplysninger;
    private ForeldrepengerUttakTjeneste uttakTjeneste;

    @Inject
    public AnnenForelderHarRettAksjonspunktUtleder(UttakRepositoryProvider repositoryProvider,
                                                   PersonopplysningerForUttak personopplysninger,
                                                   ForeldrepengerUttakTjeneste uttakTjeneste) {
        this.ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        this.personopplysninger = personopplysninger;
        this.uttakTjeneste = uttakTjeneste;
    }

    AnnenForelderHarRettAksjonspunktUtleder() {
        // For CDI
    }

    @Override
    public List<AksjonspunktDefinisjon> utledAksjonspunkterFor(UttakInput input) {
        var ref = input.getBehandlingReferanse();
        var ytelseFordelingAggregat = ytelsesFordelingRepository.hentAggregat(ref.getBehandlingId());

        if (!Objects.equals(ref.getBehandlingType(), BehandlingType.FØRSTEGANGSSØKNAD) ||
            !personopplysninger.harOppgittAnnenpartMedNorskID(ref)) {
            return List.of();
        }

        var annenpartsGjeldendeUttaksplan = hentAnnenpartsUttak(input.getYtelsespesifiktGrunnlag());

        if (!oppgittHarAnnenForeldreRett(ytelseFordelingAggregat) &&
            !oppgittAleneomsorg(ytelseFordelingAggregat) &&
            !annenForelderHarUttakMedUtbetaling(annenpartsGjeldendeUttaksplan)) {
            ForeldrepengerGrunnlag fpGrunnlag = input.getYtelsespesifiktGrunnlag();
            if (fpGrunnlag.getAnnenpart().isPresent()) {
                var harAnnennartInnvilgetES = fpGrunnlag.getAnnenpart().get().harInnvilgetES();
                return harAnnennartInnvilgetES ? List.of() : aksjonspunkt();
            }
            return aksjonspunkt();
        }

        if (oppgittHarAnnenForeldreRett(ytelseFordelingAggregat) &&
            erFarMedmor(ref.getRelasjonsRolleType()) &&
            !annenForelderHarUttakMedUtbetaling(annenpartsGjeldendeUttaksplan)) {
            return aksjonspunkt();
        }

        return List.of();
    }

    @Override
    public boolean skalBrukesVedOppdateringAvYtelseFordeling() {
        return false;
    }

    private Optional<ForeldrepengerUttak> hentAnnenpartsUttak(ForeldrepengerGrunnlag fpGrunnlag) {
        var annenpart = fpGrunnlag.getAnnenpart();
        if (annenpart.isPresent()) {
            return uttakTjeneste.hentUttakHvisEksisterer(annenpart.get().getGjeldendeVedtakBehandlingId());
        }
        return Optional.empty();
    }

    private List<AksjonspunktDefinisjon> aksjonspunkt() {
        return List.of(AksjonspunktDefinisjon.AVKLAR_FAKTA_ANNEN_FORELDER_HAR_RETT);
    }

    private boolean erFarMedmor(RelasjonsRolleType relasjonsRolleType) {
        return RelasjonsRolleType.erFarEllerMedmor(relasjonsRolleType);
    }

    private boolean oppgittAleneomsorg(YtelseFordelingAggregat ytelseFordelingAggregat) {
        var aleneomsorg = ytelseFordelingAggregat.getOppgittRettighet().getHarAleneomsorgForBarnet();
        return Objects.equals(TRUE, aleneomsorg);
    }

    public static boolean oppgittHarAnnenForeldreRett(YtelseFordelingAggregat ytelseFordelingAggregat) {
        var harAnnenForeldreRett = ytelseFordelingAggregat.getOppgittRettighet().getHarAnnenForeldreRett();
        return harAnnenForeldreRett == null || Objects.equals(TRUE, harAnnenForeldreRett);
    }
}
