package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.hibernate.query.NativeQuery;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.OverlappVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.OverlappVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.mottak.vedtak.avstemming.VedtakOverlappAvstemTask;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerAbacSupplier;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.AksjonspunktKodeDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.AvstemmingPeriodeDto;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskStatus;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.impl.ProsessTaskEntitet;
import no.nav.vedtak.log.mdc.MDCOperations;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Path("/forvaltningUttrekk")
@ApplicationScoped
@Transactional
public class ForvaltningUttrekkRestTjeneste {

    private EntityManager entityManager;
    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;
    private ProsessTaskTjeneste taskTjeneste;
    private OverlappVedtakRepository overlappRepository;

    public ForvaltningUttrekkRestTjeneste() {
        // For CDI
    }

    @Inject
    public ForvaltningUttrekkRestTjeneste(EntityManager entityManager,
                                          FagsakRepository fagsakRepository,
                                          BehandlingRepository behandlingRepository,
                                          ProsessTaskTjeneste taskTjeneste,
                                          OverlappVedtakRepository overlappRepository) {
        this.entityManager = entityManager;
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
        this.taskTjeneste = taskTjeneste;
        this.overlappRepository = overlappRepository;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Setter saker med revurdering til under behandling", tags = "FORVALTNING-uttrekk")
    @Path("/openIkkeLopendeSaker")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT, sporingslogg = false)
    public Response openIkkeLopendeSaker(@Parameter(description = "Aksjonspunktkoden") @BeanParam @Valid AksjonspunktKodeDto dto) {
        var apDef = dto.getAksjonspunktDefinisjon();
        if (apDef == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        var query = entityManager.createNativeQuery("""
                select saksnummer, id from fagsak where fagsak_status in (:fstatus)
                and id in (select fagsak_id from behandling where behandling_status not in (:bstatus))
                """); //$NON-NLS-1$
        query.setParameter("fstatus", List.of(FagsakStatus.AVSLUTTET.getKode(), FagsakStatus.LØPENDE.getKode()));
        query.setParameter("bstatus", List.of(BehandlingStatus.IVERKSETTER_VEDTAK.getKode(), BehandlingStatus.AVSLUTTET.getKode()));
        @SuppressWarnings("unchecked")
        List<Object[]> resultatList = query.getResultList();
        var saker = resultatList.stream()
            .map(row -> new FagsakTreff((String) row[0], ((BigDecimal) row[1]).longValue())).collect(Collectors.toList()); // NOSONAR
        saker.forEach(f -> fagsakRepository.oppdaterFagsakStatus(f.fagsakId(), FagsakStatus.UNDER_BEHANDLING));
        return Response.ok().build();
    }

    public record FagsakTreff(String saksnummer, Long fagsakId) {
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Gir åpne aksjonspunkter med angitt kode", tags = "FORVALTNING-uttrekk")
    @Path("/openAutopunkt")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT, sporingslogg = false)
    public Response openAutopunkt(@Parameter(description = "Aksjonspunktkoden") @BeanParam @Valid AksjonspunktKodeDto dto) {
        var apDef = dto.getAksjonspunktDefinisjon();
        if (apDef == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        var query = entityManager.createNativeQuery("""
                select saksnummer, ytelse_type, ap.opprettet_tid, ap.frist_tid
                from fagsak fs
                join behandling bh on bh.fagsak_id = fs.id
                join aksjonspunkt ap on ap.behandling_id = bh.id
                where ap.aksjonspunkt_def = :apdef and ap.aksjonspunkt_status = :status"""); //$NON-NLS-1$
        query.setParameter("apdef", apDef.getKode());
        query.setParameter("status", AksjonspunktStatus.OPPRETTET.getKode());
        @SuppressWarnings("unchecked")
        List<Object[]> resultatList = query.getResultList();
        var åpneAksjonspunkt = resultatList.stream()
                .map(this::mapFraAksjonspunktTilDto)
                .collect(Collectors.toList());
        return Response.ok(åpneAksjonspunkt).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Flytt tilbake til omsorgrett", tags = "FORVALTNING-uttrekk")
    @Path("/flyttTilStartUttak")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT, sporingslogg = false)
    public Response flyttTilOmsorgRett() {
        var query = entityManager.createNativeQuery("""
            select saksnummer, bh.id
            from fagsak fs
            join behandling bh on bh.fagsak_id = fs.id
            join aksjonspunkt ap on ap.behandling_id = bh.id
            where aksjonspunkt_def in (:apdef) and aksjonspunkt_status = :status and behandling_type = :btype
             """); //$NON-NLS-1$
        query.setParameter("apdef", Set.of(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_SØKNADSFRIST.getKode()));
        query.setParameter("status", AksjonspunktStatus.OPPRETTET.getKode());
        query.setParameter("btype", BehandlingType.REVURDERING.getKode());
        @SuppressWarnings("unchecked")
        List<Object[]> resultatList = query.getResultList();
        var åpneAksjonspunkt = resultatList.stream().map(r -> new KabalFlytt((String) r[0], ((BigDecimal) r[1]).longValue())).toList();
        åpneAksjonspunkt.forEach(this::flyttTilbakeTilOmsorgRett);
        return Response.ok().build();
    }

    private record KabalFlytt(String saksnummer, Long behandlingId) { }

    private void flyttTilbakeTilOmsorgRett(KabalFlytt behandlingRef) {
        var behandling = behandlingRepository.hentBehandling(behandlingRef.behandlingId());
        if (!BehandlingStegType.SØKNADSFRIST_FORELDREPENGER.equals(behandling.getAktivtBehandlingSteg())) {
            return;
        }
        var tilKabalTask = ProsessTaskData.forProsessTask(MigrerTilOmsorgRettTask.class);
        tilKabalTask.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        tilKabalTask.setCallIdFraEksisterende();
        taskTjeneste.lagre(tilKabalTask);

    }

    private OpenAutopunkt mapFraAksjonspunktTilDto(Object[] row) {
        return new OpenAutopunkt((String) row[0], (String) row[1], ((Timestamp) row[2]).toLocalDateTime().toLocalDate(),
            row[3] != null ? ((Timestamp) row[3]).toLocalDateTime().toLocalDate() : null);
    }

    public record OpenAutopunkt(String saksnummer, String ytelseType, LocalDate aksjonspunktOpprettetDato, LocalDate aksjonspunktFristDato) {
    }

    @GET
    @Path("/listFagsakUtenBehandling")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent liste av saknumre for fagsak uten noen behandlinger", tags = "FORVALTNING-uttrekk", responses = {
            @ApiResponse(responseCode = "200", description = "Fagsaker uten behandling", content = @Content(array = @ArraySchema(arraySchema = @Schema(implementation = List.class), schema = @Schema(implementation = SaksnummerDto.class)), mediaType = MediaType.APPLICATION_JSON))
    })
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK)
    public List<SaksnummerDto> listFagsakUtenBehandling() {
        return fagsakRepository.hentÅpneFagsakerUtenBehandling().stream().map(SaksnummerDto::new).collect(Collectors.toList());
    }

    @POST
    @Path("/avstemPeriodeOverlappTrex")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Lagrer task for å finne overlapp. Resultat i app-logg", tags = "FORVALTNING-uttrekk")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT)
    public Response avstemPeriodeForOverlapp(@Parameter(description = "Periode") @BeanParam @Valid AvstemmingPeriodeDto dto) {
        if (!dto.getKey().equals(VedtakOverlappAvstemTask.LOG_TEMA_FOR_KEY) && !dto.getKey().equals(VedtakOverlappAvstemTask.LOG_TEMA_OTH_KEY)) return Response.ok().build();
        var fom = LocalDate.of(2018,10,20);
        var spread = 7199;
        if (dto.getFom().isAfter(fom)) fom = dto.getFom();
        var baseline = LocalDateTime.now();
        if (MDCOperations.getCallId() == null) MDCOperations.putCallId();
        var callId = MDCOperations.getCallId();
        int suffix = 1;
        for (var betweendays = fom; !betweendays.isAfter(dto.getTom()); betweendays = betweendays.plusDays(1)) {
            var prosessTaskData = ProsessTaskData.forProsessTask(VedtakOverlappAvstemTask.class);
            prosessTaskData.setProperty(VedtakOverlappAvstemTask.LOG_TEMA_KEY_KEY, dto.getKey());
            prosessTaskData.setProperty(VedtakOverlappAvstemTask.LOG_FOM_KEY, betweendays.toString());
            prosessTaskData.setProperty(VedtakOverlappAvstemTask.LOG_TOM_KEY, betweendays.toString());
            prosessTaskData.setNesteKjøringEtter(baseline.plusSeconds(LocalDateTime.now().getNano() % spread));
            prosessTaskData.setCallId(callId + "_" + suffix);
            prosessTaskData.setPrioritet(50);
            taskTjeneste.lagre(prosessTaskData);
            suffix++;
        }

        return Response.ok().build();
    }

    @POST
    @Path("/avstemPeriodeOverlappSpøkelseEnDag")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Lagrer task for å finne overlapp. Resultat i app-logg", tags = "FORVALTNING-uttrekk")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT)
    public Response avstemPeriodeForSpøkelseEnDag(@Parameter(description = "Periode") @BeanParam @Valid AvstemmingPeriodeDto dto) {
        if (!dto.getKey().equals(VedtakOverlappAvstemTask.LOG_TEMA_FOR_KEY) && !dto.getKey().equals(VedtakOverlappAvstemTask.LOG_TEMA_OTH_KEY)) return Response.ok().build();
        var fom = LocalDate.of(2018,10,20);
        if (dto.getFom().isAfter(fom)) fom = dto.getFom();
        if (MDCOperations.getCallId() == null) MDCOperations.putCallId();
        var callId = MDCOperations.getCallId();
        var prosessTaskData = ProsessTaskData.forProsessTask(VedtakOverlappAvstemTask.class);
        prosessTaskData.setProperty(VedtakOverlappAvstemTask.LOG_TEMA_KEY_KEY, VedtakOverlappAvstemTask.LOG_TEMA_SPO_KEY);
        prosessTaskData.setProperty(VedtakOverlappAvstemTask.LOG_FOM_KEY, fom.toString());
        prosessTaskData.setProperty(VedtakOverlappAvstemTask.LOG_TOM_KEY, fom.toString());
        prosessTaskData.setCallId(callId + "_" + 1);
        prosessTaskData.setPrioritet(50);
        taskTjeneste.lagre(prosessTaskData);


        return Response.ok().build();
    }

    @POST
    @Path("/avstemPeriodeOverlappSpøkelseFull")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Lagrer task for å finne overlapp. Resultat i app-logg", tags = "FORVALTNING-uttrekk")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT)
    public Response avstemPeriodeForSpøkelse() {
        var fom = LocalDate.of(2018,10,20);
        var spread = 43199;
        var baseline = LocalDateTime.now();
        if (MDCOperations.getCallId() == null) MDCOperations.putCallId();
        var callId = MDCOperations.getCallId();
        int suffix = 1;
        for (var betweendays = fom; !betweendays.isAfter(LocalDate.now()); betweendays = betweendays.plusDays(1)) {
            var prosessTaskData = ProsessTaskData.forProsessTask(VedtakOverlappAvstemTask.class);
            prosessTaskData.setProperty(VedtakOverlappAvstemTask.LOG_TEMA_KEY_KEY, VedtakOverlappAvstemTask.LOG_TEMA_SPO_KEY);
            prosessTaskData.setProperty(VedtakOverlappAvstemTask.LOG_FOM_KEY, betweendays.toString());
            prosessTaskData.setProperty(VedtakOverlappAvstemTask.LOG_TOM_KEY, betweendays.toString());
            prosessTaskData.setNesteKjøringEtter(baseline.plusSeconds(LocalDateTime.now().getNano() % spread));
            prosessTaskData.setCallId(callId + "_" + suffix);
            prosessTaskData.setPrioritet(50);
            taskTjeneste.lagre(prosessTaskData);
            suffix++;
        }

        return Response.ok().build();
    }

    @POST
    @Path("/avbrytAvstemming")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Avbryter pågående avstemming", tags = "FORVALTNING-uttrekk")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT)
    public Response avbrytAvstemming() {
        finnAlleAvstemming().forEach(t -> taskTjeneste.setProsessTaskFerdig(t.getId(), ProsessTaskStatus.KLAR));
        return Response.ok().build();
    }

    @POST
    @Path("/slettTidligereAvstemmingOverlapp")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Lagrer task for å finne overlapp. Resultat i app-logg", tags = "FORVALTNING-uttrekk")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT)
    public Response slettTidligereAvstemming(@Parameter(description = "Periode") @BeanParam @Valid AvstemmingPeriodeDto dto) {
        if ("hendelseALLE".equals(dto.getKey())) {
            overlappRepository.slettAvstemtPeriode(dto.getFom());
        } else {
            overlappRepository.slettAvstemtPeriode(dto.getFom(), dto.getKey());
        }
        return Response.ok().build();
    }

    @POST
    @Path("/avstemSakOverlappTrex")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Lagrer task for å finne overlapp. Resultat i app-logg", tags = "FORVALTNING-uttrekk")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT)
    public Response avstemSakForOverlapp(@TilpassetAbacAttributt(supplierClass = SaksnummerAbacSupplier.Supplier.class)
                                             @NotNull @QueryParam("saksnummer") @Valid SaksnummerDto s) {
        var prosessTaskData = ProsessTaskData.forProsessTask(VedtakOverlappAvstemTask.class);
        prosessTaskData.setProperty(VedtakOverlappAvstemTask.LOG_TEMA_KEY_KEY, VedtakOverlappAvstemTask.LOG_TEMA_BOTH_KEY);
        prosessTaskData.setProperty(VedtakOverlappAvstemTask.LOG_SAKSNUMMER_KEY, s.getVerdi());
        prosessTaskData.setCallIdFraEksisterende();

        taskTjeneste.lagre(prosessTaskData);

        return Response.ok().build();
    }

    @GET
    @Path("/hentAvstemtSakOverlappTrex")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Prøver å finne overlapp og returnere resultat", tags = "FORVALTNING-uttrekk")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK)
    public Response hentAvstemtSakOverlappTrex(@TilpassetAbacAttributt(supplierClass = SaksnummerAbacSupplier.Supplier.class)
                                                   @NotNull @QueryParam("saksnummer") @Valid SaksnummerDto s) {
        var resultat = overlappRepository.hentForSaksnummer(new Saksnummer(s.getVerdi())).stream()
                .sorted(Comparator.comparing(OverlappVedtak::getOpprettetTidspunkt).reversed())
                .collect(Collectors.toList());
        return Response.ok(resultat).build();
    }

    private List<ProsessTaskData> finnAlleAvstemming() {

        // native sql for å håndtere join og subselect,
        // samt cast til hibernate spesifikk håndtering av parametere som kan være NULL
        @SuppressWarnings("unchecked") var query = (NativeQuery<ProsessTaskEntitet>) entityManager
            .createNativeQuery(
                "SELECT pt.* FROM PROSESS_TASK pt"
                    + " WHERE pt.status = 'KLAR'"
                    + " AND pt.task_type = 'vedtak.overlapp.avstem'"
                    + " FOR UPDATE SKIP LOCKED ",
                ProsessTaskEntitet.class);


        var resultList = query.getResultList();
        return tilProsessTask(resultList);
    }

    private List<ProsessTaskData> tilProsessTask(List<ProsessTaskEntitet> resultList) {
        return resultList.stream().map(ProsessTaskEntitet::tilProsessTask).collect(Collectors.toList());
    }

}
