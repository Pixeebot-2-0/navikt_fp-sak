package no.nav.foreldrepenger.web.app.tjenester.register;


import java.net.URI;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerAbacSupplier;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;


@ApplicationScoped
@Transactional
@Path(RedirectToRegisterRestTjeneste.BASE_PATH)
public class RedirectToRegisterRestTjeneste {
    public static final String BASE_PATH = "/register/redirect-to";

    private static final String PGI = "PensjonsgivendeA-Inntekt";
    private static final String SAMMENLIGN = "8-30";

    private static final String AAREG_REG_POSTFIX = "/aa-reg";
    private static final String AINNTEKT_REG_POSTFIX = "/a-inntekt";
    private static final String AINNTEKT_PGI_REG_POSTFIX = "/a-inntekt-pgi";
    public static final String AAREG_REG_PATH = BASE_PATH + AAREG_REG_POSTFIX;
    public static final String AINNTEKT_REG_PATH = BASE_PATH + AINNTEKT_REG_POSTFIX;
    public static final String AINNTEKT_PGI_REG_PATH = BASE_PATH + AINNTEKT_PGI_REG_POSTFIX;

    private PersoninfoAdapter personinfoAdapter;
    private FagsakRepository fagsakRepository;
    private RegisterPathTjeneste registerPathTjeneste;

    RedirectToRegisterRestTjeneste() {
        // CDI
    }

    @Inject
    public RedirectToRegisterRestTjeneste(PersoninfoAdapter personinfoAdapter, FagsakRepository fagsakRepository, RegisterPathTjeneste registerPathTjeneste) {
        this.personinfoAdapter = personinfoAdapter;
        this.fagsakRepository = fagsakRepository;
        this.registerPathTjeneste = registerPathTjeneste;
    }

    @GET
    @Operation(description = "Redirecter til aa-reg for arbeidstakeren", tags = "aktoer", responses = {
        @ApiResponse(responseCode = "307", description = "Redirecter til aa-reg for arbeidstakeren")
    })
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK)
    @Path(AAREG_REG_POSTFIX)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response getAaregUrl(@TilpassetAbacAttributt(supplierClass = SaksnummerAbacSupplier.Supplier.class) @NotNull @QueryParam("saksnummer") @Valid SaksnummerDto saksnummerDto) {
        var fagsak = fagsakRepository.hentSakGittSaksnummer(new Saksnummer(saksnummerDto.getVerdi())).orElseThrow();
        var personIdent = personinfoAdapter.hentFnrForAktør(fagsak.getAktørId());

        var respons = registerPathTjeneste.hentAaregPath(personIdent);
        var redirectUri = URI.create(respons);
        return Response.temporaryRedirect(redirectUri).build();
    }

    @GET
    @Operation(description = "Redirecter til a-inntekt 8-30 for arbeidstakeren", tags = "aktoer", responses = {
        @ApiResponse(responseCode = "307", description = "Redirecter til a-inntekt for arbeidstakeren")
    })
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK)
    @Path(AINNTEKT_REG_POSTFIX)
    public Response getAInntektUrl(@TilpassetAbacAttributt(supplierClass = SaksnummerAbacSupplier.Supplier.class) @NotNull @QueryParam("saksnummer") @Valid SaksnummerDto saksnummerDto) {
        var fagsak = fagsakRepository.hentSakGittSaksnummer(new Saksnummer(saksnummerDto.getVerdi())).orElseThrow();
        var personIdent = personinfoAdapter.hentFnrForAktør(fagsak.getAktørId());

        var respons = registerPathTjeneste.hentAinntektPath(personIdent, SAMMENLIGN);
        var redirectUri = URI.create(respons);
        return Response.temporaryRedirect(redirectUri).build();
    }

    @GET
    @Operation(description = "Redirecter til a-inntekt PGI  for arbeidstakeren", tags = "aktoer", responses = {
        @ApiResponse(responseCode = "307", description = "Redirecter til a-inntekt for arbeidstakeren")
    })
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK)
    @Path(AINNTEKT_PGI_REG_POSTFIX)
    public Response getAInntektPGIUrl(@TilpassetAbacAttributt(supplierClass = SaksnummerAbacSupplier.Supplier.class) @NotNull @QueryParam("saksnummer") @Valid SaksnummerDto saksnummerDto) {
        var fagsak = fagsakRepository.hentSakGittSaksnummer(new Saksnummer(saksnummerDto.getVerdi())).orElseThrow();
        var personIdent = personinfoAdapter.hentFnrForAktør(fagsak.getAktørId());

        var respons = registerPathTjeneste.hentAinntektPath(personIdent, PGI);
        var redirectUri = URI.create(respons);
        return Response.temporaryRedirect(redirectUri).build();
    }
}
