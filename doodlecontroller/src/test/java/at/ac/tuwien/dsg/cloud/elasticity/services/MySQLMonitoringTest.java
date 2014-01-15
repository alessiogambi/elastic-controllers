package at.ac.tuwien.dsg.cloud.elasticity.services;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.UUID;

import org.apache.tapestry5.ioc.IOCUtilities;
import org.apache.tapestry5.ioc.Registry;
import org.apache.tapestry5.ioc.RegistryBuilder;

import at.ac.tuwien.dsg.cloud.data.DynamicServiceDescription;
import at.ac.tuwien.dsg.cloud.data.StaticServiceDescription;
import at.ac.tuwien.dsg.cloud.elasticity.data.DoodleSymbolConstants;
import at.ac.tuwien.dsg.cloud.elasticity.data.SymbolConstants;
import at.ac.tuwien.dsg.cloud.elasticity.modules.DoodleElasticControlModule;
import at.ac.tuwien.dsg.cloud.elasticity.modules.DoodleServiceModule;
import at.ac.tuwien.dsg.cloud.exceptions.ServiceDeployerException;
import at.ac.tuwien.dsg.cloud.manifest.StaticServiceDescriptionFactory;
import at.ac.tuwien.dsg.cloud.modules.CloudAppModule;
import at.ac.tuwien.dsg.cloud.openstack.modules.OSCloudAppModule;
import ch.usi.cloud.controller.common.naming.FQN;

public class MySQLMonitoringTest {
	public static void main(String[] args) throws ServiceDeployerException,
			MalformedURLException {
		String _deployID = "e785ad96-f631-456b-bb24-e60e289705bf";
		UUID deployID = UUID.fromString(_deployID);
		String organizationName = "dsg";
		String customerName = "pes";
		String serviceName = "pes";
		FQN serviceFQN = new FQN(organizationName, customerName, serviceName);
		String manifestURL = "http://www.inf.usi.ch/phd/gambi/attachments/autocles/doodle-manifest.xml";

		System.getProperties().put("args:serviceFQN", serviceFQN.toString());
		System.getProperties().put("args:deployID", _deployID);
		System.getProperties().put("at.ac.tuwien.dsg.cloud.configuration",
				"src/test/resources/cloud.properties");

		Registry registry;

		// Setup the registry and get the service instance
		RegistryBuilder builder = new RegistryBuilder();
		// Load all the modules in the class path that have the right
		// manifest
		// entries
		IOCUtilities.addDefaultModules(builder);
		// Add the local modules

		builder.add(CloudAppModule.class);
		builder.add(OSCloudAppModule.class);
		builder.add(DoodleElasticControlModule.class);
		builder.add(DoodleServiceModule.class);

		// Build and start the registry
		registry = builder.build();
		registry.performRegistryStartup();

		StaticServiceDescription _service = new StaticServiceDescription(
				serviceFQN, StaticServiceDescriptionFactory
						.fromURL(manifestURL).getOrderedVees(), new URL(
						manifestURL));

		DynamicServiceDescription service = new DynamicServiceDescription(
				_service, deployID);

		Monitoring monitoring = registry.getService(
				SymbolConstants.SQL_MONITORING, Monitoring.class);

		registry.getService("ServiceUpdater", ServiceUpdater.class).update(
				service);

		// THIS IS RUDE BUT SHOULD WORK
		// TODO use default symbols to build this up !
		// String dbURL = "jdbc:mysql://"
		// + service.getVeeInstances("frontend").get(0).getPublicIp()
		// .getHostAddress() + ":3306/monitoring";
		//
		// System.out.println("MySQLMonitoringTest.main() dbURL " + dbURL);
		// // This is the trick to simulate -D user provided values
		// System.getProperties().setProperty(DoodleSymbolConstants.DB_JDBC_URL,
		// dbURL);

		String dbHost = service.getVeeInstances("frontend").get(0)
				.getPublicIp().getHostAddress();
		System.out.println("ElasticDoodleControllerTest.main() " + dbHost);
		System.getProperties().setProperty(DoodleSymbolConstants.DB_HOST,
				dbHost);

		List<Object> result = null;

		for (int i = 0; i < 5; i++) {

			String query = "SELECT avg(kpi_CREATE_POLL_RC), avg(kpi_GET_POLL_RC), avg(kpi_VOTE_RC), avg(kpi_DELETE_POLL_RC) FROM @SERVICE_TABLE as M JOIN (SELECT MAX(A.time) as maxTime, MIN(A.time) as minTime FROM (SELECT time FROM @SERVICE_TABLE ORDER BY time desc LIMIT 6)as A )as B WHERE M.time < B.maxTime AND M.time > B.minTime;";

			// result = monitoring.getData(service,
			// "Select count(*) as C from @SERVICE_TABLE;");
			result = monitoring.getData(service, query);

			for (Object _row : result) {
				Object[] row = (Object[]) _row;
				for (int j = 0; j < row.length; j++) {
					System.out.print(row[j] + ", ");
				}
				System.out.println("");
			}

			try {
				Thread.sleep(5000);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
