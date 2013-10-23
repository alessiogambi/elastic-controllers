package at.ac.tuwien.dsg.cloud.elasticity.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import at.ac.tuwien.dsg.cloud.elasticity.validators.FQNValidator;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Constraint(validatedBy = FQNValidator.class)
public @interface ValidFQN {
	public abstract String message() default "The provided serviceFQN is not valid";

	public abstract Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
