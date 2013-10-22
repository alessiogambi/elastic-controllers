package at.ac.tuwien.dsg.cloud.elasticity.services.impl.configurationselectors.rulebased;

import java.util.List;

import org.apache.tapestry5.ioc.services.TypeCoercer;
import org.slf4j.Logger;

import ch.usi.cloud.controller.impl.ConfigurationException;

import at.ac.tuwien.dsg.cloud.data.DynamicServiceDescription;
import at.ac.tuwien.dsg.cloud.data.InstanceDescription;
import at.ac.tuwien.dsg.cloud.elasticity.services.ConfigurationSelectorDAO;
import at.ac.tuwien.dsg.cloud.elasticity.services.Monitoring;

/**
 * 
 */
public class FixedDoodleConfigurationSelectorRules extends
		AbstractDoodleConfigurationSelectorRules {

	// Services
	private Logger logger;

	private ConfigurationSelectorDAO configurationSelectorDAO;
	// Configuration values
	private int minJobs;
	private int maxJobs;
	private long scaleUpCoolDownMillis;
	private long scaleDownCoolDownMillis;

	private long lastScaleUp = -1;
	private long lastScaleDown = -1;

	public FixedDoodleConfigurationSelectorRules(
			// Basic services
			Logger logger,
			// Specific Services
			TypeCoercer typeCoercer, Monitoring monitoring,
			ConfigurationSelectorDAO configurationSelectorDAO,
			// Configurations
			Integer minJobs, Integer maxJobs, Long scaleUpCoolDownMillis,
			Long scaleDownCoolDownMillis) {

		this.logger = logger;
		this.configurationSelectorDAO = configurationSelectorDAO;
		// NOT SURE WHY I NEED THIS SINCE THE ABSTRACT CLASS USE IT...
		setLogger(logger);
		setTypeCoercer(typeCoercer);
		setMonitoring(monitoring);
		setConfigurationSelectorDAO(configurationSelectorDAO);

		this.minJobs = minJobs;
		this.maxJobs = maxJobs;
		this.scaleDownCoolDownMillis = scaleDownCoolDownMillis;
		this.scaleUpCoolDownMillis = scaleUpCoolDownMillis;

		logger.info("\n\nSUMMARY: FIXED Rules are triggered if we received less than "
				+ minJobs
				+ " and more than "
				+ maxJobs
				+ " per allocated doodle as in the past minute in average. "
				+ "Cool down periods are scale down "
				+ scaleDownCoolDownMillis
				+ " and scale up " + scaleUpCoolDownMillis + "\n\n");

	}

	/**
	 * Compute the next configuration as a ratio between the limits min and max
	 * jobs
	 * 
	 * @throws ConfigurationException
	 */
	@Override
	DynamicServiceDescription applyRules(
			DynamicServiceDescription _currentConfiguration, double[] context)
			throws ConfigurationException {

		// Current configuration is service !
		List<InstanceDescription> runningNodes = _currentConfiguration
				.getVeeInstances("appserver");
		logger.debug("There are " + runningNodes.size() + " running appservers");
		int currentConfiguration = 0;
		for (InstanceDescription appserver : runningNodes) {
			if ("REGISTERED".equalsIgnoreCase(appserver.getState())) {
				currentConfiguration = currentConfiguration + 1;
			}
		}
		logger.debug("Registered nodes are " + currentConfiguration);

		logger.info("Current conf: " + currentConfiguration + "["
				+ runningNodes.size() + "]");
		double jobs = (context[0] + context[1] + context[2] + context[3]);

		logger.info("Avg jobs per app server => " + minJobs + " < "
				+ (jobs / currentConfiguration) + " < " + maxJobs);

		int targetConfiguration = currentConfiguration;

		// Store monitoring data + timestamp -> specific to this
		// ConfigurationSelector
		configurationSelectorDAO.storeActivationData(service, jobs);

		if (jobs / currentConfiguration > maxJobs) {
			// SCALE UP
			targetConfiguration++;
		} else if (targetConfiguration > 1
				&& jobs / targetConfiguration < minJobs) {
			// SCALE DOWN
			targetConfiguration--;
		}

		logger.debug("Target configuration would be " + targetConfiguration
				+ " app servers");

		if (runningNodes.size() > currentConfiguration) {
			// There are nodes that are starting. Shall we wait for them to
			// start ?
			// But
			logger.debug("But " + (runningNodes.size() - currentConfiguration)
					+ " nodes are starting.");
			logger.info("Wait " + (runningNodes.size() - currentConfiguration)
					+ "instances to register .");
			return _currentConfiguration;
		}

		// Apply Cool Down periods
		if (currentConfiguration < targetConfiguration) {
			logger.info("Scaling up");
			if (System.currentTimeMillis() - lastScaleUp < scaleUpCoolDownMillis
					&& lastScaleUp > 0) {
				logger.warn("Cannot scale up due to Cool down period. Time to wait "
						+ (scaleUpCoolDownMillis - (System.currentTimeMillis() - lastScaleUp)));
				return _currentConfiguration;

			} else {
				// TODO is this right ? Should be taken AFTER we applied the
				// configuration

				lastScaleUp = System.currentTimeMillis();
			}
		} else if (currentConfiguration > targetConfiguration) {
			logger.info("Scaling down");
			if (System.currentTimeMillis() - lastScaleDown < scaleDownCoolDownMillis
					&& lastScaleDown > 0) {
				logger.warn("Cannot scale down due to Cool down period. Time to wait "
						+ ((scaleDownCoolDownMillis) - (System
								.currentTimeMillis() - lastScaleDown)));
				return _currentConfiguration;
			} else {

				// TODO is this right ? Should be taken AFTER we applied the
				// configuration
				lastScaleDown = System.currentTimeMillis();
			}
		}

		// Apply Resource limits
		if (targetConfiguration < getMinInstances()) {
			logger.warn("THE MINIMUM NUMBER OF doodleAS HAS BEEN REACHED !");
			targetConfiguration = (int) getMinInstances();
		} else if (targetConfiguration > getMaxInstances()) {
			logger.warn("THE MAXIMUM NUMBER OF doodleAS HAS BEEN REACHED !");
			targetConfiguration = (int) getMaxInstances();
		}

		return convertArrayToServiceConf(targetConfiguration);
	}
}
