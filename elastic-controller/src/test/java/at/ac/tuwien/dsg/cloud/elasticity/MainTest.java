package at.ac.tuwien.dsg.cloud.elasticity;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.tapestry5.beanvalidator.BeanValidatorModule;
import org.apache.tapestry5.ioc.Registry;
import org.apache.tapestry5.ioc.RegistryBuilder;
import org.gambi.tapestry5.cli.services.CLIParser;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import at.ac.tuwien.dsg.cloud.elasticity.modules.ElasticControllerModule;
import at.ac.tuwien.dsg.cloud.elasticity.modules.TestModule;
import at.ac.tuwien.dsg.cloud.elasticity.services.ElasticController;
import at.ac.tuwien.dsg.cloud.elasticity.utils.ExtendedIOCUtilities;
import at.ac.tuwien.dsg.cloud.utils.CloudSymbolConstants;

public class MainTest {

	// Tapestry Registry for DI-IoC
	private static Registry registry;

	@Before
	public void setup() {

		// SETUP THE ENVIRONMENT: NOTE THAT THIS IS REALLY ANNOYNG !
		// NOTE THAT THIS SHOULD BE PASSED VIA COMMAND LINE TOO
		System.getProperties().put(
				"log4j.configuration",
				"file://"
						+ (new File("src/test/resources/log4j.properties"))
								.getAbsolutePath());
		// Options are specified via the cloud.properties file:
		System.getProperties().put(
				CloudSymbolConstants.CONFIGURATION_FILE,
				(new File("src/test/resources/cloud.properties"))
						.getAbsolutePath());

		RegistryBuilder builder = new RegistryBuilder();

		Collection<Class> exclusionFilter = new ArrayList<Class>();
		exclusionFilter.add(BeanValidatorModule.class);
		ExtendedIOCUtilities.addDefaultModulesWithExclusion(builder,
				exclusionFilter);

		builder.add(ElasticControllerModule.class);

		// Add also the TESTMOdule
		builder.add(TestModule.class);

		registry = builder.build();
		registry.performRegistryStartup();
	}

	@After
	public void shutdown() {
		registry.shutdown();
	}

	@Test
	public void CLIInputTest() {
		String[] args = new String[] {
				"-o",
				"dsg",
				"-c",
				"ale",
				"-s",
				"ale",
				"-m",
				"http://www.inf.usi.ch/phd/gambi/attachments/autocles/doodle-manifest.xml",
				"-d", "d5de4711-3044-41f8-80cd-1591f21f3108", "-C", "empty" };

		try {
			// This can generate exception if parsing or validation fail !
			CLIParser parser = registry.getService(CLIParser.class);
			parser.parse(args);

		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("An Exception was generated");
		}
	}

	@Test
	public void CLIInputTest1() {
		String[] args = new String[] {
				"-S",
				"dsg.customers.dev.services.dev",
				"-m",
				"http://www.inf.usi.ch/phd/gambi/attachments/autocles/doodle-manifest.xml",
				"-d", "d5de4711-3044-41f8-80cd-1591f21f3108", "-C", "empty" };

		try {
			// This can generate exception if parsing or validation fail !
			CLIParser parser = registry.getService(CLIParser.class);
			parser.parse(args);

		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("An Exception was generated");
		}
	}

	@Test
	public void CLIInputTest2() {
		String[] args = new String[] {
				"-o",
				"dsg",
				"-c",
				"ale",
				"-s",
				"ale",
				"-S",
				"dsg.customers.dev.services.dev",
				"-m",
				"http://www.inf.usi.ch/phd/gambi/attachments/autocles/doodle-manifest.xml",
				"-d", "d5de4711-3044-41f8-80cd-1591f21f3108", "-C", "empty" };

		try {
			// This can generate exception if parsing or validation fail !
			CLIParser parser = registry.getService(CLIParser.class);
			parser.parse(args);
			Assert.fail("An Exception was not generated");
		} catch (Exception e) {
		}
	}

	@Test
	public void startEmptyController() {
		String[] args = new String[] {
				"-S",
				"dsg.customers.dev.services.dev",
				"-m",
				"http://www.inf.usi.ch/phd/gambi/attachments/autocles/doodle-manifest.xml",
				"-d", "d5de4711-3044-41f8-80cd-1591f21f3108", "-C", "empty" };

		try {
			// This can generate exception if parsing or validation fail !
			CLIParser parser = registry.getService(CLIParser.class);
			parser.parse(args);

			ElasticController controller = registry.getService(
					"CLIElasticController", ElasticController.class);

			controller.start();
		} catch (Exception e) {
			Assert.fail("An Exception was generated");
		}
	}

	@Test
	public void startCloudEmptyController() {
		String[] args = new String[] {
				"-S",
				"dsg.customers.dev.services.dev",
				"-m",
				"http://www.inf.usi.ch/phd/gambi/attachments/autocles/doodle-manifest.xml",
				"-d", "d5de4711-3044-41f8-80cd-1591f21f3108", "-C",
				"cloud-empty" };

		try {
			// This can generate exception if parsing or validation fail !
			CLIParser parser = registry.getService(CLIParser.class);
			parser.parse(args);

			ElasticController controller = registry.getService(
					"CLIElasticController", ElasticController.class);

			controller.start();
		} catch (Exception e) {
			Assert.fail("An Exception was generated");
		}
	}
}
