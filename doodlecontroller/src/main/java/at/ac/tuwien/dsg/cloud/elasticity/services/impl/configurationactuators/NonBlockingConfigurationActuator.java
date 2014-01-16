package at.ac.tuwien.dsg.cloud.elasticity.services.impl.configurationactuators;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import at.ac.tuwien.dsg.cloud.data.DynamicServiceDescription;
import at.ac.tuwien.dsg.cloud.elasticity.services.ConfigurationActuator;

/**
 * This class implements a non blocking configuration actuator. It uses an
 * executor service to implement changes in the configuration. At the moment
 * there are not notification hubs mechanisms available. But in the future we
 * can implement some special singleton service inside the Registry that
 * provides the notifications about the configuration changes.
 * 
 * @author alessiogambi
 * 
 */
// FIXME Use PeriodExecutor !
public abstract class NonBlockingConfigurationActuator implements
		ConfigurationActuator {

	protected ExecutorService executor;
	protected DynamicServiceDescription service;

	public NonBlockingConfigurationActuator() {
		executor = Executors.newFixedThreadPool(1);
	}

	public abstract void actuateTheConfiguration(
			DynamicServiceDescription currentConfiguration,
			DynamicServiceDescription targetConfiguration);

	@Override
	public final void actuate(
			// Current conf is the actual service object !
			final DynamicServiceDescription currentConfiguration,
			final DynamicServiceDescription targetConfiguration) {

		// Note this !
		this.service = currentConfiguration;

		// I think this is to much ad the call should not be blocking in general
		// !
		// executor.execute(new Runnable() {

		// @Override
		// public void run() {
		// System.out.println("\n\nNonBlockingConfigurationActuator.actuate() "
		// + targetConfiguration);
		actuateTheConfiguration(currentConfiguration, targetConfiguration);
		// }
		// });

	}
}
