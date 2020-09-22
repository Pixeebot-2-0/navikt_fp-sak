package no.nav.foreldrepenger.domene.personopplysning;

import java.time.LocalDate;

import no.nav.foreldrepenger.domene.typer.AktørId;

public class AvklartDataForeldreAdapter {

    private AktørId aktørId;
    private LocalDate dødsdato;

    public AvklartDataForeldreAdapter(AktørId aktørId, LocalDate dødsdato) {
        this.aktørId = aktørId;
        this.dødsdato = dødsdato;
    }

    public AktørId getAktørId() {
        return aktørId;
    }

    public LocalDate getDødsdato() {
        return dødsdato;
    }
}
