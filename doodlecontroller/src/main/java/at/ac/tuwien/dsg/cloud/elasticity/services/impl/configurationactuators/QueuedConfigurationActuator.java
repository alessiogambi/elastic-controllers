package at.ac.tuwien.dsg.cloud.elasticity.services.impl.configurationactuators;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;

import at.ac.tuwien.dsg.cloud.data.DynamicServiceDescription;
import at.ac.tuwien.dsg.cloud.elasticity.services.ConfigurationActuator;
import at.ac.tuwien.dsg.cloud.services.CloudController;

/**
 * TODO TONS of code duplication !!!
 * 
 * @author alessiogambi
 * 
 */
public class QueuedConfigurationActuator extends
		NonBlockingConfigurationActuator {

	private Logger logger;
	private CloudController controller;
	final private ConfigurationActuator blockingActuator;

	// This decouples
	private BlockingQueue<DynamicServiceDescription> queuedConfigurationChanges;

	public QueuedConfigurationActuator(Logger _logger,
			CloudController controller, ConfigurationActuator blockingActuator) {
		super();

		this.logger = _logger;
		this.controller = controller;

		// This is shared between the runnable inside the executor services and
		// the thread inside the configuration actuator
		this.queuedConfigurationChanges = new LinkedBlockingQueue<DynamicServiceDescription>();

		this.blockingActuator = blockingActuator;
		// This gets initialized inside the "super()"
		executor.execute(new Runnable() {
			@Override
			public void run() {
				// logger.info("Starting the Actuator Thread !");
				while (true) {
					try {
						// This methods will block if the
						// queuedConfigurationChanges is empty !
						_actuateTheConfiguration();
					} catch (InterruptedException e) {
						logger.info("Actuator Thread Interrupted !", e);
						break;
					} catch (Throwable e) {
						e.printStackTrace();
						logger.warn("Captured but ignored", e);
					}
				}
				executor.shutdown();
			}
		});
	}

	/**
	 * This is the abstract method to implement in subclasses. We need to
	 * actuate a given configuration, before we can move on.
	 */
	@Override
	public void actuateTheConfiguration(
			// Current conf is the actual service object
			DynamicServiceDescription currentConfiguration,
			DynamicServiceDescription targetConfiguration) {

		// logger.debug("\t >>> Actuate " + targetConfiguration +
		// " service is : "
		// + service);

		if (targetConfiguration != null) {
			logger.trace("Try to enqueued a new configuration ");
			if (this.queuedConfigurationChanges.offer(targetConfiguration)) {
				logger.debug("Enqueued a new configuration "
						+ targetConfiguration + " now "
						+ this.queuedConfigurationChanges.size());
			} else {
				logger.warn("The new configuration was not enqueued: "
						+ targetConfiguration);
			}
		} else {
			logger.warn("Target configuration is NULL !!!");
		}
	}

	/*
	 * This is the method actually doing the work ! And it is blocking !
	 */
	public void _actuateTheConfiguration() throws InterruptedException {

		// Take the last element, i.e., newest inserted, and ignore all the
		// others.
		List<DynamicServiceDescription> targetConfigurations = new ArrayList<DynamicServiceDescription>();

		logger.debug("Waiting for target configurations !");

		// This is needed to block the thread ! This triggers if at least ONE
		// configuration is there !
		targetConfigurations.add(this.queuedConfigurationChanges.take());

		// Remove all the remaining confs if ANY
		this.queuedConfigurationChanges.drainTo(targetConfigurations);

		// This drains all the others inside the
		if (targetConfigurations.size() == 0) {
			logger.info("No target configurations scheduled !");
			return;
		}

		if (service == null) {
			logger.warn("The service Object is null. Why that ?! Skip to next control loop");
			return;
		}
		// Create a copy of this to have some sort of fixed point, otherwise it
		// may happen that current conf changes while we are implementing
		// things... note that this is needed only
		// because we
		// decoupled the currentConf/service/queue --> UGLY !

		// The shared var is not updated by the service updater! Why ? Cache ?

		DynamicServiceDescription currentConfiguration = new DynamicServiceDescription(
				service);

		// We need to take only the latest inserted !
		DynamicServiceDescription targetConfiguration = targetConfigurations
				.get(targetConfigurations.size() - 1);

		// THIS IS TRICKY !
		blockingActuator.actuate(currentConfiguration, targetConfiguration);
	}
}
