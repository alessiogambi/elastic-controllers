package at.ac.tuwien.dsg.cloud.elasticity.services.impl;

import java.util.Map;

import org.apache.tapestry5.ioc.ObjectLocator;
import org.slf4j.Logger;

import at.ac.tuwien.dsg.cloud.data.DynamicServiceDescription;
import at.ac.tuwien.dsg.cloud.elasticity.services.ConfigurationSelector;
import at.ac.tuwien.dsg.cloud.elasticity.services.ConfigurationSelectorProvider;

/*
 * TODO Not sure this is the right way... this provider in the end acts like a Factory.. so somewhere we should be instantite the conf selector.. 
 * Maybe via the ObjectProvider  
 */
public class ConfigurationSelectorProviderImpl implements
		ConfigurationSelectorProvider {

	private Logger logger;
	private ObjectLocator objectLocator;
	private Map<String, ConfigurationSelector> configurationSelectors;

	public ConfigurationSelectorProviderImpl(Logger logger,
			ObjectLocator objectLocator,
			Map<String, ConfigurationSelector> contributions) {
		this.logger = logger;
		this.objectLocator = objectLocator;
		this.configurationSelectors = contributions;
	}

	@Override
	public ConfigurationSelector provideByName(
			String configurationSelectorName, DynamicServiceDescription service) {
		if (configurationSelectors.containsKey(configurationSelectorName)) {
			Class<? extends ConfigurationSelector> clazz = configurationSelectors
					.get(configurationSelectorName).getClass();
			ConfigurationSelector cs = objectLocator.getService(
					configurationSelectorName, clazz);
			cs.setService(service);
			return cs;
		} else {
			String msg = "Cannot find the ConfigurationSelector  "
					+ configurationSelectorName;
			logger.error(msg);
			throw new RuntimeException(msg);
		}
	}

	@Override
	public ConfigurationSelector provideByClass(
			Class<? extends ConfigurationSelector> configurationSelectorClass,
			DynamicServiceDescription service) {
		// TODO Auto-generated method stub
		return null;
	}

}
