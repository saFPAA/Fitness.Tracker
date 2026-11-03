package com.Author.Fitness.Exception.Validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = MaxUtf8BytesValidator.class)
@Target({ FIELD, PARAMETER, ANNOTATION_TYPE, RECORD_COMPONENT })
@Retention(RUNTIME)
public @interface MaxUtf8Bytes {
    String message() default "Value exceeds the maximum UTF-8 byte length";

    int value();

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}