package no.nav.foreldrepenger.web.app.tjenester.behandling.medlem.aksjonspunkt.fp;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AbstractOverstyringshåndterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.domene.ytelsefordeling.BekreftStartdatoForPerioden;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyringAvklarStartdatoForPeriodenDto.class, adapter = Overstyringshåndterer.class)
public class OverstyringAvklarStartdatoForPeriodenHåndterer extends AbstractOverstyringshåndterer<OverstyringAvklarStartdatoForPeriodenDto> {

    private YtelseFordelingTjeneste ytelseFordelingTjeneste;

    OverstyringAvklarStartdatoForPeriodenHåndterer() {
        // for CDI proxy
    }

    @Inject
    public OverstyringAvklarStartdatoForPeriodenHåndterer(HistorikkTjenesteAdapter historikkAdapter,
                                                          YtelseFordelingTjeneste ytelseFordelingTjeneste) {
        super(historikkAdapter, AksjonspunktDefinisjon.OVERSTYRING_AV_AVKLART_STARTDATO);
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
    }

    @Override
    public OppdateringResultat håndterOverstyring(OverstyringAvklarStartdatoForPeriodenDto dto, Behandling behandling, BehandlingskontrollKontekst kontekst) {
        ytelseFordelingTjeneste.aksjonspunktAvklarStartdatoForPerioden(behandling.getId(), new BekreftStartdatoForPerioden(dto.getStartdatoFraSoknad()));
        return OppdateringResultat.utenTransisjon().build();
    }

    @Override
    protected void lagHistorikkInnslag(Behandling behandling, OverstyringAvklarStartdatoForPeriodenDto dto) {
        var opprinneligDato = dto.getOpprinneligDato();
        var startdatoFraSoknad = dto.getStartdatoFraSoknad();
        if (!startdatoFraSoknad.equals(opprinneligDato)) {
            getHistorikkAdapter().tekstBuilder()
                .medHendelse(HistorikkinnslagType.FAKTA_ENDRET)
                .medSkjermlenke(SkjermlenkeType.FAKTA_OM_MEDLEMSKAP)
                .medBegrunnelse(dto.getBegrunnelse())
                .medEndretFelt(HistorikkEndretFeltType.STARTDATO_FRA_SOKNAD, opprinneligDato, startdatoFraSoknad);
        }
    }
}
