package no.nav.foreldrepenger.behandlingslager.pip;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.JournalpostId;

@ApplicationScoped
public class PipRepository {

    public static final String SAKSNUMMER = "saksnummer";
    private EntityManager entityManager;

    public PipRepository() {
    }

    @Inject
    public PipRepository( EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Optional<PipBehandlingsData> hentDataForBehandling(Long behandlingId) {
        Objects.requireNonNull(behandlingId, "behandlingId"); // NOSONAR

        var sql = """
            SELECT b.behandling_status behandligStatus,
            b.ansvarlig_saksbehandler ansvarligSaksbehandler,
            f.id fagsakId, f.fagsak_status fagsakStatus
            FROM BEHANDLING b
            JOIN FAGSAK f ON b.fagsak_id = f.id
            WHERE b.id = :behandlingId
            """;

        var query = entityManager.createNativeQuery(sql, "PipDataResult");
        query.setParameter("behandlingId", behandlingId);

        @SuppressWarnings("rawtypes") var resultater = query.getResultList();
        if (resultater.isEmpty()) {
            return Optional.empty();
        }
        if (resultater.size() == 1) {
            return Optional.of((PipBehandlingsData) resultater.get(0));
        }
        throw new IllegalStateException("Forventet 0 eller 1 treff etter søk på behandlingId, fikk flere for behandlingId " + behandlingId);
    }

    public Optional<PipBehandlingsData> hentDataForBehandlingUuid(UUID behandlingUuid) {
        Objects.requireNonNull(behandlingUuid, "behandlingUuid"); // NOSONAR

        var sql = """
            SELECT b.behandling_status behandligStatus,
            b.ansvarlig_saksbehandler ansvarligSaksbehandler,
            f.id fagsakId, f.fagsak_status fagsakStatus
            FROM BEHANDLING b
            JOIN FAGSAK f ON b.fagsak_id = f.id
            WHERE b.uuid = :behandlingUuid
            """;

        var query = entityManager.createNativeQuery(sql, "PipDataResult");
        query.setParameter("behandlingUuid", behandlingUuid);

        @SuppressWarnings("rawtypes") var resultater = query.getResultList();
        if (resultater.isEmpty()) {
            return Optional.empty();
        }
        if (resultater.size() == 1) {
            return Optional.of((PipBehandlingsData) resultater.get(0));
        }
        throw new IllegalStateException("Forventet 0 eller 1 treff etter søk på behandlingId, fikk flere for behandlingUuid " + behandlingUuid);
    }

    public Set<AktørId> hentAktørIdKnyttetTilFagsaker(Collection<Long> fagsakIder) {
        Objects.requireNonNull(fagsakIder, SAKSNUMMER);
        if (fagsakIder.isEmpty()) {
            return Collections.emptySet();
        }
        var sql = """
            SELECT por.AKTOER_ID From Fagsak fag
            JOIN BEHANDLING beh ON fag.ID = beh.FAGSAK_ID
            JOIN GR_PERSONOPPLYSNING grp ON grp.behandling_id = beh.ID
            JOIN PO_INFORMASJON poi ON grp.registrert_informasjon_id = poi.ID
            JOIN PO_PERSONOPPLYSNING por ON poi.ID = por.po_informasjon_id
            WHERE fag.id in (:fagsakIder) AND grp.aktiv = 'J'
             UNION ALL
            SELECT br.AKTOER_ID FROM Fagsak fag
            JOIN Bruker br ON fag.BRUKER_ID = br.ID
            WHERE fag.id in (:fagsakIder) AND br.AKTOER_ID IS NOT NULL
             UNION ALL
            SELECT sa.AKTOER_ID From Fagsak fag
            JOIN BEHANDLING beh ON fag.ID = beh.FAGSAK_ID
            JOIN GR_PERSONOPPLYSNING grp ON grp.behandling_id = beh.ID
            JOIN SO_ANNEN_PART sa ON grp.so_annen_part_id = sa.ID
            WHERE fag.id in (:fagsakIder) AND grp.aktiv = 'J' AND sa.AKTOER_ID IS NOT NULL
            """;

        var query = entityManager.createNativeQuery(sql); // NOSONAR
        query.setParameter("fagsakIder", fagsakIder);

        @SuppressWarnings("unchecked")
        List<String> aktørIdList = query.getResultList();
        return aktørIdList.stream().map(AktørId::new).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Set<AktørId> hentAktørIdKnyttetTilSaksnummer(String saksnummer) {
        Objects.requireNonNull(saksnummer, SAKSNUMMER);

        var sql = """
            SELECT por.AKTOER_ID From Fagsak fag
            JOIN BEHANDLING beh ON fag.ID = beh.FAGSAK_ID
            JOIN GR_PERSONOPPLYSNING grp ON grp.behandling_id = beh.ID
            JOIN PO_INFORMASJON poi ON grp.registrert_informasjon_id = poi.ID
            JOIN PO_PERSONOPPLYSNING por ON poi.ID = por.po_informasjon_id
            WHERE fag.SAKSNUMMER = (:saksnummer) AND grp.aktiv = 'J'
             UNION ALL
            SELECT br.AKTOER_ID FROM Fagsak fag
            JOIN Bruker br ON fag.BRUKER_ID = br.ID
            WHERE fag.SAKSNUMMER = (:saksnummer) AND br.AKTOER_ID IS NOT NULL
             UNION ALL
            SELECT sa.AKTOER_ID From Fagsak fag
            JOIN BEHANDLING beh ON fag.ID = beh.FAGSAK_ID
            JOIN GR_PERSONOPPLYSNING grp ON grp.behandling_id = beh.ID
            JOIN SO_ANNEN_PART sa ON grp.so_annen_part_id = sa.ID
            WHERE fag.SAKSNUMMER = (:saksnummer) AND grp.aktiv = 'J' AND sa.AKTOER_ID IS NOT NULL
            """;

        var query = entityManager.createNativeQuery(sql); // NOSONAR
        query.setParameter(SAKSNUMMER, saksnummer);

        @SuppressWarnings("unchecked")
        List<String> aktørIdList = query.getResultList();
        return aktørIdList.stream().map(AktørId::new).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @SuppressWarnings({ "unchecked", "cast" })
    public Set<Long> fagsakIdForJournalpostId(Collection<JournalpostId> journalpostId) {
        if (journalpostId.isEmpty()) {
            return Collections.emptySet();
        }
        var sql = "SELECT fagsak_id FROM JOURNALPOST WHERE journalpost_id in (:journalpostId)";
        var query = entityManager.createNativeQuery(sql);
        query.setParameter("journalpostId", journalpostId.stream().map(j -> j.getVerdi()).collect(Collectors.toList()));

        var result = (List<BigDecimal>) query.getResultList();
        return result.stream().map(BigDecimal::longValue).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @SuppressWarnings({ "unchecked", "cast" })
    public Set<Long> behandlingsIdForOppgaveId(Collection<String> oppgaveIder) {
        if (oppgaveIder.isEmpty()) {
            return Collections.emptySet();
        }
        var sql = "SELECT behandling_id FROM OPPGAVE_BEHANDLING_KOBLING WHERE oppgave_id in (:oppgaveIder)";
        var query = entityManager.createNativeQuery(sql);
        query.setParameter("oppgaveIder", oppgaveIder);
        var result = (List<BigDecimal>) query.getResultList();
        return result.stream().map(BigDecimal::longValue).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Set<String> hentAksjonspunktTypeForAksjonspunktKoder(Collection<AksjonspunktDefinisjon> aksjonspunktKoder) {
        return aksjonspunktKoder.stream()
            .map(ak -> ak.getAksjonspunktType().getNavn()) // ja, getNavn er riktig her....
            .collect(Collectors.toSet());
    }

    @SuppressWarnings({ "unchecked", "cast" })
    public Set<Long> fagsakIderForSøker(Collection<AktørId> aktørId) {
        if (aktørId.isEmpty()) {
            return Collections.emptySet();
        }
        var sql = "SELECT f.id from FAGSAK f join BRUKER b on (f.bruker_id = b.id) where b.aktoer_id in (:aktørId)";
        var query = entityManager.createNativeQuery(sql);
        query.setParameter("aktørId", aktørId.stream().map(AktørId::getId).collect(Collectors.toList()));
        var result = (List<BigDecimal>) query.getResultList();
        return result.stream().map(BigDecimal::longValue).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @SuppressWarnings({ "unchecked", "cast" })
    public Set<Long> fagsakIdForSaksnummer(Collection<String> saksnummre) {
        if (saksnummre.isEmpty()) {
            return Collections.emptySet();
        }
        var sql = "SELECT id from FAGSAK where saksnummer in (:saksnummre) ";
        var query = entityManager.createNativeQuery(sql);
        query.setParameter("saksnummre", saksnummre);
        var result = (List<BigDecimal>) query.getResultList();
        return result.stream().map(BigDecimal::longValue).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Set<Long> behandlingsIdForUuid(Set<UUID> behandlingsUUIDer) {
        if (behandlingsUUIDer == null || behandlingsUUIDer.isEmpty()) {
            return Collections.emptySet();
        }
        var sql = "SELECT beh.id from Behandling AS beh WHERE beh.uuid IN (:uuider)";
        var query = entityManager.createQuery(sql, Long.class);
        query.setParameter("uuider", behandlingsUUIDer);
        return query.getResultStream().collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
