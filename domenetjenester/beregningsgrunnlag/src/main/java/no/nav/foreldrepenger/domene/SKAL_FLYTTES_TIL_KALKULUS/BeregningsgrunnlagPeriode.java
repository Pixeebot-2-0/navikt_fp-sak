package no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS;

import static no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPeriodeRegelType.FASTSETT;
import static no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPeriodeRegelType.FINN_GRENSEVERDI;
import static no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPeriodeRegelType.FORDEL;
import static no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPeriodeRegelType.FORESLÅ;
import static no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPeriodeRegelType.OPPDATER_GRUNNLAG_SVP;
import static no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPeriodeRegelType.VILKÅR_VURDERING;
import static no.nav.vedtak.konfig.Tid.TIDENES_ENDE;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

import com.fasterxml.jackson.annotation.JsonBackReference;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet;

@Entity(name = "BeregningsgrunnlagPeriode")
@Table(name = "BEREGNINGSGRUNNLAG_PERIODE")
public class BeregningsgrunnlagPeriode extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BG_PERIODE")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @JsonBackReference
    @ManyToOne(optional = false)
    @JoinColumn(name = "beregningsgrunnlag_id", nullable = false, updatable = false)
    private BeregningsgrunnlagEntitet beregningsgrunnlag;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "beregningsgrunnlagPeriode", cascade = CascadeType.PERSIST, orphanRemoval = true)
    private List<BeregningsgrunnlagPrStatusOgAndel> beregningsgrunnlagPrStatusOgAndelList = new ArrayList<>();

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "fomDato", column = @Column(name = "bg_periode_fom")),
        @AttributeOverride(name = "tomDato", column = @Column(name = "bg_periode_tom"))
    })
    private ÅpenDatoIntervallEntitet periode;

    @Column(name = "brutto_pr_aar")
    private BigDecimal bruttoPrÅr;

    @Column(name = "avkortet_pr_aar")
    private BigDecimal avkortetPrÅr;

    @Column(name = "redusert_pr_aar")
    private BigDecimal redusertPrÅr;

    @Column(name = "dagsats")
    private Long dagsats;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "beregningsgrunnlagPeriode", cascade = CascadeType.PERSIST, orphanRemoval = true)
    @MapKey(name = "regelType")
    private Map<BeregningsgrunnlagPeriodeRegelType, BeregningsgrunnlagPeriodeRegelSporing> regelSporingMap = new HashMap<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "beregningsgrunnlagPeriode", cascade = CascadeType.PERSIST, orphanRemoval = true)
    private List<BeregningsgrunnlagPeriodeÅrsak> beregningsgrunnlagPeriodeÅrsaker = new ArrayList<>();

    public BeregningsgrunnlagPeriode(BeregningsgrunnlagPeriode beregningsgrunnlagPeriode) {
        this.avkortetPrÅr = beregningsgrunnlagPeriode.getAvkortetPrÅr();
        this.bruttoPrÅr = beregningsgrunnlagPeriode.getBruttoPrÅr();
        this.dagsats = beregningsgrunnlagPeriode.getDagsats();
        this.periode = beregningsgrunnlagPeriode.getPeriode();
        this.redusertPrÅr = beregningsgrunnlagPeriode.getRedusertPrÅr();
        beregningsgrunnlagPeriode.getRegelSporinger().values().stream().map(BeregningsgrunnlagPeriodeRegelSporing::new)
            .forEach(this::leggTilBeregningsgrunnlagPeriodeRegel);
        beregningsgrunnlagPeriode.getBeregningsgrunnlagPeriodeÅrsaker().stream().map(BeregningsgrunnlagPeriodeÅrsak::new)
            .forEach(this::addBeregningsgrunnlagPeriodeÅrsak);
        beregningsgrunnlagPeriode.getBeregningsgrunnlagPrStatusOgAndelList().stream().map(BeregningsgrunnlagPrStatusOgAndel::new)
            .forEach(this::addBeregningsgrunnlagPrStatusOgAndel);
    }

    private BeregningsgrunnlagPeriode() { }

    public Long getId() {
        return id;
    }

    public BeregningsgrunnlagEntitet getBeregningsgrunnlag() {
        return beregningsgrunnlag;
    }

    public List<BeregningsgrunnlagPrStatusOgAndel> getBeregningsgrunnlagPrStatusOgAndelList() {
        return Collections.unmodifiableList(beregningsgrunnlagPrStatusOgAndelList);
    }

    public ÅpenDatoIntervallEntitet getPeriode() {
        if (periode.getTomDato() == null) {
            return ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(periode.getFomDato(), TIDENES_ENDE);
        }
        return periode;
    }

    public LocalDate getBeregningsgrunnlagPeriodeFom() {
        return periode.getFomDato();
    }

    public LocalDate getBeregningsgrunnlagPeriodeTom() {
        return periode.getTomDato();
    }

    public BigDecimal getBeregnetPrÅr() {
        return beregningsgrunnlagPrStatusOgAndelList.stream()
                .filter(bgpsa -> bgpsa.getBeregnetPrÅr() != null)
                .map(BeregningsgrunnlagPrStatusOgAndel::getBeregnetPrÅr)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);
    }

    void updateBruttoPrÅr() {
        bruttoPrÅr = beregningsgrunnlagPrStatusOgAndelList.stream()
                .filter(bgpsa -> bgpsa.getBruttoPrÅr() != null)
                .map(BeregningsgrunnlagPrStatusOgAndel::getBruttoPrÅr)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);
    }

    public BigDecimal getBruttoPrÅr() {
        return bruttoPrÅr;
    }

    public BigDecimal getAvkortetPrÅr() {
        return avkortetPrÅr;
    }

    public BigDecimal getRedusertPrÅr() {
        return redusertPrÅr;
    }

    public Long getDagsats() {
        return dagsats;
    }

    public List<BeregningsgrunnlagPeriodeÅrsak> getBeregningsgrunnlagPeriodeÅrsaker() {
        return Collections.unmodifiableList(beregningsgrunnlagPeriodeÅrsaker);
    }

    public List<PeriodeÅrsak> getPeriodeÅrsaker() {
        return beregningsgrunnlagPeriodeÅrsaker.stream().map(BeregningsgrunnlagPeriodeÅrsak::getPeriodeÅrsak).collect(Collectors.toList());
    }

    void addBeregningsgrunnlagPrStatusOgAndel(BeregningsgrunnlagPrStatusOgAndel bgPrStatusOgAndel) {
        Objects.requireNonNull(bgPrStatusOgAndel, "beregningsgrunnlagPrStatusOgAndel");
        if (!beregningsgrunnlagPrStatusOgAndelList.contains(bgPrStatusOgAndel)) { // NOSONAR Class defines List based fields but uses them like Sets: Ingening å tjene på å bytte til Set ettersom det er små lister
            bgPrStatusOgAndel.setBeregningsgrunnlagPeriode(this);
            beregningsgrunnlagPrStatusOgAndelList.add(bgPrStatusOgAndel);
        }
    }

    void addBeregningsgrunnlagPeriodeÅrsak(BeregningsgrunnlagPeriodeÅrsak bgPeriodeÅrsak) {
        Objects.requireNonNull(bgPeriodeÅrsak, "beregningsgrunnlagPeriodeÅrsak");
        if (!beregningsgrunnlagPeriodeÅrsaker.contains(bgPeriodeÅrsak)) { // NOSONAR Class defines List based fields but uses them like Sets: Ingening å tjene på å bytte til Set ettersom det er små lister
            bgPeriodeÅrsak.setBeregningsgrunnlagPeriode(this);
            beregningsgrunnlagPeriodeÅrsaker.add(bgPeriodeÅrsak);
        }
    }

    void leggTilBeregningsgrunnlagPeriodeRegel(BeregningsgrunnlagPeriodeRegelSporing beregningsgrunnlagPeriodeRegelSporing) {
        Objects.requireNonNull(beregningsgrunnlagPeriodeRegelSporing, "beregningsgrunnlagPeriodeRegelSporing");
        beregningsgrunnlagPeriodeRegelSporing.setBeregningsgrunnlagPeriode(this);
        regelSporingMap.put(beregningsgrunnlagPeriodeRegelSporing.getRegelType(), beregningsgrunnlagPeriodeRegelSporing);
    }

    void setBeregningsgrunnlag(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        this.beregningsgrunnlag = beregningsgrunnlag;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof BeregningsgrunnlagPeriode)) {
            return false;
        }
        BeregningsgrunnlagPeriode other = (BeregningsgrunnlagPeriode) obj;
        return Objects.equals(this.periode.getFomDato(), other.periode.getFomDato())
                && Objects.equals(this.periode.getTomDato(), other.periode.getTomDato())
                && Objects.equals(this.getBruttoPrÅr(), other.getBruttoPrÅr())
                && Objects.equals(this.getAvkortetPrÅr(), other.getAvkortetPrÅr())
                && Objects.equals(this.getRedusertPrÅr(), other.getRedusertPrÅr())
                && Objects.equals(this.getDagsats(), other.getDagsats());
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode, bruttoPrÅr, avkortetPrÅr, redusertPrÅr, dagsats);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + //$NON-NLS-1$
                "id=" + id + ", " //$NON-NLS-2$
                + "periode=" + periode + ", " // $NON-NLS-1$ //$NON-NLS-2$
                + "bruttoPrÅr=" + bruttoPrÅr + ", " //$NON-NLS-1$ //$NON-NLS-2$
                + "avkortetPrÅr=" + avkortetPrÅr + ", " //$NON-NLS-1$ //$NON-NLS-2$
                + "redusertPrÅr=" + redusertPrÅr + ", " //$NON-NLS-1$ //$NON-NLS-2$
                + "dagsats=" + dagsats + ", " //$NON-NLS-1$ //$NON-NLS-2$
                + ">"; //$NON-NLS-1$
    }

    public static Builder ny() {
        return new Builder();
    }

    public static Builder oppdater(BeregningsgrunnlagPeriode eksisterendeBeregningsgrunnlagPeriode) {
        return new Builder(eksisterendeBeregningsgrunnlagPeriode, true);
    }

    public String getRegelEvalueringForeslå() {
        return regelSporingMap.containsKey(FORESLÅ) ?  regelSporingMap.get(FORESLÅ).getRegelEvaluering() : null;
    }

    public String getRegelEvalueringFastsett() {
        return regelSporingMap.containsKey(FASTSETT) ?  regelSporingMap.get(FASTSETT).getRegelEvaluering() : null;
    }

    public String getRegelEvalueringFordel() {
        return regelSporingMap.containsKey(FORDEL) ?  regelSporingMap.get(FORDEL).getRegelEvaluering() : null;
    }

    public String getRegelInputForeslå() {
        return regelSporingMap.containsKey(FORESLÅ)  ? regelSporingMap.get(FORESLÅ).getRegelInput() : null;
    }

    public String getRegelInputFordel() {
        return regelSporingMap.containsKey(FORDEL)  ? regelSporingMap.get(FORDEL).getRegelInput() : null;
    }

    public String getRegelInputFastsett() {
        return regelSporingMap.containsKey(FASTSETT) ? regelSporingMap.get(FASTSETT).getRegelInput() : null;
    }

    public String getRegelInputFinnGrenseverdi() {
        return regelSporingMap.containsKey(FINN_GRENSEVERDI) ? regelSporingMap.get(FINN_GRENSEVERDI).getRegelInput() : null;
    }

    public String getRegelInputVilkårvurdering() {
        return regelSporingMap.containsKey(VILKÅR_VURDERING) ?  regelSporingMap.get(VILKÅR_VURDERING).getRegelInput() : null;
    }

    public String getRegelEvalueringVilkårvurdering() {
        return regelSporingMap.containsKey(VILKÅR_VURDERING) ?  regelSporingMap.get(VILKÅR_VURDERING).getRegelEvaluering() : null;
    }

    public String getRegelInputOppdatereGrunnlagSVP() {
        return regelSporingMap.containsKey(OPPDATER_GRUNNLAG_SVP) ?  regelSporingMap.get(OPPDATER_GRUNNLAG_SVP).getRegelInput() : null;
    }

    public String getRegelEvalueringFinnGrenseverdi() {
        return regelSporingMap.containsKey(FINN_GRENSEVERDI) ? regelSporingMap.get(FINN_GRENSEVERDI).getRegelEvaluering() : null;
    }

    public Map<BeregningsgrunnlagPeriodeRegelType, BeregningsgrunnlagPeriodeRegelSporing> getRegelSporinger() {
        return regelSporingMap;
    }

    public static class Builder {
        private BeregningsgrunnlagPeriode kladd;
        private boolean built;

        public Builder() {
            kladd = new BeregningsgrunnlagPeriode();
        }

        public Builder(BeregningsgrunnlagPeriode eksisterendeBeregningsgrunnlagPeriod, boolean erOppdatering) {
            if (Objects.nonNull(eksisterendeBeregningsgrunnlagPeriod.getId())) {
                throw new IllegalArgumentException("Kan ikke bygge på et lagret grunnlag");
            }
            if (erOppdatering) {
                kladd = eksisterendeBeregningsgrunnlagPeriod;
            } else {
                kladd = new BeregningsgrunnlagPeriode(eksisterendeBeregningsgrunnlagPeriod);
            }
        }

        public Builder leggTilBeregningsgrunnlagPrStatusOgAndel(BeregningsgrunnlagPrStatusOgAndel.Builder prStatusOgAndelBuilder) {
            verifiserKanModifisere();
            prStatusOgAndelBuilder.build(kladd);
            return this;
        }

        public Builder medBeregningsgrunnlagPeriode(LocalDate fraOgMed, LocalDate tilOgMed) {
            verifiserKanModifisere();
            kladd.periode = ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(fraOgMed, tilOgMed);
            return this;
        }

        public Builder medBruttoPrÅr(BigDecimal bruttoPrÅr) {
            verifiserKanModifisere();
            kladd.bruttoPrÅr = bruttoPrÅr;
            return this;
        }

        public Builder medAvkortetPrÅr(BigDecimal avkortetPrÅr) {
            verifiserKanModifisere();
            kladd.avkortetPrÅr = avkortetPrÅr;
            return this;
        }

        public Builder medRedusertPrÅr(BigDecimal redusertPrÅr) {
            verifiserKanModifisere();
            kladd.redusertPrÅr = redusertPrÅr;
            return this;
        }

        public Builder medRegelEvaluering(String regelInput, String regelEvaluering, BeregningsgrunnlagPeriodeRegelType regelType) {
            verifiserKanModifisere();
            BeregningsgrunnlagPeriodeRegelSporing.ny()
                .medRegelInput(regelInput)
                .medRegelEvaluering(regelEvaluering)
                .medRegelType(regelType)
                .build(kladd);
            return this;
        }

        public Builder leggTilPeriodeÅrsak(PeriodeÅrsak periodeÅrsak) {
            verifiserKanModifisere();
            if (!kladd.getPeriodeÅrsaker().contains(periodeÅrsak)) {
                BeregningsgrunnlagPeriodeÅrsak.Builder bgPeriodeÅrsakBuilder = new BeregningsgrunnlagPeriodeÅrsak.Builder();
                bgPeriodeÅrsakBuilder.medPeriodeÅrsak(periodeÅrsak);
                bgPeriodeÅrsakBuilder.build(kladd);
            }
            return this;
        }

        public BeregningsgrunnlagPeriode build(BeregningsgrunnlagEntitet beregningsgrunnlag) {
            verifyStateForBuild();
            beregningsgrunnlag.leggTilBeregningsgrunnlagPeriode(kladd);

            Long dagsatsSum = kladd.beregningsgrunnlagPrStatusOgAndelList.stream()
                .filter(bgpsa -> bgpsa.getDagsats() != null)
                .map(BeregningsgrunnlagPrStatusOgAndel::getDagsats)
                .reduce(Long::sum)
                .orElse(null);
            kladd.dagsats = dagsatsSum;
            built = true;
            return kladd;
        }

        private void verifiserKanModifisere() {
            if(built) {
                throw new IllegalStateException("Er allerede bygd, kan ikke oppdatere videre: " + this.kladd);
            }
        }

        private void verifyStateForBuild() {
            Objects.requireNonNull(kladd.periode, "beregningsgrunnlagPeriodeFom");
        }
    }
}
