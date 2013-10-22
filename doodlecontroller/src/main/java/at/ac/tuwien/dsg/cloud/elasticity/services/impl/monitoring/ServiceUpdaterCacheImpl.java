package at.ac.tuwien.dsg.cloud.elasticity.services.impl.monitoring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import org.slf4j.Logger;

import at.ac.tuwien.dsg.cloud.data.DynamicServiceDescription;
import at.ac.tuwien.dsg.cloud.data.InstanceDescription;
import at.ac.tuwien.dsg.cloud.data.VeeDescription;
import at.ac.tuwien.dsg.cloud.elasticity.services.ServiceUpdaterCache;
import ch.usi.cloud.controller.common.naming.FQN;

public class ServiceUpdaterCacheImpl implements ServiceUpdaterCache {

	private Logger logger;
	private boolean valid;
	private DynamicServiceDescription cached;

	public ServiceUpdaterCacheImpl(Logger logger) {
		this.logger = logger;
		this.valid = false;
	}

	public synchronized void update(DynamicServiceDescription service) {
		UUID deployId = service.getDeployID();
		FQN serviceFQN = service.getStaticServiceDescription().getServiceFQN();

		logger.info("Update service via cache: " + deployId + "  " + serviceFQN);

		// THis is the new representation
		HashMap<String, ArrayList<InstanceDescription>> _instances = new HashMap<String, ArrayList<InstanceDescription>>();

		// Compare with instances of service
		for (VeeDescription vee : service.getStaticServiceDescription()
				.getOrderedVees()) {
			_instances.put(vee.getName(), new ArrayList<InstanceDescription>());
			for (InstanceDescription instance : cached.getVeeInstances(vee
					.getName())) {
				_instances.get(vee.getName()).add(instance);
			}
		}
		service.setInstances(_instances);
	}

	@Override
	public synchronized void invalidate() {
		logger.info("Invalidate service updater cache");
		this.valid = false;
	}

	@Override
	public synchronized void store(DynamicServiceDescription service) {
		logger.info("store in cache " + service);
		cached = new DynamicServiceDescription(service);
		this.valid = true;
	}

	@Override
	public boolean isValid() {
		return this.valid;
	}
}
