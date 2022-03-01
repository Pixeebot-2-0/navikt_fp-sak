package no.nav.foreldrepenger.domene.arbeidInntektsmelding;

import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdValg;
import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdKomplettVurderingType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.AksjonspunktÅrsak;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjonBuilder;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyringBuilder;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper som mapper saksbehandlers vurdering om til domeneobjekter og validerer valg som er tatt
 */
public class ArbeidsforholdInntektsmeldingMangelMapper {

    private ArbeidsforholdInntektsmeldingMangelMapper() {
        // Skjuler default konstruktør
    }

    public static List<ArbeidsforholdValg> mapManglendeOpplysningerVurdering(ManglendeOpplysningerVurderingDto saksbehandlersVurdering,
                                                                       List<ArbeidsforholdInntektsmeldingMangel> arbeidsforholdMedMangler) {
        Arbeidsgiver arbeidsgiver = lagArbeidsgiver(saksbehandlersVurdering.getArbeidsgiverIdent());

        InternArbeidsforholdRef referanse = finnReferanse(saksbehandlersVurdering);

        // En avklaring kan gjelde flere mangler, dersom det er flere arbeidsforhold hos samme arbeidsgiver med samme mangel må samme valg gjelde for alle
        var manglerSomBlirAvklart = finnManglerSomBlirAvklart(arbeidsgiver, referanse, arbeidsforholdMedMangler,
            AksjonspunktÅrsak.MANGLENDE_INNTEKTSMELDING, AksjonspunktÅrsak.INNTEKTSMELDING_UTEN_ARBEIDSFORHOLD);

        return manglerSomBlirAvklart.stream()
            .map(mangel -> ArbeidsforholdValg.builder()
                .medArbeidsgiver(mangel.arbeidsgiver().getIdentifikator())
                .medArbeidsforholdRef(mangel.ref())
                .medVurdering(saksbehandlersVurdering.getVurdering())
                .medBegrunnelse(saksbehandlersVurdering.getBegrunnelse())
                .build())
            .collect(Collectors.toList());
    }

    private static List<ArbeidsforholdInntektsmeldingMangel> finnManglerSomBlirAvklart(Arbeidsgiver arbeidsgiver,
                                                                                       InternArbeidsforholdRef referanse,
                                                                                       List<ArbeidsforholdInntektsmeldingMangel> arbeidsforholdMedMangler,
                                                                                       AksjonspunktÅrsak... årsakPåMangel) {
        var gyldigeÅrsaker = Arrays.asList(årsakPåMangel);
        var manglerSomAvklares = arbeidsforholdMedMangler.stream()
            .filter(mangel -> mangel.arbeidsgiver().equals(arbeidsgiver) && mangel.ref().gjelderFor(referanse))
            .filter(mangel -> gyldigeÅrsaker.contains(mangel.årsak()))
            .collect(Collectors.toList());
        if (manglerSomAvklares.size() < 1) {
            throw new IllegalStateException("Feil: Finnes ingen åpne mangler på arbeidsforhold hos "
                + arbeidsgiver + " med arbeidsforholdId " + referanse);
        }
        return manglerSomAvklares;
    }

    public static Arbeidsgiver lagArbeidsgiver(String arbeidsgiverIdent) {
        if (OrgNummer.erGyldigOrgnr(arbeidsgiverIdent)) {
            return Arbeidsgiver.virksomhet(arbeidsgiverIdent);
        }
        return Arbeidsgiver.person(new AktørId(arbeidsgiverIdent));
    }

    private static InternArbeidsforholdRef finnReferanse(ManglendeOpplysningerVurderingDto dto) {
        return dto.getInternArbeidsforholdRef() == null
            ? InternArbeidsforholdRef.nullRef()
            : InternArbeidsforholdRef.ref(dto.getInternArbeidsforholdRef());
    }

    private static void validerAtArbeidsforholdErÅpentForEndring(Arbeidsgiver arbeidsgiver,
                                                                 InternArbeidsforholdRef referanse,
                                                                 List<ArbeidsforholdInntektsmeldingMangel> arbeidsforholdMedMangler,
                                                                 AksjonspunktÅrsak... gyldigeÅrsaker) {
        var årsaker = Arrays.asList(gyldigeÅrsaker);
        long arbeidsforholdMedMangelSomMatcherAvklaring = arbeidsforholdMedMangler.stream()
            .filter(arbfor -> arbfor.arbeidsgiver().equals(arbeidsgiver) && referanse.gjelderFor(arbfor.ref()))
            .filter(arbfor -> årsaker.contains(arbfor.årsak()))
            .count();
        if (arbeidsforholdMedMangelSomMatcherAvklaring != 1) {
            throw new IllegalStateException("Forventet at antall arbeidsforhold som matcher avklaring er akkurat 1. " +
                "Faktisk resultat var " + arbeidsforholdMedMangelSomMatcherAvklaring);
        }
    }


