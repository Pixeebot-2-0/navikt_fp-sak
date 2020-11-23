package no.nav.foreldrepenger.domene.person.pdl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.aktør.AdresseType;
import no.nav.foreldrepenger.behandlingslager.aktør.Adresseinfo;
import no.nav.foreldrepenger.behandlingslager.aktør.FamilierelasjonVL;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.AdressePeriode;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.Gyldighetsperiode;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.Personhistorikkinfo;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.PersonstatusPeriode;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.StatsborgerskapPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.MapRegionLandkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.Poststed;
import no.nav.foreldrepenger.behandlingslager.geografisk.PoststedKodeverkRepository;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.pdl.Bostedsadresse;
import no.nav.pdl.BostedsadresseResponseProjection;
import no.nav.pdl.Doedsfall;
import no.nav.pdl.DoedsfallResponseProjection;
import no.nav.pdl.Familierelasjon;
import no.nav.pdl.FamilierelasjonResponseProjection;
import no.nav.pdl.Familierelasjonsrolle;
import no.nav.pdl.Foedsel;
import no.nav.pdl.FoedselResponseProjection;
import no.nav.pdl.FolkeregistermetadataResponseProjection;
import no.nav.pdl.Folkeregisterpersonstatus;
import no.nav.pdl.FolkeregisterpersonstatusResponseProjection;
import no.nav.pdl.HentPersonQueryRequest;
import no.nav.pdl.Kjoenn;
import no.nav.pdl.KjoennResponseProjection;
import no.nav.pdl.KjoennType;
import no.nav.pdl.Kontaktadresse;
import no.nav.pdl.KontaktadresseResponseProjection;
import no.nav.pdl.Matrikkeladresse;
import no.nav.pdl.MatrikkeladresseResponseProjection;
import no.nav.pdl.MetadataResponseProjection;
import no.nav.pdl.Navn;
import no.nav.pdl.NavnResponseProjection;
import no.nav.pdl.Opphold;
import no.nav.pdl.OppholdResponseProjection;
import no.nav.pdl.Oppholdsadresse;
import no.nav.pdl.OppholdsadresseResponseProjection;
import no.nav.pdl.Oppholdstillatelse;
import no.nav.pdl.Person;
import no.nav.pdl.PersonBostedsadresseParametrizedInput;
import no.nav.pdl.PersonFolkeregisterpersonstatusParametrizedInput;
import no.nav.pdl.PersonKontaktadresseParametrizedInput;
import no.nav.pdl.PersonOppholdParametrizedInput;
import no.nav.pdl.PersonOppholdsadresseParametrizedInput;
import no.nav.pdl.PersonResponseProjection;
import no.nav.pdl.PersonStatsborgerskapParametrizedInput;
import no.nav.pdl.PostadresseIFrittFormat;
import no.nav.pdl.PostadresseIFrittFormatResponseProjection;
import no.nav.pdl.Postboksadresse;
import no.nav.pdl.PostboksadresseResponseProjection;
import no.nav.pdl.Sivilstand;
import no.nav.pdl.SivilstandResponseProjection;
import no.nav.pdl.Sivilstandstype;
import no.nav.pdl.Statsborgerskap;
import no.nav.pdl.StatsborgerskapResponseProjection;
import no.nav.pdl.UkjentBosted;
import no.nav.pdl.UkjentBostedResponseProjection;
import no.nav.pdl.UtenlandskAdresse;
import no.nav.pdl.UtenlandskAdresseIFrittFormat;
import no.nav.pdl.UtenlandskAdresseIFrittFormatResponseProjection;
import no.nav.pdl.UtenlandskAdresseResponseProjection;
import no.nav.pdl.Vegadresse;
import no.nav.pdl.VegadresseResponseProjection;
import no.nav.vedtak.felles.integrasjon.pdl.PdlKlient;
import no.nav.vedtak.felles.integrasjon.pdl.Tema;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
public class PersoninfoTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(PersoninfoTjeneste.class);

    private static final String HARDKODET_POSTNR = "XXXX";
    private static final String HARDKODET_POSTSTED = "UKJENT";

    private static final Map<Sivilstandstype, SivilstandType> SIVSTAND_FRA_FREG = Map.ofEntries(
        Map.entry(Sivilstandstype.UOPPGITT, SivilstandType.UOPPGITT),
        Map.entry(Sivilstandstype.UGIFT, SivilstandType.UGIFT),
        Map.entry(Sivilstandstype.GIFT, SivilstandType.GIFT),
        Map.entry(Sivilstandstype.ENKE_ELLER_ENKEMANN, SivilstandType.ENKEMANN),
        Map.entry(Sivilstandstype.SKILT, SivilstandType.SKILT),
        Map.entry(Sivilstandstype.SEPARERT, SivilstandType.SEPARERT),
        Map.entry(Sivilstandstype.REGISTRERT_PARTNER, SivilstandType.REGISTRERT_PARTNER),
        Map.entry(Sivilstandstype.SEPARERT_PARTNER, SivilstandType.SEPARERT_PARTNER),
        Map.entry(Sivilstandstype.SKILT_PARTNER, SivilstandType.SKILT_PARTNER),
        Map.entry(Sivilstandstype.GJENLEVENDE_PARTNER, SivilstandType.GJENLEVENDE_PARTNER)
    );

    private static final Map<Familierelasjonsrolle, RelasjonsRolleType> ROLLE_FRA_FREG_ROLLE = Map.ofEntries(
        Map.entry(Familierelasjonsrolle.BARN, RelasjonsRolleType.BARN),
        Map.entry(Familierelasjonsrolle.MOR, RelasjonsRolleType.MORA),
        Map.entry(Familierelasjonsrolle.FAR, RelasjonsRolleType.FARA),
        Map.entry(Familierelasjonsrolle.MEDMOR, RelasjonsRolleType.MEDMOR)
    );

    private static final Map<Sivilstandstype, RelasjonsRolleType> ROLLE_FRA_FREG_STAND = Map.ofEntries(
        Map.entry(Sivilstandstype.GIFT, RelasjonsRolleType.EKTE),
        Map.entry(Sivilstandstype.REGISTRERT_PARTNER, RelasjonsRolleType.REGISTRERT_PARTNER)
    );


    private PdlKlient pdlKlient;
    private PoststedKodeverkRepository poststedKodeverkRepository;

    PersoninfoTjeneste() {
        // CDI
    }

    @Inject
    public PersoninfoTjeneste(PdlKlient pdlKlient, PoststedKodeverkRepository repository) {
        this.pdlKlient = pdlKlient;
        this.poststedKodeverkRepository = repository;
    }

    public void hentPersoninfo(AktørId aktørId, PersonIdent personIdent, Personinfo fraTPS) {
        try {
            var query = new HentPersonQueryRequest();
            query.setIdent(aktørId.getId());
            var projection = new PersonResponseProjection()
                    .navn(new NavnResponseProjection().forkortetNavn().fornavn().mellomnavn().etternavn())
                    .foedsel(new FoedselResponseProjection().foedselsdato())
                    .doedsfall(new DoedsfallResponseProjection().doedsdato())
                    .folkeregisterpersonstatus(new FolkeregisterpersonstatusResponseProjection().forenkletStatus().status())
                    .kjoenn(new KjoennResponseProjection().kjoenn())
                    .sivilstand(new SivilstandResponseProjection().relatertVedSivilstand().type())
                    .statsborgerskap(new StatsborgerskapResponseProjection().land())
                    .familierelasjoner(new FamilierelasjonResponseProjection().relatertPersonsRolle().relatertPersonsIdent().minRolleForPerson())
                    .bostedsadresse(new BostedsadresseResponseProjection()
                        .vegadresse(new VegadresseResponseProjection().matrikkelId().adressenavn().husnummer().husbokstav().tilleggsnavn().postnummer())
                        .matrikkeladresse(new MatrikkeladresseResponseProjection().matrikkelId().bruksenhetsnummer().tilleggsnavn().postnummer())
                        .ukjentBosted(new UkjentBostedResponseProjection().bostedskommune())
                        .utenlandskAdresse(new UtenlandskAdresseResponseProjection().adressenavnNummer().bygningEtasjeLeilighet().postboksNummerNavn().bySted().regionDistriktOmraade().postkode().landkode()))
                    .oppholdsadresse(new OppholdsadresseResponseProjection()
                        .vegadresse(new VegadresseResponseProjection().matrikkelId().adressenavn().husnummer().husbokstav().tilleggsnavn().postnummer())
                        .matrikkeladresse(new MatrikkeladresseResponseProjection().matrikkelId().bruksenhetsnummer().tilleggsnavn().postnummer())
                        .utenlandskAdresse(new UtenlandskAdresseResponseProjection().adressenavnNummer().bygningEtasjeLeilighet().postboksNummerNavn().bySted().regionDistriktOmraade().postkode().landkode()))
                    .kontaktadresse(new KontaktadresseResponseProjection().type()
                        .vegadresse(new VegadresseResponseProjection().matrikkelId().adressenavn().husnummer().husbokstav().tilleggsnavn().postnummer())
                        .postboksadresse(new PostboksadresseResponseProjection().postboks().postbokseier().postnummer())
                        .postadresseIFrittFormat(new PostadresseIFrittFormatResponseProjection().adresselinje1().adresselinje2().adresselinje3().postnummer())
                        .utenlandskAdresse(new UtenlandskAdresseResponseProjection().adressenavnNummer().bygningEtasjeLeilighet().postboksNummerNavn().bySted().regionDistriktOmraade().postkode().landkode())
                        .utenlandskAdresseIFrittFormat(new UtenlandskAdresseIFrittFormatResponseProjection().adresselinje1().adresselinje2().adresselinje3().byEllerStedsnavn().postkode().landkode()))
                ;

            var person = pdlKlient.hentPerson(query, projection, Tema.FOR);

            var fødselsdato = person.getFoedsel().stream()
                .map(Foedsel::getFoedselsdato)
                .filter(Objects::nonNull)
                .findFirst().map(d -> LocalDate.parse(d, DateTimeFormatter.ISO_LOCAL_DATE)).orElse(null);
            var dødssdato = person.getDoedsfall().stream()
                .map(Doedsfall::getDoedsdato)
                .filter(Objects::nonNull)
                .findFirst().map(d -> LocalDate.parse(d, DateTimeFormatter.ISO_LOCAL_DATE)).orElse(null);
            var pdlStatus = person.getFolkeregisterpersonstatus().stream()
                .map(Folkeregisterpersonstatus::getStatus)
                .findFirst().map(PersonstatusType::fraFregPersonstatus).orElse(PersonstatusType.UDEFINERT);
            var sivilstand = person.getSivilstand().stream()
                .map(Sivilstand::getType)
                .findFirst()
                .map(st -> SIVSTAND_FRA_FREG.getOrDefault(st, SivilstandType.UOPPGITT)).orElse(SivilstandType.UOPPGITT);
            var statsborgerskap = mapStatsborgerskap(person.getStatsborgerskap());
            var familierelasjoner = mapFamilierelasjoner(person.getFamilierelasjoner(), person.getSivilstand());
            var adresser = mapAdresser(person.getBostedsadresse(), person.getKontaktadresse(), person.getOppholdsadresse());
            var fraPDL = new Personinfo.Builder().medAktørId(aktørId).medPersonIdent(personIdent)
                .medNavn(person.getNavn().stream().map(PersoninfoTjeneste::mapNavn).filter(Objects::nonNull).findFirst().orElse("MANGLER NAVN"))
                .medFødselsdato(fødselsdato)
                .medDødsdato(dødssdato)
                .medNavBrukerKjønn(mapKjønn(person))
                .medPersonstatusType(pdlStatus)
                .medSivilstandType(sivilstand)
                .medLandkode(statsborgerskap)
                .medRegion(MapRegionLandkoder.mapLandkode(statsborgerskap.getKode()))
                .medFamilierelasjon(familierelasjoner)
                .medAdresseInfoList(adresser)
                .build();

            if (!erLike(fraPDL, fraTPS)) {
                var avvik = finnAvvik(fraTPS, fraPDL);
                LOG.info("FPSAK PDL FULL: avvik {}", avvik);
            }
        } catch (Exception e) {
            LOG.info("FPSAK PDL FULL: error", e);
        }
    }

    public void hentPersoninfoHistorikk(AktørId aktørId, Personhistorikkinfo fraTPS) {
        try {
            var query = new HentPersonQueryRequest();
            query.setIdent(aktørId.getId());
            var projection = new PersonResponseProjection()
                .folkeregisterpersonstatus(new PersonFolkeregisterpersonstatusParametrizedInput().historikk(true), new FolkeregisterpersonstatusResponseProjection().forenkletStatus().status().folkeregistermetadata(new FolkeregistermetadataResponseProjection().gyldighetstidspunkt().opphoerstidspunkt()))
                .opphold(new PersonOppholdParametrizedInput().historikk(true), new OppholdResponseProjection().type().oppholdFra().oppholdTil().metadata(new MetadataResponseProjection().historisk()).folkeregistermetadata(new FolkeregistermetadataResponseProjection().ajourholdstidspunkt()))
                .statsborgerskap(new PersonStatsborgerskapParametrizedInput().historikk(true), new StatsborgerskapResponseProjection().land().gyldigFraOgMed().gyldigTilOgMed())
                .bostedsadresse(new PersonBostedsadresseParametrizedInput().historikk(true), new BostedsadresseResponseProjection().angittFlyttedato().gyldigFraOgMed().gyldigTilOgMed()
                    .vegadresse(new VegadresseResponseProjection().matrikkelId().adressenavn().husnummer().husbokstav().tilleggsnavn().postnummer())
                    .matrikkeladresse(new MatrikkeladresseResponseProjection().matrikkelId().bruksenhetsnummer().tilleggsnavn().postnummer())
                    .ukjentBosted(new UkjentBostedResponseProjection().bostedskommune())
                    .utenlandskAdresse(new UtenlandskAdresseResponseProjection().adressenavnNummer().bygningEtasjeLeilighet().postboksNummerNavn().bySted().regionDistriktOmraade().postkode().landkode()))
                .oppholdsadresse(new PersonOppholdsadresseParametrizedInput().historikk(true), new OppholdsadresseResponseProjection().gyldigFraOgMed().gyldigTilOgMed()
                    .vegadresse(new VegadresseResponseProjection().matrikkelId().adressenavn().husnummer().husbokstav().tilleggsnavn().postnummer())
                    .matrikkeladresse(new MatrikkeladresseResponseProjection().matrikkelId().bruksenhetsnummer().tilleggsnavn().postnummer())
                    .utenlandskAdresse(new UtenlandskAdresseResponseProjection().adressenavnNummer().bygningEtasjeLeilighet().postboksNummerNavn().bySted().regionDistriktOmraade().postkode().landkode()))
                .kontaktadresse(new PersonKontaktadresseParametrizedInput().historikk(true), new KontaktadresseResponseProjection().type().gyldigFraOgMed().gyldigTilOgMed()
                    .vegadresse(new VegadresseResponseProjection().matrikkelId().adressenavn().husnummer().husbokstav().tilleggsnavn().postnummer())
                    .postboksadresse(new PostboksadresseResponseProjection().postboks().postbokseier().postnummer())
                    .postadresseIFrittFormat(new PostadresseIFrittFormatResponseProjection().adresselinje1().adresselinje2().adresselinje3().postnummer())
                    .utenlandskAdresse(new UtenlandskAdresseResponseProjection().adressenavnNummer().bygningEtasjeLeilighet().postboksNummerNavn().bySted().regionDistriktOmraade().postkode().landkode())
                    .utenlandskAdresseIFrittFormat(new UtenlandskAdresseIFrittFormatResponseProjection().adresselinje1().adresselinje2().adresselinje3().byEllerStedsnavn().postkode().landkode()))
                ;

            var person = pdlKlient.hentPerson(query, projection, Tema.FOR);

            var fraPDLBuilder = Personhistorikkinfo.builder().medAktørId(aktørId.getId());
            person.getFolkeregisterpersonstatus().stream()
                .map(PersoninfoTjeneste::mapPersonstatusHistorisk)
                .forEach(fraPDLBuilder::leggTil);
            person.getStatsborgerskap().stream()
                .map(PersoninfoTjeneste::mapStatsborgerskapHistorikk)
                .forEach(fraPDLBuilder::leggTil);
            mapAdresserHistorikk(person.getBostedsadresse(), person.getKontaktadresse(), person.getOppholdsadresse(), fraPDLBuilder);
            var fraPDL = fraPDLBuilder.build();

            logInnUtOpp(person.getOpphold());
            if (!erLikeHistorikk(fraPDL, fraTPS)) {
                var avvik = finnAvvikHistorikk(fraTPS, fraPDL);
                LOG.info("FPSAK PDL HIST: avvik {}", avvik);
            }
        } catch (Exception e) {
            LOG.info("FPSAK PDL HIST: error", e);
        }
    }

    private static String mapNavn(Navn navn) {
        if (navn.getForkortetNavn() != null)
            return navn.getForkortetNavn();
        return navn.getEtternavn() + " " + navn.getFornavn() + (navn.getMellomnavn() == null ? "" : " " + navn.getMellomnavn());
    }

    private static NavBrukerKjønn mapKjønn(Person person) {
        var kode = person.getKjoenn().stream()
            .map(Kjoenn::getKjoenn)
            .filter(Objects::nonNull)
            .findFirst().orElse(KjoennType.UKJENT);
        if (KjoennType.MANN.equals(kode))
            return NavBrukerKjønn.MANN;
        return KjoennType.KVINNE.equals(kode) ? NavBrukerKjønn.KVINNE : NavBrukerKjønn.UDEFINERT;
    }

    private static PersonstatusPeriode mapPersonstatusHistorisk(Folkeregisterpersonstatus status) {
        var gyldigFra = status.getFolkeregistermetadata().getGyldighetstidspunkt() == null ? Tid.TIDENES_BEGYNNELSE :
            LocalDateTime.ofInstant(status.getFolkeregistermetadata().getGyldighetstidspunkt().toInstant(), ZoneId.systemDefault()).toLocalDate();
        var gyldigTil = status.getFolkeregistermetadata().getOpphoerstidspunkt() == null ? Tid.TIDENES_ENDE :
            LocalDateTime.ofInstant(status.getFolkeregistermetadata().getOpphoerstidspunkt().toInstant(), ZoneId.systemDefault()).toLocalDate();
        return new PersonstatusPeriode(Gyldighetsperiode.innenfor(gyldigFra, gyldigTil), PersonstatusType.fraFregPersonstatus(status.getStatus()));
    }

    private static StatsborgerskapPeriode mapStatsborgerskapHistorikk(Statsborgerskap statsborgerskap) {
        var gyldigFra = statsborgerskap.getGyldigFraOgMed() == null ? Tid.TIDENES_BEGYNNELSE :
            LocalDate.parse(statsborgerskap.getGyldigFraOgMed(), DateTimeFormatter.ISO_LOCAL_DATE);
        var gyldigTil = statsborgerskap.getGyldigTilOgMed() == null ? Tid.TIDENES_ENDE :
            LocalDate.parse(statsborgerskap.getGyldigTilOgMed(), DateTimeFormatter.ISO_LOCAL_DATE);
        return new StatsborgerskapPeriode(Gyldighetsperiode.innenfor(gyldigFra, gyldigTil), new no.nav.foreldrepenger.behandlingslager.aktør.Statsborgerskap(statsborgerskap.getLand()));
    }

    private static Landkoder mapStatsborgerskap(List<Statsborgerskap> statsborgerskap) {
        List<Landkoder> alleLand = statsborgerskap.stream()
            .map(Statsborgerskap::getLand)
            .map(Landkoder::fraKodeDefaultUdefinert)
            .collect(Collectors.toList());
        return alleLand.stream().anyMatch(Landkoder.NOR::equals) ? Landkoder.NOR : alleLand.stream().findFirst().orElse(Landkoder.UOPPGITT);
    }

    private static Set<FamilierelasjonVL> mapFamilierelasjoner(List<Familierelasjon> familierelasjoner, List<Sivilstand> sivilstandliste) {
        Set<FamilierelasjonVL> relasjoner = new HashSet<>();
        // TODO: utled samme bosted ut fra adresse

        familierelasjoner.stream()
            .map(r -> new FamilierelasjonVL(new PersonIdent(r.getRelatertPersonsIdent()), mapRelasjonsrolle(r.getRelatertPersonsRolle()), false))
            .forEach(relasjoner::add);
        sivilstandliste.stream()
            .filter(rel -> Sivilstandstype.GIFT.equals(rel.getType()) || Sivilstandstype.REGISTRERT_PARTNER.equals(rel.getType()))
            .filter(rel -> rel.getRelatertVedSivilstand() != null)
            .map(r -> new FamilierelasjonVL(new PersonIdent(r.getRelatertVedSivilstand()), mapRelasjonsrolle(r.getType()), false))
            .forEach(relasjoner::add);
        return relasjoner;
    }

    private static RelasjonsRolleType mapRelasjonsrolle(Familierelasjonsrolle type) {
        return ROLLE_FRA_FREG_ROLLE.getOrDefault(type, RelasjonsRolleType.UDEFINERT);
    }

    private static RelasjonsRolleType mapRelasjonsrolle(Sivilstandstype type) {
        return ROLLE_FRA_FREG_STAND.getOrDefault(type, RelasjonsRolleType.UDEFINERT);
    }

    void mapAdresserHistorikk(List<Bostedsadresse> bostedsadresser, List<Kontaktadresse> kontaktadresser, List<Oppholdsadresse> oppholdsadresser, Personhistorikkinfo.Builder builder) {
        bostedsadresser.forEach(b -> {
            var gyldigFra = b.getGyldigFraOgMed() == null ? Tid.TIDENES_BEGYNNELSE :
                LocalDateTime.ofInstant(b.getGyldigFraOgMed().toInstant(), ZoneId.systemDefault()).toLocalDate();
            var gyldigTil = b.getGyldigTilOgMed() == null ? Tid.TIDENES_ENDE :
                LocalDateTime.ofInstant(b.getGyldigTilOgMed().toInstant(), ZoneId.systemDefault()).toLocalDate();
            mapAdresser(List.of(b), List.of(), List.of()).forEach(a -> builder.leggTil(mapAdresseinfoTilAdressePeriode(gyldigFra, gyldigTil, a)));
        });
        kontaktadresser.forEach(k -> {
            var gyldigFra = k.getGyldigFraOgMed() == null ? Tid.TIDENES_BEGYNNELSE :
                LocalDateTime.ofInstant(k.getGyldigFraOgMed().toInstant(), ZoneId.systemDefault()).toLocalDate();
            var gyldigTil = k.getGyldigTilOgMed() == null ? Tid.TIDENES_ENDE :
                LocalDateTime.ofInstant(k.getGyldigTilOgMed().toInstant(), ZoneId.systemDefault()).toLocalDate();
            mapAdresser(List.of(), List.of(k), List.of()).forEach(a -> builder.leggTil(mapAdresseinfoTilAdressePeriode(gyldigFra, gyldigTil, a)));
        });
        oppholdsadresser.forEach(o -> {
            var gyldigFra = o.getGyldigFraOgMed() == null ? Tid.TIDENES_BEGYNNELSE :
                LocalDateTime.ofInstant(o.getGyldigFraOgMed().toInstant(), ZoneId.systemDefault()).toLocalDate();
            var gyldigTil = o.getGyldigTilOgMed() == null ? Tid.TIDENES_ENDE :
                LocalDateTime.ofInstant(o.getGyldigTilOgMed().toInstant(), ZoneId.systemDefault()).toLocalDate();
            mapAdresser(List.of(), List.of(), List.of(o)).forEach(a -> builder.leggTil(mapAdresseinfoTilAdressePeriode(gyldigFra, gyldigTil, a)));
        });
    }

    private static AdressePeriode mapAdresseinfoTilAdressePeriode(LocalDate fom, LocalDate tom, Adresseinfo adresseinfo) {
        return AdressePeriode.builder().medGyldighetsperiode(Gyldighetsperiode.innenfor(fom, tom))
            .medAdresselinje1(adresseinfo.getAdresselinje1())
            .medAdresselinje2(adresseinfo.getAdresselinje2())
            .medAdresselinje3(adresseinfo.getAdresselinje3())
            .medAdresselinje4(adresseinfo.getAdresselinje4())
            .medAdresseType(adresseinfo.getGjeldendePostadresseType())
            .medPostnummer(adresseinfo.getPostNr())
            .medPoststed(adresseinfo.getPoststed())
            .medLand(adresseinfo.getLand())
            .build();
    }

    private List<Adresseinfo> mapAdresser(List<Bostedsadresse> bostedsadresser, List<Kontaktadresse> kontaktadresser, List<Oppholdsadresse> oppholdsadresser) {
        List<Adresseinfo> resultat = new ArrayList<>();
        bostedsadresser.stream().map(Bostedsadresse::getVegadresse).map(a -> mapVegadresse(AdresseType.BOSTEDSADRESSE, a)).filter(Objects::nonNull).forEach(resultat::add);
        bostedsadresser.stream().map(Bostedsadresse::getMatrikkeladresse).map(a -> mapMatrikkeladresse(AdresseType.BOSTEDSADRESSE, a)).filter(Objects::nonNull).forEach(resultat::add);
        bostedsadresser.stream().map(Bostedsadresse::getUkjentBosted).filter(Objects::nonNull).map(this::mapUkjentadresse).forEach(resultat::add);
        bostedsadresser.stream().map(Bostedsadresse::getUtenlandskAdresse).map(a -> mapUtenlandskadresse(AdresseType.BOSTEDSADRESSE, a)).filter(Objects::nonNull).forEach(resultat::add);

        oppholdsadresser.stream().map(Oppholdsadresse::getVegadresse).map(a -> mapVegadresse(AdresseType.MIDLERTIDIG_POSTADRESSE_NORGE, a)).filter(Objects::nonNull).forEach(resultat::add);
        oppholdsadresser.stream().map(Oppholdsadresse::getMatrikkeladresse).map(a -> mapMatrikkeladresse(AdresseType.MIDLERTIDIG_POSTADRESSE_NORGE, a)).filter(Objects::nonNull).forEach(resultat::add);
        oppholdsadresser.stream().map(Oppholdsadresse::getUtenlandskAdresse).map(a -> mapUtenlandskadresse(AdresseType.MIDLERTIDIG_POSTADRESSE_UTLAND, a)).filter(Objects::nonNull).forEach(resultat::add);

        kontaktadresser.stream().map(Kontaktadresse::getVegadresse).map(a -> mapVegadresse(AdresseType.POSTADRESSE, a)).filter(Objects::nonNull).forEach(resultat::add);
        kontaktadresser.stream().map(Kontaktadresse::getPostboksadresse).map(a -> mapPostboksadresse(AdresseType.POSTADRESSE, a)).filter(Objects::nonNull).forEach(resultat::add);
        kontaktadresser.stream().map(Kontaktadresse::getPostadresseIFrittFormat).map(a -> mapFriAdresseNorsk(AdresseType.POSTADRESSE, a)).filter(Objects::nonNull).forEach(resultat::add);
        kontaktadresser.stream().map(Kontaktadresse::getUtenlandskAdresse).map(a -> mapUtenlandskadresse(AdresseType.POSTADRESSE_UTLAND, a)).filter(Objects::nonNull).forEach(resultat::add);
        kontaktadresser.stream().map(Kontaktadresse::getUtenlandskAdresseIFrittFormat).map(a -> mapFriAdresseUtland(AdresseType.POSTADRESSE_UTLAND, a)).filter(Objects::nonNull).forEach(resultat::add);
        if (resultat.isEmpty()) {
            resultat.add(mapUkjentadresse(null));
        }
        return resultat;
    }

    private Adresseinfo mapVegadresse(AdresseType type, Vegadresse vegadresse) {
        if (vegadresse == null)
            return null;
        String postnummer = Optional.ofNullable(vegadresse.getPostnummer()).orElse(HARDKODET_POSTNR);
        var gateadresse = vegadresse.getAdressenavn().toUpperCase() + hvisfinnes(vegadresse.getHusnummer()) + hvisfinnes(vegadresse.getHusbokstav());
        return Adresseinfo.builder(type)
            // TODO: enable når sammenligning stabil .medMatrikkelId(vegadresse.getMatrikkelId())
            .medAdresselinje1(vegadresse.getTilleggsnavn() != null ? vegadresse.getTilleggsnavn().toUpperCase() : gateadresse)
            .medAdresselinje2(vegadresse.getTilleggsnavn() != null ? gateadresse : null)
            .medPostNr(postnummer)
            .medPoststed(tilPoststed(postnummer))
            .medLand(Landkoder.NOR.getKode())
            .build();
    }

    private Adresseinfo mapMatrikkeladresse(AdresseType type, Matrikkeladresse matrikkeladresse) {
        if (matrikkeladresse == null)
            return null;
        String postnummer = Optional.ofNullable(matrikkeladresse.getPostnummer()).orElse(HARDKODET_POSTNR);
        return Adresseinfo.builder(type)
            // TODO: enable når sammenligning stabil .medMatrikkelId(matrikkeladresse.getMatrikkelId())
            .medAdresselinje1(matrikkeladresse.getTilleggsnavn() != null ? matrikkeladresse.getTilleggsnavn().toUpperCase() : matrikkeladresse.getBruksenhetsnummer())
            .medAdresselinje2(matrikkeladresse.getTilleggsnavn() != null ? matrikkeladresse.getBruksenhetsnummer() : null)
            .medPostNr(postnummer)
            .medPoststed(tilPoststed(postnummer))
            .medLand(Landkoder.NOR.getKode())
            .build();
    }

    private Adresseinfo mapPostboksadresse(AdresseType type, Postboksadresse postboksadresse) {
        if (postboksadresse == null)
            return null;
        String postnummer = Optional.ofNullable(postboksadresse.getPostnummer()).orElse(HARDKODET_POSTNR);
        var postboks = "Postboks" + hvisfinnes(postboksadresse.getPostboks());
        return Adresseinfo.builder(type)
            .medAdresselinje1(postboksadresse.getPostbokseier() != null ? postboksadresse.getPostbokseier().toUpperCase() : postboks)
            .medAdresselinje2(postboksadresse.getPostbokseier() != null ? postboks : null)
            .medPostNr(postnummer)
            .medPoststed(tilPoststed(postnummer))
            .medLand(Landkoder.NOR.getKode())
            .build();
    }

    private Adresseinfo mapFriAdresseNorsk(AdresseType type, PostadresseIFrittFormat postadresse) {
        if (postadresse == null)
            return null;
        String postnummer = Optional.ofNullable(postadresse.getPostnummer()).orElse(HARDKODET_POSTNR);
        return Adresseinfo.builder(type)
            .medAdresselinje1(postadresse.getAdresselinje1() != null ? postadresse.getAdresselinje1().toUpperCase() : null)
            .medAdresselinje2(postadresse.getAdresselinje2() != null ? postadresse.getAdresselinje2().toUpperCase() : null)
            .medAdresselinje3(postadresse.getAdresselinje3() != null ? postadresse.getAdresselinje3().toUpperCase() : null)
            .medPostNr(postnummer)
            .medPoststed(tilPoststed(postnummer))
            .medLand(Landkoder.NOR.getKode())
            .build();
    }

    private Adresseinfo mapUkjentadresse(UkjentBosted ukjentBosted) {
        return Adresseinfo.builder(AdresseType.UKJENT_ADRESSE).build();
    }

    private Adresseinfo mapUtenlandskadresse(AdresseType type, UtenlandskAdresse utenlandskAdresse) {
        if (utenlandskAdresse == null)
            return null;
        var linje1 = hvisfinnes(utenlandskAdresse.getAdressenavnNummer()) + hvisfinnes(utenlandskAdresse.getBygningEtasjeLeilighet()) + hvisfinnes(utenlandskAdresse.getPostboksNummerNavn());
        var linje2 = hvisfinnes(utenlandskAdresse.getPostkode()) + hvisfinnes(utenlandskAdresse.getBySted()) + hvisfinnes(utenlandskAdresse.getRegionDistriktOmraade());
        return Adresseinfo.builder(type)
            .medAdresselinje1(linje1)
            .medAdresselinje2(linje2)
            .medAdresselinje3(utenlandskAdresse.getLandkode())
            .medLand(utenlandskAdresse.getLandkode())
            .build();
    }

    private Adresseinfo mapFriAdresseUtland(AdresseType type, UtenlandskAdresseIFrittFormat utenlandskAdresse) {
        if (utenlandskAdresse == null)
            return null;
        var postlinje = hvisfinnes(utenlandskAdresse.getPostkode()) + hvisfinnes(utenlandskAdresse.getByEllerStedsnavn());
        var sisteline = utenlandskAdresse.getAdresselinje3() != null ? postlinje + utenlandskAdresse.getLandkode()
            : (utenlandskAdresse.getAdresselinje2() != null ? utenlandskAdresse.getLandkode() : null);
        return Adresseinfo.builder(type)
            .medAdresselinje1(utenlandskAdresse.getAdresselinje1())
            .medAdresselinje2(utenlandskAdresse.getAdresselinje2() != null ? utenlandskAdresse.getAdresselinje2().toUpperCase() : postlinje)
            .medAdresselinje3(utenlandskAdresse.getAdresselinje3() != null ? utenlandskAdresse.getAdresselinje3().toUpperCase() : (utenlandskAdresse.getAdresselinje2() != null ? postlinje : utenlandskAdresse.getLandkode()))
            .medAdresselinje4(sisteline)
            .medLand(utenlandskAdresse.getLandkode())
            .build();
    }

    private static String hvisfinnes(Object object) {
        return object == null ? "" : " " + object.toString().trim().toUpperCase();
    }

    private String tilPoststed(String postnummer) {
        if (HARDKODET_POSTNR.equals(postnummer)) {
            return HARDKODET_POSTSTED;
        }
        return poststedKodeverkRepository.finnPoststed(postnummer).map(Poststed::getPoststednavn).orElse(HARDKODET_POSTSTED);
    }

    private boolean erLike(Personinfo pdl, Personinfo tps) {
        if (tps == null && pdl == null) return true;
        if (pdl == null || tps == null || tps.getClass() != pdl.getClass()) return false;
        var likerels = pdl.getFamilierelasjoner().size() == tps.getFamilierelasjoner().size() &&
            pdl.getFamilierelasjoner().containsAll(tps.getFamilierelasjoner());
        var likeadresser = pdl.getAdresseInfoList().size() == tps.getAdresseInfoList().size() &&
            pdl.getAdresseInfoList().containsAll(tps.getAdresseInfoList());
        return // Objects.equals(pdl.getNavn(), tps.getNavn()) && - avvik skyldes tegnsett
            Objects.equals(pdl.getFødselsdato(), tps.getFødselsdato()) &&
            Objects.equals(pdl.getDødsdato(), tps.getDødsdato()) &&
            pdl.getPersonstatus() == tps.getPersonstatus() &&
            pdl.getKjønn() == tps.getKjønn() &&
            likerels &&
            likeadresser &&
            pdl.getRegion() == tps.getRegion() &&
            pdl.getLandkode() == tps.getLandkode() &&
            pdl.getSivilstandType() == tps.getSivilstandType();
    }

    private boolean erLikeHistorikk(Personhistorikkinfo pdl, Personhistorikkinfo tps) {
        if (tps == null && pdl == null) return true;
        if (pdl == null || tps == null || tps.getClass() != pdl.getClass()) return false;
        var likestatus = pdl.getPersonstatushistorikk().size() == tps.getPersonstatushistorikk().size() &&
            pdl.getPersonstatushistorikk().containsAll(tps.getPersonstatushistorikk());
        var likeadresser = pdl.getAdressehistorikk().size() == tps.getAdressehistorikk().size() &&
            pdl.getAdressehistorikk().containsAll(tps.getAdressehistorikk());
        var likestb = pdl.getStatsborgerskaphistorikk().size() == tps.getStatsborgerskaphistorikk().size() &&
            pdl.getStatsborgerskaphistorikk().containsAll(tps.getStatsborgerskaphistorikk());
        return likestb && likestatus && likeadresser;
    }

    private String finnAvvik(Personinfo tps, Personinfo pdl) {
        //String navn = Objects.equals(tps.getNavn(), pdl.getNavn()) ? "" : " navn ";
        String kjonn = Objects.equals(tps.getKjønn(), pdl.getKjønn()) ? "" : " kjønn ";
        String fdato = Objects.equals(tps.getFødselsdato(), pdl.getFødselsdato()) ? "" : " fødsel ";
        String ddato = Objects.equals(tps.getDødsdato(), pdl.getDødsdato()) ? "" : " død ";
        String status = Objects.equals(tps.getPersonstatus(), pdl.getPersonstatus()) ? "" : " status " + tps.getPersonstatus().getKode() + " PDL " + pdl.getPersonstatus().getKode();
        String sivstand = Objects.equals(tps.getSivilstandType(), pdl.getSivilstandType()) ? "" : " sivilst " + tps.getSivilstandType().getKode() + " PDL " + pdl.getSivilstandType().getKode();
        String land = Objects.equals(tps.getLandkode(), pdl.getLandkode()) ? "" : " land " + tps.getLandkode().getKode() + " PDL " + pdl.getLandkode().getKode();
        String region = Objects.equals(tps.getRegion(), pdl.getRegion()) ? "" : " region " + tps.getRegion().getKode() + " PDL " + pdl.getRegion().getKode();
        String frel = pdl.getFamilierelasjoner().size() == tps.getFamilierelasjoner().size() && pdl.getFamilierelasjoner().containsAll(tps.getFamilierelasjoner()) ? ""
            : " famrel " + tps.getFamilierelasjoner().stream().map(FamilierelasjonVL::getRelasjonsrolle).collect(Collectors.toList()) + " PDL " + pdl.getFamilierelasjoner().stream().map(FamilierelasjonVL::getRelasjonsrolle).collect(Collectors.toList());
        String adresse = pdl.getAdresseInfoList().size() == tps.getAdresseInfoList().size() && pdl.getAdresseInfoList().containsAll(tps.getAdresseInfoList())  ? ""
            : " adresse " + tps.getAdresseInfoList().stream().map(Adresseinfo::getGjeldendePostadresseType).collect(Collectors.toList()) + " PDL " + pdl.getAdresseInfoList().stream().map(Adresseinfo::getGjeldendePostadresseType).collect(Collectors.toList());
        String adresse2 = pdl.getAdresseInfoList().size() == tps.getAdresseInfoList().size() && pdl.getAdresseInfoList().containsAll(tps.getAdresseInfoList())  ? ""
            : " adresse2 " + tps.getAdresseInfoList().stream().map(Adresseinfo::getPostNr).collect(Collectors.toList()) + " PDL " + pdl.getAdresseInfoList().stream().map(Adresseinfo::getPostNr).collect(Collectors.toList());
        String adresse3 = pdl.getAdresseInfoList().size() == tps.getAdresseInfoList().size() && pdl.getAdresseInfoList().containsAll(tps.getAdresseInfoList())  ? ""
            : " adresse3 " + tps.getAdresseInfoList().stream().map(Adresseinfo::getLand).collect(Collectors.toList()) + " PDL " + pdl.getAdresseInfoList().stream().map(Adresseinfo::getLand).collect(Collectors.toList());
        return "Avvik" + kjonn + fdato + ddato + status + sivstand + land + region + frel + adresse + adresse2 + adresse3;
    }

    private String finnAvvikHistorikk(Personhistorikkinfo tps, Personhistorikkinfo pdl) {
        String status = pdl.getPersonstatushistorikk().size() == tps.getPersonstatushistorikk().size() && pdl.getPersonstatushistorikk().containsAll(tps.getPersonstatushistorikk())  ? ""
            : " status " + tps.getPersonstatushistorikk() + " PDL " + pdl.getPersonstatushistorikk();
        String stb = pdl.getStatsborgerskaphistorikk().size() == tps.getStatsborgerskaphistorikk().size() && pdl.getStatsborgerskaphistorikk().containsAll(tps.getStatsborgerskaphistorikk())  ? ""
            : " borger " + tps.getStatsborgerskaphistorikk().stream().map(a -> a.getStatsborgerskap().getLandkode()).collect(Collectors.toList()) + " PDL " + pdl.getStatsborgerskaphistorikk().stream().map(a -> a.getStatsborgerskap().getLandkode()).collect(Collectors.toList());
        String adresse = pdl.getAdressehistorikk().size() == tps.getAdressehistorikk().size() && pdl.getAdressehistorikk().containsAll(tps.getAdressehistorikk())  ? ""
            : " adresse " + tps.getAdressehistorikk().stream().map(a -> a.getAdresse().getAdresseType()).collect(Collectors.toList()) + " PDL " + pdl.getAdressehistorikk().stream().map(a -> a.getAdresse().getAdresseType()).collect(Collectors.toList());
        String adresse2 = pdl.getAdressehistorikk().size() == tps.getAdressehistorikk().size() && pdl.getAdressehistorikk().containsAll(tps.getAdressehistorikk())  ? ""
            : " adresse2 " + tps.getAdressehistorikk().stream().map(a -> a.getAdresse().getPostnummer()).collect(Collectors.toList()) + " PDL " + pdl.getAdressehistorikk().stream().map(a -> a.getAdresse().getPostnummer()).collect(Collectors.toList());
        String adresse3 = pdl.getAdressehistorikk().size() == tps.getAdressehistorikk().size() && pdl.getAdressehistorikk().containsAll(tps.getAdressehistorikk())  ? ""
            : " adresse3 " + tps.getAdressehistorikk().stream().map(a -> a.getAdresse().getLand()).collect(Collectors.toList()) + " PDL " + pdl.getAdressehistorikk().stream().map(a -> a.getAdresse().getLand()).collect(Collectors.toList());
        return "Avvik" + status + stb + adresse + adresse2 + adresse3;
    }

    private void logInnUtOpp(List<Opphold> opp) {
        String opps = opp.stream()
            .filter(o -> !Oppholdstillatelse.OPPLYSNING_MANGLER.equals(o.getType()))
            .map(o -> {
                var ajour = o.getFolkeregistermetadata().getAjourholdstidspunkt() == null ? null :
                    LocalDateTime.ofInstant(o.getFolkeregistermetadata().getAjourholdstidspunkt().toInstant(), ZoneId.systemDefault()).toLocalDate().toString();
                return "OppholdType="+o.getType().toString()+" historisk "+o.getMetadata().getHistorisk()+
                    " Fra=" + (o.getOppholdFra() == null ? ajour : o.getOppholdFra()) +" Til="+o.getOppholdTil();
            })
            .collect(Collectors.joining(", "));
        if (!opp.isEmpty()) {
            LOG.info("FPSAK PDL opphold {}", opps);
        }
    }

}
