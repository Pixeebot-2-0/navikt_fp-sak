package no.nav.foreldrepenger.domene.fpinntektsmelding;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonValue;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.tilMaskertNummer;

public record OpprettForespørselRequest(@NotNull @Valid AktørIdDto aktørId,
                                        @NotNull @Valid OrganisasjonsnummerDto orgnummer,
                                        @NotNull LocalDate skjæringstidspunkt,
                                        @NotNull YtelseType ytelsetype,
                                        @NotNull @Valid SaksnummerDto fagsakSaksnummer) {
    protected record AktørIdDto(@NotNull @JsonValue String id){
        @Override
        public String toString() {
            return getClass().getSimpleName() + "<" + maskerAktørId() + ">";
        }

        private String maskerAktørId() {
            if (id == null) {
                return "";
            }
            var length = id.length();
            if (length <= 4) {
                return "*".repeat(length);
            }
            return "*".repeat(length - 4) + id.substring(length - 4);
        }
    }
    protected record SaksnummerDto(@NotNull @JsonValue String saksnr){}
    protected record OrganisasjonsnummerDto(@NotNull @JsonValue String orgnr){
        @Override
        public String toString() {
            return getClass().getSimpleName() + "<" + tilMaskertNummer(orgnr) + ">";
        }

    }
    protected enum YtelseType {
        FORELDREPENGER,
        SVANGERSKAPSPENGER
    }
}
