package at.ac.tuwien.dsg.cloud.elasticity.services;

import at.ac.tuwien.dsg.cloud.data.DynamicServiceDescription;

public interface ConfigurationSelectorProvider {

	/**
	 * Instantiate a new ConfigurationSelector for the given service
	 * 
	 * @param configurationSelectorName
	 * @param service
	 * @return
	 */
	public ConfigurationSelector provideByName(
			String configurationSelectorName, DynamicServiceDescription service);

	public ConfigurationSelector provideByClass(
			Class<? extends ConfigurationSelector> configurationSelectorClass,
			DynamicServiceDescription service);
}
