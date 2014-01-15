package at.ac.tuwien.dsg.cloud.elasticity.modules;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.tapestry5.ioc.MappedConfiguration;
import org.apache.tapestry5.ioc.MethodAdviceReceiver;
import org.apache.tapestry5.ioc.ObjectLocator;
import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.ioc.annotations.Advise;
import org.apache.tapestry5.ioc.annotations.InjectService;
import org.apache.tapestry5.ioc.annotations.Marker;
import org.apache.tapestry5.ioc.annotations.Match;
import org.apache.tapestry5.ioc.annotations.ServiceId;
import org.apache.tapestry5.ioc.annotations.Startup;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.ioc.services.TypeCoercer;
import org.apache.tapestry5.ioc.services.cron.IntervalSchedule;
import org.apache.tapestry5.ioc.services.cron.PeriodicExecutor;
import org.apache.tapestry5.plastic.MethodAdvice;
import org.apache.tapestry5.plastic.MethodInvocation;
import org.slf4j.Logger;

import at.ac.tuwien.dsg.cloud.data.DynamicServiceDescription;
import at.ac.tuwien.dsg.cloud.data.InstanceDescription;
import at.ac.tuwien.dsg.cloud.elasticity.advices.CloudServiceUpdater;
import at.ac.tuwien.dsg.cloud.elasticity.data.DoodleSymbolConstants;
import at.ac.tuwien.dsg.cloud.elasticity.data.SymbolConstants;
import at.ac.tuwien.dsg.cloud.elasticity.services.ConfigurationActuator;
import at.ac.tuwien.dsg.cloud.elasticity.services.ConfigurationSelector;
import at.ac.tuwien.dsg.cloud.elasticity.services.ConfigurationSelectorDAO;
import at.ac.tuwien.dsg.cloud.elasticity.services.ConfigurationSelectorProvider;
import at.ac.tuwien.dsg.cloud.elasticity.services.Monitoring;
import at.ac.tuwien.dsg.cloud.elasticity.services.ServiceUpdater;
import at.ac.tuwien.dsg.cloud.elasticity.services.ServiceUpdaterCache;
import at.ac.tuwien.dsg.cloud.elasticity.services.WaitService;
import at.ac.tuwien.dsg.cloud.elasticity.services.impl.ConfigurationSelectorProviderImpl;
import at.ac.tuwien.dsg.cloud.elasticity.services.impl.ServiceUpdaterImpl;
import at.ac.tuwien.dsg.cloud.elasticity.services.impl.configurationactuators.BasicConfigurationActuator;
import at.ac.tuwien.dsg.cloud.elasticity.services.impl.configurationactuators.QueuedConfigurationActuator;
import at.ac.tuwien.dsg.cloud.elasticity.services.impl.configurationselectors.rulebased.FixedDoodleConfigurationSelectorRules;
import at.ac.tuwien.dsg.cloud.elasticity.services.impl.configurationselectors.rulebased.ProportionalDoodleConfigurationSelectorRules;
import at.ac.tuwien.dsg.cloud.elasticity.services.impl.monitoring.DoodleConfigurationSelectorDAO;
import at.ac.tuwien.dsg.cloud.elasticity.services.impl.monitoring.DoodleServiceUpdater;
import at.ac.tuwien.dsg.cloud.elasticity.services.impl.monitoring.MySQLMonitoring;
import at.ac.tuwien.dsg.cloud.elasticity.services.impl.monitoring.ServiceUpdaterCacheImpl;
import at.ac.tuwien.dsg.cloud.elasticity.services.impl.waiting.AbsolutePeriodWait;
import at.ac.tuwien.dsg.cloud.elasticity.services.impl.waiting.PeriodicWait;
import at.ac.tuwien.dsg.cloud.exceptions.ServiceDeployerException;
import at.ac.tuwien.dsg.cloud.services.CloudController;
import at.ac.tuwien.dsg.cloud.services.CloudInterface;
import ch.usi.cloud.controller.common.naming.FQN;
import ch.usi.cloud.controller.common.naming.FQNException;
import ch.usi.cloud.controller.common.naming.FQNType;

