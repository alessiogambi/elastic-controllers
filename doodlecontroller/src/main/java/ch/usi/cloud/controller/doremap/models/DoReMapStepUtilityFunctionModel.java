package ch.usi.cloud.controller.doremap.models;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import cern.colt.Arrays;
import ch.usi.cloud.controller.Model;
import ch.usi.cloud.controller.ModelDescriptor;
import ch.usi.cloud.controller.UtilityFunctionModel;
import ch.usi.cloud.controller.common.ConfigurationFeature;
import ch.usi.cloud.controller.common.ManifestConnector;

public class DoReMapStepUtilityFunctionModel extends UtilityFunctionModel {

	Model voteRT;
	Model createRT;
	double voteSLO;
	double createSLO;
	double costJO = 1000;
	double costAS = 500;
	double a = 15000;
	double b = 10000;
	int indexAS;
	int indexJO;
	Logger log = Logger.getLogger(ch.usi.cloud.controller.doremap.models.DoReMapStepUtilityFunctionModel.class);

	public DoReMapStepUtilityFunctionModel(ManifestConnector mc,
			String serviceFQN, List<Model> models) {
		// remove models for get and delete, we don't need them
		// keep
		Iterator<Model> i = models.iterator();
		while (i.hasNext()) {
			Model model = i.next();
			if (model.descriptor.outputFeature.name
					.equals("getpollTrainingAvgRT")
					|| model.descriptor.outputFeature.name
							.equals("deletepollTrainingAvgRT")) {
				i.remove();
			} else if (model.descriptor.outputFeature.name
					.equals("votepollTrainingAvgRT")) {
				// save reference to model and SLO for vote
				voteRT = model;
				voteSLO = model.descriptor.outputFeature.max;
				// use same descriptor features
				// this preserves the order of input features in arrays
				descriptor = new ModelDescriptor(serviceFQN);
				descriptor.inputFeatures = new ArrayList<ConfigurationFeature>();
				descriptor.inputFeatures.addAll(model.descriptor.inputFeatures);
				// We have only one feature, the value of the utility function
				ConfigurationFeature cf = new ConfigurationFeature(
						"Step utility",
						"a * heaviside(voteSLO - voteRT) + b * heaviside(createSLO - createRT) - AS * costAS - JO * costJO ");
				descriptor.outputFeature = cf;

			} else if (model.descriptor.outputFeature.name
					.equals("createpollTrainingAvgRT")) {
				createRT = model;
				createSLO = model.descriptor.outputFeature.max;
			}
		}
		// finally set the index for AS and JO so we can easily find them in
		// input arrays
		Iterator<ConfigurationFeature> j = descriptor.inputFeatures.iterator();
		while (j.hasNext()) {
			ConfigurationFeature cf = j.next();
			if (cf.name.contains("jopera")) {
				indexJO = descriptor.inputFeatures.indexOf(cf);
			} else if (cf.name.contains("doodleas")) {
				indexAS = descriptor.inputFeatures.indexOf(cf);
			}
		}
	}

	@Override
	public double computeUtility(double[] input) throws Exception {
		double vote = 0;
		double create = 0;
		if (voteSLO > Math.abs(voteRT.getValue(input))) {
			vote = a;
		}
		if (createSLO > Math.abs(createRT.getValue(input))) {
			create = b;
		}
		return vote + create - (input[indexAS] * costAS)
				- (input[indexJO] * costJO);
	}

	@Override
	public double[] findConf(double[][] candidateConfs, boolean findMax) throws Exception {
		log.debug("findConf: invoking getValues()");		
		double[] res = getValues(candidateConfs);

		int maxIndex = 0;
		for (int i = 1; i < res.length; i++) {
			if ((res[i] > res[maxIndex] && findMax)
					|| (res[i] < res[maxIndex] && !findMax)) {
				maxIndex = i;
			}
		}
		log.debug("findConf: found max = " + res[maxIndex] + " for conf " + Arrays.toString(candidateConfs[maxIndex]));
		return candidateConfs[maxIndex];

	}

	@Override
	public double[] findConfigurationsGivenThreshold(double[][] candidateConfs,
			double threshold, boolean below) throws Exception {
		throw new RuntimeException("Uninmplemented method");
	}

}
