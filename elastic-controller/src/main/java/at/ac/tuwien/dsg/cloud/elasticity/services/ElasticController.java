package at.ac.tuwien.dsg.cloud.elasticity.services;

public interface ElasticController {

	public void stop();

	/**
	 * This starts the Controller. This is an IDEMPotent method invocation. This
	 * assume that the service object refers to a running service in the Cloud !
	 * The object is meant to be build via ServiceDeployer.deploy(...) or
	 * ServiceDeployer.getService(...)
	 * 
	 * 
	 * @param service
	 */
	public void start();
}
