package at.ac.tuwien.dsg.cloud.elasticity.services;

import at.ac.tuwien.dsg.cloud.data.DynamicServiceDescription;

public interface ServiceUpdaterCache extends ServiceUpdater {

	public void invalidate();

	public boolean isValid();

	public void store(DynamicServiceDescription service);

	// public void update(DynamicServiceDescription service);
}
