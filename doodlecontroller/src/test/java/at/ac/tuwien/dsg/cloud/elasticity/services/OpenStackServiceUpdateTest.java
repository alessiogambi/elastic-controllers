package at.ac.tuwien.dsg.cloud.elasticity.services;

import java.util.UUID;

import org.apache.tapestry5.ioc.IOCUtilities;
import org.apache.tapestry5.ioc.Registry;
import org.apache.tapestry5.ioc.RegistryBuilder;

import at.ac.tuwien.dsg.cloud.data.DynamicServiceDescription;
import at.ac.tuwien.dsg.cloud.data.StaticServiceDescription;
import at.ac.tuwien.dsg.cloud.elasticity.modules.DoodleElasticControlModule;
import at.ac.tuwien.dsg.cloud.elasticity.modules.DoodleServiceModule;
import at.ac.tuwien.dsg.cloud.exceptions.ServiceDeployerException;
import at.ac.tuwien.dsg.cloud.manifest.StaticServiceDescriptionFactory;
import ch.usi.cloud.controller.common.naming.FQN;

public class OpenStackServiceUpdateTest {

	public static void main(String[] args) {

		try {
			UUID deployID = UUID
					.fromString("ca55d418-152e-44a9-945b-5a3c8aa24242");
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

			// Note this is an OpenStackServiceUpdater
			ServiceUpdater updater = registry.getService("OSServiceUpdater",
					ServiceUpdater.class);

			StaticServiceDescription _service = new StaticServiceDescription(
					serviceFQN, StaticServiceDescriptionFactory.fromURL(
							manifestURL).getOrderedVees());

			DynamicServiceDescription service = new DynamicServiceDescription(
					_service, deployID);

			updater.update(service);

			System.out
					.println("OpenStackServiceUpdateTest.main() Updated Service "
							+ service);
			System.out.println("OpenStackServiceUpdateTest.main()"
					+ service.getFirstNullReplicaNum("appserver"));

		} catch (ServiceDeployerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
