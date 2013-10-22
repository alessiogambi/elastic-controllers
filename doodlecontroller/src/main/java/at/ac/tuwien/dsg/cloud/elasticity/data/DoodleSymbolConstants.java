package at.ac.tuwien.dsg.cloud.elasticity.data;

public class DoodleSymbolConstants {

	// Generic Doodle controllers configurations
	public static final String COOL_PERIOD_SCALE_DOWN = "ch.usi.cloud.controller.doodleservice.scaledown.cooldown.period";
	public static final String COOL_PERIOD_SCALE_DOWN_UNIT = "ch.usi.cloud.controller.doodleservice.scaledown.cooldown.period.unit";
	public static final String COOL_PERIOD_SCALE_UP = "ch.usi.cloud.controller.doodleservice.scaleup.cooldown.period";
	public static final String COOL_PERIOD_SCALE_UP_UNIT = "ch.usi.cloud.controller.doodleservice.scaleup.cooldown.period.unit";

	// Rule based Doodle controllers configurations
	public static final String MIN_JOBS = "ch.usi.cloud.controller.doodleservice.rules.jobs.min";
	public static final String MAX_JOBS = "ch.usi.cloud.controller.doodleservice.rules.jobs.max";

	// Controllers name
	public static final String PROPORTIONAL_RULE_BASED_CONTROLLER_NAME = "ProportionalRuleBasedDoodleElasticController";
	public static final String FIXED_RULE_BASED_CONTROLLER_NAME = "FixedRuleBasedDoodleElasticController";
	public static final String RULE_BASED_CONFIGURATION_SELECTOR_DAO = "DoodleConfigurationSelectorDAO";
	public static final String PROPORTIONAL_RULE_BASED_CONTROLLER_NAME_NON_BLOCKING = "ProportionalRuleBasedDoodleElasticControllerNonBlocking";
	public static final String FIXED_RULE_BASED_CONTROLLER_NAME_NON_BLOCKING = "FixedRuleBasedDoodleElasticControllerNonBlocking";

	public static final String DB_USER_NAME = "ch.usi.cloud.controller.doodleservice.monitoring.db.user.name";
	public static final String DB_USER_PWD = "ch.usi.cloud.controller.doodleservice.monitoring.db.user.password";
	public static final String DB_JDBC_DRIVER = "ch.usi.cloud.controller.doodleservice.monitoring.db.jdbc.driver";
	public static final String DB_JDBC_URL = "ch.usi.cloud.controller.doodleservice.monitoring.db.jdbc.url";
	public static final String DB_NAME = "ch.usi.cloud.controller.doodleservice.monitoring.db.name";
	public static final String DB_HOST = "ch.usi.cloud.controller.doodleservice.monitoring.db.host";

	public static final String DB_CONTROLLER_USER_NAME = "ch.usi.cloud.controller.monitoring.db.user.name";
	public static final String DB_CONTROLLER_PWD = "ch.usi.cloud.controller.monitoring.db.user.password";
	public static final String DB_CONTROLLER_JDBC_DRIVER = "ch.usi.cloud.controller.monitoring.db.jdbc.driver";
	public static final String DB_CONTROLLER_JDBC_URL = "ch.usi.cloud.controller.monitoring.db.jdbc.url";
	public static final String DB_CONTROLLER_NAME = "ch.usi.cloud.controller.monitoring.db.name";
	public static final String DB_CONTROLLER_HOST = "ch.usi.cloud.controller.monitoring.db.host";

	public static final String BACKGROUND_MONITORING_PERIOD = "ch.usi.cloud.controller.background.monitoring.period";
	public static final String BACKGROUND_MONITORING_PERIOD_UNIT = "ch.usi.cloud.controller.background.monitoring.period.unit";

	public static final String PLATFORM = "ch.usi.cloud.platform";

}
