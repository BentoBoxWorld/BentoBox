package world.bentobox.bentobox.api.configuration;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Repeatable(ConfigComment.Line.class)
@Target({ FIELD, METHOD, TYPE })
public @interface ConfigComment {

    String value();

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ FIELD, METHOD, TYPE })
    @interface Line {
        ConfigComment[] value();
    }

}
