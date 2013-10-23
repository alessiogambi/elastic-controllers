package at.ac.tuwien.dsg.cloud.elasticity.data;

import java.util.UUID;

import org.hibernate.validator.constraints.Length;

import at.ac.tuwien.dsg.cloud.elasticity.annotations.ValidControllerName;
import at.ac.tuwien.dsg.cloud.elasticity.annotations.ValidFQN;
import ch.usi.cloud.controller.common.naming.FQN;

import com.sun.istack.NotNull;

public class ElasticControllerConfiguration {

	@ValidFQN
	private FQN serviceFQN;

	@NotNull
	@ValidControllerName
	private String controllerName;

	@NotNull
	private UUID deployID;

	@Length(max = 3)
	private String organizationName;
	@Length(max = 3)
	private String customerName;
	@Length(max = 3)
	private String serviceName;

	public FQN getServiceFQN() {
		return serviceFQN;
	}

	public void setServiceFQN(FQN serviceFQN) {
		this.serviceFQN = serviceFQN;
	}

	public String getControllerName() {
		return controllerName;
	}

	public void setControllerName(String controllerName) {
		this.controllerName = controllerName;
	}

	public UUID getDeployID() {
		return deployID;
	}

	public void setDeployID(UUID deployID) {
		this.deployID = deployID;
	}

	public String getOrganizationName() {
		return organizationName;
	}

	public void setOrganizationName(String organizationName) {
		this.organizationName = organizationName;
	}

	public String getCustomerName() {
		return customerName;
	}

	public void setCustomerName(String customerName) {
		this.customerName = customerName;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

}
