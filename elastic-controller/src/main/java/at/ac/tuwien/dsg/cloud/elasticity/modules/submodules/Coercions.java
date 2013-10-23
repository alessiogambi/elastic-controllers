package at.ac.tuwien.dsg.cloud.elasticity.modules.submodules;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

import org.apache.tapestry5.ioc.Configuration;
import org.apache.tapestry5.ioc.services.Coercion;
import org.apache.tapestry5.ioc.services.CoercionTuple;

import ch.usi.cloud.controller.common.naming.FQN;
import ch.usi.cloud.controller.common.naming.FQNException;
import ch.usi.cloud.controller.common.naming.FQNType;

/**
 * This module contributes the additional logic for Type Coercion.
 * 
 * @author alessiogambi
 * 
 */
public class Coercions {

	@SuppressWarnings("rawtypes")
	public static void contributeTypeCoercer(
			Configuration<CoercionTuple> configuration) {

		Coercion<double[], Number[]> doubleArrayToNumberArray = new Coercion<double[], Number[]>() {

			public Number[] coerce(double[] arg0) {
				if (arg0 == null) {
					return null;
				}
				if (arg0.length == 0) {
					return new Number[0];
				} else {
					Number[] result = new Number[arg0.length];
					for (int i = 0; i < arg0.length; i++) {
						// result[i] = new Double(arg0[i]);
						result[i] = arg0[i];
					}
					return result;
				}
			}
		};

		configuration.add(new CoercionTuple<double[], Number[]>(double[].class,
				Number[].class, doubleArrayToNumberArray));

		Coercion<Number[], double[]> numberArrayToDoubleArray = new Coercion<Number[], double[]>() {

			public double[] coerce(Number[] arg0) {
				if (arg0 == null) {
					return null;
				}
				if (arg0.length == 0) {
					return new double[0];
				} else {
					double[] result = new double[arg0.length];
					// Maybe there is a smarter method for that
					for (int i = 0; i < arg0.length; i++) {
						result[i] = arg0[i].doubleValue();
					}
					return result;
				}
			}
		};

		configuration.add(new CoercionTuple<Number[], double[]>(Number[].class,
				double[].class, numberArrayToDoubleArray));

		Coercion<double[], String> doubleArrayToString = new Coercion<double[], String>() {

			public String coerce(double[] arg0) {
				StringBuffer sb = new StringBuffer();
				for (int i = 0; i < arg0.length; i++) {
					sb.append(arg0[i]);
					sb.append(",");
				}
				sb.deleteCharAt(sb.lastIndexOf(","));
				return sb.toString();
			}

		};
		configuration.add(new CoercionTuple<double[], String>(double[].class,
				String.class, doubleArrayToString));

		Coercion<String, double[]> stringToDoubleArray = new Coercion<String, double[]>() {
			public double[] coerce(String arg0) {
				// Arrays.toString => [x,x,x,x]
				String _array = arg0.trim().replaceAll("\\[", "")
						.replaceAll("\\]", "");

				String[] token = _array.split(",");
				double[] result = new double[token.length];
				for (int i = 0; i < token.length; i++) {
					result[i] = Double.parseDouble(token[i]);
				}
				return result;
			}
		};

		configuration.add(new CoercionTuple<String, double[]>(String.class,
				double[].class, stringToDoubleArray));

		Coercion<String, URL> stringToURL = new Coercion<String, URL>() {
			public URL coerce(String arg0) {
				try {
					return new URL(arg0);
				} catch (MalformedURLException e) {
					throw new IllegalArgumentException(e);
				}
			}
		};

		configuration.add(new CoercionTuple<String, URL>(String.class,
				URL.class, stringToURL));

		Coercion<URL, String> uRLtoString = new Coercion<URL, String>() {
			public String coerce(URL arg0) {
				if (arg0 != null) {
					return arg0.toString();
				} else {
					return null;
				}
			}
		};

		configuration.add(new CoercionTuple<URL, String>(URL.class,
				String.class, uRLtoString));

		Coercion<UUID, String> uuidtoString = new Coercion<UUID, String>() {
			public String coerce(UUID arg0) {
				if (arg0 != null) {
					return arg0.toString();
				} else {
					return null;
				}
			}
		};

		Coercion<String, UUID> stringToUUID = new Coercion<String, UUID>() {
			public UUID coerce(String arg0) {
				if (arg0 != null) {
					return UUID.fromString(arg0);
				} else {
					return null;
				}
			}
		};

		configuration.add(new CoercionTuple<UUID, String>(UUID.class,
				String.class, uuidtoString));

		configuration.add(new CoercionTuple<String, UUID>(String.class,
				UUID.class, stringToUUID));

		Coercion<FQN, String> FQNtoString = new Coercion<FQN, String>() {
			public String coerce(FQN arg0) {
				if (arg0 != null) {
					return arg0.toString();
				} else {
					return null;
				}
			}
		};

		Coercion<String, FQN> StringToFQN = new Coercion<String, FQN>() {
			public FQN coerce(String arg0) {
				if (arg0 != null) {
					try {
						return new FQN(arg0, FQNType.SERVICE);
					} catch (FQNException e) {
						e.printStackTrace();
						throw new RuntimeException(e);
					}
				} else {
					return null;
				}
			}
		};

		configuration.add(new CoercionTuple<FQN, String>(FQN.class,
				String.class, FQNtoString));

		configuration.add(new CoercionTuple<String, FQN>(String.class,
				FQN.class, StringToFQN));

	}
}
