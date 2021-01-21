package no.nav.foreldrepenger.domene.personopplysning;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.threeten.extra.Interval;

import no.nav.foreldrepenger.behandlingslager.aktør.AdresseType;
import no.nav.foreldrepenger.behandlingslager.aktør.Adresseinfo;
import no.nav.foreldrepenger.behandlingslager.aktør.FamilierelasjonVL;
import no.nav.foreldrepenger.behandlingslager.aktør.FødtBarnInfo;
import no.nav.foreldrepenger.behandlingslager.aktør.OppholdstillatelseType;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.AdressePeriode;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.OppholdstillatelsePeriode;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.Personhistorikkinfo;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.PersonstatusPeriode;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.StatsborgerskapPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.MapRegionLandkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.vedtak.konfig.Tid;
import no.nav.vedtak.util.Tuple;

@ApplicationScoped
public class PersonopplysningInnhenter {

    private PersoninfoAdapter personinfoAdapter;

    PersonopplysningInnhenter() {
        // for CDI proxy
    }

    @Inject
    public PersonopplysningInnhenter(PersoninfoAdapter personinfoAdapter) {
        this.personinfoAdapter = personinfoAdapter;
    }

    public List<FødtBarnInfo> innhentAlleFødteForIntervaller(AktørId aktørId, List<LocalDateInterval> intervaller) {
        return personinfoAdapter.innhentAlleFødteForBehandlingIntervaller(aktørId, intervaller);
    }

    public void innhentPersonopplysninger(PersonInformasjonBuilder informasjonBuilder, AktørId søker, Optional<AktørId> annenPart,
                                          Interval opplysningsperiode, List<LocalDateInterval> fødselsIntervaller) {

        // Fase 1 - Innhent persongalleri - søker, annenpart, relevante barn og ektefelle
        Map<PersonIdent, Personinfo> innhentet = new LinkedHashMap<>();
        Personinfo søkerPersonInfo = innhentAktørId(søker, innhentet)
            .orElseThrow(() -> new IllegalArgumentException("Finner ikke personinformasjon for aktør " + søker.getId()));

        Optional<Personinfo> annenPartInfo = annenPart.flatMap(ap -> innhentAktørId(ap, innhentet));
        Set<PersonIdent> annenPartsBarn = annenPartInfo.map(this::getAnnenPartsBarn).orElse(Set.of());

        Set<PersonIdent> barnSomSkalInnhentes = finnBarnRelatertTil(søkerPersonInfo, fødselsIntervaller);
        Set<PersonIdent> ektefelleSomSkalInnhentes = finnEktefelle(søkerPersonInfo);

        barnSomSkalInnhentes.forEach(barn -> innhentPersonIdent(barn, innhentet));
        ektefelleSomSkalInnhentes.forEach(ekte -> innhentPersonIdent(ekte, innhentet));

        // Historikk for søker
        final Personhistorikkinfo personhistorikkinfo = personinfoAdapter.innhentPersonopplysningerHistorikk(søkerPersonInfo.getAktørId(), opplysningsperiode);
        if (personhistorikkinfo != null) {
            mapAdresser(personhistorikkinfo.getAdressehistorikk(), informasjonBuilder, søkerPersonInfo);
            mapStatsborgerskap(personhistorikkinfo.getStatsborgerskaphistorikk(), informasjonBuilder, søkerPersonInfo);
            mapPersonstatus(personhistorikkinfo.getPersonstatushistorikk(), informasjonBuilder, søkerPersonInfo);
            mapOppholdstillatelse(personhistorikkinfo.getOppholdstillatelsehistorikk(), informasjonBuilder, søkerPersonInfo);
        }

        // Fase 2 - mapping til

        mapInfoTilEntitet(søkerPersonInfo, informasjonBuilder, false);
        // Ektefelle
        leggTilEktefelle(søkerPersonInfo, informasjonBuilder, innhentet);

        // Medsøker (annen part). kan være samme person som Ektefelle
        annenPartInfo.ifPresent(ap -> mapInfoTilEntitet(ap, informasjonBuilder, true));

        barnSomSkalInnhentes.stream()
            .filter(barn -> innhentet.get(barn) != null)
            .forEach(barnIdent -> {
                var barn = innhentet.get(barnIdent);
                mapInfoTilEntitet(barn, informasjonBuilder, true);
                mapRelasjon(søkerPersonInfo, barn, RelasjonsRolleType.BARN, informasjonBuilder);
                mapRelasjon(barn, søkerPersonInfo, utledRelasjonsrolleTilBarn(søkerPersonInfo, barn), informasjonBuilder);
                if (annenPartsBarn.contains(barnIdent)) {
                    annenPartInfo.ifPresent(a -> mapRelasjon(a, barn, RelasjonsRolleType.BARN, informasjonBuilder));
                    annenPartInfo.ifPresent(a -> mapRelasjon(barn, a, utledRelasjonsrolleTilBarn(a, barn), informasjonBuilder));
                }
        });
    }

