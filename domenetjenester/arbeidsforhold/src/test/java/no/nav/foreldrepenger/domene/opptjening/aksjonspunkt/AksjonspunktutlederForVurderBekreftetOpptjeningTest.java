package no.nav.foreldrepenger.domene.opptjening.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjonBuilder;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyringBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareTest;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class AksjonspunktutlederForVurderBekreftetOpptjeningTest extends EntityManagerAwareTest {

    private final AktørId AKTØRID = AktørId.dummy();
    private final LocalDate skjæringstidspunkt = LocalDate.now();

    private final InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private AksjonspunktutlederForVurderBekreftetOpptjening aksjonspunktutleder;
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private OpptjeningRepository opptjeningRepository;

    @BeforeEach
    void setUp() {
        var entityManager = getEntityManager();
        fagsakRepository = new FagsakRepository(entityManager);
        behandlingRepository = new BehandlingRepository(entityManager);
        opptjeningRepository = new OpptjeningRepository(entityManager, behandlingRepository);
        aksjonspunktutleder = new AksjonspunktutlederForVurderBekreftetOpptjening(opptjeningRepository, iayTjeneste);
    }

    @Test
    public void skal_gi_aksjonspunkt_for_fiktivt_arbeidsforhold() {
        // Arrange
        Behandling behandling = opprettBehandling();
        DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusMonths(3), skjæringstidspunkt);
        iayTjeneste.lagreArbeidsforhold(behandling.getId(), AKTØRID, ArbeidsforholdInformasjonBuilder.oppdatere(Optional.empty())
            .leggTil(ArbeidsforholdOverstyringBuilder
                .oppdatere(Optional.empty())
                .leggTilOverstyrtPeriode(periode.getFomDato(), periode.getTomDato())
                .medAngittStillingsprosent(new Stillingsprosent(100))
                .medArbeidsforholdRef(InternArbeidsforholdRef.nullRef())
                .medArbeidsgiver(Arbeidsgiver.virksomhet(OrgNummer.KUNSTIG_ORG))
                .medAngittArbeidsgiverNavn("Ambassade")));
        InntektArbeidYtelseGrunnlag iayGrunnlag = iayTjeneste.finnGrunnlag(behandling.getId()).get();
        var filter = new YrkesaktivitetFilter(iayGrunnlag.getArbeidsforholdInformasjon(), (Yrkesaktivitet) null);
        Yrkesaktivitet overstyrt = filter.getYrkesaktiviteter().iterator().next();
        // Act
        boolean girAksjonspunkt = aksjonspunktutleder.girAksjonspunktForArbeidsforhold(filter, behandling.getId(), null, overstyrt);
        // Assert
        assertThat(girAksjonspunkt).isTrue();
    }

    private Behandling opprettBehandling() {
        final Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNyNB(AKTØRID));
        fagsakRepository.opprettNy(fagsak);
        final Behandling.Builder builder = Behandling.forFørstegangssøknad(fagsak);
        final Behandling behandling = builder.build();
        Behandlingsresultat.opprettFor(behandling);
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        final VilkårResultat nyttResultat = VilkårResultat.builder().buildFor(behandling);
        behandlingRepository.lagre(nyttResultat, behandlingRepository.taSkriveLås(behandling));

        opptjeningRepository.lagreOpptjeningsperiode(behandling, skjæringstidspunkt.minusMonths(10), skjæringstidspunkt, false);
        return behandling;
    }
}
