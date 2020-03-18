package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.RelatertBehandlingTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriodeAktivitet;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakOmsorgUtil;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.UttakResultatPeriodeAktivitetDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.UttakResultatPeriodeDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.UttakResultatPerioderDto;

@ApplicationScoped
public class UttakPerioderDtoTjeneste {
    private ForeldrepengerUttakTjeneste uttakTjeneste;
    private RelatertBehandlingTjeneste relatertBehandlingTjeneste;
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private ArbeidsgiverDtoTjeneste arbeidsgiverDtoTjeneste;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    public UttakPerioderDtoTjeneste() {
        // For CDI
    }

    @Inject
    public UttakPerioderDtoTjeneste(ForeldrepengerUttakTjeneste uttakTjeneste,
                                    RelatertBehandlingTjeneste relatertBehandlingTjeneste,
                                    YtelsesFordelingRepository ytelsesFordelingRepository,
                                    ArbeidsgiverDtoTjeneste arbeidsgiverDtoTjeneste,
                                    BehandlingsresultatRepository behandlingsresultatRepository,
                                    InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.uttakTjeneste = uttakTjeneste;
        this.relatertBehandlingTjeneste = relatertBehandlingTjeneste;
        this.ytelsesFordelingRepository = ytelsesFordelingRepository;
        this.arbeidsgiverDtoTjeneste = arbeidsgiverDtoTjeneste;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    public Optional<UttakResultatPerioderDto> mapFra(Behandling behandling) {
        var ytelseFordeling = ytelsesFordelingRepository.hentAggregatHvisEksisterer(behandling.getId());

        final List<UttakResultatPeriodeDto> annenpartUttaksperioder;
        final Optional<ForeldrepengerUttak> annenpartUttak;
        var annenpartBehandling = annenpartBehandling(behandling);
        if (annenpartBehandling.isPresent()) {
            annenpartUttak = uttakTjeneste.hentUttakHvisEksisterer(annenpartBehandling.get().getId());
            if (annenpartUttak.isPresent()) {
                annenpartUttaksperioder = annenpartUttak.map(u -> {
                    var annenpartBehandlingId = annenpartBehandling.orElseThrow().getId();
                    return finnUttakResultatPerioder(u, annenpartBehandlingId);
                }).orElse(List.of());
            } else {
                annenpartUttaksperioder = List.of();
            }
        } else {
            annenpartUttaksperioder = List.of();
            annenpartUttak = Optional.empty();
        }

        var perioderSøker = finnUttakResultatPerioderSøker(behandling.getId());
        var perioder = new UttakResultatPerioderDto(perioderSøker,
            annenpartUttaksperioder,
            ytelseFordeling.map(yf -> UttakOmsorgUtil.harAnnenForelderRett(yf, annenpartUttak)).orElse(false),
            ytelseFordeling.map(UttakOmsorgUtil::harAleneomsorg).orElse(false));
        return Optional.of(perioder);
    }

    private Optional<Behandling> annenpartBehandling(Behandling søkersBehandling) {
        Optional<Behandlingsresultat> behandlingsresultat = behandlingsresultatRepository.hentHvisEksisterer(søkersBehandling.getId());
        if (behandlingsresultat.isPresent()) {
            if (behandlingsresultat.get().getBehandlingVedtak() != null) {
                return relatertBehandlingTjeneste.hentAnnenPartsGjeldendeBehandlingPåVedtakstidspunkt(søkersBehandling);
            }
        }
        return relatertBehandlingTjeneste.hentAnnenPartsGjeldendeVedtattBehandling(søkersBehandling.getFagsak().getSaksnummer());
    }

    private List<UttakResultatPeriodeDto> finnUttakResultatPerioderSøker(Long behandling) {
        return uttakTjeneste.hentUttakHvisEksisterer(behandling).map(uttak -> finnUttakResultatPerioder(uttak, behandling)).orElse(List.of());
    }

    private List<UttakResultatPeriodeDto> finnUttakResultatPerioder(ForeldrepengerUttak uttakResultat, Long behandling) {
        var gjeldenePerioder = uttakResultat.getGjeldendePerioder();

        var iayGrunnlag = inntektArbeidYtelseTjeneste.finnGrunnlag(behandling);
        List<UttakResultatPeriodeDto> list = new ArrayList<>();
        for (var entitet : gjeldenePerioder) {
            UttakResultatPeriodeDto periode = map(entitet, iayGrunnlag);
            list.add(periode);
        }

        return sortedByFom(list);
    }

    private UttakResultatPeriodeDto map(ForeldrepengerUttakPeriode periode, Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlag) {
        var dto = new UttakResultatPeriodeDto.Builder()
            .medTidsperiode(periode.getFom(), periode.getTom())
            .medManuellBehandlingÅrsak(periode.getManuellBehandlingÅrsak())
            .medUtsettelseType(periode.getUtsettelseType())
            .medPeriodeResultatType(periode.getResultatType())
            .medBegrunnelse(periode.getBegrunnelse())
            .medPeriodeResultatÅrsak(periode.getResultatÅrsak())
            .medFlerbarnsdager(periode.isFlerbarnsdager())
            .medSamtidigUttak(periode.isSamtidigUttak())
            .medSamtidigUttaksprosent(periode.getSamtidigUttaksprosent())
            .medGraderingInnvilget(periode.isGraderingInnvilget())
            .medGraderingAvslåttÅrsak(periode.getGraderingAvslagÅrsak())
            .medOppholdÅrsak(periode.getOppholdÅrsak())
            .medPeriodeType(periode.getSøktKonto())
            .build();

        for (var aktivitet : periode.getAktiviteter()) {
            dto.leggTilAktivitet(map(aktivitet, inntektArbeidYtelseGrunnlag, periode.opprinneligSendtTilManuellBehandling()));
        }
        return dto;
    }

    private List<UttakResultatPeriodeDto> sortedByFom(List<UttakResultatPeriodeDto> list) {
        return list
            .stream()
            .sorted(Comparator.comparing(UttakResultatPeriodeDto::getFom))
            .collect(Collectors.toList());
    }

    private UttakResultatPeriodeAktivitetDto map(ForeldrepengerUttakPeriodeAktivitet aktivitet,
                                                 Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlag,
                                                 boolean opprinneligSendtTilManuellBehandling) {
        UttakResultatPeriodeAktivitetDto.Builder builder = new UttakResultatPeriodeAktivitetDto.Builder()
            .medProsentArbeid(aktivitet.getArbeidsprosent())
            .medGradering(aktivitet.isSøktGraderingForAktivitetIPeriode())
            .medTrekkdager(aktivitet.getTrekkdager())
            .medStønadskontoType(aktivitet.getTrekkonto())
            .medUttakArbeidType(aktivitet.getUttakArbeidType());
        mapArbeidsforhold(aktivitet, builder, inntektArbeidYtelseGrunnlag);
        if (!opprinneligSendtTilManuellBehandling) {
            builder.medUtbetalingsgrad(aktivitet.getUtbetalingsgrad());
        }
        return builder.build();
    }

    private void mapArbeidsforhold(ForeldrepengerUttakPeriodeAktivitet aktivitet,
                                   UttakResultatPeriodeAktivitetDto.Builder builder,
                                   Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlag) {
        var arbeidsgiverOptional = aktivitet.getUttakAktivitet().getArbeidsgiver();
        List<ArbeidsforholdOverstyring> overstyringer = inntektArbeidYtelseGrunnlag.map(InntektArbeidYtelseGrunnlag::getArbeidsforholdOverstyringer).orElse(Collections.emptyList());
        var arbeidsgiverDto = arbeidsgiverOptional.map(arbgiver -> arbeidsgiverDtoTjeneste.mapFra(arbgiver, overstyringer)).orElse(null);
        var ref = aktivitet.getArbeidsforholdRef();
        if (ref != null && inntektArbeidYtelseGrunnlag.isPresent() && inntektArbeidYtelseGrunnlag.get().getArbeidsforholdInformasjon().isPresent() && arbeidsgiverOptional.isPresent()) {
            var eksternArbeidsforholdId = inntektArbeidYtelseGrunnlag.get().getArbeidsforholdInformasjon().get().finnEkstern(arbeidsgiverOptional.get(), ref);
            builder.medArbeidsforhold(ref, eksternArbeidsforholdId.getReferanse(), arbeidsgiverDto);
        } else {
            builder.medArbeidsforhold(null, null, arbeidsgiverDto);
        }
    }
}
