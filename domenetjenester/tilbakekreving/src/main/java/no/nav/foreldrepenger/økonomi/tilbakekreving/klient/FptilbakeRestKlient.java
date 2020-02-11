package no.nav.foreldrepenger.økonomi.tilbakekreving.klient;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.http.client.utils.URIBuilder;

import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.felles.integrasjon.rest.OidcRestClient;

@ApplicationScoped
public class FptilbakeRestKlient {

    public static final String FPTILBAKE_HENT_ÅPEN_TILBAKEKREVING = "/behandlinger/tilbakekreving/aapen";

    public static final String FPTILBAKE_HENT_TILBAKEKREVING_VEDTAK_INFO = "/behandlinger/tilbakekreving/vedtak-info";

    private OidcRestClient restClient;

    public FptilbakeRestKlient() {
        // for CDI proxy
    }

    @Inject
    public FptilbakeRestKlient(OidcRestClient restClient) {
        this.restClient = restClient;
    }

    public boolean harÅpenTilbakekrevingsbehandling(Saksnummer saksnummer) {
        URI uriHentÅpenTilbakekreving = lagRequestUri(saksnummer);
        return restClient.get(uriHentÅpenTilbakekreving, Boolean.class);
    }

    public TilbakekrevingVedtakDto hentTilbakekrevingsVedtakInfo(UUID uuid){
        URI uriHentTilbakekrevingVedtaksInfo = lagRequestUri(uuid);
        return restClient.get(uriHentTilbakekrevingVedtaksInfo, TilbakekrevingVedtakDto.class);
    }

    private URI lagRequestUri(Saksnummer saksnummer) {
        String endpoint = FptilbakeFelles.getFptilbakeBaseUrl() + FPTILBAKE_HENT_ÅPEN_TILBAKEKREVING;
        try {
            return new URIBuilder(endpoint).addParameter("saksnummer", saksnummer.getVerdi()).build();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }
    private URI lagRequestUri(UUID uuid) {
        String endpoint = FptilbakeFelles.getFptilbakeBaseUrl() + FPTILBAKE_HENT_TILBAKEKREVING_VEDTAK_INFO;
        try {
            return new URIBuilder(endpoint).addParameter("uuid", uuid.toString()).build();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }


}
