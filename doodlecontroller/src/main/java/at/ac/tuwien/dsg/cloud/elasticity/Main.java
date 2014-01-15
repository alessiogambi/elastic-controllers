package at.ac.tuwien.dsg.cloud.elasticity;

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
import at.ac.tuwien.dsg.cloud.elasticity.services.ElasticController;
import at.ac.tuwien.dsg.cloud.manifest.StaticServiceDescriptionFactory;
import at.ac.tuwien.dsg.cloud.modules.CloudAppModule;
import at.ac.tuwien.dsg.cloud.openstack.modules.OSCloudAppModule;
import ch.usi.cloud.controller.common.naming.FQN;

/**
 * This class setup the registry load all the modules and start the various
 * components of the controller
 * 
 * @author alessiogambi
 * 
 */
public class Main {

	private Registry registry;

	public Main() {
		RegistryBuilder builder = new RegistryBuilder();
		// Load all the modules in the class path that have the right manifest
		// entries - Apparently this do not work so fine
		IOCUtilities.addDefaultModules(builder);

		// Add the local modules
		builder.add(DoodleServiceModule.class);
		builder.add(DoodleElasticControlModule.class);
		builder.add(CloudAppModule.class);
		builder.add(OSCloudAppModule.class);

		// Register the shutdown hook
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				try {
					// for operations done from this thread
					registry.cleanupThread();
					// call this to allow services clean shutdown
					registry.shutdown();
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		});

		// Build and start the registry
		registry = builder.build();
		registry.performRegistryStartup();

	}

	private void startController(DynamicServiceDescription service,
			String controllerName) {
		ElasticController controller = registry.getService(controllerName,
				ElasticController.class);

		controller.start(service);
	}

	public static void main(String[] args) {

		if (args.length < 5) {
			throw new RuntimeException(
					"Not enough parameters ! You must provide at least 5 params");
		}
		String organizationName = args[0];
		String customerName = args[1];
		String serviceName = args[2];

		UUID deployID = UUID.fromString(args[3]);

		String manifestURL = args[4];
		String _controllerName = args[5];

		FQN serviceFQN = new FQN(organizationName, customerName, serviceName);

		// TODO: Expose inputs as symbold !
		System.getProperties().put("args:deployID", deployID.toString());
		System.getProperties().put("args:serviceFQN", serviceFQN.toString());

		// This also should be a distributed contribution !!!
		// NOTE Names can be listed by the program itself !
		// TODO Controller Name Provider
		String controllerName = null;
		if ("rules-fixed".equalsIgnoreCase(_controllerName)) {
			controllerName = DoodleSymbolConstants.FIXED_RULE_BASED_CONTROLLER_NAME;
		} else if ("rules-proportional".equalsIgnoreCase(_controllerName)) {
			controllerName = DoodleSymbolConstants.PROPORTIONAL_RULE_BASED_CONTROLLER_NAME;
		} else if ("rules-fixed-nb".equalsIgnoreCase(_controllerName)) {
			controllerName = DoodleSymbolConstants.FIXED_RULE_BASED_CONTROLLER_NAME_NON_BLOCKING;
		} else if ("rules-proportional-nb".equalsIgnoreCase(_controllerName)) {
			controllerName = DoodleSymbolConstants.PROPORTIONAL_RULE_BASED_CONTROLLER_NAME_NON_BLOCKING;
		} else {
			throw new RuntimeException("Invalid controller name !"
					+ controllerName);
		}

		System.out.println("Main.main() ControllerNAME " + _controllerName
				+ " --> " + controllerName);

		// Configurations passed via the SystemProperties
		// System.getProperties()
		// .put("at.ac.tuwien.dsg.cloud.configuration",
		// "/Users/alessiogambi/jopera-dev/org.jopera.subsystems.cloud/src/cloud.properties");

		StaticServiceDescription _service;
		try {
			_service = new StaticServiceDescription(serviceFQN,
					StaticServiceDescriptionFactory.fromURL(manifestURL)
							.getOrderedVees(), new URL(manifestURL));
			DynamicServiceDescription service = new DynamicServiceDescription(
					_service, deployID);

			Main main = new Main();
			main.startController(service, controllerName);

			// ?
			Thread.currentThread().join();
			// TOOD Wait the end quietly ? Maybe some stop signal ?

		} catch (Exception e) {
			e.printStackTrace();
		}
		// Really needed ?!
		// System.exit(0);
	}
}
