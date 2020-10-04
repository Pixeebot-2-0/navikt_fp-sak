package no.nav.foreldrepenger.web.app.tjenester.behandling.anke.aksjonspunkt;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeOmgjørÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurderingOmgjør;
import no.nav.foreldrepenger.validering.ValidKodeverk;
import no.nav.vedtak.util.InputValideringRegex;

@JsonTypeName(AksjonspunktKodeDefinisjon.MANUELL_VURDERING_AV_ANKE_KODE)
@JsonAutoDetect(getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, fieldVisibility= JsonAutoDetect.Visibility.ANY)
public class AnkeVurderingResultatAksjonspunktDto extends BekreftetAksjonspunktDto {

    @NotNull
    @ValidKodeverk
    @JsonProperty("ankeVurdering")
    private AnkeVurdering ankeVurdering;

    @JsonProperty("erSubsidiartRealitetsbehandles")
    private boolean erSubsidiartRealitetsbehandles;

    // Økt størrelsen for å håndtere all fritekst som blir skrevet til ankebrev
    @Size(max = 100000)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    @JsonProperty("fritekstTilBrev")
    private String fritekstTilBrev;

    @ValidKodeverk
    @JsonProperty("ankeOmgjoerArsak")
    private AnkeOmgjørÅrsak ankeOmgjoerArsak;

    @ValidKodeverk
    @JsonProperty("ankeVurderingOmgjoer")
    private AnkeVurderingOmgjør ankeVurderingOmgjoer;

    @JsonProperty("erGodkjentAvMedunderskriver")
    private boolean erGodkjentAvMedunderskriver;

    @JsonProperty("vedtak")
    // TODO (BehandlingIdDto): bør kunne støtte behandlingUuid også?  Hvorfor heter property "vedtak"?
    private Long påAnketBehandlingId;

    @JsonProperty("erAnkerIkkePart")
    private boolean erAnkerIkkePart;

    @JsonProperty("erFristIkkeOverholdt")
    private boolean erFristIkkeOverholdt;

    @JsonProperty("erIkkeKonkret")
    private boolean erIkkeKonkret;

    @JsonProperty("erIkkeSignert")
    private boolean erIkkeSignert;

    AnkeVurderingResultatAksjonspunktDto() { // NOSONAR
        // For Jackson
    }

    public AnkeVurderingResultatAksjonspunktDto( // NOSONAR
                                                 String begrunnelse,
                                                 AnkeVurdering ankeVurdering,
                                                 AnkeOmgjørÅrsak ankeOmgjoerArsak,
                                                 String fritekstTilBrev,
                                                 AnkeVurderingOmgjør ankeVurderingOmgjoer,
                                                 boolean erSubsidiartRealitetsbehandles,
                                                 Long påAnketBehandlingId,
                                                 boolean erIkkeAnkerPart,
                                                 boolean erFristIkkeOverholdt,
                                                 boolean erIkkeKonkret,
                                                 boolean erIkkeSignert,
                                                 boolean erGodkjentAvMedunderskriver) {
        super(begrunnelse);
        this.ankeVurdering = ankeVurdering;
        this.fritekstTilBrev = fritekstTilBrev;
        this.ankeOmgjoerArsak = ankeOmgjoerArsak;
        this.ankeVurderingOmgjoer = ankeVurderingOmgjoer;
        this.erSubsidiartRealitetsbehandles = erSubsidiartRealitetsbehandles;
        this.påAnketBehandlingId = påAnketBehandlingId;
        this.erAnkerIkkePart = erIkkeAnkerPart;
        this.erFristIkkeOverholdt = erFristIkkeOverholdt;
        this.erIkkeKonkret = erIkkeKonkret;
        this.erIkkeSignert = erIkkeSignert;
        this.erGodkjentAvMedunderskriver = erGodkjentAvMedunderskriver;
    }

    public AnkeVurdering getAnkeVurdering() {
        return ankeVurdering;
    }

    public boolean erSubsidiartRealitetsbehandles() {
        return erSubsidiartRealitetsbehandles;
    }

    public String getFritekstTilBrev() {
        return fritekstTilBrev;
    }

    public AnkeOmgjørÅrsak getAnkeOmgjoerArsak() {
        return ankeOmgjoerArsak;
    }

    public AnkeVurderingOmgjør getAnkeVurderingOmgjoer() {
        return ankeVurderingOmgjoer;
    }

    public boolean erGodkjentAvMedunderskriver() {
        return erGodkjentAvMedunderskriver;
    }

    public Long hentPåAnketBehandlingId() {
        return påAnketBehandlingId;
    }

    public boolean erAnkerIkkePart() {
        return erAnkerIkkePart;
    }

    public boolean erFristIkkeOverholdt() {
        return erFristIkkeOverholdt;
    }

    public boolean erIkkeKonkret() {
        return erIkkeKonkret;
    }

    public boolean erIkkeSignert() {
        return erIkkeSignert;
    }

}
