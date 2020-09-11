package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.dto.fordeling;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.Inntektskategori;


public class FordelFastsatteVerdierDto {

    @Min(0)
    @Max(Integer.MAX_VALUE)
    private Integer refusjonPrÅr;

    @Min(0)
    @Max(Integer.MAX_VALUE)
    @NotNull
    private Integer fastsattÅrsbeløpInklNaturalytelse;

    @NotNull
    private Inntektskategori inntektskategori;


    FordelFastsatteVerdierDto() { // NOSONAR
        // Jackson
    }

    public FordelFastsatteVerdierDto(@Min(0) @Max(Integer.MAX_VALUE) Integer refusjonPrÅr, @Min(0) @Max(Integer.MAX_VALUE) @NotNull Integer fastsattÅrsbeløpInklNaturalytelse, @NotNull Inntektskategori inntektskategori) {
        this.refusjonPrÅr = refusjonPrÅr;
        this.fastsattÅrsbeløpInklNaturalytelse = fastsattÅrsbeløpInklNaturalytelse;
        this.inntektskategori = inntektskategori;
    }

    public FordelFastsatteVerdierDto(@Min(0) @Max(Integer.MAX_VALUE) @NotNull Integer fastsattÅrsbeløpInklNaturalytelse, @NotNull Inntektskategori inntektskategori) {
        this.fastsattÅrsbeløpInklNaturalytelse = fastsattÅrsbeløpInklNaturalytelse;
        this.inntektskategori = inntektskategori;
    }

    public Integer getRefusjonPrÅr() {
        return refusjonPrÅr;
    }

    public void setRefusjonPrÅr(Integer refusjonPrÅr) {
        this.refusjonPrÅr = refusjonPrÅr;
    }

    public Integer getFastsattÅrsbeløpInklNaturalytelse() {
        return fastsattÅrsbeløpInklNaturalytelse;
    }

    public Inntektskategori getInntektskategori() {
        return inntektskategori;
    }

}
