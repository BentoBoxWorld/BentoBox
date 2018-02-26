/**
 * 
 */
package us.tastybento.bskyblock.api.configuration;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target({ FIELD, METHOD })
/**
 * @author tastybento
 *
 */
public @interface ConfigComment {

    String value();

}
