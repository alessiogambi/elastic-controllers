package at.ac.tuwien.dsg.cloud.elasticity;

import java.util.ArrayList;
import java.util.Collection;

import javax.validation.ValidationException;

import org.apache.commons.cli.ParseException;
import org.apache.tapestry5.beanvalidator.BeanValidatorModule;
import org.apache.tapestry5.ioc.Registry;
import org.apache.tapestry5.ioc.RegistryBuilder;
import org.gambi.tapestry5.cli.services.CLIParser;

import at.ac.tuwien.dsg.cloud.data.DynamicServiceDescription;
import at.ac.tuwien.dsg.cloud.elasticity.modules.ElasticControllerModule;
import at.ac.tuwien.dsg.cloud.elasticity.services.ElasticController;
import at.ac.tuwien.dsg.cloud.elasticity.utils.ExtendedIOCUtilities;

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

		// Add all the modules found on the class path except to the
		// BeanValidatorModule
		Collection<Class> exclusionFilter = new ArrayList<Class>();
		exclusionFilter.add(BeanValidatorModule.class);
		ExtendedIOCUtilities.addDefaultModulesWithExclusion(builder,
				exclusionFilter);
		// Add all the locally defined modules
		builder.add(ElasticControllerModule.class);

		// Register the shutdown hook
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				// for operations done from this thread
				registry.cleanupThread();
				// call this to allow services clean shutdown
				registry.shutdown();
			}
		});

		// Build and start the registry
		registry = builder.build();
		registry.performRegistryStartup();

	}

	private void startController(String[] args) throws ValidationException,
			ParseException {

		// Parse the input options and commands
		CLIParser parser = registry.getService(CLIParser.class);
		parser.parse(args);

		// If everything is fine, then start the controller
		ElasticController controller = registry.getService(
				"CLIElasticController", ElasticController.class);
		controller.start();
	}

	public static void main(String[] args) {
		// THIS IS KIND OF NOT OPTIMAL - but it is required by the cloud-driver
		// Configurations passed via the SystemProperties
		// System.getProperties()
		// .put("at.ac.tuwien.dsg.cloud.configuration",
		// "/Users/alessiogambi/jopera-dev/org.jopera.subsystems.cloud/src/cloud.properties");
		try {
			Main main = new Main();
			main.startController(args);

			Thread.currentThread().join();
			// TOOD Wait the end quietly ? Maybe some stop signal ?

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		// Really needed ?!
	}
}
