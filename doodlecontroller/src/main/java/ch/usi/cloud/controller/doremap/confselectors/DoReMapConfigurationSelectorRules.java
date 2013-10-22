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
import ch.usi.cloud.controller.common.ChangeRequest;
import ch.usi.cloud.controller.common.ChangeRequest.RequestType;
import ch.usi.cloud.controller.common.ConfigurationFeature;
import ch.usi.cloud.controller.common.ManifestConnector;
import ch.usi.cloud.controller.common.ServiceConfiguration;
import ch.usi.cloud.controller.common.ServiceLevelObjective;
import ch.usi.cloud.controller.common.db.TablesManager;
import ch.usi.cloud.controller.impl.ConfigurationException;
import ch.usi.cloud.controller.impl.EmptyModel;
import ch.usi.cloud.controller.util.ServiceManagementUtils;

/**
 * 
 * Basic rules configuration selector for the DoReMap application
 * by using this configuration selector we can specify the minimum
 * and maximum number of jobs for doodleas and jopera that trigger
 * respectively a scaledown or a scaleup action
 * 
 * @author Mario Bisignani (bisignam@usi.ch)
 *
 */
public class DoReMapConfigurationSelectorRules extends ConfigurationSelector {
	
	public DoReMapConfigurationSelectorRules(long controlPeriod,
			ManifestConnector mc, DataSource ds, String serviceId) {
		super(controlPeriod, mc, ds, serviceId);
		minJobsAS = Integer.parseInt(System.getProperty(
				DOREMAP_MIN_JOBS_PER_DOODLEAS, Integer.toString(minJobsAS)));
		maxJobsAS = Integer.parseInt(System.getProperty(
				DOREMAP_MAX_JOBS_PER_DOODLEAS, Integer.toString(maxJobsAS)));
		minJobsJopera = Integer.parseInt(System.getProperty(
				DOREMAP_MIN_JOBS_PER_JOPERA, Integer.toString(minJobsJopera)));
		maxJobsJopera = Integer.parseInt(System.getProperty(
				DOREMAP_MAX_JOBS_PER_JOPERA, Integer.toString(maxJobsJopera)));
	}
	
	@Override
	public void init(){
		// Before used this
		// super.init();


		// we don't need to do any training. Use a null TSS
		// TODO: maybe it makes sense moving the NullTSS to a common lib for all
		// rule-based controllers
		this.tss = new NullTrainingSampleSelector(this.getDataSource(), models);
	}

	private static Logger logger = Logger.getLogger(ch.usi.cloud.controller.doremap.confselectors.DoReMapConfigurationSelectorRules.class);
	
	//Properties names for the doramp rules conf selector
	public static String DOREMAP_MIN_JOBS_PER_DOODLEAS = "DOREMAP_MIN_JOBS_PER_DOODLEAS";
	public static String DOREMAP_MAX_JOBS_PER_DOODLEAS = "DOREMAP_MAX_JOBS_PER_DOODLEAS";
	public static String DOREMAP_MIN_JOBS_PER_JOPERA = "DOREMAP_MIN_JOBS_PER_JOPERA";
	public static String DOREMAP_MAX_JOBS_PER_JOPERA = "DOREMAP_MAX_JOBS_PER_JOPERA";
	
	
	//Default Properties values
	int minJobsAS = 5; // minimum number of jobs each rubisas needs to have to
						// avoid scale down
	int maxJobsAS = 20; // max number of jobs each rubisas can have before
						// triggering scale up
	
	int minJobsJopera = 10; // minimum number of jobs each jopera needs to have to
	// avoid scale down
	int maxJobsJopera = 27; // max number of jobs each jopera can haave before
	// triggering scale up
	
	
	private double getMaxInstances(String veeName){
		for(ConfigurationFeature feature: mc.getConfiguration()){
			if(feature.name.equals(veeName)){
				return feature.max;
			}
		}
		return 0;
	}
	
	private double getMinInstances(String veeName){
		for(ConfigurationFeature feature: mc.getConfiguration()){
			if(feature.name.equals(veeName)){
				return feature.min;
			}
		}
		return 0;
	}

	@Override
	protected ServiceConfiguration getTargetConfiguration()
			throws ConfigurationException {
		
		//WE COMPUTE THE NEXT CONFIGURATION USING THE RULES
		
		if (currentConfiguration == null) {
			throw new ConfigurationException("Missing current configuration");
		}

		// this is the average system context, in terms of queueLength and
		// incoming requests in the last 6 periods
		double[] context = getServiceContextAverage(monitoringDs, 6); // [queueLength,requestCount]
		logger.info("Retrieved context: " + Arrays.toString(context));
		
		ServiceConfiguration intermediateConf;
		ServiceConfiguration finalConfiguration;
		try {
			intermediateConf = applyRule(currentConfiguration, "doodleas", maxJobsAS, minJobsAS, context);
			finalConfiguration = applyRule(intermediateConf, "jopera", maxJobsJopera, minJobsJopera, context);
		} catch (Exception e) {
			logger.error("Unable to apply configuration selector rules, not applying any rules: "+e.getMessage());
			e.printStackTrace();
			finalConfiguration=currentConfiguration;
		}
		
		logger.info("next configuration: " + finalConfiguration);
		
		return finalConfiguration;
	}

