package at.ac.tuwien.dsg.cloud.elasticity.services;

import at.ac.tuwien.dsg.cloud.data.DynamicServiceDescription;

/**
 * This service is in charge of computing the configuration that a service in
 * the cloud most have at any given time. In other terms, services implementing
 * this interface will contain the <i>control logic</i>
 * 
 * @author alessiogambi
 * 
 */
public interface ConfigurationSelector {

	public DynamicServiceDescription computeTargetConfiguration();

}
