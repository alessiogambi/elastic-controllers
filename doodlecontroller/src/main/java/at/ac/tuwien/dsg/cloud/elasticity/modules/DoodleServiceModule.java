package at.ac.tuwien.dsg.cloud.elasticity.modules;

import org.apache.tapestry5.ioc.ObjectLocator;
import org.apache.tapestry5.ioc.annotations.InjectService;
import org.apache.tapestry5.ioc.annotations.ServiceId;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.slf4j.Logger;

import at.ac.tuwien.dsg.cloud.elasticity.data.DoodleSymbolConstants;
import at.ac.tuwien.dsg.cloud.elasticity.services.ConfigurationActuator;
import at.ac.tuwien.dsg.cloud.elasticity.services.ConfigurationSelector;
import at.ac.tuwien.dsg.cloud.elasticity.services.ElasticController;
import at.ac.tuwien.dsg.cloud.elasticity.services.ServiceUpdater;
import at.ac.tuwien.dsg.cloud.elasticity.services.WaitService;
import at.ac.tuwien.dsg.cloud.elasticity.services.impl.controllers.DoodleServiceElasticController;

public class DoodleServiceModule {

	/*
	 * Shall we USE a STRATEGY BUILDER service to configure ad implement the
	 * ElasticController ?
	 */

	/**
	 * This defines a very basic form of elastic controller: it uses a blocking
	 * actuator and chooses the next system configuration according to a
	 * proportional (and predefined) scheme.
	 * 
	 * @param logger
	 * @param configurationSelector
	 * @param configurationActuator
	 * @param waitService
	 * @param serviceUpdater
	 * @return
	 */
	@ServiceId(DoodleSymbolConstants.PROPORTIONAL_RULE_BASED_CONTROLLER_NAME)
	public static ElasticController buildProportionalRBController(
			Logger logger,
			ObjectLocator objectLocator,
			@InjectService("ProportionalDoodleConfigurationSelectorRules") ConfigurationSelector configurationSelector,
			@InjectService("PeriodicWait") WaitService waitService,
			@Symbol(DoodleSymbolConstants.PLATFORM) String platformName) {

		ServiceUpdater serviceUpdater = null;
		ConfigurationActuator configurationActuator = null;
		if (platformName.equalsIgnoreCase("openstack")
				|| platformName.equalsIgnoreCase("os")) {
			serviceUpdater = objectLocator.getService("ServiceUpdater",
					ServiceUpdater.class);
			configurationActuator = objectLocator.getService(
					"BlockingConfigurationActuator",
					ConfigurationActuator.class);
		}

		// else if (platformName.equalsIgnoreCase("amazon")) {
		// serviceUpdater = objectLocator.getService("AmazonServiceUpdater",
		// ServiceUpdater.class);
		// configurationActuator = objectLocator.getService(
		// "AmazonBlockingConfigurationActuator",
		// ConfigurationActuator.class);
		// }

		return new DoodleServiceElasticController(logger,
				configurationSelector, configurationActuator, waitService,
				serviceUpdater);
	}

	/**
	 * This defines a very basic form of elastic controller: it uses a blocking
	 * actuator and chooses the next system configuration according to a
	 * proportional (and predefined) scheme.
	 * 
	 * @param logger
	 * @param configurationSelector
	 * @param configurationActuator
	 * @param waitService
	 * @param serviceUpdater
	 * @return
	 */
	@ServiceId(DoodleSymbolConstants.FIXED_RULE_BASED_CONTROLLER_NAME)
	public static ElasticController buildFixedRBController(
			Logger logger,
			ObjectLocator objectLocator,
			@InjectService("FixedDoodleConfigurationSelectorRules") ConfigurationSelector configurationSelector,
			@InjectService("AbsolutePeriodicWait") WaitService waitService,
			@Symbol(DoodleSymbolConstants.PLATFORM) String platformName) {

		ServiceUpdater serviceUpdater = null;
		ConfigurationActuator configurationActuator = null;
		if (platformName.equalsIgnoreCase("openstack")
				|| platformName.equalsIgnoreCase("os")) {
			serviceUpdater = objectLocator.getService("ServiceUpdater",
					ServiceUpdater.class);
			configurationActuator = objectLocator.getService(
					"BlockingConfigurationActuator",
					ConfigurationActuator.class);
		}

		// else if (platformName.equalsIgnoreCase("amazon")) {
		// serviceUpdater = objectLocator.getService("AmazonServiceUpdater",
		// ServiceUpdater.class);
		// configurationActuator = objectLocator.getService(
		// "AmazonBlockingConfigurationActuator",
		// ConfigurationActuator.class);
		// }

		return new DoodleServiceElasticController(logger,
				configurationSelector, configurationActuator, waitService,
				serviceUpdater);
	}

