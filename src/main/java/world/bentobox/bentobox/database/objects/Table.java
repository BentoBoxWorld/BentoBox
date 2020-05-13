package world.bentobox.bentobox.database.objects;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(TYPE)
/**
 * Annotation to explicitly name tables
 * @author tastybento
 * @since 1.14.0
 */
public @interface Table {
    /**
     * @return name of the table to be used in the database
     */
    String name();

}
