package at.ac.tuwien.dsg.cloud.elasticity.services.internal;

import at.ac.tuwien.dsg.cloud.data.DynamicServiceDescription;
import at.ac.tuwien.dsg.cloud.elasticity.services.ConfigurationSelector;

@Deprecated
public interface ConfigurationSelectorSource {

	/*
	 * TODO Not sure this is of any actual use: unless we need to combine
	 * different configuration selectors in some fancy way I suspect this
	 * interface/service is not usefull
	 */
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