public class DoodleElasticControlModule {

	/*
	 * TODO: Shall we use a STRATEGY SERVICE based on PLATFORM to instantiate
	 * the ConfigurationActuator and the other components ?
	 */

	// Register coercion
	public static WaitService buildPeriodicWait(Logger logger,
			@Symbol(SymbolConstants.CONTROL_PERIOD) Long waitPeriod,
			@Symbol(SymbolConstants.CONTROL_PERIOD_UNIT) TimeUnit timeunit) {

		TimeUnit tuMillis = TimeUnit.MILLISECONDS;
		long waitPeriodMillis = tuMillis.convert(waitPeriod, timeunit);

		return new PeriodicWait(logger, waitPeriodMillis);
	}

	public static WaitService buildAbsolutePeriodicWait(Logger logger,
			@Symbol(SymbolConstants.CONTROL_PERIOD) Long waitPeriod,
			@Symbol(SymbolConstants.CONTROL_PERIOD_UNIT) TimeUnit timeunit) {
		TimeUnit tuMillis = TimeUnit.MILLISECONDS;
		long waitPeriodMillis = tuMillis.convert(waitPeriod, timeunit);
		return new AbsolutePeriodWait(logger, waitPeriodMillis);
	}

	//
	// public static ServiceUpdater buildAmazonAsyncServiceUpdater(Logger
	// logger,
	// @InjectService("AmazonTypica") CloudInterface cloudInterface,
	// @InjectService("AmazonServiceUpdater") ServiceUpdater internal) {
	// return new ServiceUpdaterImpl(logger, cloudInterface);
	// }

	public static ServiceUpdater buildDoodleServiceUpdater(Logger logger) {
		return new DoodleServiceUpdater(logger);
	}

	@Marker(CloudServiceUpdater.class)
	public static ServiceUpdater buildServiceUpdater(Logger logger,
			CloudInterface cloudInterface) {
		return new ServiceUpdaterImpl(logger, cloudInterface);
	}

	// @Marker(CloudServiceUpdater.class)
	// public static ServiceUpdater buildAmazonServiceUpdater(Logger logger,
	// @InjectService("AmazonTypica") CloudInterface cloudInterface) {
	// return new ServiceUpdaterImpl(logger, cloudInterface);
	// }

	/**
	 * This service implements a BLOCKING configurations actuator. Once its
	 * actuate method is called the calling thread will be blocked until the
	 * target configuration is reached or an error occurred
	 * 
	 * @param logger
	 * @param controller
	 * @return
	 */
	@ServiceId("BlockingConfigurationActuator")
	public static ConfigurationActuator buildBlockingActuator(Logger logger,
			CloudController controller) {
		return new BasicConfigurationActuator(logger, controller);
	}

	@ServiceId("NonBlockingConfigurationActuator")
	public static ConfigurationActuator buildNonBlockingActuator(Logger logger,
			CloudController controller) {
		return new QueuedConfigurationActuator(logger, controller);
	}

	public static ConfigurationSelectorProvider buildConfigurationSelectorProvider(
			Logger logger, ObjectLocator objectLocator,
			Map<String, ConfigurationSelector> contributions) {
		return new ConfigurationSelectorProviderImpl(logger, objectLocator,
				contributions);
	}

	@ServiceId(SymbolConstants.SQL_MONITORING)
	public static Monitoring buildMonitoring(Logger logger,
			@Symbol(DoodleSymbolConstants.DB_USER_NAME) String dbUserName,
			@Symbol(DoodleSymbolConstants.DB_USER_PWD) String dbUserPwd,
			@Symbol(DoodleSymbolConstants.DB_JDBC_DRIVER) String jdbcDriver,
			@Symbol(DoodleSymbolConstants.DB_JDBC_URL) String dbURL) {

		BasicDataSource applicationDB = new BasicDataSource();
		/*
		 * -Dch.usi.cloud.controller.monitoring.mepgui
		 * .monitoringdb.url=jdbc:mysql ://$ip:3306/monitoring \
		 */
		applicationDB.setDriverClassName(jdbcDriver);
		applicationDB.setUsername(dbUserName);
		applicationDB.setPassword(dbUserPwd);
		applicationDB.setUrl(dbURL);

		return new MySQLMonitoring(logger, applicationDB);

	}

