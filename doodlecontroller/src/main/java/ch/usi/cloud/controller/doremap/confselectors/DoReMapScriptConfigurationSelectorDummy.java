package ch.usi.cloud.controller.doremap.confselectors;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.log4j.Logger;

import ch.usi.cloud.controller.common.ConfigurationListener;
import ch.usi.cloud.controller.common.ManifestConnector;
import ch.usi.cloud.controller.common.ServiceConfiguration;
import ch.usi.cloud.controller.common.SystemPropertyNames;
import ch.usi.cloud.controller.doremap.models.DoReMapModels;
import ch.usi.cloud.controller.impl.AbstractScriptConfigurationSelector;
import ch.usi.cloud.controller.impl.TrivialTrainingSampleSelector;
import ch.usi.cloud.controller.util.ScriptParser;
import ch.usi.cloud.controller.util.ScriptParser.Parameter;
import ch.usi.cloud.controller.util.ServiceManagementUtils;
import dk.ange.octave.OctaveEngine;
import dk.ange.octave.OctaveEngineFactory;

/**
 * This conf selector read the target conf from a file and then stops. It must
 * be used for testing and with the -DSKIP_DEPLOY=TRUE
 * 
 * @author alessiogambi
 * 
 */
public class DoReMapScriptConfigurationSelectorDummy extends AbstractScriptConfigurationSelector {

	protected Logger logger = Logger.getLogger(DoReMapScriptConfigurationSelectorDummy.class);

	public DoReMapScriptConfigurationSelectorDummy(
			long controlPeriod,
			ManifestConnector mc,
			DataSource monitoringDs,
			DataSource trainingDs,
			String serviceId) throws Exception {
		super(controlPeriod, mc, monitoringDs, trainingDs, serviceId);
		
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
		// TODO Auto-generated method stub

	}

	// Name of the parameters we must read from script
	private String workloadMixParameterName = "WorkloadMix";
	private String sampleThreadsParameterName = "Threads";

	@Override
	protected Map<Parameter, ArrayList<Parameter>> getScriptAdditionalParameters() {
		HashMap<Parameter, ArrayList<Parameter>> additionalParameters = new HashMap<Parameter, ArrayList<Parameter>>();
		ArrayList<Parameter> sampleParamForWorkloadMix = new ArrayList<ScriptParser.Parameter>();
		sampleParamForWorkloadMix.add(newParameterInstance(sampleThreadsParameterName,
				Integer.class, true));
		additionalParameters.put(
				newParameterInstance(workloadMixParameterName, String.class, true),
				sampleParamForWorkloadMix);

		return additionalParameters;

	}

	protected OctaveEngine octave;
	public static String octgprLocation = "/usr/lib/octave/packages/3.2/octgpr-1.1.5";
	public static String octaveScriptsLocation = "/usr/lib/octave/packages/3.2/octgpr-1.1.5/x86_64-pc-linux-gnu-api-v37";

	/**
	 * This implementation of InitModels is App-specific and depends on the
	 * Manifest and the exposed KPIs
	 */
	
	
	@Override
	public void initModels() {
		// init models in app-specific way
		models = DoReMapModels.initModels(mc, this.serviceFQN, octave);
		// Pass the Collection of Models to the tss
		tss.setModels(models);
	}

	
	@Override
	public void init() throws Exception {
		super.init();
		initModels();
	}
	
