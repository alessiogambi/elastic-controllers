package ch.usi.cloud.controller.doremap.models;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.sql.DataSource;

import org.apache.log4j.Logger;

import cern.colt.list.DoubleArrayList;
import cern.jet.stat.Descriptive;
import ch.usi.cloud.controller.ConfigurationSelector;
import ch.usi.cloud.controller.Model;
import ch.usi.cloud.controller.ModelDescriptor;
import ch.usi.cloud.controller.common.ConfigurationFeature;
import ch.usi.cloud.controller.common.ManifestConnector;
import ch.usi.cloud.controller.common.ServiceLevelObjective;
import ch.usi.cloud.controller.impl.KrigingModel;
import dk.ange.octave.OctaveEngine;

public class DoReMapModels {

	static Logger logger = Logger
			.getLogger(ch.usi.cloud.controller.doremap.models.DoReMapModels.class);

	public static List<Model> initModels(ManifestConnector mc,
			String serviceFQN, OctaveEngine octave) {
		List<Model> models = new ArrayList<Model>();
		// Create a model instance per SLO
		logger.debug("MC:" + mc);

		Collections.sort(mc.getSLOs());
		Collections.sort(mc.getKPIList());

		for (ServiceLevelObjective slo : mc.getSLOs()) {
			logger.info("SLO: " + slo);
			/**
			 *  TOF: somehow I get a RejectedExecutionException if I try
			 *  to instantiate multiple Kriging models in the same Octave engine.
			 *  Maybe it has to do with concurrency, no time to investigate.
			 *  I'll stick with one engine per model using the deprecated API.
			 */
			
			Model m = new KrigingModel();
			// Link the model to the Service Instance
			m.descriptor = new ModelDescriptor(serviceFQN);
			// We only use AS and JO as relevant configuration features
			for (ConfigurationFeature cf : mc.getConfiguration()) {
				if (cf.name.endsWith("jopera") || cf.name.endsWith("doodleas")) {
					m.descriptor.inputFeatures.add(cf);
				}
			}
			// The remaining input features are derived from the user defined
			// KPIs.

			m.descriptor.inputFeatures.addAll(getInputFeatures(mc.getKPIList(),
					slo));

			// By definition there is only one output features per Kriging
			// model, that is the SLO itself.
			m.descriptor.outputFeature = ConfigurationSelector
					.getFeatureFromSLO(slo);
			models.add(m);
			logger.info("Created model descriptor: " + m.descriptor);
			logger.info("Input features: " + m.descriptor.inputFeatures);
			logger.info("Output feature: " + m.descriptor.outputFeature);
			
		}
		return models;
	}

	public static Collection<? extends ConfigurationFeature> getInputFeatures(
			List<String> kpiList, ServiceLevelObjective slo) {
		// we use a fixed set of features

		List<ConfigurationFeature> inputFeatures = new ArrayList<ConfigurationFeature>();
		inputFeatures.add(ConfigurationSelector
				.getFeatureFromKPI("votepollTrainingQueueLength"));
		inputFeatures.add(ConfigurationSelector
				.getFeatureFromKPI("createpollTrainingQueueLength"));
		inputFeatures.add(ConfigurationSelector
				.getFeatureFromKPI("votepollTrainingReqCount"));
		inputFeatures.add(ConfigurationSelector
				.getFeatureFromKPI("createpollTrainingReqCount"));

		return inputFeatures;
	}

	public static Collection<? extends ConfigurationFeature> getMonitoringInputFeatures(
			List<String> kpiList, ServiceLevelObjective slo) {
		// we use a fixed set of features

		List<ConfigurationFeature> inputFeatures = new ArrayList<ConfigurationFeature>();

		inputFeatures.add(ConfigurationSelector
				.getFeatureFromKPI("votepollQueueLength"));
		inputFeatures.add(ConfigurationSelector
				.getFeatureFromKPI("createpollQueueLength"));
		inputFeatures.add(ConfigurationSelector
				.getFeatureFromKPI("votepollReqCount"));
		inputFeatures.add(ConfigurationSelector
				.getFeatureFromKPI("createpollReqCount"));

		return inputFeatures;
	}

	public static List<String> getMonitoringKPINames() {
		// we use a fixed set of features

		List<String> kpis = new ArrayList<String>();

		kpis.add("votepollQueueLength");
		kpis.add("createpollQueueLength");
		kpis.add("votepollReqCount");
		kpis.add("createpollReqCount");

		return kpis;
	}