	public static ConfigurationSelector buildProportionalDoodleConfigurationSelectorRules(
			// Basic services
			Logger logger,
			TypeCoercer typeCoercer,
			// Specific Services
			@InjectService(SymbolConstants.SQL_MONITORING) Monitoring monitoring,
			@InjectService(DoodleSymbolConstants.RULE_BASED_CONFIGURATION_SELECTOR_DAO) ConfigurationSelectorDAO configurationSelectorDAO,
			// Configurations
			@Symbol(DoodleSymbolConstants.MIN_JOBS) Integer minJobs,
			@Symbol(DoodleSymbolConstants.MAX_JOBS) Integer maxJobs,
			@Symbol(DoodleSymbolConstants.COOL_PERIOD_SCALE_UP) Long scaleUpCooldownPeriod,
			@Symbol(DoodleSymbolConstants.COOL_PERIOD_SCALE_UP_UNIT) TimeUnit scaleUpCooldownPeriodUnit,
			@Symbol(DoodleSymbolConstants.COOL_PERIOD_SCALE_DOWN) Long scaleDownCooldownPeriod,
			@Symbol(DoodleSymbolConstants.COOL_PERIOD_SCALE_DOWN_UNIT) TimeUnit scaleDownCooldownPeriodUnit) {

		TimeUnit tuMillis = TimeUnit.MILLISECONDS;
		long scaleUpCooldownPeriodMillis = tuMillis.convert(
				scaleUpCooldownPeriod, scaleUpCooldownPeriodUnit);
		long scaleDownCooldownPeriodMillis = tuMillis.convert(
				scaleDownCooldownPeriod, scaleDownCooldownPeriodUnit);

		return new ProportionalDoodleConfigurationSelectorRules(logger,
				typeCoercer, monitoring, configurationSelectorDAO, minJobs,
				maxJobs, scaleUpCooldownPeriodMillis,
				scaleDownCooldownPeriodMillis);
	}

	@ServiceId(DoodleSymbolConstants.RULE_BASED_CONFIGURATION_SELECTOR_DAO)
	public static ConfigurationSelectorDAO buildDoodleconfigurationSelectorDAO(
			Logger logger,
			@Symbol(DoodleSymbolConstants.DB_CONTROLLER_USER_NAME) String dbUserName,
			@Symbol(DoodleSymbolConstants.DB_CONTROLLER_PWD) String dbUserPwd,
			@Symbol(DoodleSymbolConstants.DB_CONTROLLER_JDBC_DRIVER) String jdbcDriver,
			@Symbol(DoodleSymbolConstants.DB_CONTROLLER_JDBC_URL) String dbURL) {

		BasicDataSource applicationDB = new BasicDataSource();
		/*
		 * -Dch.usi.cloud.controller.monitoring.mepgui
		 * .monitoringdb.url=jdbc:mysql ://$ip:3306/monitoring \
		 */
		applicationDB.setDriverClassName(jdbcDriver);
		applicationDB.setUsername(dbUserName);
		applicationDB.setPassword(dbUserPwd);
		applicationDB.setUrl(dbURL);

		return new DoodleConfigurationSelectorDAO(logger, applicationDB);
	}

