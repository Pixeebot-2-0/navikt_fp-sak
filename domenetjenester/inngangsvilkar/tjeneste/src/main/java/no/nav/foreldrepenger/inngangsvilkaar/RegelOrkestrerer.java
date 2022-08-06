package no.nav.foreldrepenger.inngangsvilkaar;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.inngangsvilkaar.søknad.InngangsvilkårEngangsstønadSøknadsfrist;


@ApplicationScoped
public class RegelOrkestrerer {

    private static final Map<VilkårType, Set<String>> LAGRE_MERKNAD_PARAMETRE =
        Map.of(VilkårType.SØKNADSFRISTVILKÅRET, Set.of(InngangsvilkårEngangsstønadSøknadsfrist.DAGER_FOR_SENT_PROPERTY));

    private InngangsvilkårTjeneste inngangsvilkårTjeneste;

    protected RegelOrkestrerer() {
        // For CDI
    }

    @Inject
    public RegelOrkestrerer(InngangsvilkårTjeneste inngangsvilkårTjeneste) {
        this.inngangsvilkårTjeneste = inngangsvilkårTjeneste;
    }

    public RegelResultat vurderInngangsvilkår(Set<VilkårType> vilkårHåndtertAvSteg, Behandling behandling, BehandlingReferanse ref) {
        var vilkårResultat = inngangsvilkårTjeneste.getBehandlingsresultat(ref.behandlingId()).getVilkårResultat();
        var matchendeVilkårPåBehandling = vilkårResultat.getVilkårene().stream()
            .filter(v -> vilkårHåndtertAvSteg.contains(v.getVilkårType()))
            .collect(toList());
        validerMaksEttVilkår(matchendeVilkårPåBehandling);

        var vilkår = matchendeVilkårPåBehandling.isEmpty() ? null : matchendeVilkårPåBehandling.get(0);
        if (vilkår == null) {
            // Intet vilkår skal eksekveres i regelmotor, men sikrer at det samlede inngangsvilkår-utfallet blir korrekt
            // ved å utlede det fra alle vilkårsutfallene
            var alleUtfall = vilkårResultat.hentAlleGjeldendeVilkårsutfall();
            var inngangsvilkårUtfall = VilkårResultatType.utledInngangsvilkårUtfall(alleUtfall);
            oppdaterBehandlingMedVilkårresultat(behandling, inngangsvilkårUtfall);
            return new RegelResultat(vilkårResultat, emptyList(), emptyMap());
        }

        var vilkårDataResultat = kjørRegelmotor(ref, vilkår);

        // Ekstraresultat
        Map<VilkårType, Object> ekstraResultater = vilkårDataResultat.ekstraVilkårresultat() == null ? Map.of() :
            Map.of(vilkårDataResultat.vilkårType(), vilkårDataResultat.ekstraVilkårresultat());

        // Inngangsvilkårutfall utledet fra alle vilkårsutfallene
        var alleUtfall = sammenslåVilkårUtfall(vilkårResultat, vilkårDataResultat);
        var inngangsvilkårUtfall = VilkårResultatType.utledInngangsvilkårUtfall(alleUtfall);
        oppdaterBehandlingMedVilkårresultat(behandling, vilkårDataResultat, inngangsvilkårUtfall);

        // Aksjonspunkter
        List<AksjonspunktDefinisjon> aksjonspunktDefinisjoner = new ArrayList<>();
        if (!vilkår.erOverstyrt()) {
            // TODO (essv): PKMANTIS-1988 Sjekk med Anita om AP for manuell vurdering skal (gjen)opprettes dersom allerede overstyrt
            aksjonspunktDefinisjoner = vilkårDataResultat.aksjonspunktDefinisjoner();
        }

        return new RegelResultat(vilkårResultat, aksjonspunktDefinisjoner, ekstraResultater);
    }

    private void validerMaksEttVilkår(List<Vilkår> vilkårSomSkalBehandle) {
        if (vilkårSomSkalBehandle.size() > 1) {
            throw new IllegalArgumentException("Kun ett vilkår skal evalueres per regelkall. " +
                "Her angis vilkår: " + vilkårSomSkalBehandle.stream().map(v -> v.getVilkårType().getKode()).collect(Collectors.joining(",")));
        }
    }

    protected VilkårData vurderVilkår(VilkårType vilkårType, BehandlingReferanse ref) {
        var inngangsvilkår = inngangsvilkårTjeneste.finnVilkår(vilkårType, ref.fagsakYtelseType());
        return inngangsvilkår.vurderVilkår(ref);
    }

    private Set<VilkårUtfallType> sammenslåVilkårUtfall(VilkårResultat vilkårResultat,
                                                        VilkårData vdRegelmotor) {
        var vilkårTyper = vilkårResultat.getVilkårene().stream()
            .collect(toMap(Vilkår::getVilkårType, v -> v));
        var vilkårUtfall = vilkårResultat.getVilkårene().stream()
            .collect(toMap(Vilkår::getVilkårType, Vilkår::getGjeldendeVilkårUtfall));

        var matchendeVilkår = vilkårTyper.get(vdRegelmotor.vilkårType());
        Objects.requireNonNull(matchendeVilkår, "skal finnes match"); //$NON-NLS-1$
        // Utfall fra automatisk regelvurdering skal legges til settet av utfall, dersom vilkår ikke er manuelt vurdert
        if (!(matchendeVilkår.erManueltVurdert() || matchendeVilkår.erOverstyrt())) {
            vilkårUtfall.put(vdRegelmotor.vilkårType(), vdRegelmotor.utfallType());
        }

        return new HashSet<>(vilkårUtfall.values());
    }

    private VilkårData kjørRegelmotor(BehandlingReferanse ref, Vilkår vilkår) {
        return vurderVilkår(vilkår.getVilkårType(), ref);
    }

    private void oppdaterBehandlingMedVilkårresultat(Behandling behandling, VilkårResultatType inngangsvilkårUtfall) {
        var builder = VilkårResultat
            .builderFraEksisterende(inngangsvilkårTjeneste.getBehandlingsresultat(behandling.getId()).getVilkårResultat())
            .medVilkårResultatType(inngangsvilkårUtfall);
        builder.buildFor(behandling);
    }

    private void oppdaterBehandlingMedVilkårresultat(Behandling behandling,
                                                     VilkårData vilkårData,
                                                     VilkårResultatType inngangsvilkårUtfall) {
        var merknadParametre = new Properties();
        if (vilkårData.merknadParametere() != null) {
            LAGRE_MERKNAD_PARAMETRE.getOrDefault(vilkårData.vilkårType(), Set.of()).stream()
                .filter(p -> vilkårData.merknadParametere().get(p) instanceof String)
                .forEach(p -> merknadParametre.setProperty(p , (String) vilkårData.merknadParametere().get(p)));
        }
        var builder = VilkårResultat
            .builderFraEksisterende(inngangsvilkårTjeneste.getBehandlingsresultat(behandling.getId()).getVilkårResultat())
            .medVilkårResultatType(inngangsvilkårUtfall);
        var vilkårBuilder = builder.getVilkårBuilderFor(vilkårData.vilkårType())
            .medVilkårUtfall(vilkårData.utfallType(), vilkårData.vilkårUtfallMerknad())
            .medMerknadParametere(merknadParametre)
            .medRegelEvaluering(vilkårData.regelEvaluering())
            .medRegelInput(vilkårData.regelInput());
        builder.leggTilVilkår(vilkårBuilder);

        builder.buildFor(behandling);
    }
}
