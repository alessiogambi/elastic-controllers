package at.ac.tuwien.dsg.cloud.elasticity.utils;


import static org.apache.tapestry5.ioc.IOCConstants.MODULE_BUILDER_MANIFEST_ENTRY_NAME;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.jar.Manifest;

import org.apache.tapestry5.ioc.LoggerSource;
import org.apache.tapestry5.ioc.Registry;
import org.apache.tapestry5.ioc.RegistryBuilder;
import org.apache.tapestry5.ioc.annotations.SubModule;
import org.apache.tapestry5.ioc.internal.LoggerSourceImpl;
import org.apache.tapestry5.ioc.internal.util.InternalUtils;
import org.slf4j.Logger;

public final class ExtendedIOCUtilities {

	private static Logger logger;

	private static ExtendedIOCUtilities INSTACE;

	/**
	 * This class extend the Plain IOCUtilites with additional methods that can
	 * be useful in a context where we need more control over modules inclusions
	 * and exclusion. Note at the moment we cannot deal with modules annotated
	 * with @Submodule.
	 * 
	 * For the moment we copy and paste all its content, as the original class
	 * is marked as final
	 * 
	 * I will pull requests to be integrated in the master branch of the project
	 */

	/**
	 * Construct a default Registry, including modules identifed via the
	 * Tapestry-Module-Classes Manifest entry. The registry will have been
	 * {@linkplain Registry#performRegistryStartup() started up} before it is
	 * returned.
	 * 
	 * @return constructed Registry, after startup
	 * @see #addDefaultModules(RegistryBuilder)
	 */
	private ExtendedIOCUtilities() {
		LoggerSource loggerSource = new LoggerSourceImpl();
		logger = loggerSource.getLogger(ExtendedIOCUtilities.class);
	}

	public static Registry buildDefaultRegistry() {
		RegistryBuilder builder = new RegistryBuilder();

		addDefaultModules(builder);

		Registry registry = builder.build();

		registry.performRegistryStartup();

		return registry;
	}

	/**
	 * Scans the classpath for JAR Manifests that contain the
	 * Tapestry-Module-Classes attribute and adds each corresponding class to
	 * the RegistryBuilder. In addition, looks for a system property named
	 * "tapestry.modules" and adds all of those modules as well. The
	 * tapestry.modules approach is intended for development.
	 * 
	 * @param builder
	 *            the builder to which modules will be added
	 * @see SubModule
	 * @see RegistryBuilder#add(String)
	 */
	public static void addDefaultModules(RegistryBuilder builder) {
		Collection<Class> emptyExclusionFilter = new ArrayList<Class>();
		addDefaultModulesWithExclusion(builder, emptyExclusionFilter);
	}

	/**
	 * This works like the plain addDefault modules but exclude the modules
	 * matched by the filter. At the moment the exclusion filter is simply a
	 * Collection<Class> to be refactory safe.
	 * 
	 * @param builder
	 */
	public static void addDefaultModulesWithExclusion(RegistryBuilder builder,
			Collection<Class> exclusionFilter) {

		if (INSTACE == null) {
			INSTACE = new ExtendedIOCUtilities();
		}

		try {
			Enumeration<URL> urls = builder.getClassLoader().getResources(
					"META-INF/MANIFEST.MF");

			while (urls.hasMoreElements()) {
				URL url = urls.nextElement();

				addModulesInManifest(builder, url, exclusionFilter);
			}

			addModulesInList(builder, System.getProperty("tapestry.modules"),
					exclusionFilter);

		} catch (IOException ex) {
			throw new RuntimeException(ex.getMessage(), ex);
		}
	}

	private static void addModulesInManifest(RegistryBuilder builder, URL url,
			Collection<Class> exclusionFilter) {
		InputStream in = null;

		Throwable fail = null;

		try {
			in = url.openStream();

			Manifest mf = new Manifest(in);

			in.close();

			in = null;

			String list = mf.getMainAttributes().getValue(
					MODULE_BUILDER_MANIFEST_ENTRY_NAME);

			addModulesInList(builder, list, exclusionFilter);
		} catch (RuntimeException ex) {
			fail = ex;
		} catch (IOException ex) {
			fail = ex;
		} finally {
			close(in);
		}

		if (fail != null)
			throw new RuntimeException(String.format(
					"Exception loading module(s) from manifest %s: %s",
					url.toString(), InternalUtils.toMessage(fail)), fail);

	}

	static void addModulesInList(RegistryBuilder builder, String list,
			Collection<Class> exclusionFilter) {
		if (list == null)
			return;

		String[] classnames = list.split(",");

		Collection<String> eFilter = new ArrayList<String>();
		for (Class c : exclusionFilter) {
			eFilter.add(c.getName());
		}
		// Here is where we apply the filters. Note that we allow to disable
		// core tapestry modules

		// System.out.println("ExtendedIOCUtilities.addModulesInList() modules "
		// + Arrays.toString(classnames));
		// System.out.println("ExtendedIOCUtilities.addModulesInList() filter "
		// + eFilter);
		for (String classname : classnames) {

			if (eFilter.contains(classname)) {
				logger.info(String.format("%s was filtered !", classname));
				continue;
			} else {
				builder.add(classname.trim());
			}
		}
	}

	/**
	 * Closes an input stream (or other Closeable), ignoring any exception.
	 * 
	 * @param closeable
	 *            the thing to close, or null to close nothing
	 */
	private static void close(Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (IOException ex) {
				// Ignore.
			}
		}
	}

}
