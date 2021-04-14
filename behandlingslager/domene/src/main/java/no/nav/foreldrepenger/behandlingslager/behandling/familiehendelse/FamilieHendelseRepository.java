package no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.RegisterdataDiffsjekker;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.vedtak.felles.jpa.HibernateVerktøy;

@ApplicationScoped
public class FamilieHendelseRepository {

    private EntityManager entityManager;
    private BehandlingLåsRepository behandlingLåsRepository;

    protected FamilieHendelseRepository() {
        // CDI proxy
    }

    @Inject
    public FamilieHendelseRepository( EntityManager entityManager, BehandlingLåsRepository behandlingLåsRepository) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$ // NOSONAR
        this.entityManager = entityManager;
        this.behandlingLåsRepository = behandlingLåsRepository;
    }

    public FamilieHendelseRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
        if (entityManager != null) {
            this.behandlingLåsRepository = new BehandlingLåsRepository(entityManager);
        }
    }

    public FamilieHendelseGrunnlagEntitet hentAggregat(Long behandlingId) {
        final var aktivtFamilieHendelseGrunnlag = getAktivtFamilieHendelseGrunnlag(behandlingId);
        if (aktivtFamilieHendelseGrunnlag.isPresent()) {
            return aktivtFamilieHendelseGrunnlag.get();
        }
        throw FamilieHendelseFeil.fantIkkeForventetGrunnlagPåBehandling(behandlingId);
    }

    public Optional<FamilieHendelseGrunnlagEntitet> hentAggregatHvisEksisterer(Long behandlingId) {
        final var aktivtFamilieHendelseGrunnlag = getAktivtFamilieHendelseGrunnlag(behandlingId);
        return aktivtFamilieHendelseGrunnlag.isPresent() ? Optional.of(aktivtFamilieHendelseGrunnlag.get()) : Optional.empty();
    }

    private Optional<FamilieHendelseGrunnlagEntitet> getAktivtFamilieHendelseGrunnlag(Long behandlingId) {
        final var query = entityManager.createQuery("FROM FamilieHendelseGrunnlag gr " + // NOSONAR //$NON-NLS-1$
            "WHERE gr.behandlingId = :behandlingId " + //$NON-NLS-1$
            "AND gr.aktiv = :aktivt", FamilieHendelseGrunnlagEntitet.class)
            .setFlushMode(FlushModeType.COMMIT); //$NON-NLS-1$
        query.setParameter("behandlingId", behandlingId); // NOSONAR //$NON-NLS-1$
        query.setParameter("aktivt", true); // NOSONAR //$NON-NLS-1$
        return HibernateVerktøy.hentUniktResultat(query);
    }

    private void lagreOgFlush(Long behandlingId, FamilieHendelseGrunnlagEntitet nyttGrunnlag) {
        Objects.requireNonNull(behandlingId, "behandlingId"); // NOSONAR $NON-NLS-1$ //$NON-NLS-1$
        if (nyttGrunnlag == null) {
            return;
        }
        final var lås = behandlingLåsRepository.taLås(behandlingId);
        final var tidligereAggregat = getAktivtFamilieHendelseGrunnlag(behandlingId);

        if (tidligereAggregat.isPresent()) {
            final var aggregat = tidligereAggregat.get();
            var diff = new RegisterdataDiffsjekker(true).getDiffEntity().diff(aggregat, nyttGrunnlag);
            if (!diff.isEmpty()) {
                aggregat.setAktiv(false);
                entityManager.persist(aggregat);
                entityManager.flush();
                lagreGrunnlag(nyttGrunnlag, behandlingId);
            }
        } else {
            lagreGrunnlag(nyttGrunnlag, behandlingId);
        }
        verifiserBehandlingLås(lås);
        entityManager.flush();
    }

    private void lagreGrunnlag(FamilieHendelseGrunnlagEntitet nyttGrunnlag, Long behandlingId) {
        nyttGrunnlag.setBehandling(behandlingId);
        lagreHendelse(nyttGrunnlag.getSøknadVersjon());

        if (nyttGrunnlag.getBekreftetVersjon().isPresent()) {
            lagreHendelse(nyttGrunnlag.getBekreftetVersjon().get());
        }

        if (nyttGrunnlag.getOverstyrtVersjon().isPresent()) {
            final var entity = nyttGrunnlag.getOverstyrtVersjon().get();
            lagreHendelse(entity);
        }

        entityManager.persist(nyttGrunnlag);
    }

    private void lagreHendelse(FamilieHendelseEntitet entity) {
        entityManager.persist(entity);
        if (entity.getTerminbekreftelse().isPresent()) {
            entityManager.persist(entity.getTerminbekreftelse().get());
        }
        if (entity.getAdopsjon().isPresent()) {
            entityManager.persist(entity.getAdopsjon().get());
        }
        for (var uidentifisertBarn : entity.getBarna()) {
            entityManager.persist(uidentifisertBarn);
        }
    }

    public void lagre(Behandling behandling, FamilieHendelseBuilder hendelseBuilder) {
        Objects.requireNonNull(behandling, "behandling"); // NOSONAR //$NON-NLS-1$
        lagre(behandling.getId(), hendelseBuilder);
    }

    public void lagre(Long behandlingId, FamilieHendelseBuilder hendelseBuilder) {
        Objects.requireNonNull(behandlingId, "behandlingId"); // NOSONAR //$NON-NLS-1$
        Objects.requireNonNull(hendelseBuilder, "hendelseBuilder"); // NOSONAR $NON-NLS-1$ //$NON-NLS-1$

        var aggregatBuilder = opprettAggregatBuilderFor(behandlingId);
        var type = hendelseBuilder.getType();
        switch (type) {
            case SØKNAD -> aggregatBuilder.medSøknadVersjon(hendelseBuilder);
            case BEKREFTET -> aggregatBuilder.medBekreftetVersjon(hendelseBuilder);
            case OVERSTYRT -> aggregatBuilder.medOverstyrtVersjon(hendelseBuilder);
            default -> throw new IllegalArgumentException("Støtter ikke HendelseVersjonType: " + type);
        }
        lagreOgFlush(behandlingId, aggregatBuilder.build());
    }

    public void lagreRegisterHendelse(Behandling behandling, FamilieHendelseBuilder hendelse) {
        Objects.requireNonNull(behandling, "behandling"); // NOSONAR $NON-NLS-1$ //$NON-NLS-1$
        Objects.requireNonNull(hendelse, "hendelse"); // NOSONAR $NON-NLS-1$ //$NON-NLS-1$

        final var aggregatBuilder = opprettAggregatBuilderFor(behandling.getId());
        // Fjern overstyr manglende fødsel i tilfelle første innhenting. Bevarer senere justering av dato
        if (erFørsteFødselRegistreringHarTidligereOverstyrtFødsel(aggregatBuilder.getKladd())) {
            aggregatBuilder.medOverstyrtVersjon(null);
        }
        var tidligereRegister = aggregatBuilder.getKladd().getBekreftetVersjon().orElse(null);
        aggregatBuilder.medBekreftetVersjon(hendelse);
        // Nullstill tidligere overstyringer dersom realendring i registerversjon
        if (!skalBeholdeOverstyrtVedEndring(aggregatBuilder.getKladd(), tidligereRegister)) {
            aggregatBuilder.medOverstyrtVersjon(null);
        }
        // nullstill overstyring ved overgang fra termin til fødsel
        if (harOverstyrtTerminOgOvergangTilFødsel(aggregatBuilder.getKladd())) {
            aggregatBuilder.medOverstyrtVersjon(null);
        }
        lagreOgFlush(behandling.getId(), aggregatBuilder.build());
    }

    private boolean skalBeholdeOverstyrtVedEndring(FamilieHendelseGrunnlagEntitet kladd, FamilieHendelseEntitet forrige) {
        var overstyrtType = kladd.getOverstyrtVersjon().map(FamilieHendelseEntitet::getType).orElse(FamilieHendelseType.UDEFINERT);
        var nyeste = kladd.getBekreftetVersjon().orElse(null);
        if (!kladd.getHarOverstyrteData() || !FamilieHendelseType.FØDSEL.equals(overstyrtType) || forrige == null || nyeste == null)
            return true;
        return Objects.equals(forrige.getAntallBarn(), nyeste.getAntallBarn()) &&
            Objects.equals(forrige.getFødselsdato(), nyeste.getFødselsdato()) &&
            Objects.equals(forrige.getInnholderDøfødtBarn(), nyeste.getInnholderDøfødtBarn()) &&
            Objects.equals(forrige.getInnholderDødtBarn(), nyeste.getInnholderDødtBarn());
    }

    private boolean erFørsteFødselRegistreringHarTidligereOverstyrtFødsel(FamilieHendelseGrunnlagEntitet kladd) {
        var overstyrtHendelseType = kladd.getOverstyrtVersjon().map(FamilieHendelseEntitet::getType).orElse(FamilieHendelseType.UDEFINERT);
        return !kladd.getHarRegisterData() && kladd.getHarOverstyrteData() && FamilieHendelseType.FØDSEL.equals(overstyrtHendelseType);
    }

    private boolean harOverstyrtTerminOgOvergangTilFødsel(FamilieHendelseGrunnlagEntitet kladd) {
        var overstyrtType = kladd.getOverstyrtVersjon().map(FamilieHendelseEntitet::getType).orElse(FamilieHendelseType.UDEFINERT);
        var registerType = kladd.getBekreftetVersjon().map(FamilieHendelseEntitet::getType).orElse(FamilieHendelseType.UDEFINERT);
        return kladd.getHarOverstyrteData() && FamilieHendelseType.TERMIN.equals(overstyrtType) && FamilieHendelseType.FØDSEL.equals(registerType);
    }

    public void lagreOverstyrtHendelse(Behandling behandling, FamilieHendelseBuilder hendelse) {
        Objects.requireNonNull(behandling, "behandling"); // NOSONAR //$NON-NLS-1$
        Objects.requireNonNull(hendelse, "hendelse"); // NOSONAR //$NON-NLS-1$

        final var aggregatBuilder = opprettAggregatBuilderFor(behandling.getId());
        aggregatBuilder.medOverstyrtVersjon(hendelse);
        lagreOgFlush(behandling.getId(), aggregatBuilder.build());
    }

    private void fjernBekreftetData(Long behandlingId) {
        Objects.requireNonNull(behandlingId, "behandlingId"); // NOSONAR //$NON-NLS-1$
        final var grunnlag = hentAggregatHvisEksisterer(behandlingId);
        if (!grunnlag.isPresent()) {
            return;
        }
        final var aggregatBuilder = opprettAggregatBuilderFor(behandlingId);
        if (grunnlag.get().getOverstyrtVersjon().isPresent()) {
            aggregatBuilder.medOverstyrtVersjon(null);
        }
        lagreOgFlush(behandlingId, aggregatBuilder.build());
    }

    /**
     * Kopierer grunnlag fra en tidligere behandling. Endrer ikke aggregater, en skaper nye referanser til disse.
     */
    public void kopierGrunnlagFraEksisterendeBehandling(Long gammelBehandlingId, Long nyBehandlingId) {
        final var familieHendelseGrunnlag = getAktivtFamilieHendelseGrunnlag(gammelBehandlingId);
        if (familieHendelseGrunnlag.isPresent()) {
            final var entitet = new FamilieHendelseGrunnlagEntitet(familieHendelseGrunnlag.get());

            lagreOgFlush(nyBehandlingId, entitet);
        }
    }

    /**
     * Kopierer data fra gammel behandling til ny behandling.
     *
     * Fjerner bekreftede og overstyrte data som var for
     *
     * @param gammelBehandling behandlingen det opprettes revurdering på
     * @param nyBehandling revurderings behandlingen
     */
    public void kopierGrunnlagFraEksisterendeBehandlingForRevurdering(Long gammelBehandlingId, Long nyBehandlingId) {
        final var familieHendelseGrunnlag = getAktivtFamilieHendelseGrunnlag(gammelBehandlingId);
        if (familieHendelseGrunnlag.isPresent()) {
            final var entitet = new FamilieHendelseGrunnlagEntitet(familieHendelseGrunnlag.get());
            entitet.setBekreftetHendelse(null);
            entitet.setOverstyrtHendelse(null);

            lagreOgFlush(nyBehandlingId, entitet);
        }
    }

    /**
     * Slette avklart data på en Behandling. Sørger for at samtidige oppdateringer på samme Behandling,
     * eller andre Behandlinger
     * på samme Fagsak ikke kan gjøres samtidig.
     *
     * @see BehandlingLås
     */
    public void slettAvklarteData(Long behandlingId, BehandlingLås lås) {
        fjernBekreftetData(behandlingId);

        verifiserBehandlingLås(lås);
        entityManager.flush();
    }

    // sjekk lås og oppgrader til skriv
    protected void verifiserBehandlingLås(BehandlingLås lås) {
        behandlingLåsRepository.oppdaterLåsVersjon(lås);
    }

    private FamilieHendelseGrunnlagBuilder opprettAggregatBuilderFor(Long behandlingId) {
        var familieHendelseAggregat = hentAggregatHvisEksisterer(behandlingId);
        return FamilieHendelseGrunnlagBuilder.oppdatere(familieHendelseAggregat);
    }

    public FamilieHendelseBuilder opprettBuilderFor(Behandling behandling) {
        var familieHendelseAggregat = hentAggregatHvisEksisterer(behandling.getId());
        final var oppdatere = FamilieHendelseGrunnlagBuilder.oppdatere(familieHendelseAggregat);
        Objects.requireNonNull(oppdatere, "oppdatere"); //$NON-NLS-1$
        return opprettBuilderFor(Optional.ofNullable(oppdatere.getKladd()), false);
    }

    public FamilieHendelseBuilder opprettBuilderForregister(Behandling behandling) {
        var familieHendelseAggregat = hentAggregatHvisEksisterer(behandling.getId());
        final var oppdatere = FamilieHendelseGrunnlagBuilder.oppdatere(familieHendelseAggregat);
        Objects.requireNonNull(oppdatere, "oppdatere"); //$NON-NLS-1$
        return opprettBuilderFor(Optional.ofNullable(oppdatere.getKladd()), true);
    }

    /**
     * Baserer seg på typen under seg hvis det ikke finnes en tilsvarende.
     * F.eks Ber du om Overstyrt så vil denne basere seg på Hendelse i følgende rekkefølge
     * 1. Overstyrt
     * 2. Bekreftet
     * 3. Søknad
     *
     * @param aggregat nåværende aggregat
     * @return Builder
     */
    private FamilieHendelseBuilder opprettBuilderFor(Optional<FamilieHendelseGrunnlagEntitet> aggregat, boolean register) {
        Objects.requireNonNull(aggregat, "aggregat"); // NOSONAR //$NON-NLS-1$
        if (aggregat.isPresent()) {
            var type = register ? HendelseVersjonType.BEKREFTET : utledTypeFor(aggregat);
            final var hendelseAggregat = aggregat.get();
            final var hendelseAggregat1 = getFamilieHendelseBuilderForType(hendelseAggregat, type);
            if (hendelseAggregat1 != null) {
                hendelseAggregat1.setType(type);
                return hendelseAggregat1;
            }
            throw FamilieHendelseFeil.ukjentVersjonstype();
        }
        throw FamilieHendelseFeil.aggregatKanIkkeVæreNull();
    }

    private FamilieHendelseBuilder getFamilieHendelseBuilderForType(FamilieHendelseGrunnlagEntitet aggregat, HendelseVersjonType type) {
        switch (type) {
            case SØKNAD:
                return FamilieHendelseBuilder.oppdatere(Optional.ofNullable(aggregat.getSøknadVersjon()), type);
            case BEKREFTET:
                if (aggregat.getBekreftetVersjon().isEmpty()) {
                    return getFamilieHendelseBuilderForType(aggregat, HendelseVersjonType.SØKNAD);
                }
                return FamilieHendelseBuilder.oppdatere(aggregat.getBekreftetVersjon(), type);
            case OVERSTYRT:
                if (aggregat.getOverstyrtVersjon().isEmpty()) {
                    return getFamilieHendelseBuilderForType(aggregat, HendelseVersjonType.BEKREFTET);
                }
                return FamilieHendelseBuilder.oppdatere(aggregat.getOverstyrtVersjon(), type);
            default:
                throw new IllegalArgumentException("Støtter ikke HendelseVersjonType: " + type);
        }
    }

    private HendelseVersjonType utledTypeFor(Optional<FamilieHendelseGrunnlagEntitet> aggregat) {
        if (aggregat.isPresent()) {
            if (aggregat.get().getHarOverstyrteData()) {
                return HendelseVersjonType.OVERSTYRT;
            }
            if (aggregat.get().getHarBekreftedeData() || aggregat.get().getSøknadVersjon() != null) {
                return HendelseVersjonType.BEKREFTET;
            }
            if (aggregat.get().getSøknadVersjon() == null) {
                return HendelseVersjonType.SØKNAD;
            }
            throw new IllegalStateException("Utvikler feil."); //$NON-NLS-1$
        }
        return HendelseVersjonType.SØKNAD;
    }

    public Optional<Long> hentIdPåAktivFamiliehendelse(Long behandlingId) {
        return getAktivtFamilieHendelseGrunnlag(behandlingId)
            .map(FamilieHendelseGrunnlagEntitet::getId);
    }

    public FamilieHendelseGrunnlagEntitet hentGrunnlagPåId(Long grunnlagId) {
        Objects.requireNonNull(grunnlagId, "grunnlagId"); // NOSONAR $NON-NLS-1$ //$NON-NLS-1$
        var query = entityManager.createQuery("FROM FamilieHendelseGrunnlag gr " + // NOSONAR //$NON-NLS-1$
            "WHERE gr.id = :grunnlagId ", FamilieHendelseGrunnlagEntitet.class)
           .setFlushMode(FlushModeType.COMMIT); //$NON-NLS-1$
        query.setParameter("grunnlagId", grunnlagId); // NOSONAR //$NON-NLS-1$
        return query.getResultStream().findFirst().orElse(null);
    }

    /*
     * Til Forvaltningsbruk der det er oppgitt feil termindato i søknad
     */
    public int oppdaterGjeldendeTermindatoForBehandling(Long behandlingId, LocalDate termindato, String begrunnelse) {
        var fhIds = new HashSet<>();
        var grunnlag = hentAggregat(behandlingId);

        if (grunnlag.getOverstyrtVersjon().map(FamilieHendelseEntitet::getTerminbekreftelse).isPresent()) {
            grunnlag.getOverstyrtVersjon().map(FamilieHendelseEntitet::getId).ifPresent(fhIds::add);
        }
        if (grunnlag.getBekreftetVersjon().map(FamilieHendelseEntitet::getTerminbekreftelse).isPresent()) {
            grunnlag.getBekreftetVersjon().map(FamilieHendelseEntitet::getId).ifPresent(fhIds::add);
        }
        if (grunnlag.getSøknadVersjon().getTerminbekreftelse().isPresent()) {
            fhIds.add(grunnlag.getSøknadVersjon().getId());
        }
        if (fhIds.isEmpty())
            return 0;
        var antall = entityManager.createNativeQuery(
            "UPDATE FH_TERMINBEKREFTELSE SET TERMINDATO = :termin, endret_av = :begr WHERE FAMILIE_HENDELSE_ID in :fhid")
            .setParameter("termin", termindato)
            .setParameter("fhid", fhIds)
            .setParameter("begr", begrunnelse)
            .executeUpdate(); //$NON-NLS-1$
        entityManager.flush();
        return antall;
    }
}
