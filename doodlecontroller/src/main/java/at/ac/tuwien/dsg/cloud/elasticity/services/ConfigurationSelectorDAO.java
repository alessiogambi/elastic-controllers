package at.ac.tuwien.dsg.cloud.elasticity.services;

import at.ac.tuwien.dsg.cloud.data.DynamicServiceDescription;

public interface ConfigurationSelectorDAO {

	public void createTargetConfigurationTable(
			DynamicServiceDescription currentConfiguration);

	public void createActivationDataTable(
			DynamicServiceDescription currentConfiguration);

	public void storeActivationData(
			DynamicServiceDescription currentConfiguration, double... data);

	public void storeTargetConfiguration(
			DynamicServiceDescription targetConfiguration);

}
