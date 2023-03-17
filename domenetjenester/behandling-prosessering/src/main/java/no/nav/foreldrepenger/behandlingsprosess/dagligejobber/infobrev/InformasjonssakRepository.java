package no.nav.foreldrepenger.behandlingsprosess.dagligejobber.infobrev;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;

/**
 * Spesialmetoder for å hente opp saker og personer som er kandidat for å sende
 * informasjonsbrev om rettighet
 */

@ApplicationScoped
public class InformasjonssakRepository {

    private static final List<String> INFOBREV_TYPER = List.of(BehandlingÅrsakType.INFOBREV_OPPHOLD.getKode(),
            BehandlingÅrsakType.INFOBREV_BEHANDLING.getKode(), BehandlingÅrsakType.INFOBREV_PÅMINNELSE.getKode());
    private static final List<String> INNVILGET_TYPER = List.of(BehandlingResultatType.INNVILGET.getKode(),
            BehandlingResultatType.FORELDREPENGER_ENDRET.getKode(),
            BehandlingResultatType.INGEN_ENDRING.getKode());
    private static final List<String> SENERE_TYPER = List.of(BehandlingResultatType.INNVILGET.getKode(),
            BehandlingResultatType.FORELDREPENGER_ENDRET.getKode(), BehandlingResultatType.FORELDREPENGER_SENERE.getKode(),
            BehandlingResultatType.INGEN_ENDRING.getKode(), BehandlingResultatType.OPPHØR.getKode());

    // Query-resultat posisjon
    private static final int POS_FAGSAKID = 0;
    private static final int POS_OPPRDATO = 1;
    private static final int POS_AKTORID = 2;
    private static final int POS_FHDATO = 3;
    private static final int POS_ENHETID = 4;
    private static final int POS_ENHETNAVN = 5;

    private EntityManager entityManager;

    InformasjonssakRepository() {
        // for CDI proxy
    }

    @Inject
    public InformasjonssakRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Liste over siste ytelsesbehandling for saker med 4 uker til innvilget
     * maksdato +andre kriterier
     */
    private static final String QUERY_INFORMASJONSBREV_VANLIG = """
        select distinct fs.id, trunc(fs.opprettet_tid), anpa.aktoer_id, minfdato, beh.behandlende_enhet, beh.behandlende_enhet_navn from fagsak fs
          join fagsak_relasjon fr on (fs.id in (fr.fagsak_en_id , fr.fagsak_to_id) and fr.aktiv='J' and fr.fagsak_to_id is null)
          join STOENADSKONTO sk on sk.STOENADSKONTOBEREGNING_ID = nvl(fr.OVERSTYRT_KONTO_BEREGNING_ID, fr.KONTO_BEREGNING_ID)
          join behandling beh on beh.fagsak_id = fs.id
          join behandling_resultat br on (br.behandling_id=beh.id and br.behandling_resultat_type in (:restyper))
          join uttak_resultat ur on (ur.behandling_resultat_id=br.id and ur.aktiv='J')
          join (select uttak_resultat_perioder_id urpid, max(tom) from uttak_resultat_periode urp
                left join uttak_resultat_periode_akt pa on pa.uttak_resultat_periode_id = urp.id
                where periode_resultat_type=:uttakinnvilget OR nvl(trekkdager_desimaler,0) > 0
                group by uttak_resultat_perioder_id having max(tom) <= :tomdato and max(tom) >= :fomdato
                ) on urpid=nvl(overstyrt_perioder_id, opprinnelig_perioder_id)
          join gr_personopplysning grpo on (beh.id=grpo.behandling_id and grpo.aktiv='J')
          join so_annen_part anpa on (grpo.so_annen_part_id=anpa.id and anpa.aktoer_id is not null)
          join gr_familie_hendelse grfh on (beh.id=grfh.behandling_id and grfh.aktiv='J')
          join (select familie_hendelse_id sfhid, min(foedsel_dato) minfdato from fh_uidentifisert_barn ub
                where ub.doedsdato is null group by familie_hendelse_id ) on sfhid=grfh.bekreftet_familie_hendelse_id
        where beh.behandling_status in (:avsluttet)
          and fs.ytelse_type = :foreldrepenger
          and fs.bruker_rolle = :relrolle
          and fs.til_infotrygd='N'
          and sk.stoenadskontotype = :kontotype
          and not exists (select * from behandling bh1 join behandling_resultat br1 on br1.behandling_id=bh1.id
                where bh1.fagsak_id=beh.fagsak_id
                and br1.behandling_resultat_type in (:seneretyper)
                and br1.opprettet_tid>br.opprettet_tid and bh1.behandling_status in (:avsluttet) )
          and not exists ( select * from behandling bh2 join gr_familie_hendelse grfh2 on (bh2.id=grfh2.behandling_id and grfh2.aktiv='J')
                join fh_uidentifisert_barn ub2 on (ub2.familie_hendelse_id in (grfh2.bekreftet_familie_hendelse_id, grfh2.overstyrt_familie_hendelse_id) and ub2.doedsdato is not null )
                where bh2.fagsak_id = beh.fagsak_id
                and grfh2.opprettet_tid > grfh.opprettet_tid )
          and not exists (select * from fagsak fs1 join bruker bru1 on fs1.bruker_id=bru1.id
                join behandling beh1 on beh1.fagsak_id = fs1.id
                join behandling_arsak ba1 on ba1.behandling_id=beh1.id
                where bru1.aktoer_id=anpa.aktoer_id
                and ba1.behandling_arsak_type in (:infobrev)
                and fs1.opprettet_tid > fs.opprettet_tid )
        """;

