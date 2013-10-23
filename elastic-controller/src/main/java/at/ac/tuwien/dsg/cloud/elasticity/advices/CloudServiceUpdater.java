package at.ac.tuwien.dsg.cloud.elasticity.advices;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CloudServiceUpdater {
	/**
	 * Marker annotation to discriminate ServiceUpdater that hit the cloud, and
	 * therefore must be cached, from the others, such as the Cache itself and
	 * the application level Service updater like DoodleServiceUpdater.
	 */
}
