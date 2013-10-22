package at.ac.tuwien.dsg.cloud.elasticity.data;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "service")
@XmlAccessorType(XmlAccessType.FIELD)
public class DoodleRegistry {

	@XmlElement(name = "ip")
	private List<String> ips = new ArrayList<String>();

	public DoodleRegistry() {
	}

	public void setIps(List<String> ips) {
		this.ips = new ArrayList<String>();
		this.ips.addAll(ips);
	}

	public List<String> getIps() {
		return ips;
	}
}
