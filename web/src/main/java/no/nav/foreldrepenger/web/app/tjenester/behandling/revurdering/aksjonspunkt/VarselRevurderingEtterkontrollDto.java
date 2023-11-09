package no.nav.foreldrepenger.web.app.tjenester.behandling.revurdering.aksjonspunkt;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;

@JsonTypeName(AksjonspunktKodeDefinisjon.VARSEL_REVURDERING_ETTERKONTROLL_KODE)
public class VarselRevurderingEtterkontrollDto extends VarselRevurderingDto {

    VarselRevurderingEtterkontrollDto() {
        // for jackson
    }

    public VarselRevurderingEtterkontrollDto(String begrunnelse, boolean sendVarsel,
            String fritekst, LocalDate frist, Venteårsak ventearsak) {
        super(begrunnelse, sendVarsel, fritekst, frist, ventearsak);
    }

}
