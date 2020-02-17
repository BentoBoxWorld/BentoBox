package world.bentobox.bentobox.api.configuration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Poslovitch, tastybento
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigEntry {

    String path();
    String since() default "1.0";
    boolean overrideOnChange() default false;
    boolean experimental() default false;
    boolean needsReset() default false;

    /**
     * Sets whether this config entry should be printed in the final config file or not.
     * @return {@code true} if this config entry should be printed in the final config file, {@code false} otherwise.
     */
    boolean hidden() default false;

    /**
     * Sets a link to a video explaining this configuration option.
     * @return the link to a video explaining this configuration option.
     * @since 1.5.3
     */
    String video() default "";

    /**
     * Sets whether this config entry requires restarting the server in order to take changes into account.
     * @return {@code true} if this config entry requires restarting the server in order to take changes into account, {@code false} otherwise.
     * @since 1.12.0
     */
    boolean needsRestart() default false;
}