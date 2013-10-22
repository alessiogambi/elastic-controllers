package at.ac.tuwien.dsg.cloud.elasticity.services.impl.waiting;

import org.slf4j.Logger;

import at.ac.tuwien.dsg.cloud.elasticity.services.WaitService;

public class PeriodicWait implements WaitService {

	private Logger logger;
	private long periodMillis;

	/**
	 * This class wait for the given amount of time once invoked
	 * 
	 * @param logger
	 * @param period
	 */
	public PeriodicWait(Logger logger, Long periodMillis) {
		this.logger = logger;
		this.periodMillis = periodMillis;
		
		this.logger.info("SUMMARY: Period (millis): " + this.periodMillis );
	}

	@Override
	public void waitMe() {
		try {
			this.logger.debug("Going to sleep");
			Thread.sleep(periodMillis);
			this.logger.debug("Waiking up");
		} catch (InterruptedException e) {
			logger.info("waitMe interrupted");
		}
	}
}
