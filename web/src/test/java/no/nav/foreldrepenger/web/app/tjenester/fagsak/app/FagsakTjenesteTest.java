package no.nav.foreldrepenger.web.app.tjenester.fagsak.app;

import static java.lang.String.valueOf;
import static java.time.Month.JANUARY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.DekningsgradTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.HendelseVersjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.testutilities.aktør.FiktiveFnr;
import no.nav.foreldrepenger.behandlingslager.testutilities.aktør.NavBrukerBuilder;
import no.nav.foreldrepenger.behandlingslager.testutilities.fagsak.FagsakBuilder;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.ProsesseringAsynkTjeneste;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;

@ExtendWith(MockitoExtension.class)
public class FagsakTjenesteTest {

    private static final String FNR = new FiktiveFnr().nesteFnr();
    private static final AktørId AKTØR_ID = AktørId.dummy();
    private static final Saksnummer SAKSNUMMER = new Saksnummer("123");

    private FagsakTjeneste tjeneste;
    @Mock
    private FagsakRepository fagsakRepository;
    @Mock
    private BehandlingRepository behandlingRepository;
    @Mock
    private PersoninfoAdapter personinfoAdapter;
    @Mock
    private FamilieHendelseTjeneste hendelseTjeneste;
    @Mock
    private DekningsgradTjeneste dekningsgradTjeneste;

    private static FamilieHendelseGrunnlagEntitet byggHendelseGrunnlag(LocalDate fødselsdato, LocalDate oppgittFødselsdato) {
        final var hendelseBuilder = FamilieHendelseBuilder.oppdatere(Optional.empty(), HendelseVersjonType.SØKNAD);
        if (oppgittFødselsdato != null) {
            hendelseBuilder.medFødselsDato(oppgittFødselsdato);
        }
        return FamilieHendelseGrunnlagBuilder.oppdatere(Optional.empty())
                .medSøknadVersjon(hendelseBuilder)
                .medBekreftetVersjon(FamilieHendelseBuilder.oppdatere(Optional.empty(), HendelseVersjonType.BEKREFTET)
                        .medFødselsDato(fødselsdato))
                .build();
    }

    @BeforeEach
    public void oppsett() {
        var prosesseringAsynkTjeneste = mock(ProsesseringAsynkTjeneste.class);
        tjeneste = new FagsakTjeneste(fagsakRepository, behandlingRepository, prosesseringAsynkTjeneste, personinfoAdapter, null, hendelseTjeneste, null,
                dekningsgradTjeneste);
    }

    @Test
    public void skal_hente_saker_på_fnr() {
        var navBruker = new NavBrukerBuilder().medAktørId(AKTØR_ID).build();
        when(personinfoAdapter.hentAktørForFnr(new PersonIdent(FNR))).thenReturn(Optional.of(AKTØR_ID));

        var fagsak = FagsakBuilder.nyEngangstønad(RelasjonsRolleType.MORA).medBruker(navBruker).medSaksnummer(SAKSNUMMER).build();
        // Whitebox.setInternalState(fagsak, "id", -1L);
        fagsak.setId(-1L);
        when(fagsakRepository.hentForBruker(AKTØR_ID)).thenReturn(Collections.singletonList(fagsak));

        var fødselsdato = LocalDate.of(2017, JANUARY, 1);
        final var grunnlag = byggHendelseGrunnlag(fødselsdato, fødselsdato);
        when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(anyLong()))
                .thenReturn(Optional.of(Behandling.forFørstegangssøknad(fagsak).build()));
        when(hendelseTjeneste.finnAggregat(any())).thenReturn(Optional.of(grunnlag));
        var dekningsgrad = Optional.of(Dekningsgrad._100);
        when(dekningsgradTjeneste.finnDekningsgrad(any())).thenReturn(dekningsgrad);

        var view = tjeneste.søkFagsakDto(FNR);

        assertThat(view).hasSize(1);
        assertThat(view.get(0).getSaksnummer()).isEqualTo(fagsak.getSaksnummer().getVerdi());
        assertThat(view.get(0).getBarnFodt()).isEqualTo(fødselsdato);
        assertThat(view.get(0).getFagsakYtelseType()).isEqualTo(FagsakYtelseType.ENGANGSTØNAD);
        assertThat(view.get(0).getDekningsgrad()).isEqualTo(Dekningsgrad._100.getVerdi());
    }

    @Test
    public void skal_hente_saker_på_saksreferanse() {
        var navBruker = new NavBrukerBuilder().medAktørId(AKTØR_ID).build();
        var fagsak = FagsakBuilder.nyEngangstønad(RelasjonsRolleType.MORA).medBruker(navBruker).medSaksnummer(SAKSNUMMER).build();
        // Whitebox.setInternalState(fagsak, "id", -1L);
        fagsak.setId(-1L);
        when(fagsakRepository.hentSakGittSaksnummer(SAKSNUMMER)).thenReturn(Optional.of(fagsak));

        final var fødselsdato = LocalDate.of(2017, JANUARY, 1);
        final var grunnlag = byggHendelseGrunnlag(fødselsdato, fødselsdato);
        when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(anyLong()))
                .thenReturn(Optional.of(Behandling.forFørstegangssøknad(fagsak).build()));
        when(hendelseTjeneste.finnAggregat(any())).thenReturn(Optional.of(grunnlag));
        var dekningsgrad = Optional.of(Dekningsgrad._80);
        when(dekningsgradTjeneste.finnDekningsgrad(any())).thenReturn(dekningsgrad);

        var view = tjeneste.søkFagsakDto(SAKSNUMMER.getVerdi());

        assertThat(view).hasSize(1);
        assertThat(view.get(0).getSaksnummer()).isEqualTo(fagsak.getSaksnummer().getVerdi());
        assertThat(view.get(0).getBarnFodt()).isEqualTo(fødselsdato);
        assertThat(view.get(0).getDekningsgrad()).isEqualTo(Dekningsgrad._80.getVerdi());
    }

    @Test
    public void skal_returnere_tomt_view_når_fagsakens_bruker_er_ukjent_for_tps() {
        // Arrange
        var navBruker = new NavBrukerBuilder().medAktørId(AKTØR_ID).build();
        var fagsak = FagsakBuilder.nyEngangstønad(RelasjonsRolleType.MORA).medBruker(navBruker).medSaksnummer(SAKSNUMMER).build();
        // Whitebox.setInternalState(fagsak, "id", -1L);
        fagsak.setId(-1L);
        var view = tjeneste.søkFagsakDto(valueOf(SAKSNUMMER));

        assertThat(view).isEmpty();
    }

    @Test
    public void skal_returnere_tomt_view_dersom_søkestreng_ikke_er_gyldig_fnr_eller_saksnr() {
        var view = tjeneste.søkFagsakDto("ugyldig_søkestreng");
        assertThat(view).isEmpty();
    }

    @Test
    public void skal_returnere_tomt_view_ved_ukjent_fnr() {
        when(personinfoAdapter.hentAktørForFnr(new PersonIdent(FNR))).thenReturn(Optional.empty());

        var view = tjeneste.søkFagsakDto(FNR);

        assertThat(view).isEmpty();
    }

    @Test
    public void skal_returnere_tomt_view_ved_ukjent_saksnr() {
        var view = tjeneste.søkFagsakDto(valueOf(SAKSNUMMER));
        assertThat(view).isEmpty();
    }
}