	@Override
	protected ConfigurationFeature getOutputFeature(String kPIName) {
		return new ConfigurationFeature("fakeOuputFeature", "fakeOutputFeature");
	}
	
	private ServiceConfiguration applyRule(ServiceConfiguration initialConfiguration, 
										  String veeName, int maxJobs, int minJobs, 
										  double[] context) throws Exception{
		
		ChangeRequest chReq;
		ChangeRequest.RequestType chReqType = null;
		
		int instancesNum = currentConfiguration.getInstancesCount(veeName);

		logger.info("Current "+veeName+": " + instancesNum);

		if (context[1] / instancesNum > maxJobs) {
			if(instancesNum < getMaxInstances(veeName)){
				logger.info("Current configuration is too small with " 
							+ context[1] / instancesNum 
							+ " jobs per "+veeName+" (Scaling up)");
			
				// then scale up
				instancesNum++;
				chReqType=RequestType.ADD;
				
			}else{
				logger.warn("THE MAXIMUM NUMBER OF "+veeName+" HAS BEEN REACHED, skipping new configuration with "+(instancesNum+1)+" "+veeName);
			}
		} else if (context[1] / instancesNum < minJobsAS && instancesNum != 1) {
			if(instancesNum > getMinInstances(veeName)){
				logger.info("Current configuration is too big with " + context[0]
				            / instancesNum + " jobs per "+veeName+" (Scaling down)");
				
				// then scale down
				instancesNum--;
				chReqType=RequestType.REMOVE;
				
			}else{
				logger.warn("THE MINIMUM NUMBER OF "+veeName+" HAS BEEN REACHED, skipping new configuration with "+(instancesNum-1)+" "+veeName);
			}
		}
		
		if(chReqType != null){
		
			//We can finally create the changeRequest object for doodleas
			chReq = new ChangeRequest(veeName, ""/*used only for REMOVE_SPECIFIC_*/, chReqType);
	
			logger.info("Requested "+instancesNum+": " + instancesNum);
			logger.info("ChangeRequest for "+veeName+": " + chReqType);
			
			//We apply the modification to the currentConfiguraiton object
			return ServiceManagementUtils.computeNewConf(initialConfiguration, chReq);
			
		}else{
			
			return initialConfiguration;
			
		}
	}
	

	@Override
	protected Collection<? extends ConfigurationFeature> getInputFeatures(
			List<String> kpiList, ServiceLevelObjective slo) {
		//new Exception("This method is not implemented.").printStackTrace();
		return new ArrayList<ConfigurationFeature>();
	}
	
	/**
	 * This version takes an average of the last N monitoring periods. This is
	 * basically a smoother version of the workload and queue length trace
	 */

	public double[] getServiceContextAverage(DataSource ds, int periods) {
		
		StringBuffer query = new StringBuffer();
		// we'll compute the average equal to the size of our actuator time
		query.append("SELECT avg(kpi_createpollQueueLength+kpi_getpollQueueLength+kpi_votepollQueueLength+kpi_deletepollQueueLength), " +
							"avg(kpi_createpollReqCount+kpi_getpollReqCount+kpi_votepollReqCount+kpi_deletepollReqCount)");
		query.append(" FROM " + TablesManager.getTableNameFromServiceId(mc.getServiceId()));
		query.append("  where rowid in (select rowid from "
				+ TablesManager.getTableNameFromServiceId(mc.getServiceId())
				+ " where kpi_createpollQueueLength not null and " +
						"kpi_getpollQueueLength not null and " +
						"kpi_votepollQueueLength not null and " +
						"kpi_deletepollQueueLength not null and " +
						"kpi_createpollReqCount not null and " +
						"kpi_getpollReqCount not null and "+
						"kpi_votepollReqCount not null and "+
						"kpi_deletepollReqCount not null and "		
				+ "time > " + (System.currentTimeMillis() - 600000) /*NOTE: if we use this method we must be sure that the remote VM with the monitoringendpoint
																			and the machine on which the controller is running are perfectly synchronized*/
				+ " ORDER BY rowid DESC LIMIT " + periods + ");");
		if (logger.isDebugEnabled()) {
			logger.debug("QUERY: " + query);
		}
		
		//first field is avg queueLenght
		//second field is avg requestsCount
		double[] output = new double[2];

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
			logger.warn("DB does not contain data from at least  10 minutes ago. Waiting for updates");
			// db is historical, no new data has yet come in. set context to 0
			output[0] = output[1] = 0;
			// TODO Auto-generated catch block
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

}
