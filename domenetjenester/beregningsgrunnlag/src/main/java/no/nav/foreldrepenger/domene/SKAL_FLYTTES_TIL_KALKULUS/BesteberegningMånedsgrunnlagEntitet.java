package no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import com.fasterxml.jackson.annotation.JsonBackReference;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

@Entity(name = "BesteberegningMånedsgrunnlagEntitet")
@Table(name = "BG_BESTEBEREGNING_MAANED")
public class BesteberegningMånedsgrunnlagEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BESTEBEREGNING_MAANED")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @JsonBackReference
    @OneToOne
    @JoinColumn(name = "besteberegninggrunnlag_id", updatable = false, unique = true)
    private BesteberegninggrunnlagEntitet besteberegninggrunnlag;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "besteberegningMåned", cascade = CascadeType.PERSIST)
    private List<BesteberegningInntektEntitet> inntekter = new ArrayList<>();

    @Embedded
    private DatoIntervallEntitet periode;


    public BesteberegningMånedsgrunnlagEntitet() {
    }

    public BesteberegningMånedsgrunnlagEntitet(BesteberegningMånedsgrunnlagEntitet besteberegningMånedsgrunnlagEntitet) {
        this.periode = besteberegningMånedsgrunnlagEntitet.getPeriode();
        besteberegningMånedsgrunnlagEntitet.getInntekter().stream()
            .map(BesteberegningInntektEntitet::new)
            .forEach(this::leggTilInntekt);
    }

    public Long getId() {
        return id;
    }

    public long getVersjon() {
        return versjon;
    }

    public BesteberegninggrunnlagEntitet getBesteberegninggrunnlag() {
        return besteberegninggrunnlag;
    }

    public List<BesteberegningInntektEntitet> getInntekter() {
        return inntekter;
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    void setBesteberegninggrunnlag(BesteberegninggrunnlagEntitet besteberegninggrunnlag) {
        this.besteberegninggrunnlag = besteberegninggrunnlag;
    }

    private void leggTilInntekt(BesteberegningInntektEntitet besteberegningInntektEntitet) {
        besteberegningInntektEntitet.setBesteberegningMåned(this);
        this.inntekter.add(besteberegningInntektEntitet);
    }

    public static Builder ny() {
        return new Builder();
    }

    public static class Builder {
        private final BesteberegningMånedsgrunnlagEntitet kladd;

        public Builder() {
            kladd = new BesteberegningMånedsgrunnlagEntitet();
        }

        public Builder(BesteberegningMånedsgrunnlagEntitet eksisterendeMånedsgrunnlag, boolean erOppdatering) {
            if (Objects.nonNull(eksisterendeMånedsgrunnlag.getId())) {
                throw new IllegalArgumentException("Kan ikke bygge på et lagret grunnlag");
            }
            if (erOppdatering) {
                kladd = eksisterendeMånedsgrunnlag;
            } else {
                kladd = new BesteberegningMånedsgrunnlagEntitet(eksisterendeMånedsgrunnlag);
            }
        }

        public Builder leggTilInntekt(BesteberegningInntektEntitet besteberegningInntektEntitet) {
            kladd.leggTilInntekt(besteberegningInntektEntitet);
            return this;
        }

        public Builder medPeriode(LocalDate fom, LocalDate tom) {
            kladd.periode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
            return this;
        }

        public BesteberegningMånedsgrunnlagEntitet build() {
            Objects.requireNonNull(kladd.periode);
            return kladd;
        }

    }


}
