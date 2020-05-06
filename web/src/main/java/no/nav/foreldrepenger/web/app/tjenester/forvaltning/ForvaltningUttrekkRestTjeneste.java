package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt.DRIFT;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt.FAGSAK;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingOverlappInfotrygd;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingsprosess.dagligejobber.infobrev.InformasjonssakRepository;
import no.nav.foreldrepenger.domene.vedtak.infotrygd.overlapp.IdentifiserOverlappendeInfotrygdYtelseTjeneste;
import no.nav.foreldrepenger.domene.vedtak.infotrygd.overlapp.LoggHistoriskOverlappFPInfotrygdVLTjeneste;
import no.nav.foreldrepenger.mottak.vedtak.VurderOpphørAvYtelserTask;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.AksjonspunktKodeDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.AvstemmingEnkeltSakDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.AvstemmingPeriodeDto;
import no.nav.vedtak.felles.jpa.VLPersistenceUnit;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt;

@Path("/forvaltningUttrekk")
@ApplicationScoped
@Transactional
public class ForvaltningUttrekkRestTjeneste {

    private EntityManager entityManager;
    private FagsakRepository fagsakRepository;
    private ProsessTaskRepository prosessTaskRepository;
    private InformasjonssakRepository informasjonssakRepository;
    private IdentifiserOverlappendeInfotrygdYtelseTjeneste spbsLog;
    private LoggHistoriskOverlappFPInfotrygdVLTjeneste forLog;

    public ForvaltningUttrekkRestTjeneste() {
        // For CDI
    }

