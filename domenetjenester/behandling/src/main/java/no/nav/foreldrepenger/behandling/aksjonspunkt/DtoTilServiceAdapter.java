package no.nav.foreldrepenger.behandling.aksjonspunkt;

import jakarta.enterprise.inject.Stereotype;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter.ContainerOfDtoTilServiceAdapter;

import java.lang.annotation.*;

/**
 * Marker type definerer adapter for å transformere en Dto til et tjenestekall.
 * <p>
 * Beans bør være @ApplicationScoped eller @RequestScoped slik at de ikke
 * trenger å destroyes etter oppslag.
 */
@Repeatable(ContainerOfDtoTilServiceAdapter.class)
@Qualifier
@Stereotype
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
@Documented
public @interface DtoTilServiceAdapter {

    /**
     * Identifiserer Dto denne adapteren håndterer
     */
    Class<?> dto();

    /**
     * Identifiserer adapter som håndterer DTO
     */
    Class<?> adapter();

    /** For søk på annotation. */
    class Literal extends AnnotationLiteral<DtoTilServiceAdapter> implements DtoTilServiceAdapter {

        private Class<?> dto;
        private Class<?> adapter;

        public Literal(Class<?> dto, Class<?> adapter) {
            this.dto = dto;
            this.adapter = adapter;

        }

        @Override
        public Class<?> dto() {
            return dto;
        }

        @Override
        public Class<?> adapter() {
            return adapter;
        }

    }

    /**
     * container for repeatable annotations.
     *
     * @see https://docs.oracle.com/javase/tutorial/java/annotations/repeating.html
     */
    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE })
    @Documented
    @interface ContainerOfDtoTilServiceAdapter {
        DtoTilServiceAdapter[] value();
    }
}