	public static ConfigurationSelector buildFixedDoodleConfigurationSelectorRules(
			// Basic services
			Logger logger,
			TypeCoercer typeCoercer,
			// Specific Services
			@InjectService(SymbolConstants.SQL_MONITORING) Monitoring monitoring,
			@InjectService(DoodleSymbolConstants.RULE_BASED_CONFIGURATION_SELECTOR_DAO) ConfigurationSelectorDAO configurationSelectorDAO,
			// Configurations
			@Symbol(DoodleSymbolConstants.MIN_JOBS) Integer minJobs,
			@Symbol(DoodleSymbolConstants.MAX_JOBS) Integer maxJobs,
			@Symbol(DoodleSymbolConstants.COOL_PERIOD_SCALE_UP) Long scaleUpCooldownPeriod,
			@Symbol(DoodleSymbolConstants.COOL_PERIOD_SCALE_UP_UNIT) TimeUnit scaleUpCooldownPeriodUnit,
			@Symbol(DoodleSymbolConstants.COOL_PERIOD_SCALE_DOWN) Long scaleDownCooldownPeriod,
			@Symbol(DoodleSymbolConstants.COOL_PERIOD_SCALE_DOWN_UNIT) TimeUnit scaleDownCooldownPeriodUnit) {

		TimeUnit tuMillis = TimeUnit.MILLISECONDS;
		long scaleUpCooldownPeriodMillis = tuMillis.convert(
				scaleUpCooldownPeriod, scaleUpCooldownPeriodUnit);
		long scaleDownCooldownPeriodMillis = tuMillis.convert(
				scaleDownCooldownPeriod, scaleDownCooldownPeriodUnit);

		return new FixedDoodleConfigurationSelectorRules(logger, typeCoercer,
				monitoring, configurationSelectorDAO, minJobs, maxJobs,
				scaleUpCooldownPeriodMillis, scaleDownCooldownPeriodMillis);
	}

	public ServiceUpdaterCache buildServiceUpdaterCache(Logger logger) {
		return new ServiceUpdaterCacheImpl(logger);
	}

	// Autobuilding services
	public static void bind(ServiceBinder binder) {
		// binder.bind(DoodleServiceUpdater.class);
	}

	/**
	 * Other types of service declaration and setup activities
	 * 
	 * @throws FQNException
	 *             , IllegalArgumentException
	 */

	@Startup
	public static void backgroundMonitor(
			PeriodicExecutor executor,

			final Logger logger,
			final CloudInterface cloudInterface,
			// TODO THink on how to easily manage the multiple implementations
			// of this interface. If its a just a matter of
			// decoration/logging/delay, than JUST use advice
			// From the logs it seems that this is actually PlainOpenStack...
			final ServiceUpdaterCache cache,
			//
			@Symbol(DoodleSymbolConstants.BACKGROUND_MONITORING_PERIOD) Long pollingTime,
			@Symbol(DoodleSymbolConstants.BACKGROUND_MONITORING_PERIOD_UNIT) TimeUnit pollingTimeUnit,
			@Symbol("args:serviceFQN") String _serviceFQN,
			@Symbol("args:deployID") String _deployID) throws FQNException,
			IllegalArgumentException {

		final UUID deployID = UUID.fromString(_deployID);
		final FQN serviceFQN = new FQN(_serviceFQN, FQNType.SERVICE);

		TimeUnit tuMillis = TimeUnit.MILLISECONDS;
		long pollingTimeMillis = tuMillis.convert(pollingTime, pollingTimeUnit);

		logger.info("Background Monitoring will run every " + pollingTimeMillis
				+ " millis");
		executor.addJob(new IntervalSchedule(pollingTimeMillis),
				"Background Monitoring", new Runnable() {
					public void run() {
						try {
							for (InstanceDescription instance : cloudInterface
									.getInstances(serviceFQN, deployID)) {
								String instanceState = instance.getState();

								if ("ERROR".equalsIgnoreCase(instanceState)) {
									logger.warn("Instance "
											+ instance.getInstanceId()
											+ " is in error state. Invalidate the cache !");
									cache.invalidate();
									break;
								}
							}

						} catch (ServiceDeployerException e) {
							logger.warn("Error while getting info on cloud "
									+ e.getMessage());
						}
					}
				});
	}