    public List<InformasjonssakData> finnSakerMedInnvilgetMaksdatoInnenIntervall(LocalDate fom, LocalDate tom) {
        /*
         * Plukker fagsakId, aktørId fra fagsaker som møter disse kriteriene: - Saker
         * (Foreldrepenger, Mors sak, Ikke Stengt) med avsluttet behandling som har max
         * uttaksdato i gitt intervall - Begrenset til ikke aleneomsorg - Begrenset til
         * levende barn - Begrenset til at det er oppgitt annen part
         */
        var avsluttendeStatus = BehandlingStatus.getFerdigbehandletStatuser().stream().map(BehandlingStatus::getKode)
                .toList();
        var query = entityManager.createNativeQuery(QUERY_INFORMASJONSBREV_VANLIG);
        query.setParameter("fomdato", fom);
        query.setParameter("tomdato", tom);
        query.setParameter("uttakinnvilget", PeriodeResultatType.INNVILGET.getKode());
        query.setParameter("kontotype", StønadskontoType.MØDREKVOTE.getKode());
        query.setParameter("foreldrepenger", FagsakYtelseType.FORELDREPENGER.getKode());
        query.setParameter("relrolle", RelasjonsRolleType.MORA.getKode());
        query.setParameter("infobrev", INFOBREV_TYPER);
        query.setParameter("restyper", INNVILGET_TYPER);
        query.setParameter("seneretyper", SENERE_TYPER);
        query.setParameter("avsluttet", avsluttendeStatus);
        @SuppressWarnings("unchecked")
        List<Object[]> resultatList = query.getResultList();
        return toInformasjonssakData(resultatList);
    }

    private List<InformasjonssakData> toInformasjonssakData(List<Object[]> resultatList) {
        List<InformasjonssakData> returnList = new ArrayList<>();
        resultatList.forEach(resultat -> {
            var builder = InformasjonssakData.InformasjonssakDataBuilder
                    .ny(((BigDecimal) resultat[POS_FAGSAKID]).longValue())
                    .medAktørIdAnnenPart((String) resultat[POS_AKTORID])
                    .medOpprettetDato(((Timestamp) resultat[POS_OPPRDATO]).toLocalDateTime().toLocalDate())
                    .medHendelseDato(((Timestamp) resultat[POS_FHDATO]).toLocalDateTime().toLocalDate())
                    .medEnhet((String) resultat[POS_ENHETID])
                    .medEnhetNavn((String) resultat[POS_ENHETNAVN]);
            returnList.add(builder.build());
        });
        return returnList;
    }

