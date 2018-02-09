package us.tastybento.bskyblock.api.configuration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import us.tastybento.bskyblock.Constants.GameType;

/**
 *
 *
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
    GameType specificTo() default GameType.BOTH;

}