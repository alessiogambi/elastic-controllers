package at.ac.tuwien.dsg.cloud.elasticity.services.impl.configurationactuators;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;

import at.ac.tuwien.dsg.cloud.data.DynamicServiceDescription;
import at.ac.tuwien.dsg.cloud.data.InstanceDescription;
import at.ac.tuwien.dsg.cloud.data.VeeDescription;
import at.ac.tuwien.dsg.cloud.services.CloudController;

public class QueuedConfigurationActuator extends
		NonBlockingConfigurationActuator {

	private Logger logger;
	private CloudController controller;

	// TODO register to notification shutodown hub !
	private ExecutorService executor;
	private BlockingQueue<DynamicServiceDescription> queuedConfigurationChanges;

	// This is the reference to the real object ! MUST ONLY BE READ ! Not sure
	// about thread safetiness here, nor concurrent/iterations
	private DynamicServiceDescription service;

	public QueuedConfigurationActuator(Logger _logger,
			CloudController controller) {
		super();

		this.logger = _logger;
		this.controller = controller;

		// This is shared between the runnable inside the executor services and
		// the thread inside the configuration actuator
		this.queuedConfigurationChanges = new LinkedBlockingQueue<DynamicServiceDescription>();

		// Not sure really elegant ...
		executor.execute(new Runnable() {
			@Override
			public void run() {
				while (true) {
					try {
						_actuateTheConfiguration();
					} catch (InterruptedException e) {
						logger.info("Actuator Thread Interrupted !", e);
						break;
					}
				}
				executor.shutdown();
			}
		});
	}

	@Override
	public void actuateTheConfiguration(
			DynamicServiceDescription currentConfiguration,
			DynamicServiceDescription targetConfiguration) {

		if (service == null) {
			service = currentConfiguration;
		} else {
			if (service != currentConfiguration) {
				logger.warn("Service object changed ?!");
			}
		}

		if (this.queuedConfigurationChanges.offer(targetConfiguration)) {
			logger.info("Enqueued a new configuration " + targetConfiguration);
		} else {
			logger.warn("The new configuration was not enqueued: "
					+ targetConfiguration);
		}
	}

	/*
	 * This is the method actually doing the work ! And it is blocking !
	 */
	public void _actuateTheConfiguration() throws InterruptedException {

		// This should create a new copy of the object... just in case. Not sure
		// about thread safetyness
		DynamicServiceDescription currentConfiguration = new DynamicServiceDescription(
				service);

		// Take the last element, i.e., newest inserted, and ignore all the
		// others.
		List<DynamicServiceDescription> targetConfigurations = new ArrayList<DynamicServiceDescription>();

		// This is needed to block the thread !
		targetConfigurations.add(this.queuedConfigurationChanges.take());
		int confQueued = this.queuedConfigurationChanges
				.drainTo(targetConfigurations);
		if (confQueued < 1) {
			logger.info("No target configurations scheduled !");
			return;
		}

		// Here we implement it
		DynamicServiceDescription targetConfiguration = targetConfigurations
				.get(targetConfigurations.size() - 1);

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
