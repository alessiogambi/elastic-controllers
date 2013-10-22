package at.ac.tuwien.dsg.cloud.elasticity.services.impl.monitoring;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.dbcp.BasicDataSource;
import org.slf4j.Logger;

import at.ac.tuwien.dsg.cloud.data.DynamicServiceDescription;
import at.ac.tuwien.dsg.cloud.elasticity.services.Monitoring;
import at.ac.tuwien.dsg.cloud.elasticity.util.TablesManagerApplicationDB;

public class MySQLMonitoring implements Monitoring {

	private Logger logger;
	private BasicDataSource applicationDB;

	// Should be a provided generated object ?!
	// We need to distinguish resources vs services !
	// ApplicationDB
	public MySQLMonitoring(Logger logger, BasicDataSource applicationDB) {
		this.logger = logger;
		this.applicationDB = applicationDB;

		logger.info("MySQLMonitoring  configurations:\n" + "\t"
				+ this.applicationDB);
	}

	@Override
	public List<Object> getData(DynamicServiceDescription service, String query) {
		// Inject in the query all the service specific elements and execute,
		// extract the results and return a list of tuples

		logger.debug("Original QUERY " + query);

		String tableName = TablesManagerApplicationDB
				.getMonitoringTableName(service);

		query = query.replaceAll("@SERVICE_TABLE", tableName);

		List<Object> valuesFromDB = executeQuery(query);

		logger.debug("Retrieved " + (valuesFromDB.size() - 1) + " values");

		return valuesFromDB;
	}

