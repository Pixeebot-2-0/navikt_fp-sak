package no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold;

import no.nav.vedtak.felles.jpa.HibernateVerktøy;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@ApplicationScoped
public class ArbeidsforholdValgRepository {

    private EntityManager entityManager;

    ArbeidsforholdValgRepository() {
        // CDI
    }

    @Inject
    public ArbeidsforholdValgRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager");
        this.entityManager = entityManager;
    }

    public List<ArbeidsforholdValg> hentArbeidsforholdValgForBehandling(Long behandlingId) {
        final var query = entityManager.createQuery("FROM ArbeidsforholdValg arb " + // NOSONAR //$NON-NLS-1$
                "WHERE arb.behandlingId = :behandlingId AND arb.aktiv = :aktiv", ArbeidsforholdValg.class);
        query.setParameter("behandlingId", behandlingId); // NOSONAR //$NON-NLS-1$
        query.setParameter("aktiv", true); // NOSONAR //$NON-NLS-1$
        return query.getResultList();
    }

    public void lagre(ArbeidsforholdValg nyttNotat, Long behandlingId) {
        var eksisterendeNotatOpt = hentNøyaktigNotatForBehandling(behandlingId, nyttNotat.getArbeidsgiver().getIdentifikator());
        if (eksisterendeNotatOpt.isPresent() && !eksisterendeNotatOpt.get().equals(nyttNotat)) {
            var eksisterendeNotat = eksisterendeNotatOpt.get();
            eksisterendeNotat.setBegrunnelse(nyttNotat.getBegrunnelse());
            eksisterendeNotat.setVurdering(nyttNotat.getVurdering());
            lagre(eksisterendeNotat);
        } else {
            nyttNotat.setBehandlingId(behandlingId);
            lagre(nyttNotat);
        }
    }

    public void fjernValg(ArbeidsforholdValg arbeidsforholdValg) {
        if (arbeidsforholdValg.getId() == null || !arbeidsforholdValg.erAktiv()) {
            throw new IllegalStateException("FEIL: Valg som skal deaktiveres må ha "
                    + "id og være aktive. Valg som ble forsøkt deaktivert: " + arbeidsforholdValg);
        }
        arbeidsforholdValg.setAktiv(false);
        lagre(arbeidsforholdValg);
    }

    private Optional<ArbeidsforholdValg> hentNøyaktigNotatForBehandling(Long behandlingId, String arbeidsgiverIdent) {
        final var query = entityManager.createQuery("FROM ArbeidsforholdValg arb " + // NOSONAR //$NON-NLS-1$
            "WHERE arb.behandlingId = :behandlingId " +
            "AND arb.arbeidsgiverIdent = :arbeidsgiverIdent ", ArbeidsforholdValg.class);
        query.setParameter("behandlingId", behandlingId);
        query.setParameter("arbeidsgiverIdent", arbeidsgiverIdent);
        return HibernateVerktøy.hentUniktResultat(query);
    }

    private void lagre(ArbeidsforholdValg arbeidsforholdValg) {
        entityManager.persist(arbeidsforholdValg);
        entityManager.flush();
    }
}
