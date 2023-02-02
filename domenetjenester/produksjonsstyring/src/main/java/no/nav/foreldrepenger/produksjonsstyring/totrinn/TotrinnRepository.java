package no.nav.foreldrepenger.produksjonsstyring.totrinn;


import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import no.nav.vedtak.felles.jpa.HibernateVerktøy;

@ApplicationScoped
public class TotrinnRepository {

    private EntityManager entityManager;

    TotrinnRepository() {
        // CDI
    }

    @Inject
    public TotrinnRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }


    public void lagreOgFlush(Totrinnresultatgrunnlag totrinnresultatgrunnlag) {
        var behandlingId = totrinnresultatgrunnlag.getBehandlingId();
        var aktivtTotrinnresultatgrunnlag = getAktivtTotrinnresultatgrunnlag(behandlingId);
        if (aktivtTotrinnresultatgrunnlag.isPresent()) {
            var grunnlag = aktivtTotrinnresultatgrunnlag.get();
            grunnlag.setAktiv(false);
            entityManager.persist(grunnlag);
        }
        lagreTotrinnsresultatgrunnlag(totrinnresultatgrunnlag);
        entityManager.flush();
    }

    public void lagreOgFlush(Collection<Totrinnsvurdering> totrinnaksjonspunktvurderinger) {
        var behandlingIds = totrinnaksjonspunktvurderinger.stream().map(t -> t.getBehandlingId()).collect(Collectors.toSet());
        if (behandlingIds.size() > 1) {
            throw new IllegalArgumentException("Alle totrinnsvurderinger må ha samme behandling. Fant " + behandlingIds);
        }

        var behandlingId = behandlingIds.stream().findFirst().orElseThrow();
        var aktiveVurderinger = getAktiveTotrinnaksjonspunktvurderinger(behandlingId);
        if (!aktiveVurderinger.isEmpty()) {
            aktiveVurderinger.forEach(vurdering -> {
                vurdering.setAktiv(false);
                entityManager.persist(vurdering);
            });
        }
        totrinnaksjonspunktvurderinger.forEach(this::lagreTotrinnaksjonspunktvurdering);
        entityManager.flush();
    }


    public Optional<Totrinnresultatgrunnlag> hentTotrinngrunnlag(Long behandlingId) {
        return getAktivtTotrinnresultatgrunnlag(behandlingId);
    }

    public Collection<Totrinnsvurdering> hentTotrinnaksjonspunktvurderinger(Long behandlingId) {
        return getAktiveTotrinnaksjonspunktvurderinger(behandlingId);
    }

    private void lagreTotrinnsresultatgrunnlag(Totrinnresultatgrunnlag totrinnresultatgrunnlag) {
        entityManager.persist(totrinnresultatgrunnlag);
    }

    private void lagreTotrinnaksjonspunktvurdering(Totrinnsvurdering totrinnsvurdering) {
        entityManager.persist(totrinnsvurdering);
    }

    private Optional<Totrinnresultatgrunnlag> getAktivtTotrinnresultatgrunnlag(Long behandlingId) {
        var query = entityManager.createQuery(
            "SELECT trg FROM Totrinnresultatgrunnlag trg WHERE trg.behandling.id = :behandling_id AND trg.aktiv = 'J'", //$NON-NLS-1$
            Totrinnresultatgrunnlag.class);

        query.setParameter("behandling_id", behandlingId); //$NON-NLS-1$
        return HibernateVerktøy.hentUniktResultat(query);
    }

    private Collection<Totrinnsvurdering> getAktiveTotrinnaksjonspunktvurderinger(Long behandlingId) {
        var query = entityManager.createQuery(
            "SELECT tav FROM Totrinnsvurdering tav WHERE tav.behandling.id = :behandling_id AND tav.aktiv = 'J'", //$NON-NLS-1$
            Totrinnsvurdering.class);

        query.setParameter("behandling_id", behandlingId); //$NON-NLS-1$
        return query.getResultList();
    }
}
