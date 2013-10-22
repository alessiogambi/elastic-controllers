package at.ac.tuwien.dsg.cloud.elasticity.services.impl.monitoring;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.apache.commons.dbcp.BasicDataSource;
import org.slf4j.Logger;

import at.ac.tuwien.dsg.cloud.data.DynamicServiceDescription;
import at.ac.tuwien.dsg.cloud.data.VeeDescription;
import at.ac.tuwien.dsg.cloud.elasticity.services.ConfigurationSelectorDAO;
import at.ac.tuwien.dsg.cloud.elasticity.util.TablesManagerApplicationDB;

public class DoodleConfigurationSelectorDAO implements ConfigurationSelectorDAO {

	private Logger logger;
	private BasicDataSource applicationDB;

	public DoodleConfigurationSelectorDAO(Logger logger,
			BasicDataSource applicationDB) {
		this.logger = logger;
		this.applicationDB = applicationDB;

		// this.logger.info("DoodleConfigurationSelectorDAO " + logger.getName()
		// + " DB " + applicationDB);
	}

	private void executeCreate(String query) {
		logger.debug("Executing CREATE  " + query);
		Connection conn = null;
		PreparedStatement st = null;
		try {
			logger.debug("Trying to get a connection from the data source");
			conn = this.applicationDB.getConnection();
			logger.debug("Got connection from the data source");
			st = conn.prepareStatement(query);
			st.execute();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if (st != null) {
				try {
					st.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void executeInsert(String query) {
		logger.debug("Executing INSERT: " + query);
		Connection conn = null;
		PreparedStatement st = null;
		try {
			// NOTE THIS IS NOT REALLY GOOD... CHECK THE NUMBER OF OPEN
			// CONNECTIONS AND SO ON !
			logger.debug("Trying to get a connection from the data source");
			conn = this.applicationDB.getConnection();
			logger.debug("Got connection from the data source");
			st = conn.prepareStatement(query);
			st.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
			int result = st.executeUpdate();
			logger.debug("Query executed with result " + result);
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if (st != null) {
				try {
					st.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void createActivationDataTable(
			DynamicServiceDescription currentConfiguration) {
		String tableName = TablesManagerApplicationDB
				.getControllerActivationTableName(currentConfiguration);

		logger.info("Creating Activation Data table for service "
				+ currentConfiguration.getStaticServiceDescription()
						.getServiceFQN());

		StringBuffer query = new StringBuffer();

		query.append("CREATE TABLE IF NOT EXISTS ");
		query.append(tableName);
		query.append(" ( time TIMESTAMP ,");
		for (VeeDescription vee : currentConfiguration
				.getStaticServiceDescription().getOrderedVees()) {
			query.append(vee.getName());
			query.append(" DOUBLE ");
			query.append(" , ");
		}
		// Add the data Column and values
		query.append("avgJob DOUBLE ");
		query.append(")");
		query.append(";");

		executeCreate(query.toString());

		query = null;
	}

	public void createTargetConfigurationTable(
			DynamicServiceDescription currentConfiguration) {
		String tableName = TablesManagerApplicationDB
				.getControllerTargetConfigurationTableName(currentConfiguration);

		logger.info("Creating Target Configuration table for service "
				+ currentConfiguration.getStaticServiceDescription()
						.getServiceFQN());

		StringBuffer query = new StringBuffer();

		query.append("CREATE TABLE IF NOT EXISTS ");
		query.append(tableName);
		query.append(" ( time TIMESTAMP ");
		for (VeeDescription vee : currentConfiguration
				.getStaticServiceDescription().getOrderedVees()) {
			query.append(", ");
			query.append(vee.getName());
			query.append(" DOUBLE ");
		}
		query.append(");");

		executeCreate(query.toString());

		query = null;
	}

	@Override
	public void storeActivationData(
			DynamicServiceDescription currentConfiguration, double... data) {

		String tableName = TablesManagerApplicationDB
				.getControllerActivationTableName(currentConfiguration);

		StringBuffer query = new StringBuffer();
		StringBuffer values = new StringBuffer();
		// INSERT INTO table_name (column1,column2,column3,...)
		// VALUES (value1,value2,value3,...);

		query.append("INSERT INTO ");
		query.append(tableName);
		query.append(" ");

		query.append("(time,");
		values.append("(?,");
		for (VeeDescription vee : currentConfiguration
				.getStaticServiceDescription().getOrderedVees()) {
			query.append(vee.getName());
			query.append(",");

			values.append(currentConfiguration.getVeeInstances(vee.getName())
					.size());
			values.append(",");
		}
		// We know there is only 1
		values.append(data[0]);
		values.append(")");

		// Add the data Column and values
		query.append("avgJob");
		query.append(") ");
		query.append("VALUES ");

		query.append(values);
		query.append(";");

		executeInsert(query.toString());
		query = null;
		values = null;
	}

	@Override
	public void storeTargetConfiguration(
			DynamicServiceDescription targetConfiguration) {

		logger.debug("storeTargetConfiguration " + targetConfiguration);

		String tableName = TablesManagerApplicationDB
				.getControllerTargetConfigurationTableName(targetConfiguration);

		StringBuffer query = new StringBuffer();
		StringBuffer values = new StringBuffer();

		query.append("INSERT INTO ");
		query.append(tableName);
		query.append(" ");

		query.append("(time,");
		values.append("(?,");
		for (VeeDescription vee : targetConfiguration
				.getStaticServiceDescription().getOrderedVees()) {
			query.append(vee.getName());
			query.append(",");
			values.append(targetConfiguration.getVeeInstances(vee.getName())
					.size());
			values.append(",");
		}
		query.deleteCharAt(query.lastIndexOf(","));
		query.append(") ");

		values.deleteCharAt(values.lastIndexOf(","));
		values.append(")");

		// Add the data Column and values
		query.append("VALUES ");
		query.append(values);
		query.append(";");

		executeInsert(query.toString());
		query = null;
		values = null;
	}

}
