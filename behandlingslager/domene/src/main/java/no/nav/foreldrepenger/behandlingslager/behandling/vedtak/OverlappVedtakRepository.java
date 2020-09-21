package no.nav.foreldrepenger.behandlingslager.behandling.vedtak;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import no.nav.foreldrepenger.domene.typer.Saksnummer;

@ApplicationScoped
public class OverlappVedtakRepository {

    private EntityManager entityManager;

    public OverlappVedtakRepository() {
        // for CDI proxy
    }

    @Inject
    public OverlappVedtakRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public List<OverlappVedtak> hentForSaksnummer(Saksnummer saksnummer) {
        TypedQuery<OverlappVedtak> query = entityManager
            .createQuery("from OverlappVedtak where saksnummer=:saksnummer",
                OverlappVedtak.class);
        query.setParameter("saksnummer", saksnummer); // NOSONAR
        return query.getResultList();
    }

    public void slettAvstemtEnkeltsak(Saksnummer saksnummer) {
        Query query = entityManager.createNativeQuery(
            "DELETE FROM OVERLAPP_VEDTAK WHERE saksnummer=:saksnummer and hendelse=:hendelse");
        query.setParameter("saksnummer", saksnummer); // NOSONAR
        query.setParameter("hendelse", OverlappVedtak.HENDELSE_AVSTEM_SAK + "-" + saksnummer.getVerdi()); // NOSONAR
        query.executeUpdate();
    }

    public void lagre(OverlappVedtak.Builder overlappBuilder) {
        OverlappVedtak overlapp = overlappBuilder.build();
        entityManager.persist(overlapp);
        entityManager.flush();
    }

    public void lagre(List<OverlappVedtak.Builder> overlappene) {
        overlappene.stream().map(OverlappVedtak.Builder::build).forEach(entityManager::persist);
        entityManager.flush();
    }

}
