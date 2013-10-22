package at.ac.tuwien.dsg.cloud.elasticity.services;

import at.ac.tuwien.dsg.cloud.data.DynamicServiceDescription;

public interface ServiceUpdater {

	/**
	 * This method takes a service object and update it to reflect the actual
	 * status of the running service at the time of the invocation. This means
	 * that if virtual machines are booting or deleting, they WILL appear inside
	 * the object data structure (with the current status)
	 * 
	 * @param service
	 */
	public void update(DynamicServiceDescription service);
}
