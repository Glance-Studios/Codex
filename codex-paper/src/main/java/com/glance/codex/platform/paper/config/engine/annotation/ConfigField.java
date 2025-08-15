package com.glance.codex.platform.paper.config.engine.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigField {
    /** controls codec order within a bean */
    int order() default Integer.MAX_VALUE;
    /** inline comments when dumping this field */
    String[] comments() default {};
}
