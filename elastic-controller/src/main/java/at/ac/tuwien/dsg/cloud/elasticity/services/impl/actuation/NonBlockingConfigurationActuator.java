package at.ac.tuwien.dsg.cloud.elasticity.services.impl.actuation;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;

import at.ac.tuwien.dsg.cloud.data.DynamicServiceDescription;
import at.ac.tuwien.dsg.cloud.elasticity.services.ConfigurationActuator;

/**
 * This class implements a non blocking configuration actuator. It uses an
 * executor service to implement changes in the configuration.
 * 
 * 
 * <b>TODO</b>:At the moment there are not notification hubs mechanisms
 * available. But in the future we can implement some special singleton service
 * inside the Registry that provides the notifications about the configuration
 * changes.
 * 
 * For example we can use Parallel Execution
 * 
 * 
 * @author alessiogambi
 * 
 */
public class NonBlockingConfigurationActuator implements ConfigurationActuator {

	private ExecutorService executor;
	private BlockingConfigurationActuator blockingConfigurationActuator;

	public NonBlockingConfigurationActuator(Logger logger,
			final BlockingConfigurationActuator blockingConfigurationActuator) {
		executor = Executors.newFixedThreadPool(1);
		this.blockingConfigurationActuator = blockingConfigurationActuator;
	}

	@Override
	public final void actuate(
			final DynamicServiceDescription targetConfiguration) {

		executor.execute(new Runnable() {

			@Override
			public void run() {
				blockingConfigurationActuator.actuate(targetConfiguration);
			}
		});

	}
}
