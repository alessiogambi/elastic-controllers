package at.ac.tuwien.dsg.cloud.elasticity.controllers;

import org.apache.tapestry5.ioc.annotations.InjectService;
import org.slf4j.Logger;

import at.ac.tuwien.dsg.cloud.data.DynamicServiceDescription;
import at.ac.tuwien.dsg.cloud.elasticity.services.ConfigurationActuator;
import at.ac.tuwien.dsg.cloud.elasticity.services.ConfigurationSelector;
import at.ac.tuwien.dsg.cloud.elasticity.services.ElasticController;
import at.ac.tuwien.dsg.cloud.elasticity.services.ServiceUpdater;
import at.ac.tuwien.dsg.cloud.elasticity.services.WaitService;

/**
 * This defines an additional elastic controller: it uses a blocking actuator
 * and chooses the next system configuration according to a proportional (and
 * predefined) scheme:
 * <ul>
 * <li>If the total Average CPU usage of the application servers in the last 4
 * intervals is grater that 70% add a new instance</li>
 * <li></li>
 * </ul>
 * 
 */
public class AvgCpuElasticController implements ElasticController {

	private Logger logger;

	@InjectService("AvgCpu")
	private ConfigurationSelector configurationSelector;

	@InjectService("OSBlockingConfigurationActuator")
	private ConfigurationActuator configurationActuator;

	@InjectService("AbsoluteWait")
	private WaitService waitService;

	@InjectService("OSServiceUpdater")
	private ServiceUpdater serviceUpdater;

	/*
	 * This object represent the "current" service status.
	 */
	private DynamicServiceDescription service;

	private boolean running;
	private Thread thread;

	public AvgCpuElasticController(
	// Resources
			Logger logger) {

		this.logger = logger;

		this.running = false;
	}

	@Override
	public synchronized void start(final DynamicServiceDescription _service) {
		this.logger.info("AvgCpuElasticController START()");
		if (running) {
			return;
		} else {
			try {
				service = _service;

				configurationSelector.setService(service);

				// Maybe this runnable should be provided as service as well ?!
				thread = new Thread(new Runnable() {

					@Override
					public void run() {

						while (running) {
							try {
								waitService.waitMe();
								serviceUpdater.update(service);
								DynamicServiceDescription targetServiceConfiguration = configurationSelector
										.getTargetConfiguration();
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
