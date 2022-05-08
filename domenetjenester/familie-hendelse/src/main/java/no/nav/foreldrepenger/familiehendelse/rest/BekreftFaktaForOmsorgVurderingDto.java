package no.nav.foreldrepenger.familiehendelse.rest;


import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.MANUELL_KONTROLL_AV_OM_BRUKER_HAR_OMSORG_KODE)
public class BekreftFaktaForOmsorgVurderingDto extends BekreftetAksjonspunktDto {

    @NotNull
    private Boolean omsorg;

    @Valid
    @Size(max = 50)
    private List<PeriodeDto> ikkeOmsorgPerioder;



    BekreftFaktaForOmsorgVurderingDto() { // NOSONAR
        //For Jackson
    }

    public BekreftFaktaForOmsorgVurderingDto(String begrunnelse) { // NOSONAR
        super(begrunnelse);
    }

    public Boolean getOmsorg() {
        return omsorg;
    }

    public void setOmsorg(Boolean omsorg) {
        this.omsorg = omsorg;
    }

    public List<PeriodeDto> getIkkeOmsorgPerioder() {
        return ikkeOmsorgPerioder;
    }

    public void setIkkeOmsorgPerioder(List<PeriodeDto> ikkeOmsorgPerioder) {
        this.ikkeOmsorgPerioder = ikkeOmsorgPerioder;
    }

    @JsonTypeName(AksjonspunktKodeDefinisjon.MANUELL_KONTROLL_AV_OM_BRUKER_HAR_OMSORG_KODE)
    public static class BekreftOmsorgVurderingDto extends BekreftFaktaForOmsorgVurderingDto {


        @SuppressWarnings("unused") // NOSONAR
        private BekreftOmsorgVurderingDto() {
            // For Jackson
        }

        public BekreftOmsorgVurderingDto(String begrunnelse) { // NOSONAR
            super(begrunnelse);
        }


    }

}
