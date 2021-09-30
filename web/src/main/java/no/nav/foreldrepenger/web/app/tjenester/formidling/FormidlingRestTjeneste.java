package no.nav.foreldrepenger.web.app.tjenester.formidling;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.abac.FPSakBeskyttetRessursAttributt;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingAbacSuppliers;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingIdDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.BehandlingFormidlingDtoTjeneste;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;


@Path(FormidlingRestTjeneste.BASE_PATH)
@ApplicationScoped
@Transactional
// Tilbyr data til fp-formidling, formidlingsløsning ut mot søker.
public class FormidlingRestTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(FormidlingRestTjeneste.class);

    public static final String BASE_PATH = "/formidling";
    public static final String RESSURSER_PART_PATH = "/ressurser";
    public static final String RESSURSER_PATH = BASE_PATH + RESSURSER_PART_PATH;
    public static final String DOKMAL_INNVFP_PART_PATH = "/dokumentmal/innvfp";
    public static final String DOKMAL_INNVFP_PATH = BASE_PATH + DOKMAL_INNVFP_PART_PATH;

    private BehandlingRepository behandlingRepository;
    private BehandlingFormidlingDtoTjeneste behandlingFormidlingDtoTjeneste;

    @Inject
    public FormidlingRestTjeneste(BehandlingRepository behandlingRepository,
                                  BehandlingFormidlingDtoTjeneste behandlingFormidlingDtoTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.behandlingFormidlingDtoTjeneste = behandlingFormidlingDtoTjeneste;
    }

    public FormidlingRestTjeneste() {
    }

    @GET
    @Path(RESSURSER_PART_PATH)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Operation(description = "Hent behandling med tilhørende ressurslenker for bruk i formidling", tags = "formidling")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public Response hentBehandlingDtoForBrev(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.BehandlingIdAbacDataSupplier.class)
        @NotNull @Parameter(description = "UUID for behandlingen") @QueryParam("behandlingId") @Valid BehandlingIdDto behandlingIdDto) {
        var behandling = behandlingRepository.hentBehandlingHvisFinnes(behandlingIdDto.getBehandlingUuid());
        var dto = behandling.map(value -> behandlingFormidlingDtoTjeneste.lagDtoForFormidling(value)).orElse(null);
        var responseBuilder = Response.ok().entity(dto);
        return responseBuilder.build();
    }

}
