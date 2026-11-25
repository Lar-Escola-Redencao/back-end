package br.org.larescolaredencao.validacao;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.validator.constraints.CompositionType;
import org.hibernate.validator.constraints.ConstraintComposition;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.Email;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Target({FIELD, PARAMETER, RECORD_COMPONENT})
@Retention(RUNTIME)
@Constraint(validatedBy = {})
@ConstraintComposition(CompositionType.OR)
@Email
@CpfValido
@ReportAsSingleViolation
public @interface EmailOuCpf {
    String message() default "Informe um e-mail ou CPF válido.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
