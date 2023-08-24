package no.nav.foreldrepenger.domene.uttak;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;

import java.time.LocalDate;
import java.util.Optional;

public interface PersonopplysningerForUttak {

    Optional<LocalDate> søkersDødsdato(BehandlingReferanse ref);

    Optional<LocalDate> søkersDødsdatoGjeldendePåDato(BehandlingReferanse ref, LocalDate dato);

    boolean harOppgittAnnenpartMedNorskID(BehandlingReferanse ref);

    boolean ektefelleHarSammeBosted(BehandlingReferanse ref);

    boolean annenpartHarSammeBosted(BehandlingReferanse ref);

    boolean barnHarSammeBosted(BehandlingReferanse ref);

    boolean oppgittAnnenpartUtenNorskID(BehandlingReferanse referanse);
}
