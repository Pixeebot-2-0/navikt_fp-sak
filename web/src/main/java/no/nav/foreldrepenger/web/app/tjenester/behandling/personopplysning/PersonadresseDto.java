package no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning;

import static no.nav.foreldrepenger.web.app.util.StringUtils.formaterMedStoreOgSmåBokstaver;

import java.time.LocalDate;

import no.nav.foreldrepenger.behandlingslager.aktør.AdresseType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonAdresseEntitet;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;

public class PersonadresseDto {

    private LocalDate fom;
    private LocalDate tom;
    private AdresseType adresseType;
    private String adresselinje1;
    private String adresselinje2;
    private String adresselinje3;
    private String postNummer;
    private String poststed;
    private String land;

    public static PersonadresseDto tilDto(PersonAdresseEntitet adresse) {
        var dto = new PersonadresseDto();
        dto.setFom(adresse.getPeriode().getFomDato());
        dto.setTom(adresse.getPeriode().getTomDato());
        dto.setAdresselinje1(formaterMedStoreOgSmåBokstaver(adresse.getAdresselinje1()));
        dto.setAdresselinje2(formaterMedStoreOgSmåBokstaver(adresse.getAdresselinje2()));
        dto.setAdresselinje3(formaterMedStoreOgSmåBokstaver(adresse.getAdresselinje3()));
        dto.setPoststed(formaterMedStoreOgSmåBokstaver(adresse.getPoststed()));
        dto.setPostNummer(adresse.getPostnummer());
        dto.setLand(Landkoder.navnLesbart(adresse.getLand()));
        dto.setAdresseType(adresse.getAdresseType());
        return dto;
    }

    public LocalDate getTom() { return tom; }

    public LocalDate getFom() { return fom; }

    public AdresseType getAdresseType() {
        return adresseType;
    }

    public String getAdresselinje1() {
        return adresselinje1;
    }

    public String getAdresselinje2() {
        return adresselinje2;
    }

    public String getAdresselinje3() {
        return adresselinje3;
    }

    public String getPostNummer() {
        return postNummer;
    }

    public String getPoststed() {
        return poststed;
    }

    public String getLand() {
        return land;
    }


    public void setTom(LocalDate tom) { this.tom = tom; }

    public void setFom(LocalDate fom) { this.fom = fom; }

    public void setAdresseType(AdresseType adresseType) {
        this.adresseType = adresseType;
    }

    public void setAdresselinje1(String adresselinje1) {
        this.adresselinje1 = adresselinje1;
    }

    public void setAdresselinje2(String adresselinje2) {
        this.adresselinje2 = adresselinje2;
    }

    public void setAdresselinje3(String adresselinje3) {
        this.adresselinje3 = adresselinje3;
    }

    public void setPostNummer(String postNummer) {
        this.postNummer = postNummer;
    }

    public void setPoststed(String poststed) {
        this.poststed = poststed;
    }

    public void setLand(String land) {
        this.land = land;
    }
}