    /**
     * Gir saker på medforelder der barnet ble født innenfor angitt tidsrom, medforelder ikke har søkt, barnet lever,
     * og ingen av foreldrene har foreldrepengesak på et senere barn.
     */
    private static final String QUERY_INFORMASJONSBREV_PÅMINNELSE = """
        select distinct f_far.id as fagsak_id, a.aktoer_id, b_far.behandlende_enhet, b_far.behandlende_enhet_navn
          from fagsak f_far
          join bruker a on a.id = f_far.bruker_id
          join fagsak_relasjon fr_far on (f_far.id in (fr_far.fagsak_en_id, fr_far.fagsak_to_id) and fr_far.aktiv='J')
          join stoenadskonto sk on sk.stoenadskontoberegning_id = nvl(fr_far.overstyrt_konto_beregning_id, fr_far.konto_beregning_id)
          join behandling b_far on b_far.fagsak_id = f_far.id
        where f_far.ytelse_type = :foreldrepenger
          and f_far.til_infotrygd = 'N'
          and f_far.bruker_rolle <> 'MORA'
          and fr_far.fagsak_to_id is not null
          and sk.stoenadskontotype = 'FEDREKVOTE'
          -- Far ikke søkt:
          and not exists (select * from gr_soeknad where behandling_id in (select id from behandling where fagsak_id = f_far.id))
          and exists (select f_mor.id, foedsel_dato
                from fagsak f_mor
                join behandling b_mor on b_mor.fagsak_id = f_mor.id
                join behandling_resultat br_mor on (br_mor.behandling_id = b_mor.id and br_mor.behandling_resultat_type in (:restyper))
                join gr_familie_hendelse grfh on (grfh.behandling_id = b_mor.id and grfh.aktiv = 'J')
                join (select familie_hendelse_id, min(foedsel_dato) as foedsel_dato
                      from fh_uidentifisert_barn ub
                      where ub.doedsdato is null
                      and ub.foedsel_dato >= to_date('01.10.2021', 'DD.MM.YYYY') and ub.foedsel_dato >= :fomdato and ub.foedsel_dato <= :tomdato
                      group by familie_hendelse_id) on familie_hendelse_id=grfh.bekreftet_familie_hendelse_id
                where f_mor.bruker_rolle = 'MORA' and f_mor.id in (fr_far.fagsak_en_id, fr_far.fagsak_to_id) and f_mor.id != f_far.id
                -- Mors behandling skal være nyeste vedtak:
                and not exists (select * from behandling b_mor2 join behandling_resultat br_mor2 on br_mor2.behandling_id = b_mor2.id
                      where b_mor2.fagsak_id = f_mor.id
                      and br_mor2.behandling_resultat_type in (:seneretyper)
                      and br_mor2.opprettet_tid > br_mor.opprettet_tid and b_mor2.behandling_status in (:avsluttet))
                -- Det skal ikke finnes en dødsdato på et nyere familiehendelsegrunnlag (som ikke har vedtak):
                and not exists (select * from behandling b_mor3
                      join gr_familie_hendelse grfh2 on (b_mor3.id = grfh2.behandling_id and grfh2.aktiv = 'J')
                      join fh_uidentifisert_barn ub2 on (ub2.familie_hendelse_id in (grfh2.bekreftet_familie_hendelse_id, grfh2.overstyrt_familie_hendelse_id) and ub2.doedsdato is not null)
                      where b_mor3.fagsak_id = f_mor.id
                      and grfh2.opprettet_tid > grfh.opprettet_tid)
                -- Mor ikke nyere FP-sak:
                and not exists (select * from fagsak f_mor_2 where f_mor_2.ytelse_type = :foreldrepenger and f_mor_2.bruker_id = f_mor.bruker_id and f_mor_2.id != f_mor.id and f_mor_2.opprettet_tid > f_mor.opprettet_tid))
          -- Far ikke nyere FP-sak:
          and not exists (select * from fagsak f_far_2 where f_far_2.ytelse_type = :foreldrepenger and f_far_2.bruker_id = f_far.bruker_id and f_far_2.id != f_far.id and f_far_2.opprettet_tid > f_far.opprettet_tid)
        """;

