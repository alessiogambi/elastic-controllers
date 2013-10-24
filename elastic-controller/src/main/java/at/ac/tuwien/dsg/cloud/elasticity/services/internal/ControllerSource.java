package at.ac.tuwien.dsg.cloud.elasticity.services.internal;

import at.ac.tuwien.dsg.cloud.elasticity.services.ElasticController;

public interface ControllerSource {

	public ElasticController get(String controllerName);
}
