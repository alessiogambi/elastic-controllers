package at.ac.tuwien.dsg.cloud.elasticity.services.impl.controllers;

import org.slf4j.Logger;

import at.ac.tuwien.dsg.cloud.data.DynamicServiceDescription;
import at.ac.tuwien.dsg.cloud.elasticity.services.ConfigurationActuator;
import at.ac.tuwien.dsg.cloud.elasticity.services.ConfigurationSelector;
import at.ac.tuwien.dsg.cloud.elasticity.services.ElasticController;
import at.ac.tuwien.dsg.cloud.elasticity.services.ServiceUpdater;
import at.ac.tuwien.dsg.cloud.elasticity.services.WaitService;

public class DoodleServiceElasticController implements ElasticController {

	private Logger logger;

	private ConfigurationSelector configurationSelector;

	/*
	 * Given a target configuration this class will implement it in the Cloud or
	 * raise an Exception
	 */
	private ConfigurationActuator configurationActuator;

	/*
	 * This class implements only a blocking waitMe method. This can be used to
	 * make a controller either periodic or reactive or a combination of both
	 */
	private WaitService waitService;

	private ServiceUpdater serviceUpdater;

	/*
	 * This object represent the "current" service status.
	 */
	private DynamicServiceDescription service;

	private boolean running;
	private Thread thread;

	public DoodleServiceElasticController(
			// Basic Services
			Logger logger,
			// Control Specific Services
			ConfigurationSelector configurationSelector,
			ConfigurationActuator configurationActuator,
			WaitService waitService,
			// Cloud Specific Services
			ServiceUpdater serviceUpdater) {

		this.logger = logger;

		this.configurationSelector = configurationSelector;
		this.configurationActuator = configurationActuator;
		this.waitService = waitService;
		this.serviceUpdater = serviceUpdater;

		this.running = false;

		logger.info("DoodleServiceElasticController.DoodleServiceElasticController() ");
	}

	@Override
	public synchronized void start(final DynamicServiceDescription _service) {
		this.logger.info("DoodleServiceElasticController.start()");
		if (running) {
			return;
		} else {
			try {
				service = _service;

				configurationSelector.setService(service);

				// TODO Use a periodic executor service by tapestry
				// Maybe this runnable should be provided as service as well ?!
				thread = new Thread(new Runnable() {

					@Override
					public void run() {

						while (running) {
							try {
								// Wait for any event to unlock this
								waitService.waitMe();

								// Retrieve and update the current service
								// representation
								serviceUpdater.update(service);

								// Compute next configuration based on the
								// current
								// service representation

								// NOTE Here we strongly rely on the IoC and we
								// assume that each invocation to getTargetConf
								// is
								// stateless.
								// If you need to maintain the status (i.e., the
								// confSelectorObject) you can also move its
								// creation above, before instantiating the
								// Thread.
								// In any case we need the service object to
								// identify the actual service instance

								// For the sake of clarity I split the
								// invocation in
								// three statements:

								DynamicServiceDescription targetServiceConfiguration = configurationSelector
										.getTargetConfiguration();

								// Note this can be or cannot be blocking !
								configurationActuator.actuate(service,
										targetServiceConfiguration);
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
