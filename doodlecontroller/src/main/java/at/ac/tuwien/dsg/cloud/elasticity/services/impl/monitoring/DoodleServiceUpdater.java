package at.ac.tuwien.dsg.cloud.elasticity.services.impl.monitoring;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.slf4j.Logger;

import at.ac.tuwien.dsg.cloud.data.DynamicServiceDescription;
import at.ac.tuwien.dsg.cloud.data.InstanceDescription;
import at.ac.tuwien.dsg.cloud.elasticity.data.DoodleRegistry;
import at.ac.tuwien.dsg.cloud.elasticity.services.ServiceUpdater;

public class DoodleServiceUpdater implements ServiceUpdater {

	private Logger logger;

	public DoodleServiceUpdater(Logger logger) {
		this.logger = logger;
	}

	/*
	 * YOU MUST SPECIFY THE FUCKING connection.addRequestProperty("Accept",
	 * "text/xml"); DAMN RESTLET
	 * 
	 * @param url
	 * 
	 * @return
	 * 
	 * @throws Exception
	 */
	private String getText(String url) throws Exception {
		URL website = new URL(url);

		URLConnection connection = website.openConnection();
		connection.addRequestProperty("Accept", "text/xml");
		BufferedReader in = new BufferedReader(new InputStreamReader(
				connection.getInputStream()));

		StringBuilder response = new StringBuilder();
		String inputLine;

		while ((inputLine = in.readLine()) != null)
			response.append(inputLine);

		in.close();

		return response.toString();
	}

	public void update(DynamicServiceDescription service) {

		String registryIP = null;
		try {
			registryIP = service.getVeeInstances("frontend").get(0)
					.getPublicIp().getHostAddress();
		} catch (Exception e) {
			e.printStackTrace();

			try {
				registryIP = service.getVeeInstances("frontend").get(0)
						.getPrivateIp().getHostAddress();
			} catch (Exception ee) {
				ee.printStackTrace();
				return;
			}
		}

		// NOTE THAT THIS IS HARDCODED !
		String _registryURL = String.format("http://%s:8081/admin/registry",
				registryIP);
		DoodleRegistry serviceStatus = null;
		try {
			JAXBContext contextObj = JAXBContext
					.newInstance(DoodleRegistry.class);
			Unmarshaller unMarshallerObj = contextObj.createUnmarshaller();

			String reg = getText(_registryURL);

			logger.debug("XML Configuration from registry: " + reg);
			StringReader sr = new StringReader(reg); // wrap your String
			BufferedReader br = new BufferedReader(sr); // wrap your
														// StringReader
			serviceStatus = (DoodleRegistry) unMarshallerObj.unmarshal(br);
		} catch (JAXBException e) {
			e.printStackTrace();
			return;
		} catch (IOException e) {
			e.printStackTrace();
			return;
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		List<String> registeredIPs = new ArrayList<String>();
		for (String ipport : serviceStatus.getIps()) {
			registeredIPs.add(ipport.split(":")[0]);
		}

		logger.debug("Found the following ips in the lb " + registeredIPs);

		for (InstanceDescription instance : service
				.getVeeInstances("appserver")) {
			String instanceIp = instance.getPrivateIp().getHostAddress();

			if (registeredIPs.contains(instanceIp)) {
				instance.setState("REGISTERED");
				registeredIPs.remove(instanceIp);
				logger.info("Instance " + instance.getInstanceId()
						+ " REGISTERED ");
			} else {
				// Not yet registered, leave its status as it is
				logger.info("Instance " + instance.getInstanceId()
						+ " NOT REGISTERED (" + instance.getState() + ") ");
			}
		}

		// TERMINATE MACHINES DO NOT DE-REGISTERED
		if (registeredIPs.size() > 0) {
			logger.error("The following instances are TERMINATED but still REGISTERED: "
					+ registeredIPs);

		}

	}
}
