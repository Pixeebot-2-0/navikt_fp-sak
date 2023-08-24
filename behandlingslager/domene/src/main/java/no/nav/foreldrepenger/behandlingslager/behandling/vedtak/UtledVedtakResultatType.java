package no.nav.foreldrepenger.behandlingslager.behandling.vedtak;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;

import java.util.Objects;
import java.util.Set;

public class UtledVedtakResultatType {

    private static Set<BehandlingResultatType> INNVILGET_TYPER = Set.of(BehandlingResultatType.INNVILGET,
        BehandlingResultatType.FORELDREPENGER_ENDRET,
        BehandlingResultatType.FORELDREPENGER_SENERE);

    private UtledVedtakResultatType() {
        // hide public contructor
    }

    public static VedtakResultatType utled(BehandlingType behandlingType, BehandlingResultatType behandlingResultatType) {
        if (BehandlingType.KLAGE.equals(behandlingType)) {
            return VedtakResultatType.VEDTAK_I_KLAGEBEHANDLING;
        }
        if (BehandlingType.ANKE.equals(behandlingType)) {
            return VedtakResultatType.VEDTAK_I_ANKEBEHANDLING;
        }
        if (BehandlingType.INNSYN.equals(behandlingType)) {
            return VedtakResultatType.VEDTAK_I_INNSYNBEHANDLING;
        }
        if (INNVILGET_TYPER.contains(behandlingResultatType)) {
            return VedtakResultatType.INNVILGET;
        }
        if (BehandlingResultatType.INGEN_ENDRING.equals(behandlingResultatType)) {
            throw new IllegalStateException("Utviklerfeil: Kan ikke utlede vedtakresultat fra INGEN ENDRING");
        }
        if (BehandlingResultatType.OPPHØR.equals(behandlingResultatType)) {
            return VedtakResultatType.OPPHØR;
        }
        return VedtakResultatType.AVSLAG;
    }

    public static VedtakResultatType utledES(BehandlingType behandlingType, BehandlingResultatType behandlingResultatType) {
        Objects.requireNonNull(behandlingResultatType);

        if (BehandlingResultatType.INNVILGET.equals(behandlingResultatType)) {
            return VedtakResultatType.INNVILGET;
        }
        return VedtakResultatType.AVSLAG;
    }
}
