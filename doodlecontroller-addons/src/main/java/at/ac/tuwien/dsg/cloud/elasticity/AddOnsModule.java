package at.ac.tuwien.dsg.cloud.elasticity;

import org.apache.tapestry5.ioc.ObjectLocator;
import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.ioc.annotations.InjectService;
import org.apache.tapestry5.ioc.annotations.ServiceId;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.slf4j.Logger;

import at.ac.tuwien.dsg.cloud.elasticity.controllers.AvgCpuElasticController;
import at.ac.tuwien.dsg.cloud.elasticity.data.DoodleSymbolConstants;
import at.ac.tuwien.dsg.cloud.elasticity.services.ConfigurationActuator;
import at.ac.tuwien.dsg.cloud.elasticity.services.ConfigurationSelector;
import at.ac.tuwien.dsg.cloud.elasticity.services.ElasticController;
import at.ac.tuwien.dsg.cloud.elasticity.services.ServiceUpdater;
import at.ac.tuwien.dsg.cloud.elasticity.services.WaitService;
import at.ac.tuwien.dsg.cloud.elasticity.services.impl.controllers.DoodleServiceElasticController;

public class AddOnsModule {

	@ServiceId("AvgCPU")
	public static ElasticController buildAnotherController(
			Logger logger,
			ObjectLocator objectLocator,
			@InjectService("ProportionalDoodleConfigurationSelectorRules") ConfigurationSelector configurationSelector,
			@InjectService("AbsoluteWait") WaitService waitService,
			// TODO Remove this thing and directly inject the right services
			@Symbol(DoodleSymbolConstants.PLATFORM) String platformName) {

		// TODO A strategy pattern maybe ?
		ServiceUpdater serviceUpdater = null;
		ConfigurationActuator configurationActuator = null;
		if (platformName.equalsIgnoreCase("openstack")
				|| platformName.equalsIgnoreCase("os")) {
			serviceUpdater = objectLocator.getService("OSServiceUpdater",
					ServiceUpdater.class);
			configurationActuator = objectLocator.getService(
					"OSBlockingConfigurationActuator",
					ConfigurationActuator.class);
		} else if (platformName.equalsIgnoreCase("amazon")) {
			serviceUpdater = objectLocator.getService("AmazonServiceUpdater",
					ServiceUpdater.class);
			configurationActuator = objectLocator.getService(
					"AmazonBlockingConfigurationActuator",
					ConfigurationActuator.class);
		}

		return new DoodleServiceElasticController(logger,
				configurationSelector, configurationActuator, waitService,
				serviceUpdater);
	}

	public static void bind(ServiceBinder binder) {
		binder.bind(ElasticController.class, AvgCpuElasticController.class)
				.withId("AvgCPU");
	}

}
