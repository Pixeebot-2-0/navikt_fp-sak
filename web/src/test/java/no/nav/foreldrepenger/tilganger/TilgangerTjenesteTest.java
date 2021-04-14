package no.nav.foreldrepenger.tilganger;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.vedtak.felles.integrasjon.ldap.LdapBruker;

public class TilgangerTjenesteTest {

    private static final String gruppenavnSaksbehandler = "Saksbehandler";
    private static final String gruppenavnVeileder = "Veileder";
    private static final String gruppenavnBeslutter = "Beslutter";
    private static final String gruppenavnOverstyrer = "Overstyrer";
    private static final String gruppenavnEgenAnsatt = "EgenAnsatt";
    private static final String gruppenavnKode6 = "Kode6";
    private static final String gruppenavnKode7 = "Kode7";
    private static final Boolean skalViseDetaljerteFeilmeldinger = true;
    private TilgangerTjeneste tilgangerTjeneste;

    @BeforeEach
    public void setUp() {
        tilgangerTjeneste = new TilgangerTjeneste(gruppenavnSaksbehandler, gruppenavnVeileder, gruppenavnBeslutter, gruppenavnOverstyrer,
                gruppenavnEgenAnsatt, gruppenavnKode6, gruppenavnKode7, skalViseDetaljerteFeilmeldinger);
    }

    @Test
    public void skalMappeSaksbehandlerGruppeTilKanSaksbehandleRettighet() {
        var brukerUtenforSaksbehandlerGruppe = getTestBruker();
        var brukerISaksbehandlerGruppe = getTestBruker(gruppenavnSaksbehandler);

        var innloggetBrukerUtenSaksbehandlerRettighet = tilgangerTjeneste.getInnloggetBruker(null,
                brukerUtenforSaksbehandlerGruppe);
        var innloggetBrukerMedSaksbehandlerRettighet = tilgangerTjeneste.getInnloggetBruker(null, brukerISaksbehandlerGruppe);

        assertThat(innloggetBrukerUtenSaksbehandlerRettighet.getKanSaksbehandle()).isFalse();
        assertThat(innloggetBrukerMedSaksbehandlerRettighet.getKanSaksbehandle()).isTrue();
    }

    @Test
    public void skalMappeVeilederGruppeTilKanVeiledeRettighet() {
        var brukerUtenforVeilederGruppe = getTestBruker();
        var brukerIVeilederGruppe = getTestBruker(gruppenavnVeileder);

        var innloggetBrukerUtenVeilederRettighet = tilgangerTjeneste.getInnloggetBruker(null, brukerUtenforVeilederGruppe);
        var innloggetBrukerMedVeilederRettighet = tilgangerTjeneste.getInnloggetBruker(null, brukerIVeilederGruppe);

        assertThat(innloggetBrukerUtenVeilederRettighet.getKanVeilede()).isFalse();
        assertThat(innloggetBrukerMedVeilederRettighet.getKanVeilede()).isTrue();
    }

    @Test
    public void skalMappeBeslutterGruppeTilKanBeslutteRettighet() {
        var brukerUtenforBeslutterGruppe = getTestBruker();
        var brukerIBeslutterGruppe = getTestBruker(gruppenavnBeslutter);

        var innloggetBrukerUtenBeslutterRettighet = tilgangerTjeneste.getInnloggetBruker(null, brukerUtenforBeslutterGruppe);
        var innloggetBrukerMedBeslutterRettighet = tilgangerTjeneste.getInnloggetBruker(null, brukerIBeslutterGruppe);

        assertThat(innloggetBrukerUtenBeslutterRettighet.getKanBeslutte()).isFalse();
        assertThat(innloggetBrukerMedBeslutterRettighet.getKanBeslutte()).isTrue();
    }

    @Test
    public void skalMappeOverstyrerGruppeTilKanOverstyreRettighet() {
        var brukerUtenforOverstyrerGruppe = getTestBruker();
        var brukerIOverstyrerGruppe = getTestBruker(gruppenavnOverstyrer);

        var innloggetBrukerUtenOverstyrerRettighet = tilgangerTjeneste.getInnloggetBruker(null, brukerUtenforOverstyrerGruppe);
        var innloggetBrukerMedOverstyrerRettighet = tilgangerTjeneste.getInnloggetBruker(null, brukerIOverstyrerGruppe);

        assertThat(innloggetBrukerUtenOverstyrerRettighet.getKanOverstyre()).isFalse();
        assertThat(innloggetBrukerMedOverstyrerRettighet.getKanOverstyre()).isTrue();
    }

    @Test
    public void skalMappeEgenAnsattGruppeTilKanBehandleEgenAnsattRettighet() {
        var brukerUtenforEgenAnsattGruppe = getTestBruker();
        var brukerIEgenAnsattGruppe = getTestBruker(gruppenavnEgenAnsatt);

        var innloggetBrukerUtenEgenAnsattRettighet = tilgangerTjeneste.getInnloggetBruker(null, brukerUtenforEgenAnsattGruppe);
        var innloggetBrukerMedEgenAnsattRettighet = tilgangerTjeneste.getInnloggetBruker(null, brukerIEgenAnsattGruppe);

        assertThat(innloggetBrukerUtenEgenAnsattRettighet.getKanBehandleKodeEgenAnsatt()).isFalse();
        assertThat(innloggetBrukerMedEgenAnsattRettighet.getKanBehandleKodeEgenAnsatt()).isTrue();
    }

    @Test
    public void skalMappeKode6GruppeTilKanBehandleKode6Rettighet() {
        var brukerUtenforKode6Gruppe = getTestBruker();
        var brukerIKode6Gruppe = getTestBruker(gruppenavnKode6);

        var innloggetBrukerUtenKode6Rettighet = tilgangerTjeneste.getInnloggetBruker(null, brukerUtenforKode6Gruppe);
        var innloggetBrukerMedKode6Rettighet = tilgangerTjeneste.getInnloggetBruker(null, brukerIKode6Gruppe);

        assertThat(innloggetBrukerUtenKode6Rettighet.getKanBehandleKode6()).isFalse();
        assertThat(innloggetBrukerMedKode6Rettighet.getKanBehandleKode6()).isTrue();
    }

    @Test
    public void skalMappeKode7GruppeTilKanBehandleKode7Rettighet() {
        var brukerUtenforKode7Gruppe = getTestBruker();
        var brukerIKode7Gruppe = getTestBruker(gruppenavnKode7);

        var innloggetBrukerUtenKode7Rettighet = tilgangerTjeneste.getInnloggetBruker(null, brukerUtenforKode7Gruppe);
        var innloggetBrukerMedKode7Rettighet = tilgangerTjeneste.getInnloggetBruker(null, brukerIKode7Gruppe);

        assertThat(innloggetBrukerUtenKode7Rettighet.getKanBehandleKode7()).isFalse();
        assertThat(innloggetBrukerMedKode7Rettighet.getKanBehandleKode7()).isTrue();
    }

    private static LdapBruker getTestBruker(String... grupper) {
        return new LdapBruker("Testbruker", List.of(grupper));
    }

}
