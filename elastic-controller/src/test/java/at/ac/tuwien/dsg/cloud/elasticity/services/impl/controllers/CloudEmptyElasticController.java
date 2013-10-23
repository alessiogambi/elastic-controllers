package at.ac.tuwien.dsg.cloud.elasticity.services.impl.controllers;

import org.apache.tapestry5.ioc.annotations.InjectService;
import org.slf4j.Logger;

import at.ac.tuwien.dsg.cloud.data.DynamicServiceDescription;
import at.ac.tuwien.dsg.cloud.elasticity.services.ElasticController;

public class CloudEmptyElasticController implements ElasticController {

	private Logger logger;

	private DynamicServiceDescription service;

	public CloudEmptyElasticController(Logger logger,
			@InjectService("Service") DynamicServiceDescription service) {
		this.logger = logger;
		this.service = service;

	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}

	@Override
	public void start() {
		logger.info("Started");
		logger.info("ServiceConfiguration: " + service);

	}
}
