package ch.usi.cloud.controller.doremap.confselectors;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.sql.DataSource;

import org.apache.log4j.Logger;

import ch.usi.cloud.controller.ConfigurationSelector;
import ch.usi.cloud.controller.Model;
import ch.usi.cloud.controller.ModelDescriptor;
import ch.usi.cloud.controller.NullTrainingSampleSelector;
import ch.usi.cloud.controller.common.ConfigurationFeature;
import ch.usi.cloud.controller.common.ManifestConnector;
import ch.usi.cloud.controller.common.ServiceConfiguration;
import ch.usi.cloud.controller.common.ServiceLevelObjective;
import ch.usi.cloud.controller.common.db.TablesManager;
import ch.usi.cloud.controller.impl.ConfigurationException;
import ch.usi.cloud.controller.impl.EmptyModel;

/**
 * 
 * Basic rules configuration selector for Doodle. Triggers changes whenever
 * thresholds are passed it DOES NOT wait for changes being actuated before
 * asking for new ones
 * 
 * * @author Giovanni Toffetti (toffettg@usi.ch)
 * 
 */
public class DoodleConfigurationSelectorRules extends ConfigurationSelector {

	int doodleAS = -1;
	// Note the name may change !!
	// String vmType = "doodleas";
	String vmType = "appserver";

	private static Logger logger = Logger
			.getLogger(ch.usi.cloud.controller.doremap.confselectors.DoodleConfigurationSelectorRules.class);

	// Properties names for the doramp rules conf selector
	public static String DOREMAP_MIN_JOBS_PER_DOODLEAS = "DOREMAP_MIN_JOBS_PER_DOODLEAS";
	public static String DOREMAP_MAX_JOBS_PER_DOODLEAS = "DOREMAP_MAX_JOBS_PER_DOODLEAS";

	// Default Properties values
	int minJobs = 100; // minimum number of jobs each rubisas needs to have to
						// avoid scale down
	int maxJobs = 300; // max number of jobs each rubisas can have before
						// triggering scale up

	// Those acgually can be put inside the change request actuator
	long scaleUpCoolDown = 1 * 60 * 1000;
	long scaleDownCoolDown = 2 * 60 * 1000;

	private long lastScaleUp = -1;
	private long lastScaleDown = -1;