	/**
	 * Service contributions
	 */
	// Define useful application defaults configurations !
	// Remember that they are symbols, therefore immutable by definition
	// Those can be overridden, by specifying either Sys props or by registering
	// a new SymbolSource before ApplicationDefaults !
	public static void contributeApplicationDefaults(
			MappedConfiguration<String, String> configuration) {

		configuration.add(DoodleSymbolConstants.PLATFORM, "openstack");

		configuration.add(DoodleSymbolConstants.BACKGROUND_MONITORING_PERIOD,
				"2");
		configuration.add(
				DoodleSymbolConstants.BACKGROUND_MONITORING_PERIOD_UNIT,
				TimeUnit.MINUTES.name());

		configuration.add(SymbolConstants.CONTROL_PERIOD, "10000");
		configuration.add(SymbolConstants.CONTROL_PERIOD_UNIT,
				TimeUnit.MILLISECONDS.name());
		// Rule-based default configurations
		configuration.add(DoodleSymbolConstants.MIN_JOBS, "10");
		configuration.add(DoodleSymbolConstants.MAX_JOBS, "30");

		configuration.add(DoodleSymbolConstants.COOL_PERIOD_SCALE_DOWN, "2");
		configuration.add(DoodleSymbolConstants.COOL_PERIOD_SCALE_DOWN_UNIT,
				TimeUnit.MINUTES.name());
		configuration.add(DoodleSymbolConstants.COOL_PERIOD_SCALE_UP, "1");
		configuration.add(DoodleSymbolConstants.COOL_PERIOD_SCALE_UP_UNIT,
				TimeUnit.MINUTES.name());

		// Monitoring DB configuration defaults
		// This is the monitor of the application that the controller will query
		// at runtime for the KPIs
		configuration.add(DoodleSymbolConstants.DB_USER_NAME, "mep");
		configuration.add(DoodleSymbolConstants.DB_USER_PWD, "monitoring");
		configuration.add(DoodleSymbolConstants.DB_NAME, "monitoring");
		configuration.add(DoodleSymbolConstants.DB_JDBC_DRIVER,
				"com.mysql.jdbc.Driver");
		// This is a recursive definition of symbols
		configuration.add(DoodleSymbolConstants.DB_JDBC_URL, "jdbc:mysql://${"
				+ DoodleSymbolConstants.DB_HOST + "}:3306/${"
				+ DoodleSymbolConstants.DB_NAME + "}");

		// This is the local DB of the controller
		configuration.add(DoodleSymbolConstants.DB_CONTROLLER_USER_NAME,
				"controller");
		configuration
				.add(DoodleSymbolConstants.DB_CONTROLLER_PWD, "controller");

		configuration
				.add(DoodleSymbolConstants.DB_CONTROLLER_HOST, "localhost");
		configuration.add(DoodleSymbolConstants.DB_CONTROLLER_NAME,
				"Controller");

		configuration.add(DoodleSymbolConstants.DB_CONTROLLER_JDBC_DRIVER,
				"com.mysql.jdbc.Driver");
		configuration.add(DoodleSymbolConstants.DB_CONTROLLER_JDBC_URL,
				"jdbc:mysql://${" + DoodleSymbolConstants.DB_CONTROLLER_HOST
						+ "}:3306/${"
						+ DoodleSymbolConstants.DB_CONTROLLER_NAME + "}");

	}

	public static void contributeConfigurationSelectorProvider(
			MappedConfiguration<String, ConfigurationSelector> configuration,
			@InjectService("DoodleConfigurationSelectorRules") ConfigurationSelector configurationSelector) {

		configuration.add("DoodleConfigurationSelectorRules",
				configurationSelector);
	}

	/**
	 * Service Advice
	 */

	// Now the cloud-driver contains already a 2-level cache, so we do not need
	// another cache level inside the controller. Moreover now we may have
	// several actions implemented at once
	// and capturing the "actuate" methods to invalidate the cache is not
	// enough. If we do that, we result to implement an instable system !!!

