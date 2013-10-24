package at.ac.tuwien.dsg.cloud.elasticity.services.impl.actuation;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import at.ac.tuwien.dsg.cloud.data.DynamicServiceDescription;
import at.ac.tuwien.dsg.cloud.data.InstanceDescription;
import at.ac.tuwien.dsg.cloud.data.VeeDescription;
import at.ac.tuwien.dsg.cloud.elasticity.services.ConfigurationActuator;
import at.ac.tuwien.dsg.cloud.services.CloudController;

/**
 * This class contains the logic that implements a basic form of
 * BlockingConfigurationActuator; this means that the calling thread will block
 * until the actuate method returns
 * 
 * @author alessiogambi
 * 
 */
public class BlockingConfigurationActuator implements ConfigurationActuator {

	private final DynamicServiceDescription service;
	private Logger logger;
	private CloudController controller;

	public BlockingConfigurationActuator(Logger logger,
			final DynamicServiceDescription service, CloudController controller) {
		this.logger = logger;
		this.controller = controller;
		this.service = service;
	}

	@Override
	public void actuate(DynamicServiceDescription targetConfiguration) {

		// Compute the differnce and deploy/undeploy the VEE
		List<InstanceDescription> instancesToRemove = new ArrayList<InstanceDescription>();
		for (VeeDescription vee : service.getStaticServiceDescription()
				.getOrderedVees()) {

			for (InstanceDescription instance : service.getVeeInstances(vee
					.getName())) {
				if (!targetConfiguration.getVeeInstances(vee.getName())
						.contains(instance)) {
					logger.info("Instance " + instance
							+ " marked to be removed ");
					instancesToRemove.add(instance);
				}
			}
		}

		List<InstanceDescription> instancesToAdd = new ArrayList<InstanceDescription>();
		for (VeeDescription vee : targetConfiguration
				.getStaticServiceDescription().getOrderedVees()) {

			for (InstanceDescription instance : targetConfiguration
					.getVeeInstances(vee.getName())) {
				if (!service.getVeeInstances(vee.getName()).contains(instance)) {
					logger.info("Instance " + instance + " marked to be added");
					instancesToAdd.add(instance);
				}
			}
		}

		// Now add the ones to be added

		// Finally implements the changes
		try {
			List<String> instancesToRemoveIDs = new ArrayList<String>();
			for (InstanceDescription instance : instancesToRemove) {
				instancesToRemoveIDs.add(instance.getInstanceId());
			}

			logger.info("Removing from Cloud: " + instancesToRemove);
			controller.removeVEEsbyInstanceID(service, instancesToRemoveIDs);
			// Here remove from current !!!
			// FIXME Note that this is not working fine !!!
			for (InstanceDescription instance : instancesToRemove) {
				service.removeReplica(instance.getReplicaFQN());
			}

			logger.info("After Remove: " + service);
		} catch (Exception e) {
			logger.error("Error while removing replicas " + instancesToRemove,
					e);
		}

		for (InstanceDescription instance : instancesToAdd) {
			try {
				logger.info("Launchin " + instance.getReplicaFQN() + " "
						+ targetConfiguration.getDeployID());

				// Now we need to take the Current to the target !
				controller.launchVEEwithReplicaFQN(instance.getReplicaFQN(),
						service, service.getDeployID());
				logger.info("After Deploy " + service);
			} catch (Exception e) {
				logger.error("Error while deployng new replica " + instance, e);
			}
		}
	}

}
