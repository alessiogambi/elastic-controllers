package at.ac.tuwien.dsg.cloud.elasticity.validators;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import at.ac.tuwien.dsg.cloud.elasticity.annotations.ValidControllerName;
import at.ac.tuwien.dsg.cloud.elasticity.services.ControllerSource;

public class ControllerNameValidator implements
		ConstraintValidator<ValidControllerName, String> {

	// THis is kind of naive, it's just a place where to store names for
	// contributed controllers :)
	private ControllerSource controllerSource;

	public ControllerNameValidator(ControllerSource controllerSource) {
		this.controllerSource = controllerSource;
	}

	public void initialize(ValidControllerName annotation) {
	}

	public boolean isValid(String controllerName,
			ConstraintValidatorContext arg1) {
		if (controllerName == null) {
			return true;
		}
		try {
			return controllerSource.get(controllerName) != null;
		} catch (Throwable e) {
			return false;
		}
	}
}
