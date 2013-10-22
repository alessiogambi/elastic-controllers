package at.ac.tuwien.dsg.cloud.elasticity.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.junit.Test;

import at.ac.tuwien.dsg.cloud.elasticity.data.DoodleRegistry;

public class DoodleServiceUpdaterTest {

	@Test
	public void updateService() throws JAXBException, IOException {
		URL registryURL = new URL("http://128.130.172.203:8081/admin/registry");

		JAXBContext contextObj = JAXBContext.newInstance(DoodleRegistry.class);
		Unmarshaller unMarshallerObj = contextObj.createUnmarshaller();
		DoodleRegistry serviceStatus = (DoodleRegistry) unMarshallerObj
				.unmarshal(new BufferedReader(new InputStreamReader(registryURL
						.openStream())));

		System.out.println(serviceStatus.getIps());

	}
}
