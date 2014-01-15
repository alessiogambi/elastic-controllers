package at.ac.tuwien.dsg.cloud.elasticity.services;

import java.net.URL;
import java.util.UUID;

import org.apache.tapestry5.ioc.IOCUtilities;
import org.apache.tapestry5.ioc.Registry;
import org.apache.tapestry5.ioc.RegistryBuilder;

import at.ac.tuwien.dsg.cloud.data.DynamicServiceDescription;
import at.ac.tuwien.dsg.cloud.data.StaticServiceDescription;
import at.ac.tuwien.dsg.cloud.data.VeeDescription;
import at.ac.tuwien.dsg.cloud.elasticity.modules.DoodleElasticControlModule;
import at.ac.tuwien.dsg.cloud.elasticity.modules.DoodleServiceModule;
import at.ac.tuwien.dsg.cloud.manifest.StaticServiceDescriptionFactory;
import at.ac.tuwien.dsg.cloud.modules.CloudAppModule;
import at.ac.tuwien.dsg.cloud.openstack.modules.OSCloudAppModule;
import ch.usi.cloud.controller.common.naming.FQN;

public class ControlInterfaceActuatorTest {

	public static void main(String[] args) {
		try {
			UUID deployID = UUID
					.fromString("7cf6674b-e7c7-4376-ad01-06c43f783d7b");

			String organizationName = "dsg";
			String customerName = "aaa";
			String serviceName = "aaa";
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

			builder.add(CloudAppModule.class);
			builder.add(OSCloudAppModule.class);
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
							manifestURL).getOrderedVees(), new URL(manifestURL));

			DynamicServiceDescription currentConfiguration = new DynamicServiceDescription(
					_service, deployID);
			updater.update(currentConfiguration);

			ConfigurationActuator actuator = registry.getService(
					"OSBlockingConfigurationActuator",
					ConfigurationActuator.class);

			VeeDescription vee = currentConfiguration
					.getVeeDescription("appserver");
			String veeName = vee.getName();

			int replicaNumber = currentConfiguration.getReplicaNum(veeName)
					.incrementAndGet();

			organizationName = currentConfiguration
					.getStaticServiceDescription().getServiceFQN()
					.getOrganizationName();
			customerName = currentConfiguration.getStaticServiceDescription()
					.getServiceFQN().getCustomerName();
			serviceName = currentConfiguration.getStaticServiceDescription()
					.getServiceFQN().getServiceName();

			FQN replicaFQN = new FQN(organizationName, customerName,
					serviceName, "", veeName, replicaNumber);

			// Deep Copy like of the current configuration
			DynamicServiceDescription targetConfiguration = new DynamicServiceDescription(
					currentConfiguration);

			// targetConfiguration.addVeeInstance(vee, new InstanceDescription(
			// replicaFQN, "", "", "", null, null));
			//
			// replicaFQN = new FQN(organizationName, customerName, serviceName,
			// "", veeName, replicaNumber++);
			//
			// targetConfiguration.addVeeInstance(vee, new InstanceDescription(
			// replicaFQN, "", "", "", null, null));
			//
			// replicaFQN = new FQN(organizationName, customerName, serviceName,
			// "", veeName, replicaNumber++);
			//
			// targetConfiguration.addVeeInstance(vee, new InstanceDescription(
			// replicaFQN, "", "", "", null, null));

			targetConfiguration.removeLastReplica(vee);
			// targetConfiguration.removeLastReplica( vee );
			// targetConfiguration.removeLastReplica( vee );

			actuator.actuate(currentConfiguration, targetConfiguration);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
