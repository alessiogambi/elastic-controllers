package at.ac.tuwien.dsg.cloud.elasticity.util;

import at.ac.tuwien.dsg.cloud.data.DynamicServiceDescription;

public class TablesManagerApplicationDB {

	public static String getMonitoringTableName(
			DynamicServiceDescription service) {
		return "monitoring_" + getEscapedName(service);
	}

	public static String getControllerActivationTableName(
			DynamicServiceDescription service) {
		return "controller_activation_" + getEscapedName(service);
	}

	public static String getControllerTargetConfigurationTableName(
			DynamicServiceDescription service) {
		return "controller_target_configuration_" + getEscapedName(service);
	}

	private static String getEscapedName(DynamicServiceDescription service) {
		String _serviceFQN = service.getStaticServiceDescription()
				.getServiceFQN().toString();
		return _serviceFQN.replaceAll("\\W", "_");
	}
}
