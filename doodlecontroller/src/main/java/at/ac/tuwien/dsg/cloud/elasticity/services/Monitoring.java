package at.ac.tuwien.dsg.cloud.elasticity.services;

import java.util.List;

import at.ac.tuwien.dsg.cloud.data.DynamicServiceDescription;

public interface Monitoring {

	// THIS IS TOO BROAD... BUT FOR THE MOMENT SHOULD BE FINE !
	public List<Object> getData(DynamicServiceDescription service, String query);
}
