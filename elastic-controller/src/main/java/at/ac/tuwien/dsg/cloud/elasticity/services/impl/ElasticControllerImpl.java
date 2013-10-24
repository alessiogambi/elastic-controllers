package at.ac.tuwien.dsg.cloud.elasticity.services.impl;

import org.slf4j.Logger;

import at.ac.tuwien.dsg.cloud.data.DynamicServiceDescription;
import at.ac.tuwien.dsg.cloud.elasticity.services.ConfigurationActuator;
import at.ac.tuwien.dsg.cloud.elasticity.services.ConfigurationSelector;
import at.ac.tuwien.dsg.cloud.elasticity.services.ElasticController;
import at.ac.tuwien.dsg.cloud.elasticity.services.ServiceUpdater;
import at.ac.tuwien.dsg.cloud.elasticity.services.WaitService;

/**
 * This class implements a simple basic ElasticController and its lifecycle
 * 
 * TODO: For example we can use PeriodicExecutor
 * 
 * @author alessiogambi
 * 
 */
public class ElasticControllerImpl implements ElasticController {

	private Logger logger;

	/*
	 * This encodes the control logic of the controller.
	 */
	private ConfigurationSelector configurationSelector;

	/*
	 * Cloud actuators to implement the configuration decided by the logic
	 */
	private ConfigurationActuator configurationActuator;

	private WaitService waitService;

	/*
	 * Force an update of the shared objects: TODO maybe this can be also done
	 * with a periodic executor (+chache) that simply does this job at runtime.
	 * We then export service as shadowProperty as we should be able to have the
	 * updated situation at all time
	 */
	private ServiceUpdater serviceUpdater;
	private DynamicServiceDescription service;

	private boolean running;

	// Shall we use ParallelExecutor instead ?
	private Thread thread;

	public ElasticControllerImpl(
			// Basic Services
			Logger logger,
			// Control Specific Services
			ConfigurationSelector configurationSelector,
			ConfigurationActuator configurationActuator,
			WaitService waitService,
			// Cloud Specific Services
			DynamicServiceDescription service, ServiceUpdater serviceUpdater) {

		this.logger = logger;

		this.configurationSelector = configurationSelector;
		this.configurationActuator = configurationActuator;
		this.waitService = waitService;

		this.service = service;
		this.serviceUpdater = serviceUpdater;

		this.running = false;
	}

	@Override
	public synchronized void start() {
		if (running) {
			// Idempotent
			return;
		} else {
			logger.info("ElasticController.start()");
			try {
				// TODO Remove + after Injection ?
				serviceUpdater.update(service);
				// Maybe this runnable should be provided as service as well ?!
				thread = new Thread(new Runnable() {

					@Override
					public void run() {

						while (running) {
							try {
								// Wait for any event to unlock this
								waitService.waitMe();
								// Retrieve and update the current service
								// representation. Is this necessary ?
								// TODO Can we have this as a service with a
								// shadown property
								serviceUpdater.update(service);
								DynamicServiceDescription targetServiceConfiguration = configurationSelector
										.computeTargetConfiguration();

								// TODO Apply additional constraints, i.e.,
								// validate the target configuration, mix/max,
								// cooldown, etc.

								configurationActuator
										.actuate(targetServiceConfiguration);
							} catch (Throwable e) {
								logger.error("Error in main control loop:", e);
							}
						}
					}
				});
				running = true;
				thread.start();

			} catch (Exception e) {
				String msg = "Problems in starting the Controller";
				logger.error(msg, e);
				throw new RuntimeException(msg, e);
			}

		}
	}

	@Override
	public synchronized void stop() {
		if (!running) {
			return;
		} else {
			try {
				running = false;
				// TODO Note this interrupt this thread but not all the other
				// running
				// ones, like deployers, actuators, etc !!
				thread.interrupt();

			} catch (Exception e) {
				logger.error("While stopping the controller");
				e.printStackTrace();
			}
		}
	}

}
