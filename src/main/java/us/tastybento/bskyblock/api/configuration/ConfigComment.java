package us.tastybento.bskyblock.api.configuration;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Repeatable(ConfigComment.Line.class)
@Target({ FIELD, METHOD })
public @interface ConfigComment {
    
    String value();
    
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ FIELD, METHOD })
    @interface Line {
        ConfigComment[] value();
    }

}
