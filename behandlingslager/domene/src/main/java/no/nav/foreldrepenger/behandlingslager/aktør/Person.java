package no.nav.foreldrepenger.behandlingslager.aktør;

import javax.persistence.MappedSuperclass;

import no.nav.foreldrepenger.domene.typer.AktørId;

@MappedSuperclass
public abstract class Person extends Aktør {

    public Person(AktørId aktørId) {
        super(aktørId);
    }

}