	// // This seems not to be honored as a receiver I get a lot of more
	// stuff...
	// // not only implementations of ConfigurationActuator !
	// @Advise(serviceInterface = ConfigurationActuator.class)
	// @Match("*ConfigurationActuator")
	// public static void invalidateServiceUpdaterCache(
	// MethodAdviceReceiver receiver, final ServiceUpdaterCache cache) {
	//
	// MethodAdvice advice = new MethodAdvice() {
	//
	// @Override
	// public void advise(MethodInvocation invocation) {
	// Method m = invocation.getMethod();
	//
	// // System.out.println("\n\nAdvise " + m.getName());
	// // System.out.println(invocation.getMethod().getModifiers());
	// // Class<?>[] pType = m.getParameterTypes();
	// // System.out.println(pType.length);
	// // Type[] gpType = m.getGenericParameterTypes();
	// // for (int i = 0; i < pType.length; i++) {
	// // System.out.println("ParameterType" + pType[i]);
	// // System.out.println("GenericParameterType" + gpType[i]);
	// // }
	//
	// DynamicServiceDescription current = (DynamicServiceDescription)
	// invocation
	// .getParameter(0);
	//
	// DynamicServiceDescription target = (DynamicServiceDescription) invocation
	// .getParameter(1);
	//
	// if (!current.equals(target)) {
	// // System.out.println(current + "\n is different from \n"
	// // + target);
	// //
	// "We probabily need to update the cache after the actuation is done, so invalidate it and force the update !");
	// // System.out.println("Invalidate the cache");
	// cache.invalidate();
	// }
	// // System.out.println("Do the normal call");
	// invocation.proceed();
	// }
	// };
	// // Advice only the actuate method
	// // System.out.println("\t\t ConfigurationActuator: Receiver interface: "
	// // + receiver.getInterface().getName());
	//
	// for (Method m : receiver.getInterface().getMethods()) {
	// // System.out.println("Processing " + m.getName());
	// // public void actuate(DynamicServiceDescription
	// // currentConfiguration, DynamicServiceDescription
	// // targetConfiguration);
	// if ("actuate".equals(m.getName())) {
	// // System.out.println("\t\t ConfigurationActuator Advising: "
	// // + m.getName());
	// receiver.adviseMethod(m, advice);
	// }
	// }
	// };
	//
	// // @Advise(serviceInterface = ServiceUpdater.class)
	// @Advise
	// @CloudServiceUpdater
	// public static void addServiceUpdaterAdvisors(
	// MethodAdviceReceiver receiver,
	// final ServiceUpdaterCache cache,
	// @InjectService("DoodleServiceUpdater") final ServiceUpdater
	// doodleServiceUpdater) {
	//
	// MethodAdvice advice = new MethodAdvice() {
	//
	// @Override
	// public void advise(MethodInvocation invocation) {
	// DynamicServiceDescription service = (DynamicServiceDescription)
	// invocation
	// .getParameter(0);
	//
	// if (cache.isValid()) {
	// // Use the cached version to update the input references -
	// // cache.updateService(service);
	// cache.update(service);
	// // This trap the original invocation
	// // SKIP the : invocation.proceed();
	// } else {
	// // This will make all the calls to the cloud
	// invocation.proceed();
	// // And store into the cache
	// cache.store(service);
	// }
	// // Update the status with the instances using the Loadbalancer
	// doodleServiceUpdater.update(service);
	// }
	// };
	//
	// // Advice only the actuate method
	// System.out.println("\t\t ServiceUpdater: Receiver interface: "
	// + receiver.getInterface().getName());
	//
	// for (Method m : receiver.getInterface().getMethods()) {
	// if ("update".equals(m.getName())) {
	//
	// System.out.println("\t\t Advise " + m.getName() + " of  "
	// + receiver.getInterface().getName());
	// receiver.adviseMethod(m, advice);
	// }
	// }
	// };
}
