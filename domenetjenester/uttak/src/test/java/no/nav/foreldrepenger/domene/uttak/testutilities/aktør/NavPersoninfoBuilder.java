package no.nav.foreldrepenger.domene.uttak.testutilities.aktør;

import static no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn.KVINNE;

import java.time.LocalDate;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;

public class NavPersoninfoBuilder {

    private static final String DEFAULT_NAVN = "Anonym Person";
    private static final AktørId DEFAULT_AKTØR_ID = AktørId.dummy();
    private static final PersonIdent DEFAULT_OFF_TEST_FNR = PersonIdent.fra("10108000398");
    private static final LocalDate DEFAULT_FØDSELDATO = LocalDate.of(1980,10,10);
    private static final Språkkode DEFAULT_FORETRUKKET_SPRÅK = Språkkode.NB;
    private static final String DEFAULT_DISKRESJONSKODE = "6";
    private static final PersonstatusType DEFAULT_PERSONSTATUSTYPE = PersonstatusType.BOSA;
    private NavBrukerKjønn kjønn = KVINNE;

    private AktørId aktørId;
    private PersonIdent personIdent;
    private String navn;
    private LocalDate fødselsdato;
    private Språkkode foretrukketSpråk;

    private String diskresjonskode;
    private PersonstatusType personstatusType;
    public NavPersoninfoBuilder() {
    }


    public NavPersoninfoBuilder medAktørId(AktørId aktørId) {
        this.aktørId = aktørId;
        return this;
    }

    public NavPersoninfoBuilder medKjønn(NavBrukerKjønn kjønn) {
        this.kjønn = kjønn;
        return this;
    }

    public Personinfo build() {
        if (aktørId == null) {
            aktørId = DEFAULT_AKTØR_ID;
        }
        if (personIdent == null) {
            personIdent = DEFAULT_OFF_TEST_FNR;
        }
        if (navn == null) {
            navn = DEFAULT_NAVN;
        }
        if (fødselsdato == null) {
            fødselsdato = DEFAULT_FØDSELDATO;
        }
        if (foretrukketSpråk == null) {
            foretrukketSpråk = DEFAULT_FORETRUKKET_SPRÅK;
        }
        if (diskresjonskode == null) {
            diskresjonskode = DEFAULT_DISKRESJONSKODE;
        }
        if (personstatusType == null) {
            personstatusType = DEFAULT_PERSONSTATUSTYPE;
        }
        return new Personinfo.Builder()
            .medAktørId(aktørId)
            .medPersonIdent(personIdent)
            .medNavn(navn)
            .medFødselsdato(fødselsdato)
            .medNavBrukerKjønn(kjønn)
            .medDiskresjonsKode(diskresjonskode)
            .medPersonstatusType(personstatusType)
            .medForetrukketSpråk(foretrukketSpråk)
            .build();
    }
}
