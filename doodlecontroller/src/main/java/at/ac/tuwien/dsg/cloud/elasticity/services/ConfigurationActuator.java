package at.ac.tuwien.dsg.cloud.elasticity.services;

import at.ac.tuwien.dsg.cloud.data.DynamicServiceDescription;

public interface ConfigurationActuator {

	public void actuate(DynamicServiceDescription currentConfiguration, DynamicServiceDescription targetConfiguration);
}
