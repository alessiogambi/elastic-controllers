package at.ac.tuwien.dsg.cloud.elasticity.services.impl.configurationactuators;

import at.ac.tuwien.dsg.cloud.data.DynamicServiceDescription;
import at.ac.tuwien.dsg.cloud.elasticity.services.ConfigurationActuator;

/**
 * This class contains the logic that implements a basic form of
 * BlockingConfigurationActuator
 * 
 * @author alessiogambi
 * 
 */
public abstract class BlockingConfigurationActuator implements
		ConfigurationActuator {

	public abstract void actuateTheConfiguration(
			DynamicServiceDescription currentConfiguration,
			DynamicServiceDescription targetConfiguration);

	@Override
	public final void actuate(DynamicServiceDescription currentConfiguration,
			DynamicServiceDescription targetConfiguration) {

		actuateTheConfiguration(currentConfiguration, targetConfiguration);
	}

}
