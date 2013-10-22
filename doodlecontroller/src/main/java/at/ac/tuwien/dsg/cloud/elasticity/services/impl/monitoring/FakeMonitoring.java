package at.ac.tuwien.dsg.cloud.elasticity.services.impl.monitoring;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import at.ac.tuwien.dsg.cloud.data.DynamicServiceDescription;
import at.ac.tuwien.dsg.cloud.elasticity.services.Monitoring;

public class FakeMonitoring implements Monitoring {

	private Logger logger;

	public FakeMonitoring(Logger logger) {
		this.logger = logger;
	}

	@Override
	public List<Object> getData(DynamicServiceDescription service, String query) {
		logger.warn("FakeMonitoring.getData() NOT IMPLEMENTED");
		return new ArrayList<Object>();
	}

}