	public DoodleConfigurationSelectorRules(long controlPeriod,
			ManifestConnector mc, DataSource ds, String serviceId) {
		super(controlPeriod, mc, ds, serviceId);

		try {
			minJobs = Integer.parseInt(System.getProperty(
					DOREMAP_MIN_JOBS_PER_DOODLEAS, Integer.toString(minJobs)));
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			maxJobs = Integer.parseInt(System.getProperty(
					DOREMAP_MAX_JOBS_PER_DOODLEAS, Integer.toString(maxJobs)));
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			scaleUpCoolDown = Long.parseLong(System.getProperty(
					"scaleup.cooldown", Long.toString(scaleUpCoolDown)));
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			scaleDownCoolDown = Long.parseLong(System.getProperty(
					"scaledown.cooldown", Long.toString(scaleDownCoolDown)));
		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.info("\n\nSUMMARY: Rules are triggered if we received less than "
				+ minJobs
				+ " and more than "
				+ maxJobs
				+ " per allocated doodle as in the past minute in average."
				+ "Cool down periods are scale down "
				+ scaleDownCoolDown
				+ " and scale up " + scaleUpCoolDown + "\n\n");

	}

	@Override
	public void init() {
		// Before used this
		// super.init();
		// we don't need to do any training. Use a null TSS
		// TODO: maybe it makes sense moving the NullTSS to a common lib for all
		// rule-based controllers
		this.tss = new NullTrainingSampleSelector(this.getDataSource(), models);

		// Create a model instance per SLO
		if (logger.isInfoEnabled()) {
			logger.info("MC:" + mc);
		}

		mc.getSLOs().size();

		Model m = new EmptyModel();

		// set in the model descriptor the appropriate input and output
		// parameters
		initModel(m);
		models.add(m);
		if (logger.isInfoEnabled()) {
			logger.info("Created model descriptor: " + m.descriptor);

		}
	}

	/**
	 * This is to just initialize the models with the right System Configuration
	 * parameters
	 * 
	 * @param m
	 */
	protected void initModel(Model m) {
		// Link the model to the Service Instance
		m.descriptor = new ModelDescriptor(serviceFQN);
		// By default the configuration space defines part of the input
		// features: this is our (only) controllable variable so far.
		logger.info("\n\n Configurations from manifest:"
				+ mc.getConfiguration());

		for (ConfigurationFeature f : mc.getConfiguration()) {
			// TODO: Only doodleas is elastic for the moment
			if (f.name.endsWith(vmType)) {
				m.descriptor.inputFeatures.add(f);
				if (logger.isInfoEnabled()) {
					logger.info("Adding KPI as feature: " + f.name);
				}
			} else {
				logger.info(f.name + " NOT ADDED TO THE FEATURES");
			}
		}

		if (m.descriptor.inputFeatures.size() == 0) {
			logger.error("NO INPUT FEATURES IN THE MODEL DESCRIPTOR. EXIT !");
			System.exit(-1);
		}

	}

	private double getMaxInstances(String veeName) {
		for (ConfigurationFeature feature : mc.getConfiguration()) {
			// Use contains instead of equals
			if (feature.name.contains(veeName)) {
				return feature.max;
			}
		}
		return 0;
	}

	private double getMinInstances(String veeName) {
		for (ConfigurationFeature feature : mc.getConfiguration()) {
			if (feature.name.contains(veeName)) {
				return feature.min;
			}
		}
		return 0;
	}

	/**
	 * Compute the next configuration
	 */
	@Override
	protected ServiceConfiguration getTargetConfiguration()
			throws ConfigurationException {

		if (currentConfiguration == null) {
			throw new ConfigurationException("Missing current configuration");
		}

		// We have only one model
		Model model = models.get(0);

		if (doodleAS == -1) {
			try {
				logger.info("Doodle AS == -1");
				logger.info("currentConfiguration " + currentConfiguration);
				logger.info("model.descriptor.inputFeatures.size() = "
						+ model.descriptor.inputFeatures.size());
				doodleAS = currentConfiguration
						.getInstancesCount(model.descriptor.inputFeatures
								.get(0).name);
			} catch (Exception e) {

				e.printStackTrace();
			}
		}

		// incoming requests in the last 6 periods ( half a minute)
		double[] context = getRCAverage(monitoringDs, 6);
		// NOTE THE Columns may have different names !
		logger.info("\n\n\n\n \t\t Retrieved context: "
				+ Arrays.toString(context) + "\n\n\n\n ");

		double jobs = (context[0] + context[1] + context[2] + context[3]);
		logger.info("Current doodleAS: "
				+ currentConfiguration
						.getInstancesCount(model.descriptor.inputFeatures
								.get(0).name));

		logger.warn(" jobs per app server => \n\n" + minJobs + " < "
				+ (jobs / doodleAS) + " < " + maxJobs);

		if (jobs / doodleAS > maxJobs) {

			if (doodleAS < getMaxInstances(model.descriptor.inputFeatures
					.get(0).name)) {
				logger.info("Current configuration is too small with " + jobs
						/ doodleAS + " jobs per doodleAS (Scaling up)");

				if (System.currentTimeMillis() - lastScaleUp < scaleUpCoolDown
						&& lastScaleUp > 0) {
					logger.warn("Cannot scale up due to Cool down period. Time to wait "
							+ (System.currentTimeMillis() - lastScaleUp));
				} else {
					// then scale up
					doodleAS++;
					lastScaleUp = System.currentTimeMillis();
				}
			} else {
				logger.warn("THE MAXIMUM NUMBER OF doodleAS HAS BEEN REACHED, skipping new configuration with "
						+ (doodleAS + 1) + " doodleAS");
			}

		} else if (jobs / doodleAS < minJobs && doodleAS > 1) {

			if (doodleAS > getMinInstances(model.descriptor.inputFeatures
					.get(0).name)) {
				logger.info("Current configuration is too big with " + jobs
						/ doodleAS + " jobs per doodleAS (Scaling down)");

				if (System.currentTimeMillis() - lastScaleDown < scaleDownCoolDown
						&& lastScaleDown > 0) {
					logger.warn("Cannot scale down due to Cool down period. Time to wait "
							+ (System.currentTimeMillis() - lastScaleDown));
				} else {
					// then scale up
					doodleAS--;
					lastScaleDown = System.currentTimeMillis();
				}
			} else {
				logger.warn("THE MINIMUM NUMBER OF doodleAS HAS BEEN REACHED, skipping new configuration with "
						+ (doodleAS - 1) + " doodleAS");
			}
		}

		logger.info("Requested doodleAS: " + doodleAS);

		ServiceConfiguration nextConf = convertArrayToServiceConf(doodleAS,
				currentConfiguration,
				model.descriptor.inputFeatures.get(0).name);

		logger.info("next configuration: " + nextConf);
		return nextConf;

	}

	@Override
	protected ConfigurationFeature getOutputFeature(String kPIName) {
		return new ConfigurationFeature("fakeOuputFeature", "fakeOutputFeature");
	}

	@Override
	protected Collection<? extends ConfigurationFeature> getInputFeatures(
			List<String> kpiList, ServiceLevelObjective slo) {
		// new Exception("This method is not implemented.").printStackTrace();
		return new ArrayList<ConfigurationFeature>();
	}

	/**
	 * This version takes an average of the last N monitoring periods. This is
	 * basically a smoother version of the workload and queue length trace
	 */

	public double[] getRCAverage(DataSource ds, int periods) {

		StringBuffer query = new StringBuffer();
		// we'll compute the average equal to the size of our actuator time
		// NOTE Names can be different !!
		// query.append("SELECT avg(kpi_createRC), avg(kpi_getRC), avg(kpi_voteRC), avg(kpi_deleteRC)");
		query.append("SELECT avg(kpi_CREATE_POLL_RC), avg(kpi_GET_POLL_RC), avg(kpi_VOTE_RC), avg(kpi_DELETE_POLL_RC)");
		query.append(" FROM "
				+ TablesManager.getTableNameFromServiceId(mc.getServiceId()));
		// query.append("  where rowid in (select rowid from "
		// + TablesManager.getTableNameFromServiceId(mc.getServiceId())
		// + " where (kpi_createRC not null or " + "kpi_getRC not null or "
		// + "kpi_voteRC not null or " + "kpi_deleteRC not null) and " +
		// "time > "
		// + (System.currentTimeMillis() - 600000)
		query.append("  where rowid in (select rowid from "
				+ TablesManager.getTableNameFromServiceId(mc.getServiceId())
				+ " where (kpi_CREATE_POLL_RC not null or "
				+ "kpi_GET_POLL_RC not null or " + "kpi_VOTE_RC not null or "
				+ "kpi_DELETE_POLL_RC not null) and " + "time > "
				+ (System.currentTimeMillis() - 600000) /*
														 * 
														 * NOTE: if we use this
														 * method we must be
														 * sure that the remote
														 * VM with the
														 * monitoringendpoint
														 * and the machine on
														 * which the controller
														 * is running are
														 * perfectly
														 * synchronized
														 */
				+ " ORDER BY rowid DESC LIMIT " + periods + ");");
		if (logger.isDebugEnabled()) {
			logger.debug("QUERY: " + query);
		}

		// avg(kpi_createRC), avg(kpi_getRC), avg(voteRC), avg(deleteRC)
		double[] output = new double[4];

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

	public ServiceConfiguration convertArrayToServiceConf(int doodleAS,
			ServiceConfiguration currentConfiguration, String theVmType)
			throws ConfigurationException {
		// now we convert the array into a ServiceConfiguration
		ServiceConfiguration nextConf = new ServiceConfiguration();
		if (currentConfiguration != null) {
			for (String type : currentConfiguration.getVEETypes()) {
				// copy instances in new conf
				for (String id : currentConfiguration.getInstances(type)) {
					nextConf.addVEEInstance(type, id);
				}
				if (type.equals(theVmType)) {
					int instances = nextConf.getInstancesCount(type);
					if (instances < doodleAS) {
						// need to add instances
						for (int j = 0; j < (doodleAS - instances); j++) {
							// GT: avoid duplicate instances, check if it exists
							// before adding
							int k = 1;
							while (nextConf.getInstances(type).contains(
									type + ".replicas." + (instances + j + k))) {
								k++;
							}
							nextConf.addVEEInstance(type, type + ".replicas."
									+ (instances + j + k));
						}
					} else if (instances > doodleAS) {
						// need to remove instances
						for (int j = 0; j < instances - doodleAS; j++) {
							String lastInstance = getLastInstance(nextConf,
									type);
							nextConf.removeVEEInstance(type, lastInstance);
						}
					}
				}
			}
		} else {
			throw new ConfigurationException("empty current configuration");
		}
		return nextConf;
	}

	private static String getLastInstance(ServiceConfiguration nextConf,
			String type) {
		List<String> instances = nextConf.getInstances(type);
		String last = instances.get(0);
		// System.err.println(last);
		for (String instance : instances) {
			if (Long.parseLong(instance.substring(instance.indexOf("replicas.") + 9)) > Long
					.parseLong(last.substring(last.indexOf("replicas.") + 9))) {
				last = instance;
			}
		}
		return last;
	}

}
