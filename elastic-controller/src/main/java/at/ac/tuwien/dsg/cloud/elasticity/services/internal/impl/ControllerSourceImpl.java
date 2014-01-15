package at.ac.tuwien.dsg.cloud.elasticity.services.internal.impl;

import java.util.Map;

import at.ac.tuwien.dsg.cloud.elasticity.services.ElasticController;
import at.ac.tuwien.dsg.cloud.elasticity.services.internal.ControllerSource;

public class ControllerSourceImpl implements ControllerSource {

	private Map<String, ElasticController> contributions;

	public ControllerSourceImpl(Map<String, ElasticController> contributions) {
		this.contributions = contributions;
	}

	@Override
	public ElasticController get(String controllerName) {
		if (contributions.containsKey(controllerName)) {
			return contributions.get(controllerName);
		} else {
			throw new RuntimeException(String.format(
					"Elastic controller %s is not defined!", controllerName));
		}
	}

}