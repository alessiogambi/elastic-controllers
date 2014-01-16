package at.ac.tuwien.dsg.cloud.elasticity.services.impl.configurationactuators;

import java.util.ArrayList;
import java.util.Collection;

import org.slf4j.Logger;

import at.ac.tuwien.dsg.cloud.data.DynamicServiceDescription;
import at.ac.tuwien.dsg.cloud.data.InstanceDescription;
import at.ac.tuwien.dsg.cloud.data.VeeDescription;
import at.ac.tuwien.dsg.cloud.exceptions.ServiceDeployerException;
import at.ac.tuwien.dsg.cloud.services.CloudController;

public class BasicConfigurationActuator extends BlockingConfigurationActuator {

	private Logger logger;
	private CloudController controller;

	public BasicConfigurationActuator(Logger logger, CloudController controller) {
		super();
		this.logger = logger;
		this.controller = controller;
	}

	@Override
	public void actuateTheConfiguration(
			DynamicServiceDescription currentConfiguration,
			DynamicServiceDescription targetConfiguration) {

		// Compute the differnce and deploy/undeploy the VEE
		Collection<VeeDescription> instancesToRemove = new ArrayList<VeeDescription>();
		for (VeeDescription vee : currentConfiguration
				.getStaticServiceDescription().getOrderedVees()) {

			for (InstanceDescription instance : currentConfiguration
					.getVeeInstances(vee.getName())) {

				// TODO Here we should remove only instances that CAN ACTUALLY
				// BE
				// REMOVED, i.e., THE ONES IN RUNNING STATUS !!

				if (!targetConfiguration.getVeeInstances(vee.getName())
						.contains(instance)
						&& !"RUNNING".equalsIgnoreCase(instance.getState())) {
					logger.info("Vee " + vee.getName() + "(Instance "
							+ instance + ") marked to be removed ");
					instancesToRemove.add(vee);
				}

			}
		}

		Collection<VeeDescription> instancesToAdd = new ArrayList<VeeDescription>();
		for (VeeDescription vee : targetConfiguration
				.getStaticServiceDescription().getOrderedVees()) {

			for (InstanceDescription instance : targetConfiguration
					.getVeeInstances(vee.getName())) {
				if (!currentConfiguration.getVeeInstances(vee.getName())
						.contains(instance)) {
					logger.info("Vee " + vee.getName() + " marked to be added");
					instancesToAdd.add(vee);
				}
			}
		}

		// Now add the ones to be added

		// Finally implements the changes
		try {
			logger.info("Removing from Cloud the following vees: "
					+ instancesToRemove);

			// This modifies the service object passed as input !
			controller.removeVEEs(instancesToRemove, currentConfiguration);

			// Here remove from current !!!
			// FIXME Note that this is not working fine !!!
			// for (InstanceDescription instance : instancesToRemove) {
			// currentConfiguration.removeReplica(instance.getReplicaFQN());
			// }

			logger.info("After Remove: " + currentConfiguration);
		} catch (ServiceDeployerException e) {
			logger.error("Error while removing replicas " + instancesToRemove,
					e);
		} catch (Exception e) {
			logger.error("Error while removing replicas " + instancesToRemove,
					e);
		}

		try {
			logger.info("Launching the following vees :" + instancesToAdd);

			// Now we need to take the Current to the target !
			controller.launchVEEs(instancesToAdd, currentConfiguration);

			logger.info("After Deploy " + currentConfiguration);
		} catch (ServiceDeployerException e) {
			logger.error("Error while adding replicas " + instancesToAdd, e);
		} catch (Exception e) {
			logger.error("Error while addingreplicas " + instancesToAdd, e);
		}

	}
}
