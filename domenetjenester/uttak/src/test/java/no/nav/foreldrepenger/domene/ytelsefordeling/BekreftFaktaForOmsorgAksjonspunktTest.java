package no.nav.foreldrepenger.domene.ytelsefordeling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryStubProvider;

public class BekreftFaktaForOmsorgAksjonspunktTest {

    private final UttakRepositoryStubProvider repositoryProvider = new UttakRepositoryStubProvider();
    private final YtelsesFordelingRepository ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
    private final YtelseFordelingTjeneste ytelseFordelingTjeneste = new YtelseFordelingTjeneste(ytelsesFordelingRepository);

    @Test
    public void skal_lagre_ned_bekreftet_aksjonspunkt_omsorg() {
        var behandling = opprettBehandling();
        var iDag = LocalDate.now();
        // simulerer svar fra GUI
        List<DatoIntervallEntitet> ikkeOmsorgPerioder = new ArrayList<>();
        var ikkeOmsorgPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(iDag.minusMonths(2),
            iDag.minusMonths(1));
        ikkeOmsorgPerioder.add(ikkeOmsorgPeriode);
        ytelseFordelingTjeneste.aksjonspunktBekreftFaktaForOmsorg(behandling.getId(), false, ikkeOmsorgPerioder);

        var perioderUtenOmsorgOpt = ytelsesFordelingRepository.hentAggregat(
            behandling.getId()).getPerioderUtenOmsorg();
        assertThat(perioderUtenOmsorgOpt).isPresent();
        var periodeUtenOmsorg = perioderUtenOmsorgOpt.get().getPerioder();
        assertThat(periodeUtenOmsorg).hasSize(1);
        assertThat(periodeUtenOmsorg.get(0).getPeriode()).isEqualTo(ikkeOmsorgPeriode);

        var overstyrtOmsorg = ytelsesFordelingRepository.hentAggregat(behandling.getId()).getOverstyrtOmsorg();
        assertThat(overstyrtOmsorg).isNotNull();
        assertThat(overstyrtOmsorg).isFalse();

        //må nullstille etter endret til har omsorg
        ytelseFordelingTjeneste.aksjonspunktBekreftFaktaForOmsorg(behandling.getId(), true, null);
        perioderUtenOmsorgOpt = ytelsesFordelingRepository.hentAggregat(behandling.getId()).getPerioderUtenOmsorg();
        assertThat(perioderUtenOmsorgOpt).isPresent();
        periodeUtenOmsorg = perioderUtenOmsorgOpt.get().getPerioder();
        assertThat(periodeUtenOmsorg).isEmpty();
        overstyrtOmsorg = ytelsesFordelingRepository.hentAggregat(behandling.getId()).getOverstyrtOmsorg();
        assertThat(overstyrtOmsorg).isNotNull();
        assertThat(overstyrtOmsorg).isTrue();
    }

    private Behandling opprettBehandling() {
        return ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
    }
}
