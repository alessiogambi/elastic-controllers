package at.ac.tuwien.dsg.cloud.elasticity.services.impl.configurationselectors.rulebased;

import org.apache.tapestry5.ioc.services.TypeCoercer;
import org.slf4j.Logger;

import at.ac.tuwien.dsg.cloud.data.DynamicServiceDescription;
import at.ac.tuwien.dsg.cloud.elasticity.services.ConfigurationSelectorDAO;
import at.ac.tuwien.dsg.cloud.elasticity.services.Monitoring;
import ch.usi.cloud.controller.impl.ConfigurationException;

/**
 * 
 */
public class ProportionalDoodleConfigurationSelectorRules extends
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

	public ProportionalDoodleConfigurationSelectorRules(
			// Basic services
			Logger logger,
			TypeCoercer typeCoercer,
			// Specific Services
			Monitoring monitoring,
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

		logger.info("\n\nSUMMARY: PROPORTIONAL Rules are triggered if we received less than "
				+ minJobs
				+ " and more than "
				+ maxJobs
				+ " per allocated doodle as in the past minute in average."
				+ "Cool down periods are scale down "
				+ scaleDownCoolDownMillis
				+ " and scale up " + scaleUpCoolDownMillis + "\n\n");

	}

	/**
	 * Compute the next configuration as a ratio between the limits min and max
	 * jobs
	 */
	@Override
	DynamicServiceDescription applyRules(
			DynamicServiceDescription _currentConfiguration, double[] context)
			throws ConfigurationException {

		int currentConfiguration = _currentConfiguration.getVeeInstances(
				"appserver").size();

		logger.info("Current doodleAS/appserver node RUNNING (some of them may be still in the registration process): "
				+ currentConfiguration);
		double jobs = (context[0] + context[1] + context[2] + context[3]);

		logger.warn("Jobs per app server => \n\n" + minJobs + " < "
				+ (jobs / currentConfiguration) + " < " + maxJobs);

		double target = (maxJobs + minJobs) / 2;

		configurationSelectorDAO.storeActivationData(service, jobs);

		int targetConfiguration = (int) (jobs / target);

		logger.info("Target configuration would be " + targetConfiguration);

		// Apply Cool Down periods
		if (currentConfiguration < targetConfiguration) {
			logger.info("Current configuration is too small with (Scaling up)");
			if (System.currentTimeMillis() - lastScaleUp < scaleUpCoolDownMillis
					&& lastScaleUp > 0) {
				logger.warn("Cannot scale up due to Cool down period. Time to wait "
						+ (System.currentTimeMillis() - lastScaleUp));
				return _currentConfiguration;

			} else {
				lastScaleUp = System.currentTimeMillis();
			}
		} else if (currentConfiguration > targetConfiguration) {
			logger.info("Current configuration is too big (Scaling down)");
			if (System.currentTimeMillis() - lastScaleDown < scaleDownCoolDownMillis
					&& lastScaleDown > 0) {
				logger.warn("Cannot scale down due to Cool down period. Time to wait "
						+ (System.currentTimeMillis() - lastScaleDown));
				return _currentConfiguration;
			} else {
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
