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
public abstract class NonBlockingConfigurationActuator implements
		ConfigurationActuator {

	private ExecutorService executor;

	public NonBlockingConfigurationActuator() {
		executor = Executors.newFixedThreadPool(1);
	}

	public abstract void actuateTheConfiguration(
			DynamicServiceDescription currentConfiguration,
			DynamicServiceDescription targetConfiguration);

	@Override
	public final void actuate(
			final DynamicServiceDescription currentConfiguration,
			final DynamicServiceDescription targetConfiguration) {

		executor.execute(new Runnable() {

			@Override
			public void run() {
				actuateTheConfiguration(currentConfiguration,
						targetConfiguration);
			}
		});

	}
}
