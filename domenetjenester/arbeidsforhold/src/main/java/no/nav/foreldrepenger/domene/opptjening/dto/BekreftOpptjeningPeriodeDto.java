package no.nav.foreldrepenger.domene.opptjening.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;

public class BekreftOpptjeningPeriodeDto {
    private OpptjeningAktivitetType aktivitetType;
    private LocalDate originalFom;
    private LocalDate originalTom;
    private LocalDate opptjeningFom;
    private LocalDate opptjeningTom;
    private String arbeidsgiverNavn;
    private String arbeidsgiverIdentifikator;
    private String arbeidsgiverReferanse;
    private String arbeidsforholdRef;
    private BigDecimal stillingsandel;
    private LocalDate naringRegistreringsdato;
    private boolean erManueltOpprettet = false;
    private boolean erEndret = false;
    private Boolean erGodkjent;
    private String begrunnelse;

    public BekreftOpptjeningPeriodeDto() {
    }

    public LocalDate getOriginalFom() {
        return originalFom;
    }

    public void setOriginalFom(LocalDate originalFom) {
        this.originalFom = originalFom;
    }

    public LocalDate getOriginalTom() {
        return originalTom;
    }

    public void setOriginalTom(LocalDate originalTom) {
        this.originalTom = originalTom;
    }

    public Boolean getErEndret() {
        return erEndret;
    }

    public void setErEndret(Boolean erEndret) {
        this.erEndret = erEndret != null ? erEndret : false; // NOSONAR
    }

    public OpptjeningAktivitetType getAktivitetType() {
        return aktivitetType;
    }

    public void setAktivitetType(OpptjeningAktivitetType aktivitetType) {
        this.aktivitetType = aktivitetType;
    }

    public LocalDate getOpptjeningFom() {
        return opptjeningFom;
    }

    public void setOpptjeningFom(LocalDate opptjeningFom) {
        this.opptjeningFom = opptjeningFom;
    }

    public LocalDate getOpptjeningTom() {
        return opptjeningTom;
    }

    public void setOpptjeningTom(LocalDate opptjeningTom) {
        this.opptjeningTom = opptjeningTom;
    }

    public String getArbeidsgiverNavn() {
        return arbeidsgiverNavn;
    }

    public void setArbeidsgiverNavn(String arbeidsgiverNavn) {
        this.arbeidsgiverNavn = arbeidsgiverNavn;
    }

    public String getArbeidsgiverIdentifikator() {
        if (arbeidsgiverIdentifikator == null && arbeidsgiverReferanse != null) {
            return arbeidsgiverReferanse;
        }
        return arbeidsgiverIdentifikator;
    }

    public void setArbeidsgiverIdentifikator(String arbeidsgiverIdentifikator) {
        this.arbeidsgiverIdentifikator = arbeidsgiverIdentifikator;
        this.arbeidsgiverReferanse = arbeidsgiverIdentifikator;
    }

    public String getArbeidsgiverReferanse() {
        return arbeidsgiverReferanse;
    }

    public void setArbeidsgiverReferanse(String arbeidsgiverReferanse) {
        this.arbeidsgiverReferanse = arbeidsgiverReferanse;
    }

    public BigDecimal getStillingsandel() {
        return stillingsandel;
    }

    public void setStillingsandel(BigDecimal stillingsandel) {
        this.stillingsandel = stillingsandel;
    }

    public LocalDate getNaringRegistreringsdato() {
        return naringRegistreringsdato;
    }

    public void setNaringRegistreringsdato(LocalDate naringRegistreringsdato) {
        this.naringRegistreringsdato = naringRegistreringsdato;
    }

    public boolean getErManueltOpprettet() {
        return erManueltOpprettet;
    }

    public void setErManueltOpprettet(Boolean erManueltOpprettet) {
        this.erManueltOpprettet = erManueltOpprettet != null ? erManueltOpprettet : false; // NOSONAR
    }

    public Boolean getErGodkjent() {
        return erGodkjent;
    }

    public void setErGodkjent(Boolean erGodkjent) {
        this.erGodkjent = erGodkjent;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public void setBegrunnelse(String begrunnelse) {
        this.begrunnelse = begrunnelse;
    }

    public String getArbeidsforholdRef() {
        return arbeidsforholdRef;
    }

    public void setArbeidsforholdRef(String arbeidsforholdRef) {
        this.arbeidsforholdRef = arbeidsforholdRef;
    }
}
