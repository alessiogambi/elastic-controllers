package at.ac.tuwien.dsg.cloud.elasticity.services;

import at.ac.tuwien.dsg.cloud.data.DynamicServiceDescription;

/**
 * This service takes care of actuating the targetConfiguration of a service in
 * the Cloud.
 * 
 * @author alessiogambi
 * 
 */
public interface ConfigurationActuator {

	public void actuate(DynamicServiceDescription configuration);
}