    public List<InformasjonssakData> finnSakerDerMedforelderIkkeHarSøktOgBarnetBleFødtInnenforIntervall(LocalDate fom, LocalDate tom) {
        var avsluttendeStatus = BehandlingStatus.getFerdigbehandletStatuser().stream().map(BehandlingStatus::getKode)
            .toList();
        var query = entityManager.createNativeQuery(QUERY_INFORMASJONSBREV_PÅMINNELSE);
        query.setParameter("fomdato", fom);
        query.setParameter("tomdato", tom);
        query.setParameter("foreldrepenger", FagsakYtelseType.FORELDREPENGER.getKode());
        query.setParameter("restyper", INNVILGET_TYPER);
        query.setParameter("seneretyper", SENERE_TYPER);
        query.setParameter("avsluttet", avsluttendeStatus);
        @SuppressWarnings("unchecked")
        List<Object[]> resultatList = query.getResultList();
        return toPåminnelseData(resultatList);
    }

    private List<InformasjonssakData> toPåminnelseData(List<Object[]> resultatList) {
        List<InformasjonssakData> returnList = new ArrayList<>();
        resultatList.forEach(resultat -> {
            var builder = InformasjonssakData.InformasjonssakDataBuilder
                .ny(((BigDecimal) resultat[0]).longValue())
                .medAktørIdAnnenPart((String) resultat[1])
                .medEnhet((String) resultat[2])
                .medEnhetNavn((String) resultat[3]);
            returnList.add(builder.build());
        });
        return returnList;
    }

    private List<OverlappData> toOverlappData(List<Object[]> resultatList) {
        List<OverlappData> returnList = new ArrayList<>();
        resultatList.forEach(resultat -> {
            var builder = OverlappData.OverlappDataBuilder.ny()
                    .medSaksnummer((String) resultat[0])
                    .medYtelseType((String) resultat[1])
                    .medAktørId((String) resultat[2])
                    .medRolle((String) resultat[3])
                    .medBehandlingId(((BigDecimal) resultat[4]).longValue())
                    .medTidligsteDato(((Timestamp) resultat[5]).toLocalDateTime().toLocalDate());
            returnList.add(builder.build());
        });
        return returnList;
    }

    private static final String QUERY_AVSTEMMING_ANDRE = """
            select distinct saksnummer, ytelse_type, bru.aktoer_id braid, bruker_rolle, beh.id, minbrfom
            from fagsak fs join bruker bru on fs.bruker_id = bru.id join behandling beh on fagsak_id=fs.id
             join behandling_resultat br on (br.behandling_id=beh.id and br.behandling_resultat_type in (:restyper))
             join (select beh1.fagsak_id fsmax, max(br1.opprettet_tid) maxbr from behandling beh1
                 join behandling_resultat br1 on (br1.behandling_id=beh1.id and br1.behandling_resultat_type in (:restyper))
                 where beh1.behandling_type in (:behtyper) and beh1.behandling_status in (:avsluttet) group by beh1.fagsak_id )
               on (fsmax=beh.fagsak_id and br.opprettet_tid = maxbr)
             left outer join br_resultat_behandling brr on (brr.behandling_id=beh.id and brr.aktiv='J')
             left outer join (select BEREGNINGSRESULTAT_FP_ID utbbrpid, min(BR_PERIODE_FOM) minbrfom from br_periode brp
                     left join br_andel ba on ba.br_periode_id = brp.id
                     where ba.dagsats > 0  group by BEREGNINGSRESULTAT_FP_ID
                  ) on utbbrpid = brr.BG_BEREGNINGSRESULTAT_FP_ID
            where beh.behandling_status in (:avsluttet) and beh.behandling_type in (:behtyper)
             and fs.til_infotrygd='N' and fs.ytelse_type in (:foreldrepenger) and minbrfom is not null
            """;

