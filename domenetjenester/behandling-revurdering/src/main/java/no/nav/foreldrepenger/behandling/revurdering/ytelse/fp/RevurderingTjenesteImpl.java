package no.nav.foreldrepenger.behandling.revurdering.ytelse.fp;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.revurdering.RevurderingEndring;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingFeil;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjenesteFelles;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.utlanddok.OpptjeningIUtlandDokStatusRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittDekningsgradEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;

@FagsakYtelseTypeRef("FP")
@ApplicationScoped
public class RevurderingTjenesteImpl implements RevurderingTjeneste {

    private BehandlingRepository behandlingRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private FamilieHendelseRepository familieHendelseRepository;
    private PersonopplysningRepository personopplysningRepository;
    private MedlemskapRepository medlemskapRepository;
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private OpptjeningIUtlandDokStatusRepository opptjeningIUtlandDokStatusRepository;
    private RevurderingTjenesteFelles revurderingTjenesteFelles;
    private RevurderingEndring revurderingEndring;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private VergeRepository vergeRepository;

    public RevurderingTjenesteImpl() {
        // for CDI proxy
    }

    @Inject
    public RevurderingTjenesteImpl(BehandlingRepositoryProvider repositoryProvider,
            BehandlingskontrollTjeneste behandlingskontrollTjeneste,
            InntektArbeidYtelseTjeneste iayTjeneste,
            @FagsakYtelseTypeRef("FP") RevurderingEndring revurderingEndring,
            RevurderingTjenesteFelles revurderingTjenesteFelles,
            VergeRepository vergeRepository) {
        this.iayTjeneste = iayTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        this.familieHendelseRepository = repositoryProvider.getFamilieHendelseRepository();
        this.personopplysningRepository = repositoryProvider.getPersonopplysningRepository();
        this.medlemskapRepository = repositoryProvider.getMedlemskapRepository();
        this.opptjeningIUtlandDokStatusRepository = repositoryProvider.getOpptjeningIUtlandDokStatusRepository();
        this.revurderingEndring = revurderingEndring;
        this.revurderingTjenesteFelles = revurderingTjenesteFelles;
        this.vergeRepository = vergeRepository;
    }

    @Override
    public Behandling opprettManuellRevurdering(Fagsak fagsak, BehandlingÅrsakType revurderingsÅrsak, OrganisasjonsEnhet enhet) {
        Behandling behandling = opprettRevurdering(fagsak, revurderingsÅrsak, true, enhet);
        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling);
        behandlingskontrollTjeneste.lagreAksjonspunkterFunnet(kontekst,
                List.of(AksjonspunktDefinisjon.KONTROLL_AV_MANUELT_OPPRETTET_REVURDERINGSBEHANDLING));
        return behandling;
    }

    @Override
    public Behandling opprettAutomatiskRevurdering(Fagsak fagsak, BehandlingÅrsakType revurderingsÅrsak, OrganisasjonsEnhet enhet) {
        return opprettRevurdering(fagsak, revurderingsÅrsak, false, enhet);
    }

    private Behandling opprettRevurdering(Fagsak fagsak, BehandlingÅrsakType revurderingsÅrsak, boolean manueltOpprettet, OrganisasjonsEnhet enhet) {
        Behandling origBehandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId())
                .orElseThrow(() -> RevurderingFeil.FACTORY.tjenesteFinnerIkkeBehandlingForRevurdering(fagsak.getId()).toException());

        // lås original behandling først
        behandlingskontrollTjeneste.initBehandlingskontroll(origBehandling);

        // deretter opprett revurdering
        Behandling revurdering = revurderingTjenesteFelles.opprettRevurderingsbehandling(revurderingsÅrsak, origBehandling, manueltOpprettet, enhet);
        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(revurdering);
        behandlingskontrollTjeneste.opprettBehandling(kontekst, revurdering);

        // Kopier vilkår (samme vilkår vurderes i Revurdering)
        revurderingTjenesteFelles.kopierVilkårsresultat(origBehandling, revurdering, kontekst);

        // Kopier grunnlagsdata
        this.kopierAlleGrunnlagFraTidligereBehandling(origBehandling, revurdering);

        return revurdering;
    }

    @Override
    public void kopierAlleGrunnlagFraTidligereBehandling(Behandling original, Behandling ny) {
        Long originalBehandlingId = original.getId();
        Long nyBehandlingId = ny.getId();
        familieHendelseRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
        personopplysningRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
        medlemskapRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);

        var ytelseFordelingAggregat = ytelsesFordelingRepository.hentAggregatHvisEksisterer(originalBehandlingId);
        if (BehandlingType.REVURDERING.equals(ny.getType())) {
            ytelsesFordelingRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
            if (ytelseFordelingAggregat.isPresent()) {
                ytelseFordelingAggregat.get().getGjeldendeAktivitetskravPerioder().ifPresent(entitet -> {
                    ytelsesFordelingRepository.lagreOpprinnelige(nyBehandlingId, entitet);
                    ytelsesFordelingRepository.tilbakestillSaksbehandledeAktivitetskravPerioder(nyBehandlingId);
                });
            }
        } else {
            // Kopierer kun oppgitt for ny 1gang. Bør kanskje kopiere alt?
            ytelseFordelingAggregat.ifPresent(yfa -> {
                OppgittFordelingEntitet oppgittFordeling = yfa.getOppgittFordeling();
                if (oppgittFordeling != null) {
                    var kopi = revurderingTjenesteFelles.kopierOppgittFordelingFraForrigeBehandling(
                        oppgittFordeling);
                    ytelsesFordelingRepository.lagre(nyBehandlingId, kopi);
                }
                OppgittRettighetEntitet oppgittRettighet = yfa.getOppgittRettighet();
                if (oppgittRettighet != null) {
                    ytelsesFordelingRepository.lagre(nyBehandlingId, oppgittRettighet);
                }
                OppgittDekningsgradEntitet entitet = yfa.getOppgittDekningsgrad();
                if (entitet != null) {
                    ytelsesFordelingRepository.lagre(nyBehandlingId, entitet);
                }
            });
        }
        vergeRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
        opptjeningIUtlandDokStatusRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);

        // gjør til slutt, innebærer kall til abakus
        iayTjeneste.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
    }

    @Override
    public Boolean kanRevurderingOpprettes(Fagsak fagsak) {
        return revurderingTjenesteFelles.kanRevurderingOpprettes(fagsak);
    }

    @Override
    public boolean erRevurderingMedUendretUtfall(Behandling behandling) {
        return revurderingEndring.erRevurderingMedUendretUtfall(behandling);
    }
}
