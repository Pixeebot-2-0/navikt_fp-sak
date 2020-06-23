package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.oppdrag;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.AssertFalse;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.sikkerhet.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;

@JsonInclude(value = JsonInclude.Include.NON_ABSENT, content = JsonInclude.Include.NON_EMPTY)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
public class OppdragPatchDto implements AbacDto {

    @Min(0)
    @Max(Long.MAX_VALUE)
    @NotNull
    @JsonProperty("behandingId")
    private Long behandlingId;

    @NotNull
    @JsonProperty("brukerErMottaker")
    private Boolean brukerErMottaker;

    @Size(min = 9, max = 9)
    @Pattern(regexp = "^\\d*$")
    @JsonProperty("arbeidsgiverOrgnr")
    private String arbeidsgiverOrgNr;

    @NotNull
    @Pattern(regexp = "^UEND|ENDR|NY$")
    @JsonProperty("kodeEndring")
    private String kodeEndring;

    @NotNull
    @Min(10000000000L)
    @Max(300000000000L)
    @JsonProperty("fagsystemId")
    private Long fagsystemId;

    @Valid
    @NotNull
    @Size(min = 1, max = 20)
    @JsonProperty("oppdragslinjer")
    private List<OppdragslinjePatchDto> oppdragslinjer;

    @JsonProperty("bruk-ompostering116")
    private boolean brukOmpostering116 = false;

    @JsonProperty("omposter-fom")
    private LocalDate omposterFom;

    @AssertTrue
    boolean isEntenBrukerMottakerEllerArbeidsgiverOppgitt() {
        boolean arbeidsgiverOppgitt = arbeidsgiverOrgNr != null;
        return brukerErMottaker != arbeidsgiverOppgitt;
    }

    @AssertFalse(message = "må sette bruk-ompostering116 for å bruke omposter-fom")
    boolean isOmposteringFomSattUtenBrukOmpostering116Satt() {
        return omposterFom != null && !brukOmpostering116;
    }

    @AssertTrue
    boolean isAntallDagerSannsynlig() {
        long sum = 0;
        for (OppdragslinjePatchDto dto : oppdragslinjer) {
            sum += ChronoUnit.DAYS.between(dto.getFom(), dto.getTom()) + 1;
        }
        return sum < 365;
    }

    @AssertTrue
    boolean isSumBeløpSannsynlig() {
        long estimertSum = 0;
        for (OppdragslinjePatchDto dto : oppdragslinjer) {
            if ("ENG".equals(dto.getSatsType())) {
                estimertSum += dto.getSats();
            } else {
                long dager = ChronoUnit.DAYS.between(dto.getFom(), dto.getTom()) + 1;
                estimertSum += dager * dto.getSats();
            }
        }
        //beløp for FP kan være opptil 6G over ett år
        return estimertSum < 600000;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public boolean erBrukerMottaker() {
        return brukerErMottaker;
    }

    public long getFagsystemId() {
        return fagsystemId;
    }

    public String getArbeidsgiverOrgNr() {
        return arbeidsgiverOrgNr;
    }

    public String getKodeEndring() {
        return kodeEndring;
    }

    public List<OppdragslinjePatchDto> getOppdragslinjer() {
        return oppdragslinjer;
    }

    public LocalDate getOmposterFom() {
        return omposterFom;
    }

    public boolean taMedOmpostering116() {
        return brukOmpostering116;
    }

    @Override
    public AbacDataAttributter abacAttributter() {
        return AbacDataAttributter.opprett()
            .leggTil(AppAbacAttributtType.BEHANDLING_ID, behandlingId);
    }
}
