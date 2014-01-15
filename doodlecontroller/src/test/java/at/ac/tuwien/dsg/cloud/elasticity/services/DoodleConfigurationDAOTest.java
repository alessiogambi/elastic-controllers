package at.ac.tuwien.dsg.cloud.elasticity.services;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

import org.apache.tapestry5.ioc.IOCUtilities;
import org.apache.tapestry5.ioc.Registry;
import org.apache.tapestry5.ioc.RegistryBuilder;

import at.ac.tuwien.dsg.cloud.data.DynamicServiceDescription;
import at.ac.tuwien.dsg.cloud.data.StaticServiceDescription;
import at.ac.tuwien.dsg.cloud.elasticity.data.DoodleSymbolConstants;
import at.ac.tuwien.dsg.cloud.elasticity.modules.DoodleElasticControlModule;
import at.ac.tuwien.dsg.cloud.elasticity.modules.DoodleServiceModule;
import at.ac.tuwien.dsg.cloud.exceptions.ServiceDeployerException;
import at.ac.tuwien.dsg.cloud.manifest.StaticServiceDescriptionFactory;
import at.ac.tuwien.dsg.cloud.modules.CloudAppModule;
import at.ac.tuwien.dsg.cloud.openstack.modules.OSCloudAppModule;
import ch.usi.cloud.controller.common.naming.FQN;

public class DoodleConfigurationDAOTest {
	public static void main(String[] args) throws ServiceDeployerException,
			MalformedURLException {

		UUID deployID = UUID.fromString("bc9afd03-e397-4635-9366-6ed09634c1c5");

		String organizationName = "aaa";
		String customerName = "bbb";
		String serviceName = "ccc";

		FQN serviceFQN = new FQN(organizationName, customerName, serviceName);
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

		StaticServiceDescription _service = new StaticServiceDescription(
				serviceFQN, StaticServiceDescriptionFactory
						.fromURL(manifestURL).getOrderedVees(), new URL(manifestURL));

		DynamicServiceDescription service = new DynamicServiceDescription(
				_service, deployID);

		// This should be provided as INPUT in the real world via
		// customization parameters (can also be done inside the service but
		// I do not like it)

		registry.getService("OSServiceUpdater", ServiceUpdater.class).update(
				service);

		// Note this is an OpenStackServiceUpdater
		ConfigurationSelectorDAO dao = registry.getService(
				DoodleSymbolConstants.RULE_BASED_CONFIGURATION_SELECTOR_DAO,
				ConfigurationSelectorDAO.class);

		System.out.println("DoodleConfigurationDAOTest.main() " + dao);
		dao.createActivationDataTable(service);
		dao.createTargetConfigurationTable(service);
		dao.storeTargetConfiguration(service);
		dao.storeActivationData(service, 10.0);
	}
}
