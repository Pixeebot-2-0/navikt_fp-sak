package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagInputProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.domene.prosess.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.mappers.til_kalkulus.OppdatererDtoMapper;
import no.nav.foreldrepenger.domene.rest.BeregningHåndterer;
import no.nav.foreldrepenger.domene.rest.dto.FastsettBGTidsbegrensetArbeidsforholdDto;
import no.nav.foreldrepenger.domene.rest.dto.FastsettBeregningsgrunnlagATFLDto;
import no.nav.foreldrepenger.domene.rest.historikk.FastsettBGTidsbegrensetArbeidsforholdHistorikkTjeneste;
import no.nav.foreldrepenger.domene.rest.historikk.FastsettBeregningsgrunnlagATFLHistorikkTjeneste;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagTilstand;

@ApplicationScoped
@DtoTilServiceAdapter(dto = no.nav.foreldrepenger.domene.rest.dto.FastsettBeregningsgrunnlagATFLDto.class, adapter = AksjonspunktOppdaterer.class)
public class FastsettBeregningsgrunnlagATFLOppdaterer implements AksjonspunktOppdaterer<no.nav.foreldrepenger.domene.rest.dto.FastsettBeregningsgrunnlagATFLDto> {

    private HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    private FastsettBeregningsgrunnlagATFLHistorikkTjeneste fastsettBeregningsgrunnlagATFLHistorikkTjeneste;
    private FastsettBGTidsbegrensetArbeidsforholdHistorikkTjeneste fastsettBGTidsbegrensetArbeidsforholdHistorikkTjeneste;
    private BeregningsgrunnlagInputProvider beregningsgrunnlagInputTjeneste;
    private BeregningHåndterer beregningHåndterer;

    protected FastsettBeregningsgrunnlagATFLOppdaterer() {
        // CDI
    }

    @Inject
    public FastsettBeregningsgrunnlagATFLOppdaterer(HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                                                    FastsettBeregningsgrunnlagATFLHistorikkTjeneste fastsettBeregningsgrunnlagATFLHistorikkTjeneste,
                                                    FastsettBGTidsbegrensetArbeidsforholdHistorikkTjeneste fastsettBGTidsbegrensetArbeidsforholdHistorikkTjeneste,
                                                    BeregningsgrunnlagInputProvider beregningsgrunnlagInputTjeneste,
                                                    BeregningHåndterer beregningHåndterer) {
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
        this.fastsettBeregningsgrunnlagATFLHistorikkTjeneste = fastsettBeregningsgrunnlagATFLHistorikkTjeneste;
        this.fastsettBGTidsbegrensetArbeidsforholdHistorikkTjeneste = fastsettBGTidsbegrensetArbeidsforholdHistorikkTjeneste;
        this.beregningsgrunnlagInputTjeneste = beregningsgrunnlagInputTjeneste;
        this.beregningHåndterer = beregningHåndterer;
    }

    @Override
    public OppdateringResultat oppdater(FastsettBeregningsgrunnlagATFLDto dto, AksjonspunktOppdaterParameter param) {
        var aktivtGrunnlag = beregningsgrunnlagTjeneste.hentBeregningsgrunnlagEntitetAggregatForBehandling(param.getBehandlingId());
        var ref = param.getRef();
        var tjeneste = beregningsgrunnlagInputTjeneste.getTjeneste(ref.getFagsakYtelseType());
        var input = tjeneste.lagInput(ref);
        var forrigeGrunnlag = beregningsgrunnlagTjeneste.hentSisteBeregningsgrunnlagGrunnlagEntitet(param.getBehandlingId(),
            BeregningsgrunnlagTilstand.FORESLÅTT_UT)
            .flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag);
        if (dto.tidsbegrensetInntektErFastsatt()) {
            var tidsbegrensetDto = new FastsettBGTidsbegrensetArbeidsforholdDto(dto.getBegrunnelse(), dto.getFastsatteTidsbegrensedePerioder(), dto.getInntektFrilanser());
            beregningHåndterer.håndterFastsettBGTidsbegrensetArbeidsforhold(input, OppdatererDtoMapper.mapFastsettBGTidsbegrensetArbeidsforholdDto(tidsbegrensetDto));
            fastsettBGTidsbegrensetArbeidsforholdHistorikkTjeneste.lagHistorikk(param, aktivtGrunnlag, forrigeGrunnlag, tidsbegrensetDto);
            var builder = OppdateringResultat.utenTransisjon();
            return builder.build();
        }
        beregningHåndterer.håndterFastsettBeregningsgrunnlagATFL(input, OppdatererDtoMapper.mapFastsettBeregningsgrunnlagATFLDto(dto));
        fastsettBeregningsgrunnlagATFLHistorikkTjeneste.lagHistorikk(param, dto, aktivtGrunnlag);
        var builder = OppdateringResultat.utenTransisjon();
        håndterEventueltOverflødigAksjonspunkt(param.getBehandling())
            .ifPresent(ap -> builder.medEkstraAksjonspunktResultat(ap.getAksjonspunktDefinisjon(), AksjonspunktStatus.AVBRUTT));
        return builder.build();
    }

    /*
        Ved tilbakehopp hender det at avbrutte aksjonspunkter gjenopprettes feilaktig. Denne funksjonen sørger for å rydde opp
        i dette for det ene scenarioet det er mulig i beregningsmodulen. Også implementert i det motsatte tilfellet.
        Se https://jira.adeo.no/browse/PFP-2042 for mer informasjon.
     */
    private Optional<Aksjonspunkt> håndterEventueltOverflødigAksjonspunkt(Behandling behandling) {
        return behandling.getÅpentAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_TIDSBEGRENSET_ARBEIDSFORHOLD);
    }
}
