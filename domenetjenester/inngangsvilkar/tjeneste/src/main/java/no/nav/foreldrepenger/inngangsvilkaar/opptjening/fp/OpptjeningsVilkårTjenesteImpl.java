package no.nav.foreldrepenger.inngangsvilkaar.opptjening.fp;

import java.time.LocalDate;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.impl.InngangsvilkårOversetter;
import no.nav.foreldrepenger.inngangsvilkaar.opptjening.OpptjeningsVilkårTjeneste;
import no.nav.foreldrepenger.inngangsvilkaar.opptjening.OpptjeningsgrunnlagAdapter;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.OpptjeningsvilkårResultat;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.fp.OpptjeningsvilkårForeldrepenger;

@ApplicationScoped
@FagsakYtelseTypeRef("FP")
public class OpptjeningsVilkårTjenesteImpl implements OpptjeningsVilkårTjeneste {
    private OpptjeningInntektArbeidYtelseTjeneste opptjeningTjeneste;
    private InngangsvilkårOversetter inngangsvilkårOversetter;

    public OpptjeningsVilkårTjenesteImpl() {
    }

    @Inject
    public OpptjeningsVilkårTjenesteImpl(InngangsvilkårOversetter inngangsvilkårOversetter,
                                       OpptjeningInntektArbeidYtelseTjeneste opptjeningTjeneste) {
        this.inngangsvilkårOversetter = inngangsvilkårOversetter;
        this.opptjeningTjeneste = opptjeningTjeneste;
    }


    @Override
    public VilkårData vurderOpptjeningsVilkår(BehandlingReferanse behandlingReferanse) {
        var behandlingId = behandlingReferanse.getBehandlingId();
        var aktørId = behandlingReferanse.getAktørId();
        var skjæringstidspunkt = behandlingReferanse.getUtledetSkjæringstidspunkt();

        var relevanteOpptjeningAktiveter = opptjeningTjeneste.hentRelevanteOpptjeningAktiveterForVilkårVurdering(behandlingReferanse);
        var relevanteOpptjeningInntekter = opptjeningTjeneste.hentRelevanteOpptjeningInntekterForVilkårVurdering(behandlingId, aktørId, skjæringstidspunkt);
        var opptjening = opptjeningTjeneste.hentOpptjening(behandlingId);

        var behandlingstidspunkt = LocalDate.now(); // TODO (FC): Avklar hva denne bør være

        var grunnlag = new OpptjeningsgrunnlagAdapter(behandlingstidspunkt, opptjening.getFom(),
            opptjening.getTom())
            .mapTilGrunnlag(relevanteOpptjeningAktiveter, relevanteOpptjeningInntekter);

        // returner egen output i tillegg for senere lagring
        var output = new OpptjeningsvilkårResultat();
        var evaluation = new OpptjeningsvilkårForeldrepenger().evaluer(grunnlag, output);

        var vilkårData = inngangsvilkårOversetter.tilVilkårData(VilkårType.OPPTJENINGSVILKÅRET, evaluation, grunnlag);
        vilkårData.setEkstraVilkårresultat(output);

        return vilkårData;
    }
}