    private void mapPersonstatus(List<PersonstatusPeriode> personstatushistorikk, PersonInformasjonBuilder informasjonBuilder, Personinfo personinfo) {
        for (PersonstatusPeriode personstatus : personstatushistorikk) {
            final PersonstatusType status = personstatus.getPersonstatus();
            final DatoIntervallEntitet periode = fødselsJustertPeriode(personstatus.getGyldighetsperiode().getFom(), personinfo.getFødselsdato(), personstatus.getGyldighetsperiode().getTom());

            informasjonBuilder
                .leggTil(informasjonBuilder.getPersonstatusBuilder(personinfo.getAktørId(), periode).medPersonstatus(status));
        }
    }

    private void mapOppholdstillatelse(List<OppholdstillatelsePeriode> oppholdshistorikk, PersonInformasjonBuilder informasjonBuilder, Personinfo personinfo) {
        for (OppholdstillatelsePeriode tillatelse : oppholdshistorikk) {
            final OppholdstillatelseType type = tillatelse.getTillatelse();
            final DatoIntervallEntitet periode = fødselsJustertPeriode(tillatelse.getGyldighetsperiode().getFom(), personinfo.getFødselsdato(), tillatelse.getGyldighetsperiode().getTom());

            informasjonBuilder
                .leggTil(informasjonBuilder.getOppholdstillatelseBuilder(personinfo.getAktørId(), periode).medOppholdstillatelse(type));
        }
    }

    private void mapStatsborgerskap(List<StatsborgerskapPeriode> statsborgerskaphistorikk, PersonInformasjonBuilder informasjonBuilder, Personinfo personinfo) {
        for (StatsborgerskapPeriode statsborgerskap : statsborgerskaphistorikk) {
            final Landkoder landkode = Landkoder.fraKode(statsborgerskap.getStatsborgerskap().getLandkode());

            Region region = MapRegionLandkoder.mapLandkode(statsborgerskap.getStatsborgerskap().getLandkode());

            final DatoIntervallEntitet periode = fødselsJustertPeriode(statsborgerskap.getGyldighetsperiode().getFom(), personinfo.getFødselsdato(), statsborgerskap.getGyldighetsperiode().getTom());

            informasjonBuilder
                .leggTil(informasjonBuilder.getStatsborgerskapBuilder(personinfo.getAktørId(), periode, landkode, region));
        }
    }

    private void mapAdresser(List<AdressePeriode> adressehistorikk, PersonInformasjonBuilder informasjonBuilder, Personinfo personinfo) {
        AktørId aktørId = personinfo.getAktørId();
        for (AdressePeriode adresse : adressehistorikk) {
            final DatoIntervallEntitet periode = fødselsJustertPeriode(adresse.getGyldighetsperiode().getFom(), personinfo.getFødselsdato(), adresse.getGyldighetsperiode().getTom());
            var adresseBuilder = informasjonBuilder.getAdresseBuilder(aktørId, periode, adresse.getAdresse().getAdresseType())
                .medMatrikkelId(adresse.getAdresse().getMatrikkelId())
                .medAdresselinje1(adresse.getAdresse().getAdresselinje1())
                .medAdresselinje2(adresse.getAdresse().getAdresselinje2())
                .medAdresselinje3(adresse.getAdresse().getAdresselinje3())
                .medAdresselinje4(adresse.getAdresse().getAdresselinje4())
                .medLand(adresse.getAdresse().getLand())
                .medPostnummer(adresse.getAdresse().getPostnummer())
                .medPoststed(adresse.getAdresse().getPoststed());
            informasjonBuilder.leggTil(adresseBuilder);
        }
    }

