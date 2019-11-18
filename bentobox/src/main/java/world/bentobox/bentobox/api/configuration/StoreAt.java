package world.bentobox.bentobox.api.configuration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines where this data object will be stored.
 * @author tastybento
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface StoreAt {

    /**
     * Path where this will be stored. If blank, it will be the database folder.
     */
    String path() default "";

    /**
     * Filename
     */
    String filename() default "";

}
