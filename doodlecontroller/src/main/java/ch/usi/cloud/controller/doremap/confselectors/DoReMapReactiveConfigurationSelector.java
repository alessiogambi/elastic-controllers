package ch.usi.cloud.controller.doremap.confselectors;

import java.util.Collection;
import java.util.List;

import javax.sql.DataSource;

import org.apache.log4j.Logger;

import cern.colt.Arrays;
import ch.usi.cloud.controller.ConfigurationSelector;
import ch.usi.cloud.controller.TrainingSampleSelectorAggregatedConfig;
import ch.usi.cloud.controller.UtilityFunctionModel;
import ch.usi.cloud.controller.common.ConfigurationFeature;
import ch.usi.cloud.controller.common.ManifestConnector;
import ch.usi.cloud.controller.common.ServiceConfiguration;
import ch.usi.cloud.controller.common.ServiceLevelObjective;
import ch.usi.cloud.controller.doremap.models.DoReMapModels;
import ch.usi.cloud.controller.impl.ConfigurationException;
import dk.ange.octave.OctaveEngine;
import dk.ange.octave.OctaveEngineFactory;

public class DoReMapReactiveConfigurationSelector extends ConfigurationSelector {

	protected OctaveEngine octave;
	protected UtilityFunctionModel utilityModel;
	private static Logger logger = Logger.getLogger(DoReMapReactiveConfigurationSelector.class);

	public DoReMapReactiveConfigurationSelector(long controlPeriod,
			ManifestConnector mc, DataSource ds, String serviceId) {
		super(controlPeriod, mc, ds, serviceId);
		octave = new OctaveEngineFactory().getScriptEngine();		
	}

	@Override
	public void initModels() {
		models = DoReMapModels.initUtilityModel(mc, serviceFQN, octave);
		utilityModel = (UtilityFunctionModel) models.get(models.size() - 1);
		this.tss = new TrainingSampleSelectorAggregatedConfig(this.monitoringDs, models);
	}

	/**
	 * This configuration selector collects the average of the request counts and queue lengths
	 * over a period and uses them to estimate the workload intensity and mix.
	 * With this estimate, it queries a model representing a utility function 
	 * considering SLOs and VM usage costs. The model can be the composition of several other 
	 * Kriging models.
	 */

	@Override
	protected ServiceConfiguration getTargetConfiguration()
			throws ConfigurationException {

		logger.debug("Querying avg values for: " + DoReMapModels.getMonitoringKPINames());
		
		// get avg workload in a window
		double[] avgStatus = DoReMapModels.getAvgKPIInWindow(utilityModel, DoReMapModels.getMonitoringKPINames(),
				this.monitoringDs, 60000);
		
		logger.debug("Retrieved vaues: " + Arrays.toString(avgStatus));
		
		// query model to find conf with highest utility given this wkld
		
		double[] conf;
		try {
			conf = DoReMapModels.findConfWithMaxUtility(utilityModel, avgStatus);
			return  convertArraytoConfiguration(currentConfiguration, conf, utilityModel);
		} catch (Exception e) {	
			// suppress printing stack trace
			// a warning is issued at the exception thrower
			// keep the current conf
			return currentConfiguration;
		}
		
		
		
	}

	@Override
	protected ConfigurationFeature getOutputFeature(String kPIName) {
		System.err
				.println("Unimplemented Method: getOutputFeature in DoReMapReactiveConfigurationSelector");
		return null;
	}

	@Override
	protected Collection<? extends ConfigurationFeature> getInputFeatures(
			List<String> kpiList, ServiceLevelObjective slo) {
		return DoReMapModels.getInputFeatures(kpiList, slo);

	}

}
