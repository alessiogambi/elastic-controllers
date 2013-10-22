package at.ac.tuwien.dsg.cloud.elasticity.services.impl.configurationselectors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.log4j.Logger;

import ch.usi.cloud.controller.NullTrainingSampleSelector;
import ch.usi.cloud.controller.common.ManifestConnector;
import ch.usi.cloud.controller.impl.AbstractScriptConfigurationSelector;
import ch.usi.cloud.controller.util.ScriptParser;
import ch.usi.cloud.controller.util.ScriptParser.Parameter;
import dk.ange.octave.OctaveEngine;
import dk.ange.octave.OctaveEngineFactory;

/**
 * 
 * @author Alessi Gambi (alessio.gambi@usi.ch)
 * 
 */
public class DoodleEmptyScriptConfigurationSelector extends
		AbstractScriptConfigurationSelector {

	protected Logger logger = Logger
			.getLogger(DoodleEmptyScriptConfigurationSelector.class);
	protected OctaveEngine octave;

	// public static String octgprLocation =
	// "/usr/lib/octave/packages/3.2/octgpr-1.1.5";
	// public static String octaveScriptsLocation =
	// "/usr/lib/octave/packages/3.2/octgpr-1.1.5/x86_64-pc-linux-gnu-api-v37";

	// InetAddress workloadGen;
	// InetAddress frontendIp;
	// Integer loadbalancerPort;

	private long sampleDurationMillis;
	private final long DEFAULT_SAMPLE_DURATION_MILLIS = 30 * 1000;

	public DoodleEmptyScriptConfigurationSelector(long controlPeriod,
			ManifestConnector mc, DataSource monitoringDs, String serviceId)
			throws Exception {
		super(controlPeriod, mc, monitoringDs, null, serviceId);

		tss = new NullTrainingSampleSelector(this.getDataSource(), models);
		octave = new OctaveEngineFactory().getScriptEngine();

		try {
			sampleDurationMillis = Long.parseLong(System.getProperty(
					"sample.durationMillis", ""
							+ DEFAULT_SAMPLE_DURATION_MILLIS));
		} catch (Exception e) {
			logger.warn("Invalid value for sample.durationMillis. Use default",
					e);
			sampleDurationMillis = DEFAULT_SAMPLE_DURATION_MILLIS;
		}
		
		// logger.info("System.getProperty(SystemPropertyNames.OCTGPR_ABSOLUTE_PATH "
		// + System.getProperty(SystemPropertyNames.OCTGPR_ABSOLUTE_PATH));
		// if (System.getProperty(SystemPropertyNames.OCTGPR_ABSOLUTE_PATH) !=
		// null) {
		// octgprLocation =
		// System.getProperty(SystemPropertyNames.OCTGPR_ABSOLUTE_PATH);
		// }
		//
		// logger.info("System.getProperty(SystemPropertyNames.OCTGPR_ADDITIONAL_PATH "
		// + System.getProperty(SystemPropertyNames.OCTGPR_ADDITIONAL_PATH));
		//
		// if (System.getProperty(SystemPropertyNames.OCTGPR_ADDITIONAL_PATH) !=
		// null) {
		// octaveScriptsLocation =
		// System.getProperty(SystemPropertyNames.OCTGPR_ADDITIONAL_PATH);
		// }
		//
		// octave.eval("addpath('" + octaveScriptsLocation + "')");
		// octave.eval("addpath('" + octgprLocation + "')");
	}

	@Override
	public void stopWorkload() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void collectData() {
		logger.info("Collecting data, i.e. sleep for  " + sampleDurationMillis);
		try {
			Thread.currentThread().sleep(sampleDurationMillis);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void startWorkload() {

	}

	/*
	 * No additional parameter
	 */
	@Override
	protected Map<Parameter, ArrayList<Parameter>> getScriptAdditionalParameters() {
		return new HashMap<ScriptParser.Parameter, ArrayList<Parameter>>();
	}

	/**
	 * This implementation of InitModels is App-specific and depends on the
	 * Manifest and the exposed KPIs
	 */

	@Override
	public void initModels() {

		// No need for models
		this.tss = new NullTrainingSampleSelector(this.getDataSource(), models);
	}

	@Override
	public void init() throws Exception {

		super.init();
		initModels();
	}
}
