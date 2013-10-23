package at.ac.tuwien.dsg.cloud.elasticity.modules;

import org.apache.tapestry5.ioc.MappedConfiguration;

import at.ac.tuwien.dsg.cloud.elasticity.services.ElasticController;
import at.ac.tuwien.dsg.cloud.elasticity.services.impl.controllers.CloudEmptyElasticController;

public class TestModule {
	public static void contributeControllerSource(
			MappedConfiguration<String, ElasticController> configurations) {

		// Default
		configurations.add("empty", new ElasticController() {

			@Override
			public void stop() {
			}

			@Override
			public void start() {
			}
		});

		configurations.addInstance("cloud-empty",
				CloudEmptyElasticController.class);
	}
}
