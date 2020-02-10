package no.nav.foreldrepenger.web.app.tjenester.registrering.svp;

import com.fasterxml.jackson.annotation.JsonTypeName;

import javax.validation.Valid;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.MedInntektArbeidYtelseRegistrering;

@JsonTypeName(AksjonspunktKodeDefinisjon.REGISTRER_PAPIRSØKNAD_SVANGERSKAPSPENGER_KODE)
public class ManuellRegistreringSvangerskapspengerDto extends MedInntektArbeidYtelseRegistrering {


    private boolean migrertFraInfotrygd;

    @Valid
    private List<SvpTilretteleggingArbeidsforholdDto> tilretteleggingArbeidsforhold;


    public List<SvpTilretteleggingArbeidsforholdDto> getTilretteleggingArbeidsforhold() {
        return tilretteleggingArbeidsforhold;
    }

    public void setTilretteleggingArbeidsforhold(List<SvpTilretteleggingArbeidsforholdDto> tilretteleggingArbeidsforhold) {
        this.tilretteleggingArbeidsforhold = tilretteleggingArbeidsforhold;
    }

    public boolean isMigrertFraInfotrygd() {
        return migrertFraInfotrygd;
    }

    public void setMigrertFraInfotrygd(boolean migrertFraInfotrygd) {
        this.migrertFraInfotrygd = migrertFraInfotrygd;
    }

}
