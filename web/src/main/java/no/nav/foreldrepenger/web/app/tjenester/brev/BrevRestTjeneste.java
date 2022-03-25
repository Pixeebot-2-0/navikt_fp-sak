package no.nav.foreldrepenger.web.app.tjenester.brev;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.UPDATE;

import java.util.List;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.abac.FPSakBeskyttetRessursAttributt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.foreldrepenger.dokumentbestiller.brevmal.BrevmalTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.dto.BestillBrevDto;
import no.nav.foreldrepenger.kontrakter.formidling.v1.BrevmalDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingAbacSuppliers;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;

@Path(BrevRestTjeneste.BASE_PATH)
@ApplicationScoped
@Transactional
public class BrevRestTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(BrevRestTjeneste.class);

    static final String BASE_PATH = "/brev";
    private static final String VARSEL_REVURDERING_PART_PATH = "/varsel/revurdering";
    public static final String VARSEL_REVURDERING_PATH = BASE_PATH + VARSEL_REVURDERING_PART_PATH;
    private static final String BREV_BESTILL_PART_PATH = "/bestill";
    public static final String BREV_BESTILL_PATH = BASE_PATH + BREV_BESTILL_PART_PATH;
    private static final String BREV_MALER_PART_PATH = "/maler";
    public static final String BREV_MALER_PATH = BASE_PATH + BREV_MALER_PART_PATH;

    private DokumentBestillerTjeneste dokumentBestillerTjeneste;
    private DokumentBehandlingTjeneste dokumentBehandlingTjeneste;
    private BehandlingRepository behandlingRepository;
    private BrevmalTjeneste brevmalTjeneste;

    public BrevRestTjeneste() {
        // For Rest-CDI
    }

    @Inject
    public BrevRestTjeneste(DokumentBestillerTjeneste dokumentBestillerTjeneste,
                            DokumentBehandlingTjeneste dokumentBehandlingTjeneste,
                            BehandlingRepository behandlingRepository,
                            BrevmalTjeneste brevmalTjeneste) {
        this.dokumentBestillerTjeneste = dokumentBestillerTjeneste;
        this.dokumentBehandlingTjeneste = dokumentBehandlingTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.brevmalTjeneste = brevmalTjeneste;
    }

    @GET
    @Path(BREV_MALER_PART_PATH)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Henter liste over tilgjengelige brevtyper", tags = "brev")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public List<BrevmalDto> hentMaler(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
                                          @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        return brevmalTjeneste.hentBrevmalerFor(behandling); // NOSONAR
    }

    @POST
    @Path(BREV_BESTILL_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Bestiller generering og sending av brevet", tags = "brev")
    @BeskyttetRessurs(action = UPDATE, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public void bestillDokument(@TilpassetAbacAttributt(supplierClass = BestillBrevAbacDataSupplier.class)
                                    @Parameter(description = "Inneholder kode til brevmal og data som skal flettes inn i brevet") @Valid BestillBrevDto bestillBrevDto) { // NOSONAR
        var behandlingId = bestillBrevDto.getBehandlingId() == null ? behandlingRepository.hentBehandling(bestillBrevDto.getBehandlingUuid()).getId()
            : bestillBrevDto.getBehandlingId();
        LOG.info("Brev med brevmalkode={} bestilt på behandlingId={}", bestillBrevDto.getBrevmalkode(), behandlingId);
        dokumentBestillerTjeneste.bestillDokument(bestillBrevDto, HistorikkAktør.SAKSBEHANDLER);
        oppdaterBehandlingBasertPåManueltBrev(bestillBrevDto.getBrevmalkode(), behandlingId);
    }

    private void oppdaterBehandlingBasertPåManueltBrev(DokumentMalType brevmalkode, Long behandlingId) {
        if (DokumentMalType.VARSEL_OM_REVURDERING.equals(brevmalkode)) {
            settBehandlingPåVent(Venteårsak.AVV_RESPONS_REVURDERING, behandlingId);
        } else if (DokumentMalType.INNHENTE_OPPLYSNINGER.equals(brevmalkode)) {
            settBehandlingPåVent(Venteårsak.AVV_DOK, behandlingId);
        } else if (DokumentMalType.FORLENGET_SAKSBEHANDLINGSTID.equals(brevmalkode)) {
            dokumentBehandlingTjeneste.utvidBehandlingsfristManuelt(behandlingId);
        } else if (DokumentMalType.FORLENGET_SAKSBEHANDLINGSTID_MEDL.equals(brevmalkode)) {
            dokumentBehandlingTjeneste.utvidBehandlingsfristManueltMedlemskap(behandlingId);
        }
    }

    private void settBehandlingPåVent(Venteårsak avvResponsRevurdering, Long behandlingId) {
        dokumentBehandlingTjeneste.settBehandlingPåVent(behandlingId, avvResponsRevurdering);
    }

    @POST
    @Path("/kvittering")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Kvitterer at brevet ble produsert og sendt. BREV_SENT historikk blir lagt og behandling dokument blir oppdatert med journalpostId.", tags = "brev")
    @BeskyttetRessurs(action = UPDATE, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public void kvittering(@TilpassetAbacAttributt(supplierClass = DokumentProdusertDataSupplier.class) @Valid no.nav.foreldrepenger.kontrakter.formidling.v1.DokumentProdusertDto kvittering) {
        dokumentBehandlingTjeneste.kvitterBrevSent(kvittering); // NOSONAR
    }

    @GET
    @Path(VARSEL_REVURDERING_PART_PATH)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Operation(description = "Sjekk har varsel sendt om revurdering", tags = "brev")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public Boolean harSendtVarselOmRevurdering(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
        @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        return dokumentBehandlingTjeneste.erDokumentBestilt(behandling.getId(), DokumentMalType.VARSEL_OM_REVURDERING); // NOSONAR
    }

    public static class BestillBrevAbacDataSupplier implements Function<Object, AbacDataAttributter> {

        @Override
        public AbacDataAttributter apply(Object obj) {
            var req = (BestillBrevDto) obj;
            var attributter = AbacDataAttributter.opprett();
            if (req.getBehandlingUuid() != null) {
                attributter.leggTil(AppAbacAttributtType.BEHANDLING_UUID, req.getBehandlingUuid());
            }
            if (req.getBehandlingId() != null) {
                attributter.leggTil(AppAbacAttributtType.BEHANDLING_ID, req.getBehandlingId());
            }
            return attributter;
        }
    }

    public static class DokumentProdusertDataSupplier implements Function<Object, AbacDataAttributter> {

        @Override
        public AbacDataAttributter apply(Object obj) {
            var req = (no.nav.foreldrepenger.kontrakter.formidling.v1.DokumentProdusertDto) obj;
            return AbacDataAttributter.opprett().leggTil(AppAbacAttributtType.BEHANDLING_UUID, req.behandlingUuid());
        }
    }

}
