package at.ac.tuwien.dsg.cloud.elasticity.services;

import at.ac.tuwien.dsg.cloud.data.DynamicServiceDescription;

/**
 * <p>
 * This service can be used to monitor controllers activity:
 * <ul>
 * <li>when the controller is waken up</li>
 * <li>when the controller compute the target configuration</li>
 * </ul>
 * </p>
 * <p>
 * At the moment is not really clear if this service should be accessed directly
 * or simply added via method decoration or advice
 * </p>
 * 
 * @author alessiogambi
 * 
 */
@Deprecated
public interface ConfigurationSelectorDAO {

	public void createTargetConfigurationTable(
			DynamicServiceDescription currentConfiguration);

	public void createActivationDataTable(
			DynamicServiceDescription currentConfiguration);

	public void storeActivationData(
			DynamicServiceDescription currentConfiguration, Object... data);

	public void storeTargetConfiguration(
			DynamicServiceDescription targetConfiguration);

}
