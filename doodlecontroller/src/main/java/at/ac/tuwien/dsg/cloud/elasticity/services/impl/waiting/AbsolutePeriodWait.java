package at.ac.tuwien.dsg.cloud.elasticity.services.impl.waiting;

import java.util.Date;

import org.slf4j.Logger;

import at.ac.tuwien.dsg.cloud.elasticity.services.WaitService;

public class AbsolutePeriodWait implements WaitService {

	private Logger logger;
	private long waitPeriodMillis;
	private long nextInvocation;

	/**
	 * This class unlock the waitMe only if the waitPeriod at precise schedule,
	 * starting from the FIRST call If you miss one or more periods, the service
	 * will block the minimum amount of time to trigger the next one
	 * 
	 * @param logger
	 * @param period
	 */
	public AbsolutePeriodWait(Logger logger, Long periodMillis) {
		this.logger = logger;
		this.waitPeriodMillis = periodMillis;
		this.nextInvocation = -1;

		this.logger.info("SUMMARY: Abs. Period (millis): "
				+ this.waitPeriodMillis);

	}

	private void waitNow(long waitTime) {
		try {
			this.logger.debug("Last wait was " + nextInvocation + " -- "
					+ new Date(nextInvocation).getSeconds());
			this.logger.debug("Going to sleep for " + waitTime);
			Thread.sleep(waitTime);
			this.logger.debug("Waking up");
		} catch (InterruptedException e) {
			logger.info("waitMe interrupted");
		}
	}

	@Override
	public void waitMe() {
		logger.info("-- WAIT ME: START");
		if (nextInvocation == -1) {
			nextInvocation = System.currentTimeMillis();
			waitNow(waitPeriodMillis);
		} else {
			// Compute the delta between last and now, and wait until the NEXT
			// time
			long now = System.currentTimeMillis();
			while (now > nextInvocation) {
				nextInvocation = nextInvocation + waitPeriodMillis;
				logger.debug("Move forward");
			}
			waitNow(nextInvocation - now);
		}

		logger.info("-- WAIT ME: DONE");

	}
}