    public static ArbeidsforholdInformasjonBuilder mapManueltArbeidsforhold(ManueltArbeidsforholdDto saksbehandlersVurdering,
                                                                            List<ArbeidsforholdInntektsmeldingMangel> arbeidsforholdMedMangler,
                                                                            ArbeidsforholdInformasjonBuilder informasjonBuilder) {
        if (saksbehandlersVurdering.getVurdering().equals(ArbeidsforholdKomplettVurderingType.OPPRETT_BASERT_PÅ_INNTEKTSMELDING)) {
            Arbeidsgiver arbeidsgiver = lagArbeidsgiver(saksbehandlersVurdering.getArbeidsgiverIdent());
            validerAtArbeidsforholdErÅpentForEndring(arbeidsgiver, InternArbeidsforholdRef.nullRef(), arbeidsforholdMedMangler, AksjonspunktÅrsak.INNTEKTSMELDING_UTEN_ARBEIDSFORHOLD);
            leggTilArbeidsforhold(saksbehandlersVurdering, informasjonBuilder);
        } else if (saksbehandlersVurdering.getVurdering().equals(ArbeidsforholdKomplettVurderingType.MANUELT_OPPRETTET_AV_SAKSBEHANDLER)) {
            leggTilArbeidsforhold(saksbehandlersVurdering, informasjonBuilder);
        } else if (saksbehandlersVurdering.getVurdering().equals(ArbeidsforholdKomplettVurderingType.FJERN_FRA_BEHANDLINGEN)) {
            fjernOpprettetArbeidsforhold(saksbehandlersVurdering.getArbeidsgiverIdent(), informasjonBuilder);
        } else {
            throw new IllegalStateException("Ugyldig vurdering: " + saksbehandlersVurdering.getVurdering());
        }
        return informasjonBuilder;
    }

    private static void fjernOpprettetArbeidsforhold(String arbeidsgiversIdent, ArbeidsforholdInformasjonBuilder informasjonBuilder) {
        Arbeidsgiver arbeidsgiver = lagArbeidsgiver(arbeidsgiversIdent);
        informasjonBuilder.fjernOverstyringerSomGjelder(arbeidsgiver);
    }

    private static void leggTilArbeidsforhold(ManueltArbeidsforholdDto saksbehandlersVurdering, ArbeidsforholdInformasjonBuilder informasjonBuilder) {
        Arbeidsgiver arbeidsgiver = lagArbeidsgiver(saksbehandlersVurdering.getArbeidsgiverIdent());
        var ref = saksbehandlersVurdering.getInternArbeidsforholdRef() == null
                ? InternArbeidsforholdRef.nullRef()
                : InternArbeidsforholdRef.ref(saksbehandlersVurdering.getInternArbeidsforholdRef());
        informasjonBuilder.fjernOverstyringVedrørende(arbeidsgiver, ref);
        ArbeidsforholdOverstyringBuilder builder = informasjonBuilder.getOverstyringBuilderFor(arbeidsgiver, InternArbeidsforholdRef.nullRef());
        builder.medArbeidsgiver(arbeidsgiver)
            .medArbeidsforholdRef(ref)
            .medHandling(mapTilHandling(saksbehandlersVurdering))
            .medAngittStillingsprosent(mapStillingsprosent(saksbehandlersVurdering))
            .medBeskrivelse(saksbehandlersVurdering.getBegrunnelse());
        if (saksbehandlersVurdering.getArbeidsgiverNavn() != null
            && saksbehandlersVurdering.getVurdering().equals(ArbeidsforholdKomplettVurderingType.MANUELT_OPPRETTET_AV_SAKSBEHANDLER)) {
            builder.medAngittArbeidsgiverNavn(saksbehandlersVurdering.getArbeidsgiverNavn());
        }
        var tom = saksbehandlersVurdering.getTom() == null ? DatoIntervallEntitet.TIDENES_ENDE : saksbehandlersVurdering.getTom();
        builder.leggTilOverstyrtPeriode(saksbehandlersVurdering.getFom(), tom);
        informasjonBuilder.leggTil(builder);
    }

    private static ArbeidsforholdHandlingType mapTilHandling(ManueltArbeidsforholdDto saksbehandlersVurdering) {
        return switch(saksbehandlersVurdering.getVurdering()) {
            case OPPRETT_BASERT_PÅ_INNTEKTSMELDING -> ArbeidsforholdHandlingType.BASERT_PÅ_INNTEKTSMELDING;
            case MANUELT_OPPRETTET_AV_SAKSBEHANDLER ->  ArbeidsforholdHandlingType.LAGT_TIL_AV_SAKSBEHANDLER;
            default -> throw new IllegalStateException("Ukjent ArbeidsforholdKomplettVurderingType " + saksbehandlersVurdering.getVurdering());
        };
    }

    private static Stillingsprosent mapStillingsprosent(ManueltArbeidsforholdDto saksbehandlersVurdering) {
        return new Stillingsprosent(saksbehandlersVurdering.getStillingsprosent());
    }
}
