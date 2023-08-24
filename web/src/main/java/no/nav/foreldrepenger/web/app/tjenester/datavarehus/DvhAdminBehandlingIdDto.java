package no.nav.foreldrepenger.web.app.tjenester.datavarehus;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.QueryParam;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;

import java.util.UUID;

/**
 * Forenklet versjon som tillater regenerering for alle behandlinger
 */
public class DvhAdminBehandlingIdDto implements AbacDto {

    @NotNull
    @QueryParam("behandlingUuid")
    @Valid
    @JsonProperty
    private UUID behandlingUuid;

    public UUID getBehandlingUuid() {
        return behandlingUuid;
    }

    @Override
    public AbacDataAttributter abacAttributter() {
        return AbacDataAttributter.opprett();
    }

    @Override
    public String toString() {
        return behandlingUuid.toString();
    }
}
