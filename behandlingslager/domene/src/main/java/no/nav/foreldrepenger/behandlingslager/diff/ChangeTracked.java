package no.nav.foreldrepenger.behandlingslager.diff;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Markerer at et felt i en entitet skal sjekkes for endringer når registerdata blir hentet inn på nytt (PK-41326).
 *
 * Default vil annotasjonen gjelde både Engangsstønad og Foreldrepenger,
 * men det er mulig å overstyre dette når den bare skal gjelde for én av dem.
 */
@Target(FIELD)
@Retention(RUNTIME)
public @interface ChangeTracked {
}