	public List<ServiceLevelObjective> getSLOs(ManifestConnector mc) {
		return mc.getSLOs();
	}

	public static List<String> getSLOKPIs(ManifestConnector mc) {
		List<String> result = new ArrayList<String>();
		for (ServiceLevelObjective slo : mc.getSLOs()) {
			result.add(slo.KPIName);
		}
		Collections.sort(result);
		return result;
	}

	public static List<String> getReducedWkldKPIs(ManifestConnector mc) {
		List<String> result = new ArrayList<String>();
		for (String kpi : mc.getKPIList()) {
			if (kpi.equals("createpollReqCount")
					|| kpi.equals("votepollReqCount")) {
				result.add(kpi);
			}
		}
		Collections.sort(result);
		return result;
	}

	public static List<String> getReducedWkldTrainingKPIs(ManifestConnector mc) {
		List<String> result = new ArrayList<String>();
		for (String kpi : mc.getKPIList()) {
			if (kpi.equals("createpollTrainingReqCount")
					|| kpi.equals("votepollTrainingReqCount")) {
				result.add(kpi);
			}
		}
		Collections.sort(result);
		return result;
	}

	public static Double[] getSLOLimits(ManifestConnector mc) {
		List<Double> result = new ArrayList<Double>();
		for (ServiceLevelObjective slo : mc.getSLOs()) {
			result.add(slo.maxThreshold);
		}
		return result.toArray(new Double[0]);
	}

	public static List<String> getThroughputKPIs(ManifestConnector mc) {
		List<String> throughputKPIs = new ArrayList<String>();
		for (String kpi : mc.getKPIList()) {
			if (kpi.contains("TX")) {
				throughputKPIs.add(kpi.substring(kpi.lastIndexOf(".") + 1));
			}
		}
		Collections.sort(throughputKPIs);
		return throughputKPIs;
	}

	public static List<String> getTrainingAvgRTKPIs(ManifestConnector mc) {

		List<String> trainingAVGRTKPIs = new ArrayList<String>();

		for (String kpi : mc.getKPIList()) {
			if (kpi.contains("TrainingAvgRT")) {
				trainingAVGRTKPIs.add(kpi.substring(kpi.lastIndexOf(".") + 1));
			}
		}
		Collections.sort(trainingAVGRTKPIs);

		return trainingAVGRTKPIs;
	}

	public static List<String> getTrainingReqCountKPIs(ManifestConnector mc) {
		List<String> throughputKPIs = new ArrayList<String>();
		for (String kpi : mc.getKPIList()) {
			if (kpi.contains("TrainingReqCount")) {
				throughputKPIs.add(kpi.substring(kpi.lastIndexOf(".") + 1));
			}
		}
		return throughputKPIs;
	}

	public static double[] getAvgKPIInWindow(Model model, List<String> kpis,
			DataSource ds, long window) {
		return getAvgKPIInWindow(model, kpis, ds, -1, window);
	}

	public static double[] getSingleKPIInWindow(Model model, String kpi,
			DataSource ds, long window) {
		return getSingleKPIInWindow(model, kpi, ds, -1, window);
	}