	private List<Object> executeQuery(String query) {
		logger.debug("Executing QUERY " + query);
		List<Object> valuesFromDB = new ArrayList<Object>();

		Connection conn = null;
		Statement stat = null;
		try {
			// NOTE THIS IS NOT REALLY GOOD... CHECK THE NUMBER OF OPEN
			// CONNECTIONS AND SO ON !
			conn = this.applicationDB.getConnection();
			stat = conn.createStatement();

			ResultSet rs = stat.executeQuery(query);
			ResultSetMetaData md = rs.getMetaData();
			int columns = md.getColumnCount();
			logger.debug("Columns " + columns);
			// Header
			Object[] header = new Object[columns];
			for (int i = 0; i < columns; ++i) {
				logger.debug("Columns i " + md.getColumnName(i + 1));
				header[i] = md.getColumnName(i + 1);
			}
			valuesFromDB.add(header);
			logger.debug("Rows ");
			while (rs.next()) {
				Object[] row = new Object[columns];
				for (int i = 0; i < columns; ++i) {
					row[i] = rs.getObject(i + 1);
					logger.debug(i + " " + row[i]);
				}
				valuesFromDB.add(row);
				logger.debug("");
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (stat != null) {
				try {
					stat.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			if (conn != null)
				try {
					conn.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}

		logger.debug("MySQLMonitoring.executeQuery() " + valuesFromDB.size());

		return valuesFromDB;
	}
	// This will be usedful in the future !
	// private long getLastTimeStamp(FQN serviceFQN) throws SQLException {
	// logger.info("Retrieving information about all the instances of all the services from the DB");
	//
	// Connection conn = null;
	// Statement stat = null;
	//
	// long l = 0L;
	// try {
	// conn = this.applicationDB.getConnection();
	// stat = conn.createStatement();
	//
	// logger.info("Executing query: Select MAX(time) from "
	// + TablesManagerApplicationDB
	// .getMonitoringTableNameFromServiceFQN(serviceFQN
	// .toString()) + " ;");
	//
	// ResultSet rs = stat.executeQuery("SELECT MAX(time) from "
	// + TablesManagerApplicationDB
	// .getMonitoringTableNameFromServiceFQN(serviceFQN
	// .toString()) + " ;");
	//
	// if (rs.next()) {
	// if (rs.getString(1) != null) {
	// l = rs.getTimestamp(1).getTime();
	// return l;
	// }
	// logger.debug("Null has been returned has max timestamp, returning 0 as last timestamp");
	// l = 0L;
	// return l;
	// }
	// return l;
	// } finally {
	// if (stat != null) {
	// stat.close();
	// }
	//
	// if (conn != null)
	// conn.close();
	// }
	// }
	//
	// private ArrayList<KPIsMeasurement> getAllDataBeforeTimeStamp(
	// DynamicServiceDescription service, Long timeStamp)
	// throws SQLException {
	// logger.info("Retrieving all the Kpis values with timestamp before "
	// + timeStamp);
	//
	// ArrayList measurementsList = new ArrayList();
	//
	// Connection conn = null;
	// PreparedStatement st = null;
	// try {
	// StringBuffer sb = new StringBuffer();
	//
	// sb.append("SELECT *");
	// sb.append(" FROM ");
	// sb.append(TablesManagerApplicationDB
	// .getMonitoringTableNameFromServiceFQN(service
	// .getStaticServiceDescription().getServiceFQN()
	// .toString()));
	// sb.append(" WHERE time <= ?");
	// sb.append(" ORDER BY time DESC LIMIT 100;");
	//
	// logger.info("Trying to get a connection from the data source");
	// conn = this.applicationDB.getConnection();
	// logger.debug("Got connection from the data source");
	//
	// st = conn.prepareStatement(sb.toString());
	// st.setTimestamp(1, new Timestamp(timeStamp.longValue()));
	//
	// ResultSet rs = st.executeQuery();
	//
	// KPIsMeasurement measurements = null;
	//
	// int i = 0;
	//
	// while (rs.next()) {
	// Long timestampz = Long.valueOf(rs.getTimestamp("time")
	// .getTime());
	//
	// // measurements = new KPIsMeasurement(timestampz.longValue(),
	// // service);
	//
	// for (String kpiName : this.serviceKPIs) {
	// logger.debug("(getAllDataBeforeTimeStamp) Adding to the list of measurements: \n\t\tKPI name: "
	// + kpiName
	// + "\n"
	// + "\t\t"
	// + "KPI column name: "
	// + TablesManagerApplicationDB
	// .getColumnNameForKpi(kpiName)
	// + "\n"
	// + "\t\t"
	// + "KPI value: "
	// + rs.getString(TablesManagerApplicationDB
	// .getColumnNameForKpi(kpiName)));
	//
	// if (rs.getString(TablesManagerApplicationDB
	// .getColumnNameForKpi(kpiName)) != null) {
	// measurements
	// .addKpiMeasurement(
	// kpiName,
	// Double.valueOf(Double.parseDouble(rs
	// .getString(TablesManagerApplicationDB
	// .getColumnNameForKpi(kpiName)))));
	// } else {
	// measurements.addKpiMeasurement(kpiName, null);
	// }
	// }
	// ++i;
	// if (measurements != null) {
	// measurementsList.add(measurements);
	// }
	// }
	// } finally {
	// if (st != null) {
	// st.close();
	// }
	//
	// if (conn != null) {
	// conn.close();
	// }
	// }
	//
	// return measurementsList;
	// }
	//
	// private ArrayList<KPIsMeasurement> getAllDataAfterTimeStamp(
	// DynamicServiceDescription service, Long timeStamp)
	// throws SQLException {
	// logger.debug("Retrieving all the Kpis values with timestamp after "
	// + timeStamp);
	//
	// ArrayList measurementsList = new ArrayList();
	//
	// Connection conn = null;
	// PreparedStatement st = null;
	// try {
	// StringBuffer sb = new StringBuffer();
	//
	// sb.append("SELECT *");
	// sb.append(" FROM ");
	// sb.append(TablesManagerApplicationDB
	// .getMonitoringTableNameFromServiceFQN(service
	// .getStaticServiceDescription().getServiceFQN()
	// .toString()));
	//
	// sb.append(" WHERE time > ?");
	// sb.append(" ORDER BY time ASC;");
	//
	// logger.debug("Trying to get a connection from the data source");
	// conn = this.applicationDB.getConnection();
	// logger.debug("Got connection from the data source");
	//
	// st = conn.prepareStatement(sb.toString());
	// st.setTimestamp(1, new Timestamp(timeStamp.longValue()));
	//
	// logger.debug("Executing query (TIME:" + timeStamp + ") "
	// + sb.toString());
	//
	// ResultSet rs = st.executeQuery();
	//
	// KPIsMeasurement measurements = null;
	//
	// int i = 0;
	//
	// while (rs.next()) {
	// logger.debug("New ROW " + i);
	//
	// Long timestampz = Long.valueOf(rs.getTimestamp("time")
	// .getTime());
	//
	// // measurements = new KPIsMeasurement(timestampz.longValue(),
	// // service);
	//
	// for (String kpiName : this.serviceKPIs) {
	// logger.debug("(getAllDataAfterTimeStamp) Adding to the list of measurements: \n\t\tKPI name: "
	// + kpiName
	// + "\n"
	// + "\t\t"
	// + "KPI column name: "
	// + TablesManagerApplicationDB
	// .getColumnNameForKpi(kpiName)
	// + "\n"
	// + "\t\t"
	// + "KPI value: "
	// + rs.getString(TablesManagerApplicationDB
	// .getColumnNameForKpi(kpiName)));
	//
	// if (rs.getString(TablesManagerApplicationDB
	// .getColumnNameForKpi(kpiName)) != null) {
	// measurements
	// .addKpiMeasurement(
	// kpiName,
	// Double.valueOf(Double.parseDouble(rs
	// .getString(TablesManagerApplicationDB
	// .getColumnNameForKpi(kpiName)))));
	// } else {
	// measurements.addKpiMeasurement(kpiName, null);
	// }
	// }
	// ++i;
	//
	// if (measurements != null) {
	// measurementsList.add(measurements);
	// }
	// }
	// } finally {
	// if (st != null) {
	// st.close();
	// }
	//
	// if (conn != null) {
	// conn.close();
	// }
	// }
	//
	// return measurementsList;
	// }

}
