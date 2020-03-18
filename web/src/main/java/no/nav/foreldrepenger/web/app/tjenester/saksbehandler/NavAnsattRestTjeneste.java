package no.nav.foreldrepenger.web.app.tjenester.saksbehandler;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt.APPLIKASJON;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;


import io.swagger.v3.oas.annotations.Operation;
import no.nav.foreldrepenger.tilganger.InnloggetNavAnsattDto;
import no.nav.foreldrepenger.tilganger.TilgangerTjeneste;
import no.nav.vedtak.felles.jpa.Transaction;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;

@Path("/nav-ansatt")
@ApplicationScoped
@Transaction
public class NavAnsattRestTjeneste {
    private TilgangerTjeneste tilgangerTjeneste;

    public NavAnsattRestTjeneste() {
        //NOSONAR
    }

    @Inject
    public NavAnsattRestTjeneste(TilgangerTjeneste tilgangerTjeneste) {
        this.tilgangerTjeneste = tilgangerTjeneste;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        description = "Returnerer fullt navn for ident",
        tags = "nav-ansatt",
        summary = ("Ident hentes fra sikkerhetskonteksten som er tilgjengelig etter innlogging.")
    )
    @BeskyttetRessurs(action = READ, ressurs = APPLIKASJON, sporingslogg = false)
    public InnloggetNavAnsattDto innloggetBruker() {
        return tilgangerTjeneste.innloggetBruker();
    }

}
