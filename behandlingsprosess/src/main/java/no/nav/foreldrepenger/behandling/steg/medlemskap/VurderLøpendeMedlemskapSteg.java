package no.nav.foreldrepenger.behandling.steg.medlemskap;

import java.util.Collections;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapVilkårPeriodeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.inngangsvilkaar.medlemskap.VurderLøpendeMedlemskap;

@BehandlingStegRef(kode = "VULOMED")
@BehandlingTypeRef("BT-004")
@FagsakYtelseTypeRef("FP")
@ApplicationScoped
public class VurderLøpendeMedlemskapSteg implements BehandlingSteg {

    private BehandlingsresultatRepository behandlingsresultatRepository;
    private VurderLøpendeMedlemskap vurderLøpendeMedlemskap;
    private MedlemskapVilkårPeriodeRepository medlemskapVilkårPeriodeRepository;
    private BehandlingRepository behandlingRepository;

    @Inject
    public VurderLøpendeMedlemskapSteg(VurderLøpendeMedlemskap vurderLøpendeMedlemskap,
            BehandlingRepositoryProvider provider) {
        this.vurderLøpendeMedlemskap = vurderLøpendeMedlemskap;
        this.medlemskapVilkårPeriodeRepository = provider.getMedlemskapVilkårPeriodeRepository();
        this.behandlingRepository = provider.getBehandlingRepository();
        this.behandlingsresultatRepository = provider.getBehandlingsresultatRepository();
    }

    VurderLøpendeMedlemskapSteg() {
        // CDI
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();
        if (skalVurdereLøpendeMedlemskap(behandlingId)) {
            var vurderingsTilDataMap = vurderLøpendeMedlemskap.vurderLøpendeMedlemskap(behandlingId);
            if (!vurderingsTilDataMap.isEmpty()) {
                var behandling = behandlingRepository.hentBehandling(behandlingId);

                var builder = medlemskapVilkårPeriodeRepository.hentBuilderFor(behandling);
                var perioderBuilder = builder.getPeriodeBuilder();

                vurderingsTilDataMap.forEach((vurderingsdato, vilkårData) -> {
                    var periodeBuilder = perioderBuilder.getBuilderForVurderingsdato(vurderingsdato);
                    periodeBuilder.medVurderingsdato(vurderingsdato);
                    periodeBuilder.medVilkårUtfall(vilkårData.getUtfallType());
                    Optional.ofNullable(vilkårData.getVilkårUtfallMerknad()).ifPresent(periodeBuilder::medVilkårUtfallMerknad);
                    perioderBuilder.leggTil(periodeBuilder);
                });
                builder.medMedlemskapsvilkårPeriode(perioderBuilder);
                medlemskapVilkårPeriodeRepository.lagreMedlemskapsvilkår(behandling, builder);

                var resultat = medlemskapVilkårPeriodeRepository.utledeVilkårStatus(behandling);
                var vilkårBuilder = VilkårResultat
                        .builderFraEksisterende(getBehandlingsresultat(behandlingId).getVilkårResultat());
                Avslagsårsak avslagsårsak = null;
                if (VilkårUtfallType.IKKE_OPPFYLT.equals(resultat.vilkårUtfallType())) {
                    avslagsårsak = Avslagsårsak.fraKode(resultat.vilkårUtfallMerknad().getKode());
                }
                vilkårBuilder.leggTilVilkårResultat(VilkårType.MEDLEMSKAPSVILKÅRET_LØPENDE, resultat.vilkårUtfallType(), resultat.vilkårUtfallMerknad(), null,
                        avslagsårsak, false, false, null, null);

                var lås = kontekst.getSkriveLås();
                behandlingRepository.lagre(vilkårBuilder.buildFor(behandling), lås);
            }
        }
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private boolean skalVurdereLøpendeMedlemskap(Long behandlingId) {
        var behandlingsresultat = Optional.ofNullable(getBehandlingsresultat(behandlingId));
        return behandlingsresultat.map(b -> b.getVilkårResultat().getVilkårene()).orElse(Collections.emptyList())
                .stream()
                .anyMatch(v -> v.getVilkårType().equals(VilkårType.MEDLEMSKAPSVILKÅRET)
                        && v.getGjeldendeVilkårUtfall().equals(VilkårUtfallType.OPPFYLT));
    }

    private Behandlingsresultat getBehandlingsresultat(Long behandlingId) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandlingId).orElse(null);
    }
}