    private static final String QUERY_AVSTEMMING_UTBET2022 = """
            select distinct saksnummer, ytelse_type, bru.aktoer_id braid, bruker_rolle, beh.id, minbrfom
            from fagsak fs join bruker bru on fs.bruker_id = bru.id join behandling beh on fagsak_id=fs.id
             join behandling_resultat br on (br.behandling_id=beh.id and br.behandling_resultat_type in (:restyper))
             join (select beh1.fagsak_id fsmax, max(br1.opprettet_tid) maxbr from behandling beh1
                 join behandling_resultat br1 on (br1.behandling_id=beh1.id and br1.behandling_resultat_type in (:restyper))
                 where beh1.behandling_type in (:behtyper) and beh1.behandling_status in (:avsluttet) group by beh1.fagsak_id )
               on (fsmax=beh.fagsak_id and br.opprettet_tid = maxbr)
             left outer join br_resultat_behandling brr on (brr.behandling_id=beh.id and brr.aktiv='J')
             left outer join (select BEREGNINGSRESULTAT_FP_ID utbbrpid, min(BR_PERIODE_FOM) minbrfom from br_periode brp
                     left join br_andel ba on ba.br_periode_id = brp.id
                     where ba.dagsats > 0  and BR_PERIODE_FOM > :datototo group by BEREGNINGSRESULTAT_FP_ID
                  ) on utbbrpid = brr.BG_BEREGNINGSRESULTAT_FP_ID
            where beh.behandling_status in (:avsluttet) and beh.behandling_type in (:behtyper)
             and fs.til_infotrygd='N' and fs.ytelse_type in (:foreldrepenger) and minbrfom is not null
            """;

    public List<OverlappData> finnSakerSisteVedtakInnenIntervallMedKunUtbetalte(LocalDate fom, LocalDate tom, String saksnummer) {
        /*
         * Plukker saksnummer, siste ytelsebehandling, annenpart og første uttaksdato: -
         * Saker der det finnes et beregnignsresultat/TY med utbetalt periode - inklusive
         * noen få opphør fom etter tidligste periode - Første uttaksdato = tidligste
         * periode, inklusive innvilget utsettelse. - Kan gi noen tilfelle med avslått
         * første periode
         */
        var avsluttendeStatus = BehandlingStatus.getFerdigbehandletStatuser().stream().map(BehandlingStatus::getKode)
                .toList();
        Query query;
        if (saksnummer == null) {
            query = entityManager.createNativeQuery(QUERY_AVSTEMMING_ANDRE + " and fs.opprettet_tid >= :fomdato and fs.opprettet_tid < :tomdato ");
            query.setParameter("fomdato", fom);
            query.setParameter("tomdato", tom.plusDays(1));
            //query.setParameter("datototo", LocalDate.of(2021,12,24));
        } else {
            query = entityManager.createNativeQuery(QUERY_AVSTEMMING_ANDRE + " and saksnummer = :saksnr ");
            //query.setParameter("datototo", LocalDate.of(2021,12,24));
            query.setParameter("saksnr", saksnummer);
        }
        query.setParameter("foreldrepenger", List.of(FagsakYtelseType.FORELDREPENGER.getKode(), FagsakYtelseType.SVANGERSKAPSPENGER.getKode()));
        query.setParameter("restyper", List.of(BehandlingResultatType.INNVILGET.getKode(), BehandlingResultatType.INGEN_ENDRING.getKode(),
            BehandlingResultatType.FORELDREPENGER_ENDRET.getKode(), BehandlingResultatType.FORELDREPENGER_SENERE,
            BehandlingResultatType.AVSLÅTT.getKode(),  BehandlingResultatType.OPPHØR.getKode()));
        query.setParameter("avsluttet", avsluttendeStatus);
        query.setParameter("behtyper", List.of(BehandlingType.FØRSTEGANGSSØKNAD.getKode(), BehandlingType.REVURDERING.getKode()));
        @SuppressWarnings("unchecked")
        List<Object[]> resultatList = query.getResultList();
        return toOverlappData(resultatList);
    }



    public List<Long> finnUtlandBehandlingerMedÅpentAksjonspunkt() {
        var query =  entityManager.createNativeQuery("""
           select b.id from behandling b where behandling_status<>'AVSLU'
           and b.fagsak_id in (select fagsak_id from FAGSAK_EGENSKAP)
           and exists (select * from aksjonspunkt where behandling_id = b.id and aksjonspunkt_status = 'OPPR' and aksjonspunkt_def < '7000')
        """);
        @SuppressWarnings("unchecked")
        List<BigDecimal> resultatList = query.getResultList();
        return resultatList.stream().map(BigDecimal::longValue).toList();
    }

    public void slettApForUtlandsmerking() {
        var query =  entityManager.createNativeQuery("""
           delete from aksjonspunkt where aksjonspunkt_def = '6068' and behandling_id in (select b.id from behandling b where behandling_status<>'AVSLU')
        """);
        query.executeUpdate();
    }

}
