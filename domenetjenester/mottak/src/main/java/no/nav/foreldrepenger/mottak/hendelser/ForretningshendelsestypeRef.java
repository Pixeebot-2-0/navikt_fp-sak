package no.nav.foreldrepenger.mottak.hendelser;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;

import no.nav.foreldrepenger.behandlingslager.hendelser.ForretningshendelseType;

/**
 * Marker type som implementerer interface {@link ForretningshendelseHåndterer}.
 */
@Qualifier
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Documented
public @interface ForretningshendelsestypeRef {

    String FØDSEL_HENDELSE = "FØDSEL";
    String DØD_HENDELSE = "DØD";
    String DØDFØDSEL_HENDELSE = "DØDFØDSEL";

    /**
     * Settes til navn på forretningshendelse slik det defineres i KODELISTE-tabellen, eller til YTELSE_HENDELSE
     */
    String value();

    /**
     * AnnotationLiteral som kan brukes ved CDI søk.
     */
    class ForretningshendelsestypeRefLiteral extends AnnotationLiteral<ForretningshendelsestypeRef> implements ForretningshendelsestypeRef {

        private String navn;

        public ForretningshendelsestypeRefLiteral(ForretningshendelseType forretningshendelseType) {
            this.navn = forretningshendelseType.getKode();

        }

        @Override
        public String value() {
            return navn;
        }
    }
}