    @Inject
    public ForvaltningUttrekkRestTjeneste(@VLPersistenceUnit EntityManager entityManager,
                                          BehandlingRepositoryProvider repositoryProvider,
                                          ProsessTaskRepository prosessTaskRepository,
                                          InformasjonssakRepository informasjonssakRepository,
                                          IdentifiserOverlappendeInfotrygdYtelseTjeneste spbsLog,
                                          LoggHistoriskOverlappFPInfotrygdVLTjeneste forLog) {
        this.entityManager = entityManager;
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.prosessTaskRepository = prosessTaskRepository;
        this.informasjonssakRepository = informasjonssakRepository;
        this.spbsLog = spbsLog;
        this.forLog = forLog;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Gir åpne aksjonspunkter med angitt kode", tags = "FORVALTNING-uttrekk")
    @Path("/openAutopunkt")
    @BeskyttetRessurs(action = READ, ressurs = BeskyttetRessursResourceAttributt.DRIFT, sporingslogg = false)
    public Response openAutopunkt(@Parameter(description = "Aksjonspunktkoden") @BeanParam @Valid AksjonspunktKodeDto dto) {
        AksjonspunktDefinisjon apDef = AksjonspunktDefinisjon.fraKode(dto.getAksjonspunktKode());
        if (apDef == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        Query query = entityManager.createNativeQuery("select saksnummer, ap.opprettet_tid, ap.frist_tid " +
                " from fpsak.fagsak fs join fpsak.behandling bh on bh.fagsak_id=fs.id " +
                " join FPSAK.AKSJONSPUNKT ap on ap.behandling_id=bh.id " +
                " where aksjonspunkt_def=:apdef and aksjonspunkt_status=:status "); //$NON-NLS-1$
        query.setParameter("apdef", apDef.getKode());
        query.setParameter("status", AksjonspunktStatus.OPPRETTET.getKode());
        @SuppressWarnings("unchecked")
        List<Object[]> resultatList = query.getResultList();
        List<OpenAutopunkt> åpneAksjonspunkt = resultatList.stream()
            .map(this::mapFraAksjonspunktTilDto)
            .collect(Collectors.toList());
        return Response.ok(åpneAksjonspunkt).build();
    }

    private OpenAutopunkt mapFraAksjonspunktTilDto(Object[] row) {
        OpenAutopunkt autopunkt = new OpenAutopunkt();
        autopunkt.aksjonspunktOpprettetDato = ((Timestamp)row[1]).toLocalDateTime().toLocalDate(); // NOSONAR
        autopunkt.aksjonspunktFristDato = row[2] != null ? ((Timestamp)row[2]).toLocalDateTime().toLocalDate() : null; // NOSONAR
        autopunkt.saksnummer = (String)row[0]; // NOSONAR
        return autopunkt;
    }

    public static class OpenAutopunkt {
        public String saksnummer;  // NOSONAR
        public LocalDate aksjonspunktOpprettetDato;  // NOSONAR
        public LocalDate aksjonspunktFristDato;  // NOSONAR
    }

    @GET
    @Path("/listFagsakUtenBehandling")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent liste av saknumre for fagsak uten noen behandlinger",
        tags = "FORVALTNING-uttrekk",
        responses = {
            @ApiResponse(responseCode = "200",
                description = "Fagsaker uten behandling",
                content = @Content(
                    array = @ArraySchema(
                        arraySchema = @Schema(implementation = List.class),
                        schema = @Schema(implementation = SaksnummerDto.class)),
                    mediaType = MediaType.APPLICATION_JSON
                )
            )
        })
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public List<SaksnummerDto> listFagsakUtenBehandling() {
        return fagsakRepository.hentÅpneFagsakerUtenBehandling().stream().map(SaksnummerDto::new).collect(Collectors.toList());
    }

    @POST
    @Path("/avstemPeriodeOverlappTrex")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Lagrer task for å finne overlapp. Resultat i app-logg", tags = "FORVALTNING-uttrekk")
    @BeskyttetRessurs(action = READ, ressurs = DRIFT)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response avstemPeriodeForOverlapp(@Parameter(description = "Periode") @BeanParam @Valid AvstemmingPeriodeDto dto) {
        ProsessTaskData prosessTaskData = new ProsessTaskData(VurderOpphørAvYtelserTask.TASKTYPE);
        prosessTaskData.setProperty(VurderOpphørAvYtelserTask.HIJACK_KEY_KEY, dto.getKey());
        prosessTaskData.setProperty(VurderOpphørAvYtelserTask.HIJACK_FOM_KEY, dto.getFom().toString());
        prosessTaskData.setProperty(VurderOpphørAvYtelserTask.HIJACK_TOM_KEY, dto.getTom().toString());
        prosessTaskData.setCallIdFraEksisterende();

        prosessTaskRepository.lagre(prosessTaskData);
        return Response.ok().build();
    }

    @POST
    @Path("/avstemSakOverlappTrex")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Lagrer task for å finne overlapp. Resultat i app-logg", tags = "FORVALTNING-uttrekk")
    @BeskyttetRessurs(action = READ, ressurs = DRIFT)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response avstemSakForOverlapp(@Parameter(description = "Saksnummer") @BeanParam @Valid AvstemmingEnkeltSakDto dto) {
        ProsessTaskData prosessTaskData = new ProsessTaskData(VurderOpphørAvYtelserTask.TASKTYPE);
        prosessTaskData.setProperty(VurderOpphørAvYtelserTask.HIJACK_KEY_KEY, dto.getKey());
        prosessTaskData.setProperty(VurderOpphørAvYtelserTask.HIJACK_SAKSNUMMER_KEY, dto.getSaksnummer());
        prosessTaskData.setProperty(VurderOpphørAvYtelserTask.HIJACK_PREFIX_KEY, "SAK");
        prosessTaskData.setCallIdFraEksisterende();

        prosessTaskRepository.lagre(prosessTaskData);

        return Response.ok().build();
    }

    @GET
    @Path("/avstemSakOverlappTrexInteraktiv")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Prøver å finne overlapp og returnere resultat", tags = "FORVALTNING-uttrekk")
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response hentAlleBehandlinger(@NotNull @QueryParam("saksnummer") @Parameter(description = "Eksisterende saksnummer") @Valid SaksnummerDto s) {
        List<BehandlingOverlappInfotrygd> resultat = new ArrayList<>();
        var vedtakAndre = informasjonssakRepository.finnSakerOpprettetInnenIntervallMedKunUtbetalte(null, null, s.getVerdi());
        if (!vedtakAndre.isEmpty())
            resultat.addAll(spbsLog.vurderEventueltOverlapp(vedtakAndre.get(0).getBehandlingId(), vedtakAndre.get(0).getTidligsteDato()));
        var vedtakFor = informasjonssakRepository.finnSakerOpprettetInnenIntervallMedSisteVedtak(null, null, s.getVerdi());
        if (!vedtakFor.isEmpty())
            resultat.addAll(forLog.vurderEventueltOverlapp(vedtakFor.get(0).getBehandlingId(), vedtakFor.get(0).getAnnenPartAktørId(), vedtakFor.get(0).getTidligsteDato()));
        return Response.ok(resultat).build();
    }
}
