package at.ac.tuwien.dsg.cloud.elasticity.services.impl.monitoring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;

import at.ac.tuwien.dsg.cloud.data.DynamicServiceDescription;
import at.ac.tuwien.dsg.cloud.data.InstanceDescription;
import at.ac.tuwien.dsg.cloud.data.VeeDescription;
import at.ac.tuwien.dsg.cloud.elasticity.services.ServiceUpdater;
import at.ac.tuwien.dsg.cloud.services.CloudInterface;
import ch.usi.cloud.controller.common.naming.FQN;

public class ServiceUpdaterImpl implements ServiceUpdater {

	// TODO Try to figure out why cloudInterface issues so many requests for
	// describing user data. I suspect it's to read sec group desc field, which
	// is not really informative. It should be used only to match deploy ID to
	// ServiceFQN.

	private Logger logger;
	private CloudInterface cloud;

	public ServiceUpdaterImpl(Logger logger, CloudInterface cloud) {
		this.logger = logger;
		this.cloud = cloud;
	}

	@Override
	public void update(DynamicServiceDescription service) {

		UUID deployId = service.getDeployID();
		FQN serviceFQN = service.getStaticServiceDescription().getServiceFQN();

		logger.info("update: " + deployId + "  " + serviceFQN);

		try {
			// Get all the IDs of all the instances belonging to the service to
			// update
			Set<String> instanceIDs = cloud.getServiceInstances(serviceFQN,
					deployId);

			// Get all the details
			List<InstanceDescription> instances = new ArrayList<InstanceDescription>();
			for (String instanceID : instanceIDs) {
				instances.add(cloud.getInstanceDescriptionByID(instanceID));
			}

			// THis is the new representation
			HashMap<String, ArrayList<InstanceDescription>> _instances = new HashMap<String, ArrayList<InstanceDescription>>();

			// Compare with instances of service
			for (VeeDescription vee : service.getStaticServiceDescription()
					.getOrderedVees()) {
				logger.info("Updating " + vee.getName());

				_instances.put(vee.getName(),
						new ArrayList<InstanceDescription>());

				for (InstanceDescription instance : service.getVeeInstances(vee
						.getName())) {
					// Check if the service instance was inside the cloud
					// instances
					if (!instances.contains(instance)) {
						// If the instance is not in the Cloud do not add it
						logger.info("Removing " + instance);
						// Simply ignore it
					} else {
						// If the instance is both inside the service and the
						// cloud, update it (meaning use the cloud one)
						// TODO Update it.. hope this is a fine move !!
						logger.info("Updating " + instance);
						_instances.get(vee.getName()).add(instance);
					}
					logger.info("Marking as visited " + instance);
					instances.remove(instance);
				}
			}

			// At this point we just need to add again the newly added
			// instances
			for (InstanceDescription instance : instances) {
				logger.info("Adding " + instance);
				logger.info("instance.getReplicaFQN() "
						+ instance.getReplicaFQN().getVeeName());

				_instances.get(instance.getReplicaFQN().getVeeName()).add(
						instance);
			}
			// Cleaning up
			instances.clear();

			service.setInstances(_instances);
			service.updateReplicaNumbers();

		} catch (Throwable e) {
			e.printStackTrace();
			String msg = "Cannot update service " + serviceFQN
					+ " under deploy " + deployId;
			logger.error(msg, e);

			throw new RuntimeException(e);
		}

	}
}
