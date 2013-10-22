package at.ac.tuwien.dsg.cloud.elasticity.advices;

import java.util.UUID;

import org.apache.tapestry5.ioc.IOCUtilities;
import org.apache.tapestry5.ioc.Registry;
import org.apache.tapestry5.ioc.RegistryBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import at.ac.tuwien.dsg.cloud.data.DynamicServiceDescription;
import at.ac.tuwien.dsg.cloud.data.StaticServiceDescription;
import at.ac.tuwien.dsg.cloud.elasticity.modules.DoodleElasticControlModule;
import at.ac.tuwien.dsg.cloud.elasticity.modules.DoodleServiceModule;
import at.ac.tuwien.dsg.cloud.elasticity.services.ServiceUpdater;
import at.ac.tuwien.dsg.cloud.manifest.StaticServiceDescriptionFactory;
import ch.usi.cloud.controller.common.naming.FQN;

public class CacheAdviseTest {
	private Registry registry;
	private FQN serviceFQN;
	private UUID deployID;

	@Before
	public void setup() {

		deployID = UUID.fromString("7cf6674b-e7c7-4376-ad01-06c43f783d7b");

		String organizationName = "dsg";
		String customerName = "aaa";
		String serviceName = "aaa";
		FQN serviceFQN = new FQN(organizationName, customerName, serviceName);

		System.getProperties().put("at.ac.tuwien.dsg.cloud.configuration",
				"src/test/resources/cloud.properties");

		System.getProperties().put("args:serviceFQN", serviceFQN.toString());
		System.getProperties().put("args:deployID", deployID.toString());

		RegistryBuilder builder = new RegistryBuilder();
		// Load all the modules in the class path that have the right manifest
		// entries - Apparently this do not work so fine
		IOCUtilities.addDefaultModules(builder);

		// Add the local modules
		builder.add(DoodleServiceModule.class);
		builder.add(DoodleElasticControlModule.class);

		registry = builder.build();
		registry.performRegistryStartup();
	}

	@Test
	public void fail() {
		try {
			ServiceUpdater serviceUpdater = registry
					.getService(ServiceUpdater.class);
			Assert.fail("Exception not raised");
		} catch (Exception e) {
			System.out.println("CacheAdviseTest.fail() OK");
			e.printStackTrace();
		}
	}

	@Test
	public void advise() {
		try {

			String manifestURL = "http://www.inf.usi.ch/phd/gambi/attachments/autocles/doodle-manifest.xml";

			ServiceUpdater updater = registry.getService("OSServiceUpdater",
					ServiceUpdater.class);

			StaticServiceDescription _service = new StaticServiceDescription(
					serviceFQN, StaticServiceDescriptionFactory.fromURL(
							manifestURL).getOrderedVees());

			DynamicServiceDescription currentConfiguration = new DynamicServiceDescription(
					_service, deployID);

			updater.update(currentConfiguration);

		} catch (Exception e) {
		}
	}

	@After
	public void shutdown() {
		registry.shutdown();

	}
}
