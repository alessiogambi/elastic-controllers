package at.ac.tuwien.dsg.cloud.elasticity.services.impl.configurationactuators;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import at.ac.tuwien.dsg.cloud.data.DynamicServiceDescription;
import at.ac.tuwien.dsg.cloud.data.InstanceDescription;
import at.ac.tuwien.dsg.cloud.data.VeeDescription;
import at.ac.tuwien.dsg.cloud.services.CloudController;

public class BasicConfigurationActuator extends
		BlockingConfigurationActuator {

	private Logger logger;
	private CloudController controller;

	public BasicConfigurationActuator(Logger logger,
			CloudController controller) {
		this.logger = logger;
		this.controller = controller;
	}

	@Override
	public void actuateTheConfiguration(
			DynamicServiceDescription currentConfiguration,
			DynamicServiceDescription targetConfiguration) {

		// Compute the differnce and deploy/undeploy the VEE
		List<InstanceDescription> instancesToRemove = new ArrayList<InstanceDescription>();
		for (VeeDescription vee : currentConfiguration
				.getStaticServiceDescription().getOrderedVees()) {

			for (InstanceDescription instance : currentConfiguration
					.getVeeInstances(vee.getName())) {
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
				if (!currentConfiguration.getVeeInstances(vee.getName())
						.contains(instance)) {
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
			controller.removeVEEsbyInstanceID(currentConfiguration,
					instancesToRemoveIDs);
			// Here remove from current !!!
			// FIXME Note that this is not working fine !!!
			for (InstanceDescription instance : instancesToRemove) {
				currentConfiguration.removeReplica(instance.getReplicaFQN());
			}

			logger.info("After Remove: " + currentConfiguration);
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
						currentConfiguration,
						currentConfiguration.getDeployID());
				logger.info("After Deploy " + currentConfiguration);
			} catch (Exception e) {
				logger.error("Error while deployng new replica " + instance, e);
			}
		}
	}
}
