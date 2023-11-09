package no.nav.foreldrepenger.økonomistøtte.queue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

/**
 * se {@link ØkonomiImplementasjonVelger}
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
public @interface TestOnlyMqDisabled {
    class TestOnlyMqDisabledLiteral extends AnnotationLiteral<TestOnlyMqDisabled> implements TestOnlyMqDisabled {
    }
}