	@Override
	public void run() {
		int sampleLineNum = 0;
		try {
			init();

			// BEFORE ANYTHING CHECK THAT THE ACTUAL CONFIGURATION IS REACHED !
			
			//?? What is the sense of that ???
			//we are comparing the currentConfiguration object to itself ??
			boolean confReached = ServiceManagementUtils.checkServiceConf(this.currentConfiguration,
					this.currentConfiguration);

			int i = 0;
			while (!confReached && i < ServiceManagementUtils.maxIterationsForConf) {
				Thread.sleep(ServiceManagementUtils.cycle);
				i++;
				confReached = ServiceManagementUtils.checkServiceConf(this.currentConfiguration,
						this.currentConfiguration);
			}

			// Start the loop
			for (ArrayList<Integer> conf : scriptParser.getSampleParamsIntegerHashMap().get(
					"Instances")) {

				if (logger.isInfoEnabled()) {
					logger.info("Moving to next sample: " + getVMConfigurationAsString(conf));
				}

				confReached = false;

				final ServiceConfiguration targetConf = getConfForSample(sampleLineNum);

				if (logger.isDebugEnabled()) {
					logger.debug("Next target configuration: " + targetConf);
				}
				for (final ConfigurationListener listener : confListeners) {
					threadPool.submit(new Runnable() {
						public void run() {
							listener.handleTargetConfiguration(targetConf);
						};
					});
				}

				i = 0;
				while (!confReached && i < ServiceManagementUtils.maxIterationsForConf) {
					confReached = ServiceManagementUtils.checkServiceConf(this.currentConfiguration,
							targetConf);

					if (confReached) {
						break;
					}
					i++;
					Thread.sleep(ServiceManagementUtils.cycle);
				}

				if (!confReached) {
					logger.fatal("System has not reached expected configuration in "
							+ (ServiceManagementUtils.maxIterationsForConf * ServiceManagementUtils.cycle / 1000)
							+ " seconds. Returning from confSelector.run().");
					return;
				}

				PrintAVG runnable;
				Thread t;
				runnable = new PrintAVG();
				t = new Thread(runnable);
				startWorkload();
				// Mark Here the starting time. I.e. wait at least window.size
				// before computing means
				runnable.systemConfiguration = targetConf;
				runnable.threadCount = null;

				runnable.originalStartTime = System.currentTimeMillis();
				logger.info("Start the data collection at " + runnable.originalStartTime);

				if (System.getProperty("doremap.dummy.WaitTimeBetweenConfigurations") != null) {
					try {

						// This will be blocked after the rampUp and the
						// SleepTime !
						t.start();
						// This wait the end
						Thread.sleep(Long.parseLong(System
								.getProperty("doremap.dummy.WaitTimeBetweenConfigurations")));

					} catch (InterruptedException e) {

					} catch (Exception e) {
						e.printStackTrace();
						try {

							Thread.sleep(60000);
						} catch (Exception e1) {
							e1.printStackTrace();
						}
					}

					runnable.stop = true;
					logger.info("Stop the data collection at " + System.currentTimeMillis());
					runnable = null;
					t = null;

				}

				logger.info("No workload... I'm Dummy ");
				sampleLineNum++;
			}

		} catch (IOException e) {
			e.printStackTrace();
			logger.fatal("Can't read from sample file. Returning from confSelector.run()");
			return;
		} catch (InterruptedException e) {
			e.printStackTrace();
			logger.fatal("Conf selector thread interrupted while sleeping. Returning from confSelector.run()");
			return;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private class PrintAVG implements Runnable {

		private Logger dataLogger = Logger.getLogger("kpi-data-logger");
		public boolean stop = false;
		// Default window size
		long window = 60 * 1000;
		// Original start time
		public long originalStartTime = -1;
		// timeout between samples
		long sampleTimeout = 5000;
		public ServiceConfiguration systemConfiguration = null;
		public ArrayList<Integer> threadCount = null;

		public void run() {
			if (System.getProperty("printavg.aggregation.window") != null) {
				try {
					window = Long.parseLong(System.getProperty("printavg.aggregation.window"));
					logger.info("printavg.aggregation.window set to " + window);
				} catch (Exception e) {
					logger.error("Wrong long for window in PrintAVG", e);
				}
			}
			if (System.getProperty("printavg.aggregation.sampletimeout") != null) {
				try {
					sampleTimeout = Long.parseLong(System
							.getProperty("printavg.aggregation.sampletimeout"));
					logger.info("printavg.aggregation.sampletimeout set to " + sampleTimeout);
				} catch (Exception e) {
					logger.error(
							"Wrong long for window in PrintAVG for printavg.aggregation.sampletimeout",
							e);
				}
			}
			if (originalStartTime == -1) {
				originalStartTime = System.currentTimeMillis();
			}
			List<String> slosName = DoReMapModels.getSLOKPIs(mc);
			// All the kpis that contains ReqCount
			List<String> rcounts = DoReMapModels.getTrainingReqCountKPIs(mc);
			// SLO Limits
			Double[] sloLimits = DoReMapModels.getSLOLimits(mc);

			StringBuffer confSb = new StringBuffer();
			if (systemConfiguration != null) {
				for (String vee : systemConfiguration.getVEETypes()) {
					confSb.append(vee);
					confSb.append(",");
				}
			}
			if (threadCount != null) {
				for (int i = 0; i < threadCount.size(); i++) {
					confSb.append("threadCount_" + i);
					confSb.append(",");
				}
			}

			StringBuffer sb = new StringBuffer();
			// Configuration
			sb.append(confSb);
			// SLO
			for (String sloName : slosName) {
				sb.append(sloName);
				sb.append(",");
				sb.append(sloName);
				sb.append("-violated");
				sb.append(",");
				sb.append(sloName);
				sb.append("-mean");
				sb.append(",");
				sb.append(sloName);
				sb.append("-median");
				sb.append(",");
				sb.append(sloName);
				sb.append("-variance");
				sb.append(",");
				sb.append(sloName);
				sb.append("-90qt");
				sb.append(",");
			}
			// RC-Count input feature
			for (String inputFeature : rcounts) {
				sb.append(inputFeature);
				sb.append("-abs");
				sb.append(",");
				sb.append(inputFeature);
				sb.append("-rel");
				sb.append(",");
				sb.append(inputFeature);
				sb.append("-tot-abs");
				sb.append(",");
				sb.append(inputFeature);
				sb.append("-tot-rel");
				sb.append(",");
			}
			// RC Total
			sb.append("reqCountAggregate");
			sb.append("-abs");
			sb.append(",");
			sb.append("reqCountAggregate");
			sb.append("-rel");
			sb.append(",");

			dataLogger.info(sb.toString());

			// Build the Configuration string for this sample
			confSb = new StringBuffer();
			if (systemConfiguration != null) {
				for (String vee : systemConfiguration.getVEETypes()) {
					confSb.append(systemConfiguration.getInstancesCount(vee));
					confSb.append(",");
				}
			}
			if (threadCount != null) {
				for (int i = 0; i < threadCount.size(); i++) {
					confSb.append(threadCount.get(i));
					confSb.append(",");
				}
			}

			while (!stop) {

				try {

					long now = System.currentTimeMillis();
					// Take the original window from the manifest SLO section
					if ((now - originalStartTime) < window) {
						logger.debug("Not enough data was collected. Just wait for "
								+ sampleTimeout);

					} else {
						double[] avgRT_SLO = DoReMapModels.getAvgKPIInWindow(models.get(0),
								slosName, monitoringDs, now, window);
						double[] avgRT_means = DoReMapModels.getMean(models.get(0), slosName, monitoringDs,
								now, window);
						double[] avgRT_medians = DoReMapModels.getMedian(models.get(0), slosName,
								monitoringDs, now, window);
						double[] avgRT_vars = DoReMapModels.getSampleVariance(models.get(0),
								slosName, monitoringDs, now, window);
						double[] avgRT_nintyQuantiles = DoReMapModels.getNintyQuantile(
								models.get(0), slosName, monitoringDs, now, window);

						double[] avgRC = DoReMapModels.getAvgKPIInWindow(models.get(0), rcounts,
								monitoringDs, now, window);

						double[] sumRCs = DoReMapModels.getSum(models.get(0), rcounts, monitoringDs, now,
								window);

						double totRC = 0l;
						for (int i = 0; i < sumRCs.length; i++) {
							totRC = totRC + sumRCs[i];
						}

						sb = new StringBuffer();
						// Configuration
						sb.append(confSb);
						// SLO
						for (int i = 0; i < avgRT_SLO.length; i++) {
							sb.append(avgRT_SLO[i]);
							sb.append(",");
							sb.append((sloLimits[i] >= avgRT_SLO[i]));
							sb.append(",");
							sb.append(avgRT_means[i]);
							sb.append(",");
							sb.append(avgRT_medians[i]);
							sb.append(",");
							sb.append(avgRT_vars[i]);
							sb.append(",");
							sb.append(avgRT_nintyQuantiles[i]);
							sb.append(",");
						}
						// Rcount KPIs
						for (int i = 0; i < avgRC.length; i++) {
							sb.append(avgRC[i]);
							sb.append(",");
							sb.append((avgRC[i] / window * 1000l));
							sb.append(",");
							sb.append(sumRCs[i]);
							sb.append(",");
							sb.append((sumRCs[i] / window * 1000l));
							sb.append(",");
						}
						// RCount aggregate
						sb.append(totRC);
						sb.append(",");
						sb.append((totRC / window * 1000l));
						sb.append(",");
						dataLogger.info(sb.toString());
					}

					try {
						Thread.sleep(sampleTimeout);
					} catch (Exception e) {
						e.printStackTrace();
					}
				} catch (Exception e) {
					e.printStackTrace();
					logger.error( e );
				}
			}

			logger.info("PrintAVG stopped");
		}
	}
}
