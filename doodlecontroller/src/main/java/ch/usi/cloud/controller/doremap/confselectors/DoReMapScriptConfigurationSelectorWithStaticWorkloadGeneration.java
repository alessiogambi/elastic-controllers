package ch.usi.cloud.controller.doremap.confselectors;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.log4j.Logger;

import ch.usi.cloud.controller.NullTrainingSampleSelector;
import ch.usi.cloud.controller.common.ManifestConnector;
import ch.usi.cloud.controller.common.SystemPropertyNames;
import ch.usi.cloud.controller.common.util.JSchUtils;
import ch.usi.cloud.controller.common.util.StringBooleanCouple;
import ch.usi.cloud.controller.impl.AbstractScriptConfigurationSelector;
import ch.usi.cloud.controller.impl.TrivialTrainingSampleSelector;
import ch.usi.cloud.controller.util.ScriptParser;
import ch.usi.cloud.controller.util.ScriptParser.Parameter;

import com.jcraft.jsch.JSchException;

import dk.ange.octave.OctaveEngine;
import dk.ange.octave.OctaveEngineFactory;

/**
 * 
 * @author Mario Bisignani (bisignam@usi.ch)
 * 
 */
public class DoReMapScriptConfigurationSelectorWithStaticWorkloadGeneration extends AbstractScriptConfigurationSelector {

	protected Logger logger = Logger.getLogger(DoReMapScriptConfigurationSelectorWithStaticWorkloadGeneration.class);
	protected OctaveEngine octave;
	public static String octgprLocation = "/usr/lib/octave/packages/3.2/octgpr-1.1.5";
	public static String octaveScriptsLocation = "/usr/lib/octave/packages/3.2/octgpr-1.1.5/x86_64-pc-linux-gnu-api-v37";

	
	InetAddress workloadGen;
	InetAddress frontendIp;
	Integer loadbalancerPort;

	public DoReMapScriptConfigurationSelectorWithStaticWorkloadGeneration(
			long controlPeriod,
			ManifestConnector mc,
			DataSource monitoringDs,
			String serviceId) throws Exception {
		super(controlPeriod, mc, monitoringDs, null, serviceId);
		
		tss = new TrivialTrainingSampleSelector(this.getDataSource(), models);
		octave = new OctaveEngineFactory().getScriptEngine();

		logger.info("System.getProperty(SystemPropertyNames.OCTGPR_ABSOLUTE_PATH "
				+ System.getProperty(SystemPropertyNames.OCTGPR_ABSOLUTE_PATH));
		if (System.getProperty(SystemPropertyNames.OCTGPR_ABSOLUTE_PATH) != null) {
			octgprLocation = System.getProperty(SystemPropertyNames.OCTGPR_ABSOLUTE_PATH);
		}

		logger.info("System.getProperty(SystemPropertyNames.OCTGPR_ADDITIONAL_PATH "
				+ System.getProperty(SystemPropertyNames.OCTGPR_ADDITIONAL_PATH));

		if (System.getProperty(SystemPropertyNames.OCTGPR_ADDITIONAL_PATH) != null) {
			octaveScriptsLocation = System.getProperty(SystemPropertyNames.OCTGPR_ADDITIONAL_PATH);
		}

		octave.eval("addpath('" + octaveScriptsLocation + "')");
		octave.eval("addpath('" + octgprLocation + "')");
		
		workloadGen = InetAddress.getByName(System.getProperty("workloadgenerator.ip"));
		frontendIp = InetAddress.getByName(System.getProperty("controlinterface.ip"));
		loadbalancerPort = Integer.parseInt(System.getProperty("loadbalancer.port"));
	}

	@Override
	public void stopWorkload() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void collectData() {
		// TODO Auto-generated method stub

	}

	@Override
	public void startWorkload() {
		
		StringBooleanCouple result = null;
		try {
				result = JSchUtils.execCommand(workloadGen, "root", "xunil090", "", "/opt/modules/jakarta-jmeter-2.4/doremap-experiments/launch-doremap-client.sh "+frontendIp.getHostAddress()+" "+loadbalancerPort+" 3000 600+ > /root/workloadgeneratorlog  2>&1 &", false);
		} catch (JSchException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		logger.info("Workload launched from "+workloadGen+",  result: "+result.getString());
//		if(!result.getBool()){
//			logger.error("workload generator has failed, aborting");
//			System.exit(1);
//		}
		
		logger.info("Workload has been successfully launched");
		
		logger.info("Waiting one hour for the workload to finish");
		
		try {
			Thread.sleep(3600000);
		} catch (InterruptedException e) {
			logger.info("workload waiting has been interrupted");
			e.printStackTrace();
		}
		
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
		
		//No need for models
		this.tss = new NullTrainingSampleSelector(this.getDataSource(), models);
	}
	
	@Override
	public void init() throws Exception {
		
		super.init();
		initModels();
	}
}
