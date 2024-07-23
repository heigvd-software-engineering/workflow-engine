package com.heig.entities.documentation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to dynamically document a method or a class
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Document {
    /**
     * The comment to annotate the method / type with
     * @return The comment to annotate the method / type with
     */
    String value() default "";
}
