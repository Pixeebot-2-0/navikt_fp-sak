package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.CREATE;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt.DRIFT;

import java.util.List;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.BehandlingAksjonspunktDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.ForvaltningBehandlingIdDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.SøknadGrunnlagManglerDto;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskStatus;
import no.nav.vedtak.felles.prosesstask.rest.dto.ProsessTaskIdDto;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;

@Path("/forvaltningTeknisk")
@ApplicationScoped
@Transactional
public class ForvaltningTekniskRestTjeneste {

    private static final Logger logger = LoggerFactory.getLogger(ForvaltningTekniskRestTjeneste.class);
    private static final String MANGLER_AP = "Utvikler-feil: Har ikke aksjonspunkt av type: ";

    private BehandlingRepository behandlingRepository;
    private SøknadRepository søknadRepository;
    private ProsessTaskRepository prosessTaskRepository;
    private AksjonspunktRepository aksjonspunktRepository = new AksjonspunktRepository();
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private OppgaveTjeneste oppgaveTjeneste;

    public ForvaltningTekniskRestTjeneste() {
        // For CDI
    }

    @Inject
    public ForvaltningTekniskRestTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                          ProsessTaskRepository prosessTaskRepository,
                                          OppgaveTjeneste oppgaveTjeneste,
                                          BehandlingskontrollTjeneste behandlingskontrollTjeneste) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.prosessTaskRepository = prosessTaskRepository;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.oppgaveTjeneste = oppgaveTjeneste;
    }

    @POST
    @Path("/sett-task-ferdig")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(description = "Setter prosesstask til status FERDIG",
        tags = "FORVALTNING-teknisk",
        responses = {
            @ApiResponse(responseCode = "200", description = "Task satt til ferdig."),
            @ApiResponse(responseCode = "400", description = "Fant ikke aktuell prosessTask."),
            @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil.")
        })
    @BeskyttetRessurs(action = CREATE, ressurs = DRIFT)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response setTaskFerdig(@TilpassetAbacAttributt(supplierClass = ForvaltningTekniskRestTjeneste.AbacDataSupplier.class)
                                      @Parameter(description = "Task som skal settes ferdig") @NotNull @Valid ProsessTaskIdDto taskId) {
        ProsessTaskData data = prosessTaskRepository.finn(taskId.getProsessTaskId());
        if (data != null) {
            data.setStatus(ProsessTaskStatus.FERDIG);
            data.setSisteFeil(null);
            data.setSisteFeilKode(null);
            prosessTaskRepository.lagre(data);
            return Response.ok().build();
        }
        return Response.status(Response.Status.BAD_REQUEST).build();
    }

    @POST
    @Path("/sett-oppgave-ferdig")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(description = "Ferdigstill Gosys-oppgave",
        tags = "FORVALTNING-teknisk",
        responses = {
            @ApiResponse(responseCode = "200", description = "Oppgave satt til ferdig."),
            @ApiResponse(responseCode = "400", description = "Fant ikke aktuell oppgave."),
            @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil.")
        })
    @BeskyttetRessurs(action = CREATE, ressurs = DRIFT)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response ferdigstillOppgave(@TilpassetAbacAttributt(supplierClass = ForvaltningTekniskRestTjeneste.AbacDataSupplier.class)
                                       @Parameter(description = "Oppgave som skal settes ferdig") @NotNull @Valid ProsessTaskIdDto oppgaveIdDto,
                                       @BeanParam @Valid ForvaltningBehandlingIdDto behandlingIdDto) {
        try {
            oppgaveTjeneste.ferdigstillOppgaveForForvaltning(behandlingIdDto.getBehandlingId(), oppgaveIdDto.getProsessTaskId().toString());
        } catch (Exception e) {
            logger.info("Feil fra Gosys ved ferdigstillelse", e);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        return Response.ok().build();
    }

    @POST
    @Path("/sett-oppgave-feilreg")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(description = "Ferdigstill Gosys-oppgave",
        tags = "FORVALTNING-teknisk",
        responses = {
            @ApiResponse(responseCode = "200", description = "Oppgave satt til ferdig."),
            @ApiResponse(responseCode = "400", description = "Fant ikke aktuell oppgave."),
            @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil.")
        })
    @BeskyttetRessurs(action = CREATE, ressurs = DRIFT)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response feilregistrerOppgave(@TilpassetAbacAttributt(supplierClass = ForvaltningTekniskRestTjeneste.AbacDataSupplier.class)
                                         @Parameter(description = "Oppgave som skal settes ferdig") @NotNull @Valid ProsessTaskIdDto oppgaveIdDto,
                                         @BeanParam @Valid ForvaltningBehandlingIdDto behandlingIdDto) {
        try {
            oppgaveTjeneste.feilregistrerOppgaveForForvaltning(behandlingIdDto.getBehandlingId(), oppgaveIdDto.getProsessTaskId().toString());
        } catch (Exception e) {
            logger.info("Feil fra Gosys ved ferdigstillelse", e);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        return Response.ok().build();
    }

    @POST
    @Path("/sett-aksjonspunkt-avbrutt")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(description = "Setter åpent aksjonspunkt til status AVBR",
        tags = "FORVALTNING-teknisk",
        responses = {
            @ApiResponse(responseCode = "200", description = "Aksjonspunkt avbrutt."),
            @ApiResponse(responseCode = "400", description = "Fant ikke aktuelt aksjonspunkt."),
            @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil.")
        })
    @BeskyttetRessurs(action = CREATE, ressurs = DRIFT)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response setAksjonspunktAvbrutt(@BeanParam @Valid BehandlingAksjonspunktDto dto) {
        Long behandlingId = dto.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        var lås = behandlingRepository.taSkriveLås(behandling.getId());
        Aksjonspunkt aksjonspunkt = behandling.getAksjonspunktFor(dto.getAksjonspunktKode())
            .orElseThrow(() -> new IllegalStateException(MANGLER_AP + dto.getAksjonspunktKode()));
        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling);
        behandlingskontrollTjeneste.lagreAksjonspunkterAvbrutt(kontekst, behandling.getAktivtBehandlingSteg(), List.of(aksjonspunkt));
        behandlingRepository.lagre(behandling, lås);
        return Response.ok().build();
    }

    @POST
    @Path("/sett-aksjonspunkt-entrinn")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(description = "Setter åpent aksjonspunkt til entrinn",
        tags = "FORVALTNING-teknisk",
        responses = {
            @ApiResponse(responseCode = "200", description = "Aksjonspunkt med totrinn."),
            @ApiResponse(responseCode = "400", description = "Fant ikke aktuelt aksjonspunkt."),
            @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil.")
        })
    @BeskyttetRessurs(action = CREATE, ressurs = DRIFT)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response setAksjonspunktEntrinn(@BeanParam @Valid BehandlingAksjonspunktDto dto) {
        Long behandlingId = dto.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        var lås = behandlingRepository.taSkriveLås(behandling.getId());
        Aksjonspunkt aksjonspunkt = behandling.getAksjonspunktFor(dto.getAksjonspunktKode())
            .filter(Aksjonspunkt::isToTrinnsBehandling)
            .orElseThrow(() -> new IllegalStateException(MANGLER_AP + dto.getAksjonspunktKode()));
        aksjonspunktRepository.fjernToTrinnsBehandlingKreves(aksjonspunkt);
        behandlingRepository.lagre(behandling, lås);
        return Response.ok().build();
    }

    @POST
    @Path("/sett-aksjonspunkt-totrinn")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(description = "Setter åpent aksjonspunkt til totrinn",
        tags = "FORVALTNING-teknisk",
        responses = {
            @ApiResponse(responseCode = "200", description = "Aksjonspunkt uten totrinn."),
            @ApiResponse(responseCode = "400", description = "Fant ikke aktuelt aksjonspunkt."),
            @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil.")
        })
    @BeskyttetRessurs(action = CREATE, ressurs = DRIFT)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response setAksjonspunktTotrinn(@BeanParam @Valid BehandlingAksjonspunktDto dto) {
        Long behandlingId = dto.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        var lås = behandlingRepository.taSkriveLås(behandling.getId());
        Aksjonspunkt aksjonspunkt = behandling.getAksjonspunktFor(dto.getAksjonspunktKode())
            .filter(ap -> !ap.isToTrinnsBehandling())
            .orElseThrow(() -> new IllegalStateException(MANGLER_AP + dto.getAksjonspunktKode()));
        aksjonspunktRepository.setToTrinnsBehandlingKreves(aksjonspunkt);
        behandlingRepository.lagre(behandling, lås);
        return Response.ok().build();
    }

    @POST
    @Path("/sett-behandling-entrinn")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(description = "Setter behandling til entrinn",
        tags = "FORVALTNING-teknisk",
        responses = {
            @ApiResponse(responseCode = "200", description = "Behandling er nå uten totrinn."),
            @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil.")
        })
    @BeskyttetRessurs(action = CREATE, ressurs = DRIFT)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response setBehandlingEntrinn(@BeanParam @Valid ForvaltningBehandlingIdDto dto) {
        Long behandlingId = dto.getBehandlingId();
        logger.info("Setter behandling={} til entrinn", behandlingId);
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        var lås = behandlingRepository.taSkriveLås(behandling.getId());

        behandling.nullstillToTrinnsBehandling();
        behandlingRepository.lagre(behandling, lås);
        return Response.ok().build();
    }

    @POST
    @Path("/sett-behandling-totrinn")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(description = "Setter behandling til totrinn",
        tags = "FORVALTNING-teknisk",
        responses = {
            @ApiResponse(responseCode = "200", description = "Behandling er nå med totrinn."),
            @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil.")
        })
    @BeskyttetRessurs(action = CREATE, ressurs = DRIFT)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response setBehandlingTotrinn(@BeanParam @Valid ForvaltningBehandlingIdDto dto) {
        Long behandlingId = dto.getBehandlingId();
        logger.info("Setter behandling={} til totrinn", behandlingId);
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        var lås = behandlingRepository.taSkriveLås(behandling.getId());

        behandling.setToTrinnsBehandling();
        behandlingRepository.lagre(behandling, lås);
        return Response.ok().build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Bytt aksjonspunkt til reg papir endringssøknad", tags = "FORVALTNING-teknisk")
    @Path("/endring-papir")
    @BeskyttetRessurs(action = READ, ressurs = BeskyttetRessursResourceAttributt.DRIFT, sporingslogg = false)
    public Response byttPapirSøknadTilEndring(@BeanParam @Valid ForvaltningBehandlingIdDto dto) {
        //fjern alle overstyringer gjort av saksbehandler
        var behandlingId = dto.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);

        var lås = behandlingRepository.taSkriveLås(behandling);

        if (!BehandlingStegType.REGISTRER_SØKNAD.equals(behandling.getAktivtBehandlingSteg())) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        behandling.getAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.REGISTRER_PAPIRSØKNAD_FORELDREPENGER).ifPresent(ap -> {
            BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling);
            behandlingskontrollTjeneste.lagreAksjonspunkterFunnet(kontekst, behandling.getAktivtBehandlingSteg(), List.of( AksjonspunktDefinisjon.REGISTRER_PAPIR_ENDRINGSØKNAD_FORELDREPENGER));
            behandlingskontrollTjeneste.lagreAksjonspunkterAvbrutt(kontekst, behandling.getAktivtBehandlingSteg(), List.of(ap));
        });
        behandlingRepository.lagre(behandling, lås);
        return Response.ok().build();
    }

    @POST
    @Path("/soknad-grunnlag-mangler")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(description = "Lagrer søknadsgrunnlag for gammelt format",
        tags = "FORVALTNING-teknisk",
        responses = {
            @ApiResponse(responseCode = "200", description = "Utført."),
            @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil.")
        })
    @BeskyttetRessurs(action = CREATE, ressurs = DRIFT)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response leggtilManglendeSøknadsGrunnlag(@BeanParam @Valid SøknadGrunnlagManglerDto dto) {
        Long behandlingId = dto.getBehandlingId();
        logger.info("Setter behandling={} til totrinn", behandlingId);
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        if (behandling == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        var relrolletype = "FARA".equals(dto.getRolle()) ? RelasjonsRolleType.FARA : RelasjonsRolleType.MORA;
        SøknadEntitet.Builder søknadBuilder = new SøknadEntitet.Builder()
            .medSøknadsdato(dto.getSøknadDato())
            .medMottattDato(dto.getMottattDato())
            .medElektroniskRegistrert(true)
            .medErEndringssøknad(false)
            .medSpråkkode(Språkkode.nb)
            .medRelasjonsRolleType(relrolletype)
            .medTilleggsopplysninger(dto.getTillegg())
            .medBegrunnelseForSenInnsending(dto.getForsent());
        søknadRepository.lagreOgFlush(behandling, søknadBuilder.build());
        return Response.ok().build();
    }

    public static class AbacDataSupplier implements Function<Object, AbacDataAttributter> {
        @Override
        public AbacDataAttributter apply(Object obj) {
            return AbacDataAttributter.opprett();
        }
    }

}
