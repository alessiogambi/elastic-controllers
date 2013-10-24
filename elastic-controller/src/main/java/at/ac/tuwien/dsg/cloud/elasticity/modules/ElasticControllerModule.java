package at.ac.tuwien.dsg.cloud.elasticity.modules;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.cli.Option;
import org.apache.tapestry5.ioc.Configuration;
import org.apache.tapestry5.ioc.MappedConfiguration;
import org.apache.tapestry5.ioc.OrderedConfiguration;
import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.ioc.annotations.InjectService;
import org.apache.tapestry5.ioc.annotations.ServiceId;
import org.apache.tapestry5.ioc.annotations.SubModule;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.ioc.services.SymbolSource;
import org.gambi.tapestry5.cli.CLIModule;
import org.gambi.tapestry5.cli.services.CLIValidator;
import org.gambi.tapestry5.cli.services.CLIValidatorFilter;
import org.slf4j.Logger;

import at.ac.tuwien.dsg.cloud.data.DynamicServiceDescription;
import at.ac.tuwien.dsg.cloud.data.StaticServiceDescription;
import at.ac.tuwien.dsg.cloud.elasticity.data.ElasticControllerConfiguration;
import at.ac.tuwien.dsg.cloud.elasticity.modules.submodules.Coercions;
import at.ac.tuwien.dsg.cloud.elasticity.services.ElasticController;
import at.ac.tuwien.dsg.cloud.elasticity.services.internal.ControllerSource;
import at.ac.tuwien.dsg.cloud.elasticity.services.internal.impl.ControllerSourceImpl;
import at.ac.tuwien.dsg.cloud.manifest.StaticServiceDescriptionFactory;
import ch.usi.cloud.controller.common.naming.FQN;
import ch.usi.cloud.controller.common.naming.FQNType;

@SubModule({ CLIModule.class, Coercions.class })
public class ElasticControllerModule {

	public static void bind(ServiceBinder binder) {
		binder.bind(ControllerSource.class, ControllerSourceImpl.class);
	}

	public static void contributeApplicationDefaults(
			MappedConfiguration<String, String> defaults) {
		/*
		 * Define the name that will be show in the help page of the CLI module
		 */
		defaults.add("org.gambi.tapestry5.cli.command.name", "controller");
	}

	public void contributeCLIValidator(final Logger logger,
			OrderedConfiguration<CLIValidatorFilter> configuration) {

		/*
		 * Add a CLIValidator to check that either the <o,c,s> options or the
		 * <S> option are specified, but not both
		 */
		configuration.add("ServiceFQNValidator", new CLIValidatorFilter() {

			private List<String> validate(Map<String, String> inputs) {
				List<String> failedValidation = new ArrayList<String>();
				try {
					if (inputs.get("args:service-FQN") != null
							&& (inputs.get("args:organization-name") != null
									|| inputs.get("args:customer-name") != null || inputs
									.get("args:service-name") != null)) {

						failedValidation
								.add("Specify either S/service-FQN, or organization , customer, and service names, but not ALL at the same time");
					} else if (inputs.get("args:service-FQN") == null
							&& (inputs.get("args:organization-name") == null
									|| inputs.get("args:customer-name") == null || inputs
									.get("args:service-name") == null)) {

						failedValidation
								.add("If S/service-FQN is not specified, then organization, customer, and service names cannot be null");
					}

				} catch (Exception e) {
					logger.warn("Error during validation ", e);
					failedValidation.add("SumValidation Failed!");
				}
				return failedValidation;
			}

			public void validate(Map<String, String> inputs,
					List<String> accumulator, CLIValidator delegate) {

				delegate.validate(inputs, accumulator);

				accumulator.addAll(validate(inputs));
			}
		});

	}

	/**
	 * Build a DynamicServiceDescription of the current service and export that
	 * as plain service
	 * 
	 * @return
	 */
	@ServiceId("Service")
	public DynamicServiceDescription buildService(
			@InjectService("ServiceFQN") FQN serviceFQN, // Note this
			@Symbol("args:service-manifest-URL") String manifestURL,
			@Symbol("args:deploy-ID") UUID deployID) {
		StaticServiceDescription _service;
		try {
			_service = new StaticServiceDescription(serviceFQN,
					StaticServiceDescriptionFactory.fromURL(manifestURL)
							.getOrderedVees());
			return new DynamicServiceDescription(_service, deployID);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Contribute the main options that we expect on the command line.
	 * 
	 * @param contributions
	 */
	public void contributeCLIParser(Configuration<Option> contributions) {
		contributions.add(new Option("o", "organization-name", true,
				"Organization name."));
		contributions.add(new Option("c", "customer-name", true,
				"Customer name."));
		contributions
				.add(new Option("s", "service-name", true, "Service name"));

		contributions.add(new Option("S", "service-FQN", true,
				"The FQN for the service"));

		contributions.add(new Option("m", "service-manifest-URL", true,
				"URL of the Service manifest file"));
		contributions.add(new Option("d", "deploy-ID", true,
				"UUID for the deploy"));

		contributions.add(new Option("C", "controller-name", true,
				"A valid name for the controller"));
	}

	/**
	 * This acts as syntactic sugar from the CLIoptions to the actual services
	 * 
	 * @param symbolSource
	 * @return
	 */
	@ServiceId("ServiceFQN")
	public FQN buildServiceFQN(SymbolSource symbolSource) {
		try {
			return new FQN(symbolSource.valueForSymbol("args:service-FQN"),
					FQNType.SERVICE);
		} catch (Throwable e) {
			// If the symbol is not provided then use the combination of the
			// other options or fail
			return new FQN(
					symbolSource.valueForSymbol("args:organization-name"),
					symbolSource.valueForSymbol("args:customer-name"),
					symbolSource.valueForSymbol("args:service-name"));
		}
	}

	/**
	 * Contribute the configuration object for the ElasticController. This class
	 * contains all the validation constraints that we require from the input
	 * CLI options
	 * 
	 * @param configuration
	 */
	public static void contributeApplicationConfigurationSource(
			MappedConfiguration<String, Object> configuration) {
		configuration.addInstance("ElasticController",
				ElasticControllerConfiguration.class);
	}

	@ServiceId("CLIElasticController")
	public ElasticController buildDefaultElasticController(
			@Symbol("args:controller-name") String controllerName,
			ControllerSource controllerSource) {
		return controllerSource.get(controllerName);
	}

}
