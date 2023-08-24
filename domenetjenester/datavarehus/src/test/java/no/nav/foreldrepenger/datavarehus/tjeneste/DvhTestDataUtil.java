package no.nav.foreldrepenger.datavarehus.tjeneste;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.IverksettingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

import java.time.LocalDateTime;

class DvhTestDataUtil {

    static final AksjonspunktDefinisjon AKSJONSPUNKT_DEF = AksjonspunktDefinisjon.MANUELL_VURDERING_AV_OMSORGSVILKÅRET;
    static final BehandlingStegType BEHANDLING_STEG_TYPE = BehandlingStegType.FATTE_VEDTAK;
    static final IverksettingStatus IVERKSETTING_STATUS = IverksettingStatus.IKKE_IVERKSATT;
    static final BehandlingStegStatus BEHANDLING_STEG_STATUS = BehandlingStegStatus.STARTET;
    static final String ANSVARLIG_BESLUTTER = "ansvarligBeslutter";
    static final String ANSVARLIG_SAKSBEHANDLER = "ansvarligSaksbehandler";
    static final String BEHANDLENDE_ENHET = "BE";
    static final String OPPRETTET_AV = "opprettetAv";
    static final VedtakResultatType VEDTAK_RESULTAT_TYPE = VedtakResultatType.INNVILGET;
    static final AktørId BRUKER_AKTØR_ID = AktørId.dummy();
    static final long BEHANDLING_STEG_ID = 42L;
    static final Saksnummer SAKSNUMMER  = new Saksnummer("12345");
    static final AktørId ANNEN_PART_AKTØR_ID = AktørId.dummy();
    static LocalDateTime OPPRETTET_TID = LocalDateTime.parse("2017-10-11T08:00");
    static LocalDateTime VEDTAK_DATO = LocalDateTime.parse("2017-10-11T08:00");
}
