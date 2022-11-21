package no.nav.foreldrepenger.domene.uttak.fakta.v2;

import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType.FELLESPERIODE;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType.FORELDREPENGER;
import static no.nav.foreldrepenger.domene.uttak.UttakOmsorgUtil.harAleneomsorg;
import static no.nav.foreldrepenger.domene.uttak.fakta.v2.DokumentasjonVurderingBehov.Behov.UttakÅrsak.AKTIVITETSKRAV_ARBEID;
import static no.nav.foreldrepenger.domene.uttak.fakta.v2.DokumentasjonVurderingBehov.Behov.UttakÅrsak.AKTIVITETSKRAV_ARBEID_OG_UTDANNING;
import static no.nav.foreldrepenger.domene.uttak.fakta.v2.DokumentasjonVurderingBehov.Behov.UttakÅrsak.AKTIVITETSKRAV_IKKE_OPPGITT;
import static no.nav.foreldrepenger.domene.uttak.fakta.v2.DokumentasjonVurderingBehov.Behov.UttakÅrsak.AKTIVITETSKRAV_INNLAGT;
import static no.nav.foreldrepenger.domene.uttak.fakta.v2.DokumentasjonVurderingBehov.Behov.UttakÅrsak.AKTIVITETSKRAV_INTROPROG;
import static no.nav.foreldrepenger.domene.uttak.fakta.v2.DokumentasjonVurderingBehov.Behov.UttakÅrsak.AKTIVITETSKRAV_KVALPROG;
import static no.nav.foreldrepenger.domene.uttak.fakta.v2.DokumentasjonVurderingBehov.Behov.UttakÅrsak.AKTIVITETSKRAV_TRENGER_HJELP;
import static no.nav.foreldrepenger.domene.uttak.fakta.v2.DokumentasjonVurderingBehov.Behov.UttakÅrsak.AKTIVITETSKRAV_UTDANNING;

import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakOmsorgUtil;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.regler.uttak.felles.Virkedager;

@ApplicationScoped
public class AktivitetskravDokumentasjonUtleder {

    private ForeldrepengerUttakTjeneste foreldrepengerUttakTjeneste;

    @Inject
    public AktivitetskravDokumentasjonUtleder(ForeldrepengerUttakTjeneste foreldrepengerUttakTjeneste) {
        this.foreldrepengerUttakTjeneste = foreldrepengerUttakTjeneste;
    }

    AktivitetskravDokumentasjonUtleder() {
        //CDI
    }

    Optional<DokumentasjonVurderingBehov.Behov> utledBehov(UttakInput input,
                                                           OppgittPeriodeEntitet periode,
                                                           YtelseFordelingAggregat ytelseFordelingAggregat) {
        var behandlingReferanse = input.getBehandlingReferanse();
        ForeldrepengerGrunnlag fpGrunnlag = input.getYtelsespesifiktGrunnlag();
        var familieHendelse = fpGrunnlag.getFamilieHendelser().getGjeldendeFamilieHendelse();
        if (helePeriodenErHelg(periode) || RelasjonsRolleType.erMor(behandlingReferanse.relasjonRolle()) || harAleneomsorg(ytelseFordelingAggregat)
            || familieHendelse.erStebarnsadopsjon() || MorsAktivitet.UFØRE.equals(periode.getMorsAktivitet()) || MorsAktivitet.IKKE_OPPGITT.equals(periode.getMorsAktivitet())) {
            return Optional.empty();
        }

        var periodeType = periode.getPeriodeType();
        var harKravTilAktivitet =
            !periode.isFlerbarnsdager() && (Set.of(FELLESPERIODE, FORELDREPENGER).contains(periodeType) || bareFarHarRettOgSøkerUtsettelse(periode,
                ytelseFordelingAggregat, fpGrunnlag));
        if (!harKravTilAktivitet) {
            return Optional.empty();
        }
        return Optional.of(new DokumentasjonVurderingBehov.Behov(DokumentasjonVurderingBehov.Behov.Type.UTTAK,
            mapÅrsak(periode.getMorsAktivitet())));
    }

    private DokumentasjonVurderingBehov.Behov.Årsak mapÅrsak(MorsAktivitet morsAktivitet) {
        if (morsAktivitet == null) {
            return AKTIVITETSKRAV_IKKE_OPPGITT;
        }
        return switch (morsAktivitet) {
            case ARBEID -> AKTIVITETSKRAV_ARBEID;
            case UTDANNING -> AKTIVITETSKRAV_UTDANNING;
            case KVALPROG -> AKTIVITETSKRAV_KVALPROG;
            case INTROPROG -> AKTIVITETSKRAV_INTROPROG;
            case TRENGER_HJELP -> AKTIVITETSKRAV_TRENGER_HJELP;
            case INNLAGT -> AKTIVITETSKRAV_INNLAGT;
            case ARBEID_OG_UTDANNING -> AKTIVITETSKRAV_ARBEID_OG_UTDANNING;
            case IKKE_OPPGITT, UDEFINERT -> AKTIVITETSKRAV_IKKE_OPPGITT;
            case UFØRE -> throw new IllegalArgumentException("Er ikke behov for avklaring av " + morsAktivitet);
        };
    }

    private boolean bareFarHarRettOgSøkerUtsettelse(OppgittPeriodeEntitet periode,
                                                    YtelseFordelingAggregat ytelseFordelingAggregat,
                                                    ForeldrepengerGrunnlag foreldrepengerGrunnlag) {
        return (Set.of(UtsettelseÅrsak.ARBEID, UtsettelseÅrsak.FERIE).contains(periode.getÅrsak()) || (UtsettelseÅrsak.FRI.equals(periode.getÅrsak())
            && MorsAktivitet.forventerDokumentasjon(periode.getMorsAktivitet()))) && !harAnnenForelderRett(ytelseFordelingAggregat,
            foreldrepengerGrunnlag);
    }

    private boolean helePeriodenErHelg(OppgittPeriodeEntitet periode) {
        return Virkedager.beregnAntallVirkedager(periode.getFom(), periode.getTom()) == 0;
    }

    private boolean harAnnenForelderRett(YtelseFordelingAggregat ytelseFordelingAggregat, ForeldrepengerGrunnlag ytelsespesifiktGrunnlag) {
        var annenpart = ytelsespesifiktGrunnlag.getAnnenpart();
        return UttakOmsorgUtil.harAnnenForelderRett(ytelseFordelingAggregat,
            annenpart.isEmpty() ? Optional.empty() : foreldrepengerUttakTjeneste.hentUttakHvisEksisterer(
                annenpart.get().gjeldendeVedtakBehandlingId()));
    }
}