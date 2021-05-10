package no.nav.foreldrepenger.web.app.tjenester.registrering.dto;

import javax.validation.Valid;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import no.nav.vedtak.util.InputValideringRegex;

public class AnnenForelderDto {
    @Size(max = 11, min = 11)
    @Digits(integer = 11, fraction = 0)
    private String foedselsnummer;
    private Boolean kanIkkeOppgiAnnenForelder;
    @Valid
    private KanIkkeOppgiBegrunnelse kanIkkeOppgiBegrunnelse;

    private boolean sokerHarAleneomsorg;

    private boolean denAndreForelderenHarRettPaForeldrepenger;

    public String getFoedselsnummer() {
        return foedselsnummer;
    }

    public void setFoedselsnummer(String foedselsnummer) {
        this.foedselsnummer = foedselsnummer;
    }

    public Boolean getKanIkkeOppgiAnnenForelder() {
        return kanIkkeOppgiAnnenForelder;
    }

    public void setKanIkkeOppgiAnnenForelder(Boolean kanIkkeOppgiAnnenForelder) {
        this.kanIkkeOppgiAnnenForelder = kanIkkeOppgiAnnenForelder;
    }

    public KanIkkeOppgiBegrunnelse getKanIkkeOppgiBegrunnelse() {
        return kanIkkeOppgiBegrunnelse;
    }

    public void setKanIkkeOppgiBegrunnelse(KanIkkeOppgiBegrunnelse kanIkkeOppgiBegrunnelse) {
        this.kanIkkeOppgiBegrunnelse = kanIkkeOppgiBegrunnelse;
    }

    public boolean getSokerHarAleneomsorg() {
        return sokerHarAleneomsorg;
    }

    public void setSokerHarAleneomsorg(Boolean sokerHarAleneomsorg) {
        this.sokerHarAleneomsorg = sokerHarAleneomsorg;
    }

    public boolean getDenAndreForelderenHarRettPaForeldrepenger() {
        return denAndreForelderenHarRettPaForeldrepenger;
    }

    public void setDenAndreForelderenHarRettPaForeldrepenger(Boolean denAndreForelderenHarRettPaForeldrepenger) {
        this.denAndreForelderenHarRettPaForeldrepenger = denAndreForelderenHarRettPaForeldrepenger;
    }

    public static class KanIkkeOppgiBegrunnelse {
        @NotNull
        @Size(min = 1, max = 100)
        @Pattern(regexp = InputValideringRegex.KODEVERK)
        private String arsak;
        @Size(max = 4000)
        @Pattern(regexp = InputValideringRegex.FRITEKST)
        private String begrunnelse;
        @Size(max = 20)
        @Pattern(regexp = InputValideringRegex.FRITEKST)
        private String utenlandskFoedselsnummer;
        @Size(max = 100)
        @Pattern(regexp = InputValideringRegex.NAVN)
        private String land;

        public String getArsak() {
            return arsak;
        }

        public void setArsak(String arsak) {
            this.arsak = arsak;
        }

        public String getBegrunnelse() {
            return begrunnelse;
        }

        public void setBegrunnelse(String begrunnelse) {
            this.begrunnelse = begrunnelse;
        }

        public String getUtenlandskFoedselsnummer() {
            return utenlandskFoedselsnummer;
        }

        public void setUtenlandskFoedselsnummer(String utenlandskFoedselsnummer) {
            this.utenlandskFoedselsnummer = utenlandskFoedselsnummer;
        }

        public String getLand() {
            return land;
        }

        public void setLand(String land) {
            this.land = land;
        }
    }
}
