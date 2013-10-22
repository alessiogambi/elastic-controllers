package at.ac.tuwien.dsg.cloud.elasticity.services;

import at.ac.tuwien.dsg.cloud.data.DynamicServiceDescription;

public interface ConfigurationSelector {

	public DynamicServiceDescription getTargetConfiguration();

	void setService(DynamicServiceDescription service);
}
