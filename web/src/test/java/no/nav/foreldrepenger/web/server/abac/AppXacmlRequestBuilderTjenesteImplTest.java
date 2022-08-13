package no.nav.foreldrepenger.web.server.abac;

import static no.nav.vedtak.sikkerhet.abac.NavAbacCommonAttributter.RESOURCE_FELLES_PERSON_FNR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.abac.FPSakBeskyttetRessursAttributt;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;
import no.nav.vedtak.sikkerhet.abac.AbacIdToken;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt;
import no.nav.vedtak.sikkerhet.abac.NavAbacCommonAttributter;
import no.nav.vedtak.sikkerhet.abac.PdpKlient;
import no.nav.vedtak.sikkerhet.abac.PdpRequest;
import no.nav.vedtak.sikkerhet.pdp.PdpConsumer;
import no.nav.vedtak.sikkerhet.pdp.PdpKlientImpl;
import no.nav.vedtak.sikkerhet.pdp.xacml.Category;
import no.nav.vedtak.sikkerhet.pdp.xacml.XacmlRequest;
import no.nav.vedtak.sikkerhet.pdp.xacml.XacmlRequestBuilder;
import no.nav.vedtak.sikkerhet.pdp.xacml.XacmlResponse;

@ExtendWith(MockitoExtension.class)
public class AppXacmlRequestBuilderTjenesteImplTest {

    public static final String JWT_TOKEN = "eyAidHlwIjogIkpXVCIsICJraWQiOiAiU0gxSWVSU2sxT1VGSDNzd1orRXVVcTE5VHZRPSIsICJhbGciOiAiUlMyNTYiIH0.eyAiYXRfaGFzaCI6ICIyb2c1RGk5ZW9LeFhOa3VPd0dvVUdBIiwgInN1YiI6ICJzMTQyNDQzIiwgImF1ZGl0VHJhY2tpbmdJZCI6ICI1NTM0ZmQ4ZS03MmE2LTRhMWQtOWU5YS1iZmEzYThhMTljMDUtNjE2NjA2NyIsICJpc3MiOiAiaHR0cHM6Ly9pc3NvLXQuYWRlby5ubzo0NDMvaXNzby9vYXV0aDIiLCAidG9rZW5OYW1lIjogImlkX3Rva2VuIiwgImF1ZCI6ICJPSURDIiwgImNfaGFzaCI6ICJiVWYzcU5CN3dTdi0wVlN0bjhXLURnIiwgIm9yZy5mb3JnZXJvY2sub3BlbmlkY29ubmVjdC5vcHMiOiAiMTdhOGZiMzYtMGI0Ny00YzRkLWE4YWYtZWM4Nzc3Y2MyZmIyIiwgImF6cCI6ICJPSURDIiwgImF1dGhfdGltZSI6IDE0OTgwMzk5MTQsICJyZWFsbSI6ICIvIiwgImV4cCI6IDE0OTgwNDM1MTUsICJ0b2tlblR5cGUiOiAiSldUVG9rZW4iLCAiaWF0IjogMTQ5ODAzOTkxNSB9.S2DKQweQWZIfjaAT2UP9_dxrK5zqpXj8IgtjDLt5PVfLYfZqpWGaX-ckXG0GlztDVBlRK4ylmIYacTmEAUV_bRa_qWKRNxF83SlQRgHDSiE82SGv5WHOGEcAxf2w_d50XsgA2KDBCyv0bFIp9bCiKzP11uWPW0v4uIkyw2xVxMVPMCuiMUtYFh80sMDf9T4FuQcFd0LxoYcSFDEDlwCdRiF3ufw73qtMYBlNIMbTGHx-DZWkZV7CgukmCee79gwQIvGwdLrgaDrHFCJUDCbB1FFEaE3p3_BZbj0T54fCvL69aHyWm1zEd9Pys15yZdSh3oSSr4yVNIxhoF-nQ7gY-g;";
    private PdpKlientImpl pdpKlient;
    @Mock
    private PdpConsumer pdpConsumerMock;

    @BeforeEach
    public void setUp() {
        pdpKlient = new PdpKlientImpl(pdpConsumerMock, new AppXacmlRequestBuilderTjenesteImpl());
    }

    @Test
    public void kallPdpMedSamlTokenNårIdTokenErSamlToken() throws Exception {
        var idToken = AbacIdToken.withSamlToken("SAML");
        var responseWrapper = createResponse("xacmlresponse.json");
        var captor = ArgumentCaptor.forClass(XacmlRequestBuilder.class);

        when(pdpConsumerMock.evaluate(captor.capture())).thenReturn(responseWrapper);
        var pdpRequest = lagPdpRequest();
        pdpRequest.put(RESOURCE_FELLES_PERSON_FNR, Collections.singleton("12345678900"));
        pdpRequest.put(PdpKlient.ENVIRONMENT_AUTH_TOKEN, idToken);
        pdpKlient.forespørTilgang(pdpRequest);

        assertThat(captor.getValue().build().toString().contains(NavAbacCommonAttributter.ENVIRONMENT_FELLES_SAML_TOKEN)).isTrue();
    }

