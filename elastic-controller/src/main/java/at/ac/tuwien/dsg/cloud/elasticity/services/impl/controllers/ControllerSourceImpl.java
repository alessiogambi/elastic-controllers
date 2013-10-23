package at.ac.tuwien.dsg.cloud.elasticity.services.impl.controllers;

import java.util.Map;

import at.ac.tuwien.dsg.cloud.elasticity.services.ControllerSource;
import at.ac.tuwien.dsg.cloud.elasticity.services.ElasticController;

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
