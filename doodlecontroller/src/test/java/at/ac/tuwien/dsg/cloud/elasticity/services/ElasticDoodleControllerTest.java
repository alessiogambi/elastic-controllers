package at.ac.tuwien.dsg.cloud.elasticity.services;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.tapestry5.ioc.IOCUtilities;
import org.apache.tapestry5.ioc.Registry;
import org.apache.tapestry5.ioc.RegistryBuilder;

import at.ac.tuwien.dsg.cloud.data.DynamicServiceDescription;
import at.ac.tuwien.dsg.cloud.data.StaticServiceDescription;
import at.ac.tuwien.dsg.cloud.elasticity.data.DoodleSymbolConstants;
import at.ac.tuwien.dsg.cloud.elasticity.modules.DoodleElasticControlModule;
import at.ac.tuwien.dsg.cloud.elasticity.modules.DoodleServiceModule;
import at.ac.tuwien.dsg.cloud.manifest.StaticServiceDescriptionFactory;
import ch.usi.cloud.controller.common.naming.FQN;

public class ElasticDoodleControllerTest {

	public static void main(String[] args) {

		try {
			String _deployID = "321da878-4f8b-4963-8f55-daf854110465";
			UUID deployID = UUID.fromString(_deployID);
			String organizationName = "aaa";
			String customerName = "bbb";
			String serviceName = "ccc";
			FQN serviceFQN = new FQN(organizationName, customerName,
					serviceName);
			String manifestURL = "http://www.inf.usi.ch/phd/gambi/attachments/autocles/doodle-manifest.xml";

			System.getProperties()
					.put("at.ac.tuwien.dsg.cloud.configuration",
							"/Users/alessiogambi/jopera-dev/org.jopera.subsystems.cloud/src/cloud.properties");

			Registry registry;

			// Setup the registry and get the service instance
			RegistryBuilder builder = new RegistryBuilder();
			// Load all the modules in the class path that have the right
			// manifest
			// entries
			IOCUtilities.addDefaultModules(builder);
			// Add the local modules

			builder.add(at.ac.tuwien.dsg.cloud.modules.CloudAppModule.class);
			builder.add(at.ac.tuwien.dsg.cloud.openstack.modules.CloudAppModule.class);
			builder.add(DoodleElasticControlModule.class);
			builder.add(DoodleServiceModule.class);

			// Build and start the registry
			registry = builder.build();
			registry.performRegistryStartup();

			System.getProperties().put(DoodleSymbolConstants.MIN_JOBS, "1");
			System.getProperties().put(DoodleSymbolConstants.MAX_JOBS, "10");
			System.getProperties().put(
					DoodleSymbolConstants.COOL_PERIOD_SCALE_DOWN, "1000");
			System.getProperties().put(
					DoodleSymbolConstants.COOL_PERIOD_SCALE_DOWN_UNIT,
					TimeUnit.MILLISECONDS.name());

			ElasticController controller = registry.getService(
					DoodleSymbolConstants.FIXED_RULE_BASED_CONTROLLER_NAME,
					ElasticController.class);

			StaticServiceDescription _service = new StaticServiceDescription(
					serviceFQN, StaticServiceDescriptionFactory.fromURL(
							manifestURL).getOrderedVees());

			DynamicServiceDescription service = new DynamicServiceDescription(
					_service, deployID);

			// This should be provided as INPUT in the real world via
			// customization parameters (can also be done inside the service but
			// I do not like it)

			registry.getService("OSServiceUpdater", ServiceUpdater.class)
					.update(service);

			String dbHost = service.getVeeInstances("frontend").get(0)
					.getPublicIp().getHostAddress();
			System.out.println("ElasticDoodleControllerTest.main() " + dbHost);
			System.getProperties().setProperty(DoodleSymbolConstants.DB_HOST,
					dbHost);

			controller.start(service);

			try {
				Thread.sleep(300 * 1000);
			} catch (Exception e) {
				e.printStackTrace();
			}

			controller.stop();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
