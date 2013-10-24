package at.ac.tuwien.dsg.cloud.elasticity.services;

/**
 * This service can be used to allow different waiting schemata for the
 * controller. For example, some controllers may have period triggering, others
 * may need to be triggered only at given instants (i.e. every ten minutes since
 * the beginning)
 * 
 * @author alessiogambi
 * 
 */
public interface WaitService {

	public void waitMe();
}
