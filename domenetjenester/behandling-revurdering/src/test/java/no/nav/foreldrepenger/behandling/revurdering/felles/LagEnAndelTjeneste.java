package no.nav.foreldrepenger.behandling.revurdering.felles;

import static no.nav.foreldrepenger.behandling.revurdering.ytelse.fp.RevurderingBehandlingsresultatutlederTest.ARBEIDSFORHOLDLISTE;
import static no.nav.foreldrepenger.behandling.revurdering.ytelse.fp.RevurderingBehandlingsresultatutlederTest.ORGNR;

import java.math.BigDecimal;
import java.time.LocalDate;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.modell.AktivitetStatus;
import no.nav.foreldrepenger.domene.modell.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPrStatusOgAndel;

public class LagEnAndelTjeneste implements LagAndelTjeneste {

    @Override
    public void lagAndeler(BeregningsgrunnlagPeriode periode,
            boolean medOppjustertDagsat,
            boolean skalDeleAndelMellomArbeidsgiverOgBruker) {
        Dagsatser ds = new Dagsatser(medOppjustertDagsat, skalDeleAndelMellomArbeidsgiverOgBruker);
        BGAndelArbeidsforhold.Builder bga = BGAndelArbeidsforhold
                .builder()
                .medArbeidsgiver(Arbeidsgiver.virksomhet(ORGNR))
                .medArbeidsforholdRef(ARBEIDSFORHOLDLISTE.get(0))
                .medArbeidsperiodeFom(LocalDate.now().minusYears(1))
                .medArbeidsperiodeTom(LocalDate.now().plusYears(2));
        BeregningsgrunnlagPrStatusOgAndel.builder()
                .medBGAndelArbeidsforhold(bga)
                .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
                .medBeregnetPrÅr(BigDecimal.valueOf(240000))
                .medRedusertBrukersAndelPrÅr(ds.getDagsatsBruker())
                .medRedusertRefusjonPrÅr(ds.getDagsatsArbeidstaker())
                .build(periode);
    }
}