    @Test
    public void kallPdpUtenFnrResourceHvisPersonlisteErTom() throws FileNotFoundException {
        var idToken = AbacIdToken.withOidcToken(JWT_TOKEN);
        var responseWrapper = createResponse("xacmlresponse.json");
        var captor = ArgumentCaptor.forClass(XacmlRequestBuilder.class);

        when(pdpConsumerMock.evaluate(captor.capture())).thenReturn(responseWrapper);

        var pdpRequest = lagPdpRequest();
        pdpRequest.put(RESOURCE_FELLES_PERSON_FNR, Collections.emptySet());
        pdpRequest.put(PdpKlient.ENVIRONMENT_AUTH_TOKEN, idToken);
        pdpKlient.forespørTilgang(pdpRequest);

        assertThat(captor.getValue().build().toString().contains(RESOURCE_FELLES_PERSON_FNR)).isFalse();
    }

    @Test
    public void kallPdpMedJwtTokenBodyNårIdTokenErJwtToken() throws Exception {
        var idToken = AbacIdToken.withOidcToken(JWT_TOKEN);
        var responseWrapper = createResponse("xacmlresponse.json");
        var captor = ArgumentCaptor.forClass(XacmlRequestBuilder.class);

        when(pdpConsumerMock.evaluate(captor.capture())).thenReturn(responseWrapper);

        var pdpRequest = lagPdpRequest();
        pdpRequest.put(RESOURCE_FELLES_PERSON_FNR, Collections.singleton("12345678900"));
        pdpRequest.put(PdpKlient.ENVIRONMENT_AUTH_TOKEN, idToken);
        pdpKlient.forespørTilgang(pdpRequest);

        assertThat(captor.getValue().build().toString().contains(NavAbacCommonAttributter.ENVIRONMENT_FELLES_OIDC_TOKEN_BODY)).isTrue();
    }

    @Test
    public void kallPdpMedFlereAttributtSettNårPersonlisteStørreEnn1() throws FileNotFoundException {
        var idToken = AbacIdToken.withOidcToken(JWT_TOKEN);
        var responseWrapper = createResponse("xacml3response.json");
        var captor = ArgumentCaptor.forClass(XacmlRequestBuilder.class);

        when(pdpConsumerMock.evaluate(captor.capture())).thenReturn(responseWrapper);
        Set<String> personnr = new HashSet<>();
        personnr.add("12345678900");
        personnr.add("12345678901");
        personnr.add("12345678902");

        var pdpRequest = lagPdpRequest();
        pdpRequest.put(RESOURCE_FELLES_PERSON_FNR, personnr);
        pdpRequest.put(PdpKlient.ENVIRONMENT_AUTH_TOKEN, idToken);
        pdpKlient.forespørTilgang(pdpRequest);

        var xacmlRequestString = captor.getValue().build().toString();

        assertThat(xacmlRequestString.contains("12345678900")).isTrue();
        assertThat(xacmlRequestString.contains("12345678901")).isTrue();
        assertThat(xacmlRequestString.contains("12345678902")).isTrue();
    }

    @Test
    public void sporingsloggListeSkalHaSammeRekkefølgePåidenterSomXacmlRequest() throws FileNotFoundException {
        var idToken = AbacIdToken.withOidcToken(JWT_TOKEN);
        var responseWrapper = createResponse("xacml3response.json");
        var captor = ArgumentCaptor.forClass(XacmlRequestBuilder.class);

        when(pdpConsumerMock.evaluate(captor.capture())).thenReturn(responseWrapper);
        Set<String> personnr = new HashSet<>();
        personnr.add("12345678900");
        personnr.add("12345678901");
        personnr.add("12345678902");

        var pdpRequest = lagPdpRequest();
        pdpRequest.put(RESOURCE_FELLES_PERSON_FNR, personnr);
        pdpRequest.put(PdpKlient.ENVIRONMENT_AUTH_TOKEN, idToken);
        pdpKlient.forespørTilgang(pdpRequest);

        var xacmlRequest = captor.getValue().build();
        var resourceArray = xacmlRequest.request().get(Category.Resource);
        var personArray = resourceArray.stream()
            .map(XacmlRequest.Attributes::attribute)
            .flatMap(Collection::stream)
            .filter(a -> NavAbacCommonAttributter.RESOURCE_FELLES_PERSON_FNR.equals(a.attributeId()))
            .toList();

        List<String> personer = pdpRequest.getListOfString(RESOURCE_FELLES_PERSON_FNR);

        for (int i = 0; i < personer.size(); i++) {
            assertThat(personArray.get(i).value().toString()).contains(personer.get(i));
        }
    }

    private static PdpRequest lagPdpRequest() {
        var request = new PdpRequest();
        request.put(NavAbacCommonAttributter.RESOURCE_FELLES_DOMENE, "foreldrepenger");
        request.put(NavAbacCommonAttributter.XACML10_ACTION_ACTION_ID, BeskyttetRessursActionAttributt.READ.getEksternKode());
        request.put(NavAbacCommonAttributter.RESOURCE_FELLES_RESOURCE_TYPE, FPSakBeskyttetRessursAttributt.FAGSAK);
        return request;
    }

    private XacmlResponse createResponse(String jsonFile) {
        File file = new File(getClass().getClassLoader().getResource(jsonFile).getFile());
        try {
            return DefaultJsonMapper.getObjectMapper().readValue(file, XacmlResponse.class);
        } catch (Exception e) {
            //
        }
        return null;
    }
}
