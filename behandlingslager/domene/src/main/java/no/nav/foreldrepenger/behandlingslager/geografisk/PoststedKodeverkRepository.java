package no.nav.foreldrepenger.behandlingslager.geografisk;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.hibernate.jpa.QueryHints;

import no.nav.vedtak.felles.jpa.HibernateVerktøy;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
public class PoststedKodeverkRepository {

    private static final String SYNK_POSTNUMMER = "SYNK";

    private EntityManager entityManager;

    PoststedKodeverkRepository() {
        // for CDI proxy
    }

    @Inject
    public PoststedKodeverkRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    public List<Poststed> hentAllePostnummer() {
        var query = entityManager.createQuery("from Poststed p where poststednummer <> :postnr", Poststed.class)
                .setParameter("postnr", SYNK_POSTNUMMER);
        return query.getResultList();
    }

    public Optional<Poststed> finnPoststed(String postnummer) {
        var query = entityManager.createQuery("from Poststed p where poststednummer = :postnr", Poststed.class)
                .setParameter("postnr", postnummer);
        return HibernateVerktøy.hentUniktResultat(query);
    }

    public Optional<Poststed> finnPoststedReadOnly(String postnummer) {
        var query = entityManager.createQuery("from Poststed p where poststednummer = :postnr", Poststed.class)
            .setParameter("postnr", postnummer)
            .setHint(QueryHints.HINT_READONLY, "true");
        return HibernateVerktøy.hentUniktResultat(query);
    }

    public void lagrePostnummer(Poststed postnummer) {
        entityManager.persist(postnummer);
    }

    public LocalDate getPostnummerKodeverksDato() {
        return finnPoststed(SYNK_POSTNUMMER).map(Poststed::getGyldigFom).orElse(Tid.TIDENES_BEGYNNELSE);
    }

    public void setPostnummerKodeverksDato(String versjon, LocalDate synkDato) {
        if (finnPoststed(SYNK_POSTNUMMER).isPresent()) {
            oppdaterPostnummerKodeverkDato("VERSJON" + versjon, synkDato);
        } else {
            var postnummer = new Poststed(SYNK_POSTNUMMER, "VERSJON" + versjon, synkDato, Tid.TIDENES_ENDE);
            entityManager.persist(postnummer);
        }
        entityManager.flush();
    }

    private void oppdaterPostnummerKodeverkDato(String versjon, LocalDate synkDato) {
        entityManager.createNativeQuery(
                "UPDATE POSTSTED SET gyldigfom = :dato, poststednavn = :nyversjon WHERE poststednummer = :synkid")
            .setParameter("dato", synkDato)
            .setParameter("nyversjon", versjon)
            .setParameter("synkid", SYNK_POSTNUMMER)
            .executeUpdate(); //$NON-NLS-1$
    }
}
