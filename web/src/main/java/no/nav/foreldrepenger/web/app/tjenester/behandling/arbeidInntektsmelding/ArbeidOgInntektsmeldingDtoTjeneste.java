package no.nav.foreldrepenger.web.app.tjenester.behandling.arbeidInntektsmelding;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.dokumentarkiv.ArkivJournalPost;
import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.dto.arbeidInntektsmelding.ArbeidOgInntektsmeldingDto;
import no.nav.foreldrepenger.domene.arbeidsforhold.dto.arbeidInntektsmelding.ArbeidsforholdDto;
import no.nav.foreldrepenger.domene.arbeidsforhold.dto.arbeidInntektsmelding.InntektDto;
import no.nav.foreldrepenger.domene.arbeidsforhold.dto.arbeidInntektsmelding.InntektsmeldingDto;
import no.nav.foreldrepenger.domene.iay.modell.AktørArbeid;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektFilter;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.inntektsmelding.InntektsmeldingKontaktinformasjon;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.inntektsmelding.KontaktinformasjonIM;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.xml.MottattDokumentXmlParser;

@ApplicationScoped
public class ArbeidOgInntektsmeldingDtoTjeneste {

    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private DokumentArkivTjeneste dokumentArkivTjeneste;

    ArbeidOgInntektsmeldingDtoTjeneste() {
        // CDI
    }

    @Inject
    public ArbeidOgInntektsmeldingDtoTjeneste(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                              MottatteDokumentRepository mottatteDokumentRepository,
                                              InntektsmeldingTjeneste inntektsmeldingTjeneste,
                                              DokumentArkivTjeneste dokumentArkivTjeneste) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.dokumentArkivTjeneste = dokumentArkivTjeneste;
    }

    public ArbeidOgInntektsmeldingDto lagDto(BehandlingReferanse referanse) {
        var iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(referanse.getBehandlingUuid());
        var inntektsmeldinger = mapInntektsmeldinger(iayGrunnlag, referanse);
        var arbeidsforhold = mapArbeidsforhold(iayGrunnlag, referanse);
        var inntekter = mapInntekter(iayGrunnlag, referanse);
        return new ArbeidOgInntektsmeldingDto(inntektsmeldinger, arbeidsforhold, inntekter, referanse.getUtledetSkjæringstidspunkt());
    }

    private List<InntektDto> mapInntekter(InntektArbeidYtelseGrunnlag iayGrunnlag, BehandlingReferanse referanse) {
        var filter = new InntektFilter(iayGrunnlag.getAktørInntektFraRegister(referanse.getAktørId()));
        return ArbeidOgInntektsmeldingMapper.mapInntekter(filter, referanse.getUtledetSkjæringstidspunkt());
    }

    private List<ArbeidsforholdDto> mapArbeidsforhold(InntektArbeidYtelseGrunnlag iayGrunnlag, BehandlingReferanse referanse) {
        var filter = new YrkesaktivitetFilter(iayGrunnlag.getAktørArbeidFraRegister(referanse.getAktørId())
            .map(AktørArbeid::hentAlleYrkesaktiviteter)
            .orElse(Collections.emptyList()));
        var referanser = iayGrunnlag.getArbeidsforholdInformasjon()
            .map(ArbeidsforholdInformasjon::getArbeidsforholdReferanser)
            .orElse(Collections.emptyList());
        return ArbeidOgInntektsmeldingMapper.mapArbeidsforholdUtenOverstyringer(filter, referanser, referanse.getUtledetSkjæringstidspunkt());

    }

    private List<InntektsmeldingDto> mapInntektsmeldinger(InntektArbeidYtelseGrunnlag iayGrunnlag, BehandlingReferanse referanse) {
        var inntektsmeldinger = inntektsmeldingTjeneste.hentInntektsmeldinger(referanse, referanse.getUtledetSkjæringstidspunkt(), iayGrunnlag, true);
        var referanser = iayGrunnlag.getArbeidsforholdInformasjon()
            .map(ArbeidsforholdInformasjon::getArbeidsforholdReferanser)
            .orElse(Collections.emptyList());
        var motatteDokumenter = mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(referanse.getFagsakId());
        var alleInntektsmeldingerFraArkiv = dokumentArkivTjeneste.hentAlleDokumenterForVisning(referanse.getSaksnummer()).stream()
            .filter(this::gjelderInntektsmelding)
            .collect(Collectors.toList());
        return inntektsmeldinger.stream().map(im -> {
                var dokumentId = finnDokumentId(im.getJournalpostId(), alleInntektsmeldingerFraArkiv);
                var kontaktinfo = finnMotattXML(motatteDokumenter, im).flatMap(this::trekkUtKontaktInfo);
                return ArbeidOgInntektsmeldingMapper.mapInntektsmelding(im, referanser, kontaktinfo, dokumentId);
            })
            .collect(Collectors.toList());
    }

    private boolean gjelderInntektsmelding(ArkivJournalPost dok) {
        return dok.getHovedDokument() != null && dok.getHovedDokument().getDokumentType() != null &&
            dok.getHovedDokument().getDokumentType().erInntektsmelding();
    }

    private Optional<String> finnDokumentId(JournalpostId journalpostId, List<ArkivJournalPost> alleInntektsmeldingerFraArkiv) {
        return alleInntektsmeldingerFraArkiv.stream()
            .filter(im -> Objects.equals(im.getJournalpostId(), journalpostId))
            .findFirst()
            .map(d -> d.getHovedDokument().getDokumentId());
    }

    private Optional<KontaktinformasjonIM> trekkUtKontaktInfo(MottattDokument mottattIM) {
        var imWrapper = MottattDokumentXmlParser.unmarshallXml(mottattIM.getPayloadXml());
        if (imWrapper instanceof InntektsmeldingKontaktinformasjon i) {
            return Optional.of(i.finnKontaktinformasjon());
        }
        return Optional.empty();
    }

    private Optional<MottattDokument> finnMotattXML(List<MottattDokument> dokumenter, Inntektsmelding im) {
        return dokumenter.stream().filter(d -> Objects.equals(d.getJournalpostId(), im.getJournalpostId()))
            .findFirst();
    }
}
