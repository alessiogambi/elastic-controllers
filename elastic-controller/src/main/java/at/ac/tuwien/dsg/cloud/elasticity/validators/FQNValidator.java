package at.ac.tuwien.dsg.cloud.elasticity.validators;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.UnexpectedTypeException;

import at.ac.tuwien.dsg.cloud.elasticity.annotations.ValidFQN;
import ch.usi.cloud.controller.common.naming.FQN;
import ch.usi.cloud.controller.common.naming.FQNType;

public class FQNValidator implements ConstraintValidator<ValidFQN, Object> {

	public void initialize(ValidFQN annotation) {
	}

	public boolean isValid(String _serviceFQN) {
		try {
			FQN serviceFQN = new FQN(_serviceFQN, FQNType.SERVICE);
			return serviceFQN.toString().equals(
					serviceFQN.getServiceFQN(_serviceFQN));
		} catch (Throwable e) {
			return false;
		}
	}

	public boolean isValid(FQN serviceFQN) {
		try {
			// A valid serviceFQN does not have VEE
			return (serviceFQN.getVeeName() == null);
		} catch (Throwable e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean isValid(Object _serviceFQN,
			ConstraintValidatorContext validationContext) {

		if (_serviceFQN == null) {
			return true;
		} else {
			if (_serviceFQN instanceof FQN) {
				return isValid((FQN) _serviceFQN);
			} else if (_serviceFQN instanceof String) {
				return isValid((String) _serviceFQN);
			} else {
				throw new UnexpectedTypeException(
						"No validator could be found for type: "
								+ _serviceFQN.getClass().getName());

			}
		}
	}
}