    private DatoIntervallEntitet fødselsJustertPeriode(LocalDate fom, LocalDate fødselsdato, LocalDate tom) {
        var brukFom = fom.isBefore(fødselsdato) ? fødselsdato : fom;
        var safeFom = tom != null && brukFom.isAfter(tom) ? tom : brukFom;
        return tom != null ? DatoIntervallEntitet.fraOgMedTilOgMed(safeFom, tom) : DatoIntervallEntitet.fraOgMed(safeFom);
    }

    private RelasjonsRolleType utledRelasjonsrolleTilBarn(Personinfo personinfo, Personinfo barn) {
        if (barn == null) {
            return RelasjonsRolleType.UDEFINERT;
        }
        return barn.getFamilierelasjoner().stream()
            .filter(fr -> fr.getPersonIdent().equals(personinfo.getPersonIdent()))
            .map(FamilierelasjonVL::getRelasjonsrolle)
            .filter(RelasjonsRolleType::erRegistrertForeldre)
            .findFirst().orElse(RelasjonsRolleType.UDEFINERT);
    }

    private void mapRelasjon(Personinfo fra, Personinfo til, RelasjonsRolleType rolle, PersonInformasjonBuilder informasjonBuilder) {
        if (til == null || rolle == null || RelasjonsRolleType.UDEFINERT.equals(rolle)) {
            return;
        }
        informasjonBuilder
            .leggTil(informasjonBuilder.getRelasjonBuilder(fra.getAktørId(), til.getAktørId(), rolle).harSammeBosted(utledSammeBosted(fra, til)));
    }

    private boolean utledSammeBosted(Personinfo fra, Personinfo til) {
        var tilAdresser = til.getAdresseInfoList().stream()
            .filter(ad -> AdresseType.BOSTEDSADRESSE.equals(ad.getGjeldendePostadresseType()))
            .collect(Collectors.toList());
        return fra.getAdresseInfoList().stream()
            .filter(a -> AdresseType.BOSTEDSADRESSE.equals(a.getGjeldendePostadresseType()))
            .anyMatch(adr1 -> tilAdresser.stream().anyMatch(adr2 -> Adresseinfo.likeAdresser(adr1, adr2)));
    }

    private void mapInfoTilEntitet(Personinfo personinfo, PersonInformasjonBuilder informasjonBuilder, boolean lagreIHistoriskeTabeller) {
        final DatoIntervallEntitet periode = getPeriode(personinfo.getFødselsdato(), Tid.TIDENES_ENDE);
        var builder = informasjonBuilder.getPersonopplysningBuilder(personinfo.getAktørId())
            .medKjønn(personinfo.getKjønn())
            .medFødselsdato(personinfo.getFødselsdato())
            .medNavn(personinfo.getNavn())
            .medDødsdato(personinfo.getDødsdato())
            .medSivilstand(personinfo.getSivilstandType())
            .medRegion(personinfo.getRegion());
        informasjonBuilder.leggTil(builder);

        if (lagreIHistoriskeTabeller || informasjonBuilder.harIkkeFåttStatsborgerskapHistorikk(personinfo.getAktørId())) {
            informasjonBuilder
                .leggTil(informasjonBuilder.getStatsborgerskapBuilder(personinfo.getAktørId(), periode, personinfo.getLandkode(), personinfo.getRegion()));
        }

        if (lagreIHistoriskeTabeller || informasjonBuilder.harIkkeFåttAdresseHistorikk(personinfo.getAktørId())) {
            for (Adresseinfo adresse : personinfo.getAdresseInfoList()) {
                var adresseBuilder = informasjonBuilder.getAdresseBuilder(personinfo.getAktørId(), periode, adresse.getGjeldendePostadresseType())
                    .medMatrikkelId(adresse.getMatrikkelId())
                    .medAdresselinje1(adresse.getAdresselinje1())
                    .medAdresselinje2(adresse.getAdresselinje2())
                    .medAdresselinje3(adresse.getAdresselinje3())
                    .medAdresselinje4(adresse.getAdresselinje4())
                    .medPostnummer(adresse.getPostNr())
                    .medPoststed(adresse.getPoststed())
                    .medLand(adresse.getLand());
                informasjonBuilder.leggTil(adresseBuilder);
            }
        }

        if (lagreIHistoriskeTabeller || informasjonBuilder.harIkkeFåttPersonstatusHistorikk(personinfo.getAktørId())) {
            informasjonBuilder
                .leggTil(informasjonBuilder.getPersonstatusBuilder(personinfo.getAktørId(), periode).medPersonstatus(personinfo.getPersonstatus()));
        }
    }