	/**
	 * This is the non blocking version of the Proportional Rule based
	 * controller. At every cycle it schedule a new target configuration to be
	 * implemented.
	 * 
	 * @param logger
	 * @param configurationSelector
	 * @param configurationActuator
	 * @param waitService
	 * @param serviceUpdater
	 * @return
	 */
	@ServiceId(DoodleSymbolConstants.PROPORTIONAL_RULE_BASED_CONTROLLER_NAME_NON_BLOCKING)
	public static ElasticController buildProportionalRBControllerNonBlocking(
			Logger logger,
			ObjectLocator objectLocator,
			@InjectService("ProportionalDoodleConfigurationSelectorRules") ConfigurationSelector configurationSelector,
			@InjectService("PeriodicWait") WaitService waitService,
			@Symbol(DoodleSymbolConstants.PLATFORM) String platformName) {

		ServiceUpdater serviceUpdater = null;
		ConfigurationActuator configurationActuator = null;
		if (platformName.equalsIgnoreCase("openstack")
				|| platformName.equalsIgnoreCase("os")) {
			serviceUpdater = objectLocator.getService("ServiceUpdater",
					ServiceUpdater.class);
			configurationActuator = objectLocator.getService(
					"NonBlockingConfigurationActuator",
					ConfigurationActuator.class);
		}
		// else if (platformName.equalsIgnoreCase("amazon")) {
		// serviceUpdater = objectLocator.getService("AmazonServiceUpdater",
		// ServiceUpdater.class);
		// configurationActuator = objectLocator.getService(
		// "AmazonNonBlockingConfigurationActuator",
		// ConfigurationActuator.class);
		// }

		return new DoodleServiceElasticController(logger,
				configurationSelector, configurationActuator, waitService,
				serviceUpdater);
	}

	/**
	 * This is the non blocking version of the Fixed Rule based controller. At
	 * every cycle it schedule a new target configuration to be implemented.
	 * 
	 * @param logger
	 * @param configurationSelector
	 * @param configurationActuator
	 * @param waitService
	 * @param serviceUpdater
	 * @return
	 */
	@ServiceId(DoodleSymbolConstants.FIXED_RULE_BASED_CONTROLLER_NAME_NON_BLOCKING)
	public static ElasticController buildFixedRBControllerNonBlocking(
			Logger logger,
			ObjectLocator objectLocator,
			@InjectService("FixedDoodleConfigurationSelectorRules") ConfigurationSelector configurationSelector,
			@InjectService("AbsolutePeriodicWait") WaitService waitService,
			@Symbol(DoodleSymbolConstants.PLATFORM) String platformName,
			// Configuration Actuator
			@InjectService("NonBlockingConfigurationActuator") ConfigurationActuator configurationActuator) {

		ServiceUpdater serviceUpdater = null;

		// if (platformName.equalsIgnoreCase("openstack")
		// || platformName.equalsIgnoreCase("os")) {
		serviceUpdater = objectLocator.getService("ServiceUpdater",
				ServiceUpdater.class);
		// }
		// else if (platformName.equalsIgnoreCase("amazon")) {
		// serviceUpdater = objectLocator.getService("AmazonServiceUpdater",
		// ServiceUpdater.class);
		// configurationActuator = objectLocator.getService(
		// "AmazonNonBlockingConfigurationActuator",
		// ConfigurationActuator.class);
		// }
		return new DoodleServiceElasticController(logger,
				configurationSelector, configurationActuator, waitService,
				serviceUpdater);
	}
}