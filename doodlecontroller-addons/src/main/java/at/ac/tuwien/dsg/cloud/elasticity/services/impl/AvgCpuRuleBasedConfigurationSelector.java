package at.ac.tuwien.dsg.cloud.elasticity.services.impl;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

import org.apache.tapestry5.ioc.services.TypeCoercer;
import org.slf4j.Logger;

import ch.usi.cloud.controller.common.ServiceConfiguration;
import ch.usi.cloud.controller.common.naming.FQN;
import ch.usi.cloud.controller.impl.ConfigurationException;
import at.ac.tuwien.dsg.cloud.data.DynamicServiceDescription;
import at.ac.tuwien.dsg.cloud.data.InstanceDescription;
import at.ac.tuwien.dsg.cloud.data.VeeDescription;
import at.ac.tuwien.dsg.cloud.elasticity.services.ConfigurationSelector;
import at.ac.tuwien.dsg.cloud.elasticity.services.ConfigurationSelectorDAO;
import at.ac.tuwien.dsg.cloud.elasticity.services.Monitoring;
import at.ac.tuwien.dsg.cloud.exceptions.ModifyServiceStateException;

public class AvgCpuRuleBasedConfigurationSelector implements
		ConfigurationSelector {

	// Services
	private Logger logger;

	// Read Monitoring Data
	private Monitoring monitoring;
	private TypeCoercer typeCoercer;
	// Persist to Controller's internal state
	private ConfigurationSelectorDAO configurationSelectorDAO;

	// State variable
	private DynamicServiceDescription service;

	@Override
	public void setService(DynamicServiceDescription service) {
		this.service = service;
		// INitialize the DSAO ; NOT SURE THIS IS THE RIGHT PLACE !
		logger.info("Initialize the DAO and tables");
		configurationSelectorDAO.createActivationDataTable(service);
		configurationSelectorDAO.createTargetConfigurationTable(service);
	}

	private double getMaxInstances() {
		return (double) service.getVeeDescription("appserver")
				.getMaxInstances();
	}

	private double getMinInstances() {
		return (double) service.getVeeDescription("appserver")
				.getMinInstances();
	}

	/**
	 * This is the method to implement in subclasses
	 * 
	 * @return
	 */
	DynamicServiceDescription applyRules(
			DynamicServiceDescription currentConfiguration, double[] context)
			throws ConfigurationException;

	@Override
	public DynamicServiceDescription getTargetConfiguration() {

		double[] context = getRCAverage(6);

		logger.debug("Retrieved context: " + Arrays.toString(context));

		DynamicServiceDescription targetConfiguration = service;
		try {

			targetConfiguration = applyRules(service, context);

		} catch (ConfigurationException e) {
			logger.error(
					"Error while creating the target conf. Return the original one!",
					e);
		} catch (Exception e) {
			logger.error(
					"Error while creating the target conf. Return the original one!",
					e);
		}

		configurationSelectorDAO.storeTargetConfiguration(targetConfiguration);

		logger.debug("Target configuration: " + targetConfiguration);

		return targetConfiguration;

	}

	/**
	 * This version takes an average of the last N monitoring periods. This is
	 * basically a smoother version of the workload and queue length trace
	 */
	// TODO
	public double[] getRCAverage(int periods) {

		StringBuffer query = new StringBuffer();

		// select * from monitoring_aaa_customers_bbb_services_ccc as M join (
		// select max(A.time) as maxTime, min(A.time) as minTime from ( select
		// time from monitoring_aaa_customers_bbb_services_ccc order by time
		// desc LIMIT 6 ) as A ) as B where M.time < B.maxTime and M.time >
		// B.minTime;
		query.append("SELECT avg(kpi_CREATE_POLL_RC), avg(kpi_GET_POLL_RC), avg(kpi_VOTE_RC), avg(kpi_DELETE_POLL_RC) ");
		query.append("FROM @SERVICE_TABLE as M ");
		query.append("JOIN (");
		query.append("SELECT MAX(A.time) as maxTime, MIN(A.time) as minTime ");
		query.append("FROM (");
		query.append("SELECT time ");
		query.append("FROM @SERVICE_TABLE ");
		query.append("ORDER BY time desc LIMIT ");
		query.append(periods);
		query.append(")");
		query.append("as A ");
		query.append(")");
		query.append("as B ");
		query.append("WHERE M.time < B.maxTime AND M.time > B.minTime;");

		List<Object> valuesFromDB = monitoring.getData(service,
				query.toString());
		double[] output = null;

		if (valuesFromDB.size() > 1) {

			Object[] row = (Object[]) valuesFromDB.get(1);

			// if (logger.isDebugEnabled()) {
			StringBuffer sb = new StringBuffer();
			for (int j = 0; j < row.length; j++) {
				sb.append(row[j]);
				sb.append(", ");
			}

			logger.debug("Result " + sb.toString());
			// }
			// This really should be done directly inside the getData(service,
			// query, < ? extend Collection>) method !!.
			output = new double[4];

			output[0] = typeCoercer.coerce(row[0], Double.class);
			output[1] = typeCoercer.coerce(row[1], Double.class);
			output[2] = typeCoercer.coerce(row[2], Double.class);
			output[3] = typeCoercer.coerce(row[3], Double.class);

		} else {
			logger.warn("Empty Result set from query");
			output = new double[] { 0.0, 0.0, 0.0, 0.0 };
		}
		return output;
	}

	public DynamicServiceDescription convertArrayToServiceConf(
			int targetDoodleAS) throws ConfigurationException {

		// Deep Copy like of the current configuration
		DynamicServiceDescription targetConf = new DynamicServiceDescription(
				service);

		VeeDescription vee = targetConf.getVeeDescription("appserver");
		// Evolution to the target conf
		if (targetConf.getVeeInstances(vee.getName()).size() > targetDoodleAS) {
			while (targetConf.getVeeInstances(vee.getName()).size() > targetDoodleAS) {
				try {
					// Remove some of them
					logger.debug("Removing last replica of " + vee.getName()
							+ " tot : "
							+ targetConf.getVeeInstances(vee.getName()).size());
					targetConf.removeLastReplica(vee);
					logger.debug("Removed tot : "
							+ targetConf.getVeeInstances(vee.getName()).size());
				} catch (ModifyServiceStateException e) {
					e.printStackTrace();
				}
			}

		} else if (targetConf.getVeeInstances(vee.getName()).size() < targetDoodleAS) {
			while (targetConf.getVeeInstances(vee.getName()).size() < targetDoodleAS) {
				int replicaNumber = service.getFirstNullReplicaNum(vee
						.getName());
				// I guess we should implement it or shall we rutern an atomic
				// int instead ?

				String organizationName = service.getStaticServiceDescription()
						.getServiceFQN().getOrganizationName();
				String customerName = service.getStaticServiceDescription()
						.getServiceFQN().getCustomerName();
				String serviceName = service.getStaticServiceDescription()
						.getServiceFQN().getServiceName();
				String veeName = vee.getName();

				// TODO We can also avoid this and just adding new replicas with
				// ID given by the platform
				FQN replicaFQN = new FQN(organizationName, customerName,
						serviceName, "", veeName, replicaNumber);

				try {
					logger.debug("Adding " + replicaFQN);
					targetConf.addVeeInstance(vee, new InstanceDescription(
							replicaFQN, "", "", "", null, null));
				} catch (UnknownHostException e) {
					logger.debug("Error while adding " + replicaFQN, e);
				}

			}
		} else {
			logger.debug("Current and Target conf match. Just return it");
		}
		return targetConf;
		// while (targetConf.getInstances(type).contains(
		// type + ".replicas." + (instances + j + k))) {
		// k++;
		// }
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
