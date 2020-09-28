package no.nav.foreldrepenger.web.app.tjenester.kodeverk;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.domene.person.tps.TpsTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.EnhetsTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.event.BehandlingEnhetEventPubliserer;
import no.nav.foreldrepenger.web.RepositoryAwareTest;
import no.nav.foreldrepenger.web.app.jackson.JacksonJsonConfig;
import no.nav.foreldrepenger.web.app.tjenester.kodeverk.app.HentKodeverkTjeneste;
import no.nav.vedtak.felles.integrasjon.arbeidsfordeling.rest.ArbeidsfordelingRestKlient;

@ExtendWith(MockitoExtension.class)
public class KodeverkRestTjenesteTest extends RepositoryAwareTest {

    private HentKodeverkTjeneste hentKodeverkTjeneste;
    @Mock
    private BehandlingEnhetEventPubliserer eventPubliserer;
    @Mock
    private TpsTjeneste tpsTjeneste;
    @Mock
    private ArbeidsfordelingRestKlient arbeidsfordelingRestKlient;

    @BeforeEach
    public void before() {
        EnhetsTjeneste enhetsTjeneste = new EnhetsTjeneste(tpsTjeneste, arbeidsfordelingRestKlient);
        BehandlendeEnhetTjeneste beh = new BehandlendeEnhetTjeneste(enhetsTjeneste, eventPubliserer, repositoryProvider);
        hentKodeverkTjeneste = new HentKodeverkTjeneste(beh);
    }

    @Test
    public void skal_hente_kodeverk_og_gruppere_på_kodeverknavn() throws IOException {

        KodeverkRestTjeneste tjeneste = new KodeverkRestTjeneste(hentKodeverkTjeneste);
        Response response = tjeneste.hentGruppertKodeliste();

        String rawJson = (String) response.getEntity();
        assertThat(rawJson).isNotNull();

        Map<String, Object> gruppertKodeliste = new JacksonJsonConfig().getObjectMapper().readValue(rawJson, Map.class);

        assertThat(gruppertKodeliste.keySet())
                .contains(FagsakStatus.class.getSimpleName(), Avslagsårsak.class.getSimpleName(), Landkoder.class.getSimpleName());

        assertThat(gruppertKodeliste.keySet())
                .containsAll(new HashSet<>(HentKodeverkTjeneste.KODEVERDIER_SOM_BRUKES_PÅ_KLIENT.keySet()));

        assertThat(gruppertKodeliste.keySet()).hasSize(HentKodeverkTjeneste.KODEVERDIER_SOM_BRUKES_PÅ_KLIENT.size());

        var fagsakStatuser = (List<Map<String, String>>) gruppertKodeliste.get(FagsakStatus.class.getSimpleName());
        assertThat(fagsakStatuser.stream().map(k -> k.get("kode")).collect(Collectors.toList())).contains(FagsakStatus.AVSLUTTET.getKode(),
                FagsakStatus.OPPRETTET.getKode());

        var map = (Map<String, List<?>>) gruppertKodeliste.get(Avslagsårsak.class.getSimpleName());
        assertThat(map.keySet()).contains(VilkårType.ADOPSJONSVILKÅRET_ENGANGSSTØNAD.getKode(), VilkårType.MEDLEMSKAPSVILKÅRET.getKode());

        var avslagsårsaker = (List<Map<String, String>>) map.get(VilkårType.ADOPSJONSVILKÅRET_ENGANGSSTØNAD.getKode());
        assertThat(avslagsårsaker.stream().map(k -> ((Map) k).get("kode")).collect(Collectors.toList()))
                .contains(Avslagsårsak.ENGANGSSTØNAD_ALLEREDE_UTBETALT_TIL_MOR.getKode(),
                        Avslagsårsak.ENGANGSSTØNAD_ER_ALLEREDE_UTBETALT_TIL_FAR_MEDMOR.getKode());
    }

    @Test
    public void serialize_kodeverdi_enums() throws Exception {
        JacksonJsonConfig jsonConfig = new JacksonJsonConfig();

        ObjectMapper om = jsonConfig.getObjectMapper();

        String json = om.writer().withDefaultPrettyPrinter().writeValueAsString(AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT);

        System.out.println(json);
    }

}
