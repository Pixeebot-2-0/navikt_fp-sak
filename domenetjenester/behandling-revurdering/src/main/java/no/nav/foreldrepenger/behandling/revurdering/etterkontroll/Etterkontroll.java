package no.nav.foreldrepenger.behandling.revurdering.etterkontroll;

import java.time.LocalDateTime;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.DiffIgnore;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

@Entity(name = "Etterkontroll")
@Table(name = "ETTERKONTROLL")
public class Etterkontroll extends BaseEntitet {

    @Id
    @SequenceGenerator(name = "etterkontroll_sekvens", sequenceName = "SEQ_ETTERKONTROLL")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "etterkontroll_sekvens")
    private Long id;

    @Column(name = "fagsak_id", nullable = false, updatable = false)
    private Long fagsakId;

    @DiffIgnore
    @Column(name = "kontroll_tid", nullable = false)
    private LocalDateTime kontrollTidspunkt; // NOSONAR

    @Convert(converter = KontrollType.KodeverdiConverter.class)
    @Column(name = "kontroll_type", nullable = false)
    private KontrollType kontrollType;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "behandlet", nullable = false)
    private boolean erBehandlet = false;

    Etterkontroll() {
        // hibernarium
    }

    public Long getId() {
        return id;
    }

    public void setErBehandlet(boolean erBehandlet) {
        this.erBehandlet = erBehandlet;
    }

    public boolean isBehandlet() {
        return erBehandlet;
    }

    public void setKontrollTidspunktt(LocalDateTime kontrollTidspunkt) {
        this.kontrollTidspunkt = kontrollTidspunkt;
    }

    public static class Builder {
        private Etterkontroll etterkontrollKladd;

        public Builder(Long fagsakId) {
            Objects.requireNonNull(fagsakId, "fagsakId");
            etterkontrollKladd = new Etterkontroll();
            this.etterkontrollKladd.fagsakId = fagsakId;
        }

        public Etterkontroll build() {

            return etterkontrollKladd;
        }

        public Builder medKontrollType(KontrollType kontrollType) {
            this.etterkontrollKladd.kontrollType = kontrollType;
            return this;
        }

        public Builder medErBehandlet(boolean erBehandlet) {
            this.etterkontrollKladd.erBehandlet = erBehandlet;
            return this;
        }

        public Builder medKontrollTidspunkt(LocalDateTime kontrollTidspunkt) {
            this.etterkontrollKladd.kontrollTidspunkt = kontrollTidspunkt;
            return this;
        }

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Etterkontroll)) {
            return false;
        }
        var that = (Etterkontroll) o;
        return Objects.equals(fagsakId, that.fagsakId) &&
                Objects.equals(kontrollType, that.kontrollType);

    }

    @Override
    public int hashCode() {
        return Objects.hash(fagsakId, kontrollType);
    }
}
