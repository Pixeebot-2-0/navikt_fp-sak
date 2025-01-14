package no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.nom;

import jakarta.enterprise.context.Dependent;

import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;
import no.nav.vedtak.felles.integrasjon.skjerming.AbstractSkjermetPersonGCPKlient;

/*
 * Klient for å sjekke om person er skjermet. Grensesnitt se #skjermingsløsningen
 *
 * OBS: Bruker OnPrem pga ustabil forbindelse tiltjenesten i prod-gcp - mye timouts
 */
@Dependent
@RestClientConfig(tokenConfig = TokenFlow.AZUREAD_CC, endpointProperty = "skjermet.person.rs.url", endpointDefault = "https://skjermede-personer-pip.intern.nav.no",
    scopesProperty = "skjermet.person.scopes", scopesDefault = "api://prod-gcp.nom.skjermede-personer-pip/.default")
public class SkjermetPersonKlient extends AbstractSkjermetPersonGCPKlient {

}
