package no.nav.foreldrepenger.produksjonsstyring.totrinn;


import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.vedtak.felles.jpa.HibernateVerktøy;

@ApplicationScoped
public class TotrinnRepository {

    private EntityManager entityManager;

    TotrinnRepository() {
        // CDI
    }

    @Inject
    public TotrinnRepository( EntityManager entityManager) {
        this.entityManager = entityManager;
    }


    private void lagreTotrinnsresultatgrunnlag(Totrinnresultatgrunnlag totrinnresultatgrunnlag) {
        entityManager.persist(totrinnresultatgrunnlag);
    }

    private void lagreTotrinnaksjonspunktvurdering(Totrinnsvurdering totrinnsvurdering) {
        entityManager.persist(totrinnsvurdering);
    }

    public void lagreOgFlush(Behandling behandling, Totrinnresultatgrunnlag totrinnresultatgrunnlag) {
        Objects.requireNonNull(behandling, "behandling");

        var aktivtTotrinnresultatgrunnlag = getAktivtTotrinnresultatgrunnlag(behandling);
        if (aktivtTotrinnresultatgrunnlag.isPresent()) {
            var grunnlag = aktivtTotrinnresultatgrunnlag.get();
            grunnlag.setAktiv(false);
            entityManager.persist(grunnlag);
        }
        lagreTotrinnsresultatgrunnlag(totrinnresultatgrunnlag);
        entityManager.flush();
    }

    public void lagreOgFlush(Behandling behandling, Collection<Totrinnsvurdering> totrinnaksjonspunktvurderinger) {
        Objects.requireNonNull(behandling, "behandling");

        var aktiveVurderinger = getAktiveTotrinnaksjonspunktvurderinger(behandling);
        if (!aktiveVurderinger.isEmpty()) {
            aktiveVurderinger.forEach(vurdering -> {
                vurdering.setAktiv(false);
                entityManager.persist(vurdering);
            });
        }
        totrinnaksjonspunktvurderinger.forEach(this::lagreTotrinnaksjonspunktvurdering);
        entityManager.flush();
    }


    public Optional<Totrinnresultatgrunnlag> hentTotrinngrunnlag(Behandling behandling) {
        return getAktivtTotrinnresultatgrunnlag(behandling);
    }

    public Collection<Totrinnsvurdering> hentTotrinnaksjonspunktvurderinger(Behandling behandling) {
        return getAktiveTotrinnaksjonspunktvurderinger(behandling);
    }

    protected Optional<Totrinnresultatgrunnlag> getAktivtTotrinnresultatgrunnlag(Behandling behandling) {
        return getAktivtTotrinnresultatgrunnlag(behandling.getId());
    }

    protected Optional<Totrinnresultatgrunnlag> getAktivtTotrinnresultatgrunnlag(Long behandlingId) {
        var query = entityManager.createQuery(
            "SELECT trg FROM Totrinnresultatgrunnlag trg WHERE trg.behandling.id = :behandling_id AND trg.aktiv = 'J'", //$NON-NLS-1$
            Totrinnresultatgrunnlag.class);

        query.setParameter("behandling_id", behandlingId); //$NON-NLS-1$
        return HibernateVerktøy.hentUniktResultat(query);
    }

    protected Collection<Totrinnsvurdering> getAktiveTotrinnaksjonspunktvurderinger(Behandling behandling) {
        return getAktiveTotrinnaksjonspunktvurderinger(behandling.getId());
    }

    protected Collection<Totrinnsvurdering> getAktiveTotrinnaksjonspunktvurderinger(Long behandlingId) {
        var query = entityManager.createQuery(
            "SELECT tav FROM Totrinnsvurdering tav WHERE tav.behandling.id = :behandling_id AND tav.aktiv = 'J'", //$NON-NLS-1$
            Totrinnsvurdering.class);

        query.setParameter("behandling_id", behandlingId); //$NON-NLS-1$
        return query.getResultList();
    }
}
