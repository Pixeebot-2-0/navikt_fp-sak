package no.nav.foreldrepenger.tilganger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.foreldrepenger.web.app.util.LdapUtil;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

@ApplicationScoped
public class TilgangerTjeneste {

    private String gruppenavnSaksbehandler;
    private String gruppenavnVeileder;
    private String gruppenavnBeslutter;
    private String gruppenavnOverstyrer;
    private String gruppenavnOppgavestyrer;
    private String gruppenavnEgenAnsatt;
    private String gruppenavnKode6;
    private String gruppenavnKode7;

    public TilgangerTjeneste() {
        // CDI
    }

    @Inject
    public TilgangerTjeneste(
        @KonfigVerdi(value = "bruker.gruppenavn.saksbehandler") String gruppenavnSaksbehandler,
        @KonfigVerdi(value = "bruker.gruppenavn.veileder") String gruppenavnVeileder,
        @KonfigVerdi(value = "bruker.gruppenavn.beslutter") String gruppenavnBeslutter,
        @KonfigVerdi(value = "bruker.gruppenavn.overstyrer") String gruppenavnOverstyrer,
        @KonfigVerdi(value = "bruker.gruppenavn.oppgavestyrer") String gruppenavnOppgavestyrer,
        @KonfigVerdi(value = "bruker.gruppenavn.egenansatt") String gruppenavnEgenAnsatt,
        @KonfigVerdi(value = "bruker.gruppenavn.kode6") String gruppenavnKode6,
        @KonfigVerdi(value = "bruker.gruppenavn.kode7") String gruppenavnKode7
    ) {
        this.gruppenavnSaksbehandler = gruppenavnSaksbehandler;
        this.gruppenavnVeileder = gruppenavnVeileder;
        this.gruppenavnBeslutter = gruppenavnBeslutter;
        this.gruppenavnOverstyrer = gruppenavnOverstyrer;
        this.gruppenavnOppgavestyrer = gruppenavnOppgavestyrer;
        this.gruppenavnEgenAnsatt = gruppenavnEgenAnsatt;
        this.gruppenavnKode6 = gruppenavnKode6;
        this.gruppenavnKode7 = gruppenavnKode7;
    }

    public InnloggetNavAnsattDto innloggetBruker() {
        var ident = KontekstHolder.getKontekst().getUid();
        var ldapBruker = new LdapBrukeroppslag().hentBrukerinformasjon(ident);
        return getInnloggetBruker(ident, ldapBruker);
    }

    InnloggetNavAnsattDto getInnloggetBruker(String ident, LdapBruker ldapBruker) {
        var navn = ldapBruker.displayName();
        var grupper = LdapUtil.filtrerGrupper(ldapBruker.groups());
        return new InnloggetNavAnsattDto.Builder(ident, navn)
            .kanSaksbehandle(grupper.contains(gruppenavnSaksbehandler))
            .kanVeilede(grupper.contains(gruppenavnVeileder))
            .kanBeslutte(grupper.contains(gruppenavnBeslutter))
            .kanOverstyre(grupper.contains(gruppenavnOverstyrer))
            .kanOppgavestyre(grupper.contains(gruppenavnOppgavestyrer))
            .kanBehandleKodeEgenAnsatt(grupper.contains(gruppenavnEgenAnsatt))
            .kanBehandleKode6(grupper.contains(gruppenavnKode6))
            .kanBehandleKode7(grupper.contains(gruppenavnKode7))
            .build();
    }

}
