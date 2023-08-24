package no.nav.foreldrepenger.web.app.tjenester.behandling.revurdering.aksjonspunkt;

import com.fasterxml.jackson.annotation.JsonTypeName;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;

import java.time.LocalDate;

@JsonTypeName(AksjonspunktKodeDefinisjon.VARSEL_REVURDERING_MANUELL_KODE)
public class VarselRevurderingManuellDto extends VarselRevurderingDto {

    VarselRevurderingManuellDto() {
        // for jackson
    }

    public VarselRevurderingManuellDto(String begrunnelse, boolean sendVarsel,
            String fritekst, LocalDate frist, Venteårsak ventearsak) {
        super(begrunnelse, sendVarsel, fritekst, frist, ventearsak);
    }

}
