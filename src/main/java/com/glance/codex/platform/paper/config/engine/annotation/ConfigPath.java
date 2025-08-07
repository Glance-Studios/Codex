package com.glance.codex.platform.paper.config.engine.annotation;

import com.glance.codex.utils.data.NoOpConverter;
import com.glance.codex.utils.data.NoOpValidator;
import com.glance.codex.utils.data.TypeConverter;
import com.glance.codex.utils.data.Validator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigPath {

    /** path within the section */
    String value();

    /** optional inline comments to write if underlying {@link Config} componentMeta allows defaults */
    String[] comments() default {};

    /**
     * optional validator class to enforce constraints
     * <p>
     * Must have a public no-arg constructor
     */
    Class<? extends Validator<?>> validator() default NoOpValidator.class;

    /**
     * optional hook for custom codec logic
     * <p>
     * Must have a public no-arg constructor
     */
    Class<? extends TypeConverter<?>> converter() default NoOpConverter.class;

    /**
     * Whether this field should write its value to disk as a default
     * <p>
     * If false, the field is loaded but not written on save if config file is absent
     */
    boolean writeDefault() default true;

}