	public static double[] getSingleKPIInWindow(Model model, String kpi,
			DataSource ds, long startTime, long window) {

		StringBuffer query = new StringBuffer();
		query.append("SELECT ");
		StringBuffer columnName = new StringBuffer();
		columnName.append("kpi_");
		columnName.append(kpi);
		query.append(columnName.toString());

		query.append(" FROM " + model.descriptor.serviceId.replace(".", "_"));

		if (startTime == -1) {
			startTime = System.currentTimeMillis();
			query.append(" where time > " + (startTime - window));
		} else {
			query.append(" where time > " + (startTime - window)
					+ " and time < " + startTime);
		}

		columnName = new StringBuffer();
		columnName.append(" and kpi_");
		columnName.append(kpi);
		columnName.append(" NOT NULL");
		query.append(columnName.toString());

		logger.debug("\n\nQUERY: " + query + "\n\n");

		List<Double> output = new ArrayList<Double>();

		Connection c = null;
		PreparedStatement ps = null;
		try {
			c = ds.getConnection();
			ps = c.prepareStatement(query.toString());
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				output.add(rs.getDouble(1));
			}

		} catch (SQLException e) {
			logger.warn("DB does not contain data within chosen window");
			e.printStackTrace();
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if (c != null) {
				try {
					c.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		double[] result = new double[output.size()];
		for (int i = 0; i < result.length; i++) {
			result[i] = output.get(i);
		}
		output.clear();
		output = null;
		return result;
	}

	// Compute the Median
	public static double[] getMedian(Model model, List<String> kpis,
			DataSource ds, long startTime, long window) {

		double[] result = new double[kpis.size()];
		for (String kpi : kpis) {
			result[kpis.indexOf(kpi)] = Descriptive.median(new DoubleArrayList(
					getSingleKPIInWindow(model, kpi, ds, startTime, window)));
		}
		return result;
	}

	// Compute the Mean
	public static double[] getMean(Model model, List<String> kpis,
			DataSource ds, long startTime, long window) {

		double[] result = new double[kpis.size()];
		for (String kpi : kpis) {
			result[kpis.indexOf(kpi)] = Descriptive.mean(new DoubleArrayList(
					getSingleKPIInWindow(model, kpi, ds, startTime, window)));
		}
		return result;
	}

	// Compute the Stdev
	public static double[] getSampleVariance(Model model, List<String> kpis,
			DataSource ds, long startTime, long window) {

		double[] result = new double[kpis.size()];
		for (String kpi : kpis) {
			// Get the samples
			DoubleArrayList sample = new DoubleArrayList(getSingleKPIInWindow(
					model, kpi, ds, startTime, window));
			result[kpis.indexOf(kpi)] = Descriptive.sampleVariance(sample,
					Descriptive.mean(sample));
		}
		return result;
	}

	// Compute the Percentile
	public static double[] getFifthyQuantile(Model model, List<String> kpis,
			DataSource ds, long startTime, long window) {

		double[] result = new double[kpis.size()];
		for (String kpi : kpis) {
			result[kpis.indexOf(kpi)] = Descriptive.quantile(
					new DoubleArrayList(getSingleKPIInWindow(model, kpi, ds,
							window)), 0.5);
		}
		return result;
	}

	public static double[] getNintyQuantile(Model model, List<String> kpis,
			DataSource ds, long startTime, long window) {

		double[] result = new double[kpis.size()];
		for (String kpi : kpis) {
			result[kpis.indexOf(kpi)] = Descriptive.quantile(
					new DoubleArrayList(getSingleKPIInWindow(model, kpi, ds,
							window)), 0.9);
		}
		return result;
	}

	public static double[] getSum(Model model, List<String> kpis,
			DataSource ds, long startTime, long window) {

		double[] result = new double[kpis.size()];
		for (String kpi : kpis) {
			result[kpis.indexOf(kpi)] = Descriptive.sum(new DoubleArrayList(
					getSingleKPIInWindow(model, kpi, ds, window)));
		}
		return result;
	}

	public static double[][] getMultipleKPIsInWindow(Model model,
			List<String> kpis, DataSource ds, long startTime, long window) {

		double[][] result = new double[kpis.size()][];
		for (String kpi : kpis) {
			result[kpis.indexOf(kpi)] = getSingleKPIInWindow(model, kpi, ds,
					window);
		}
		return result;
	}

	public static double[] getNintyfiveQuantile(Model model, List<String> kpis,
			DataSource ds, long startTime, long window) {

		double[] result = new double[kpis.size()];
		for (String kpi : kpis) {
			result[kpis.indexOf(kpi)] = Descriptive.quantile(
					new DoubleArrayList(getSingleKPIInWindow(model, kpi, ds,
							window)), 0.95);
		}
		return result;
	}

	public static double[] getQuantiles(Model model, List<String> kpis,
			DataSource ds, long startTime, long window, double[] quantiles) {

		double[] result = new double[kpis.size() * quantiles.length];
		for (int i = 0; i < quantiles.length; i++) {
			for (String kpi : kpis) {
				logger.info("i " + i + " i * kpis.size() " + i * kpis.size()
						+ " j " + kpis.indexOf(kpi) + " tot : "
						+ (i * kpis.size() + kpis.indexOf(kpi)));
				result[i * kpis.size() + kpis.indexOf(kpi)] = Descriptive
						.mean(new DoubleArrayList(getSingleKPIInWindow(model,
								kpi, ds, window)));
			}
		}
		return result;
	}

	public static double[] getAvgKPIInWindow(Model model, List<String> kpis,
			DataSource ds, long startTime, long window) {

		StringBuffer query = new StringBuffer();
		query.append("SELECT ");
		for (String kpi : kpis) {
			StringBuffer columnName = new StringBuffer();
			columnName.append("avg(kpi_");
			columnName.append(kpi);
			columnName.append(")");
			if (kpis.indexOf(kpi) != 0) {
				query.append(", ");
			}
			query.append(columnName.toString());
		}
		query.append(" FROM " + model.descriptor.serviceId.replace(".", "_"));

		if (startTime == -1) {
			startTime = System.currentTimeMillis();
			query.append(" where time > " + (startTime - window));
		} else {
			query.append(" where time > " + (startTime - window)
					+ " and time < " + startTime);
		}

		// + " ORDER BY rowid DESC;");
		for (String kpi : kpis) {
			StringBuffer columnName = new StringBuffer();
			columnName.append(" and kpi_");
			columnName.append(kpi);
			columnName.append(" NOT NULL");
			query.append(columnName.toString());
		}

		logger.debug("QUERY: " + query);

		double[] output = new double[kpis.size()];

		Connection c = null;
		PreparedStatement ps = null;
		try {
			c = ds.getConnection();
			ps = c.prepareStatement(query.toString());
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				// add data
				for (int i = 0; i < output.length; i++) {
					output[i] = rs.getDouble(i + 1);
				}
			}

		} catch (SQLException e) {
			logger.warn("DB does not contain data within chosen window");
			e.printStackTrace();
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if (c != null) {
				try {
					c.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		return output;
	}

	public static List<Model> initUtilityModel(ManifestConnector mc,
			String serviceFQN, OctaveEngine octave) {
		List<Model> models = initModels(mc, serviceFQN, octave);

		// FIXME: make utility model a configurable parameter
		Model m = new DoReMapStepUtilityFunctionModel(mc, serviceFQN, models);

		logger.info("Created model descriptor: " + m.descriptor);
		models.add(m);
		return models;
	}

	public static double[] findConfWithMaxUtility(Model model, double[] avgWkld) throws Exception {

		// the space of candidate confs is given by the cartesian product of the
		// controllable features
		logger.debug("Model input features: " + model.descriptor.inputFeatures);
		
		List<List<Integer>> confs = new ArrayList<List<Integer>>();
		for (ConfigurationFeature cf : model.descriptor.inputFeatures) {
			if (cf.controllable) {
				logger.debug("Controllable feature: " + cf + " min: " + cf.min + " max: " + cf.max );
				if (confs.isEmpty()) {
					for (int i = (int) cf.min; i <= (int) cf.max; i++) {
						List<Integer> entry = new ArrayList<Integer>();
						entry.add(i);
						confs.add(entry);
					}							
				} else {
					List<List<Integer>> temp = new ArrayList<List<Integer>>();
					for (int i = (int) cf.min; i <= (int) cf.max; i++) {
						Iterator<List<Integer>> ite = confs.iterator();
						
						while (ite.hasNext()) {
							List<Integer> entry = ite.next();							
							List<Integer> newEntry = new ArrayList<Integer>(
									entry);
							newEntry.add(i);
							temp.add(newEntry);						
						}						
					}
					confs.clear();
					confs.addAll(temp);
					
				}
			}
		}
		
		logger.debug("Confs: " + confs );
		int configPars = confs.get(0).size();

		// convert to array
		double[][] input = new double[confs.size()][model.descriptor.inputFeatures
				.size()];

		if (logger.isDebugEnabled()) {
			logger.debug("Preparing " + confs.size()
					+ " candidate configurations");
		}

		// copy conf values
		for (int i = 0; i < confs.size(); i++) {
			List<Integer> conf = confs.get(i);
			for (int j = 0; j < conf.size(); j++) {
				input[i][j] = conf.get(j);
			}
		}

		// now set other KPIs
		for (int i = 0; i < confs.size(); i++) {
			for (int j = 0; j < avgWkld.length; j++) {
				input[i][j + configPars] = avgWkld[j];
			}
//			if (logger.isDebugEnabled()) {
//				logger.debug("Proposed set of conf(" + i + "): "
//						+ Arrays.toString(input[i]));
//			}
		}

		return model.findConf(input, true);

	}

}