    private DatoIntervallEntitet getPeriode(LocalDate fom, LocalDate tom) {
        return DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom != null ? tom : Tid.TIDENES_ENDE);
    }

    private void leggTilEktefelle(Personinfo søkerPersonInfo, PersonInformasjonBuilder informasjonBuilder, Map<PersonIdent, Personinfo> innhentet) {
        søkerPersonInfo.getFamilierelasjoner().stream()
            .filter(f -> f.getRelasjonsrolle().equals(RelasjonsRolleType.EKTE) ||
                f.getRelasjonsrolle().equals(RelasjonsRolleType.REGISTRERT_PARTNER))
            .filter(ekte -> innhentet.get(ekte.getPersonIdent()) != null)
            .forEach(relasjon -> {
                var ekte = innhentet.get(relasjon.getPersonIdent());
                mapInfoTilEntitet(ekte, informasjonBuilder, true);
                mapRelasjon(søkerPersonInfo, ekte, relasjon.getRelasjonsrolle(), informasjonBuilder);
                mapRelasjon(ekte, søkerPersonInfo, relasjon.getRelasjonsrolle(), informasjonBuilder);
            });
    }

    private Set<PersonIdent> finnEktefelle(Personinfo personinfo) {
        return personinfo.getFamilierelasjoner().stream()
            .filter(f -> f.getRelasjonsrolle().equals(RelasjonsRolleType.EKTE) ||
                f.getRelasjonsrolle().equals(RelasjonsRolleType.REGISTRERT_PARTNER))
            .map(FamilierelasjonVL::getPersonIdent)
            .collect(Collectors.toSet());
    }

    private Set<PersonIdent> finnBarnRelatertTil(Personinfo personinfo, List<LocalDateInterval> fødselsintervall) {
        return personinfo.getFamilierelasjoner().stream()
            .filter(r -> r.getRelasjonsrolle().equals(RelasjonsRolleType.BARN))
            .map(rel -> personinfoAdapter.hentFødselsdato(rel.getPersonIdent()).map(f -> new Tuple<>(rel.getPersonIdent(), f)).orElse(null))
            .filter(Objects::nonNull)
            .filter(t -> fødselsintervall.stream().anyMatch(i -> i.encloses(t.getElement2())))
            .map(Tuple::getElement1)
            .collect(Collectors.toSet());
    }

    private Set<PersonIdent> getAnnenPartsBarn(Personinfo annenPartInfo) {
        return annenPartInfo.getFamilierelasjoner().stream()
            .filter(f -> RelasjonsRolleType.BARN.equals(f.getRelasjonsrolle()))
            .map(FamilierelasjonVL::getPersonIdent)
            .collect(Collectors.toSet());
    }

    private Optional<Personinfo> innhentAktørId(AktørId aktørId, Map<PersonIdent, Personinfo> innhentet) {
        var personinfo = personinfoAdapter.innhentPersonopplysningerFor(aktørId);
        personinfo.ifPresent(pi -> innhentet.put(pi.getPersonIdent(), pi));
        return personinfo;
    }

    private Optional<Personinfo> innhentPersonIdent(PersonIdent ident, Map<PersonIdent, Personinfo> innhentet) {
        if (innhentet.get(ident) != null)
            return Optional.of(innhentet.get(ident));
        var personinfo = personinfoAdapter.innhentPersonopplysningerFor(ident);
        personinfo.ifPresent(pi -> innhentet.put(pi.getPersonIdent(), pi));
        return personinfo;
    }
}
