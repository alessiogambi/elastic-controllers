package at.ac.tuwien.dsg.cloud.elasticity.services;

import java.util.concurrent.TimeUnit;

public class TimeConversionTest {

	public static void main(String[] args) {
		TimeUnit tu = TimeUnit.MILLISECONDS;
		
		System.out.println("TimeConversionTest.main()"
				+ tu.convert(100, TimeUnit.MILLISECONDS));
		System.out.println("TimeConversionTest.main()"
				+ tu.convert(1, TimeUnit.HOURS));
		System.out.println("TimeConversionTest.main()"
				+ tu.convert(1, TimeUnit.SECONDS));
	}
}
