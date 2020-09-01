package no.nav.foreldrepenger.domene.uttak.testutilities.behandling.personopplysning;

import java.time.LocalDate;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.domene.typer.AktørId;


public final class Personopplysning {

    private AktørId aktørId;
    private NavBrukerKjønn brukerKjønn;
    private SivilstandType sivilstand;
    private String navn;
    private LocalDate dødsdato;
    private LocalDate fødselsdato;
    private Region region;

    public AktørId getAktørId() {
        return aktørId;
    }

    public NavBrukerKjønn getBrukerKjønn() {
        return brukerKjønn;
    }

    public SivilstandType getSivilstand() {
        return sivilstand;
    }

    public String getNavn() {
        return navn;
    }

    public LocalDate getDødsdato() {
        return dødsdato;
    }

    public LocalDate getFødselsdato() {
        return fødselsdato;
    }

    public Region getRegion() {
        return region;
    }

    private Personopplysning(Builder builder) {
        this.aktørId = builder.aktørId;
        this.brukerKjønn = builder.brukerKjønn;
        this.sivilstand = builder.sivilstand;
        this.navn = builder.navn;
        this.dødsdato = builder.dødsdato;
        this.fødselsdato = builder.fødselsdato;
        this.region = builder.region;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private AktørId aktørId;
        private NavBrukerKjønn brukerKjønn;
        private SivilstandType sivilstand;
        private String navn;
        private LocalDate dødsdato;
        private LocalDate fødselsdato;
        private Region region;

        private Builder() {
        }

        public Personopplysning build() {
            return new Personopplysning(this);
        }

        public Builder aktørId(AktørId aktørId) {
            this.aktørId = aktørId;
            return this;
        }

        public Builder brukerKjønn(NavBrukerKjønn brukerKjønn) {
            this.brukerKjønn = brukerKjønn;
            return this;
        }

        public Builder sivilstand(SivilstandType sivilstand) {
            this.sivilstand = sivilstand;
            return this;
        }

        public Builder navn(String navn) {
            this.navn = navn;
            return this;
        }

        public Builder dødsdato(LocalDate dødsdato) {
            this.dødsdato = dødsdato;
            return this;
        }

        public Builder fødselsdato(LocalDate fødselsdato) {
            this.fødselsdato = fødselsdato;
            return this;
        }

        public Builder region(Region region) {
            this.region = region;
            return this;
        }
    }
}
