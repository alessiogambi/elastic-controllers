package at.ac.tuwien.dsg.cloud.elasticity;

import java.io.File;
import java.util.concurrent.TimeUnit;

import at.ac.tuwien.dsg.cloud.elasticity.data.DoodleSymbolConstants;

public class FixedRulesMain {

	public static void main(String[] args) {
		// Environment settings:
		File log4jfile = new File("src/test/resources/log4j.properties");
		System.getProperties().put("log4j.configuration",
				"file:" + log4jfile.getAbsolutePath());

		File cloudFile = new File("src/test/resources/cloud.properties");
		System.getProperties().put("at.ac.tuwien.dsg.cloud.configuration",
				cloudFile.getAbsolutePath());

		System.getProperties().put(
				"ch.usi.cloud.controller.doodleservice.monitoring.db.host",
				"128.130.172.203");

		System.getProperties().put(
				DoodleSymbolConstants.BACKGROUND_MONITORING_PERIOD, "30");
		System.getProperties().put(
				DoodleSymbolConstants.BACKGROUND_MONITORING_PERIOD_UNIT,
				TimeUnit.SECONDS.name());

		// Inputs
		String[] _args = new String[] {
				"dsg",
				"tes",
				"tes",
				"fb9fba22-f023-4b87-9bb0-9fb02f5f759f",
				"http://128.130.172.198:8081/memcached/autocles-experiment258991610805652--6b4dea74-8d08-400c-9deb-df0356b5b9a6xml",
				"fixed-rules" };

		Main.main(_args);
	}
}
