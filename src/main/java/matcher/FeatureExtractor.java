package matcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import model.AbstractProductPage.Specifications;
import model.SourceProductPage;
import models.matcher.BagsOfWordsManager;

/**
 * Computes features on pair of attributes
 * @author federico
 *
 */
public class FeatureExtractor {

	/**
	 * Jensen-Shannon divergence (uses token distribution from each attribute of the pair)
	 * @param bags
	 * @return
	 */
	public static double getJSD(BagsOfWordsManager bags) {
		Map<String, Double> distProbCatalog = new HashMap<>();
		Map<String, Double> distProbSource = new HashMap<>();
		Map<String, Double> distProbM = new HashMap<>();
		Set<String> allWords = new HashSet<>(bags.getCatalogBagOfWords());
		allWords.addAll(bags.getSourceBagOfWords());
		// int lM = allWords.size();

		// get frequency of all words
		bags.getCatalogBagOfWords().forEach(word -> {
			Double pCat = distProbCatalog.getOrDefault(word, 0.0);
			distProbCatalog.put(word, pCat + 1);
		});
		bags.getSourceBagOfWords().forEach(word -> {
			Double pSource = distProbSource.getOrDefault(word, 0.0);
			distProbSource.put(word, pSource + 1);
		});
		// calculate probability distribuition
		allWords.forEach(word -> {
			Double pCat = distProbCatalog.getOrDefault(word, 0.0) / distProbCatalog.size();
			Double pSource = distProbSource.getOrDefault(word, 0.0) / distProbSource.size();
			Double pM = (pCat + pSource) / 2;
			distProbCatalog.put(word, pCat);
			distProbSource.put(word, pSource);
			distProbM.put(word, pM);
		});

		return (getKL(distProbCatalog, distProbM) + getKL(distProbSource, distProbM)) / 2;
	}

	// Kullback-Leibler divergence
	public static double getKL(Map<String, Double> pCatalog, Map<String, Double> pSource) {
		double kl = 0.0;
		Set<String> words = pCatalog.keySet();

		for (String word : words) {
			if (pCatalog.get(word) == 0.0)
				continue;
			if (pSource.get(word) == 0.0) // p2[i] should never be 0
				continue;

			kl += pCatalog.get(word) * Math.log(pCatalog.get(word) / pSource.get(word));
		}

		return kl;
	}

	// Jaccard coefficient
	public static double getJC(BagsOfWordsManager bags) {
		Set<String> intersection = new HashSet<>(bags.getCatalogBagOfWords());
		Set<String> union = new HashSet<>(bags.getCatalogBagOfWords());

		intersection.retainAll(bags.getSourceBagOfWords());
		union.addAll(bags.getSourceBagOfWords());

		return intersection.size() / (double) union.size();
	}

	// Mutual Information
	public static double getMI(List<Entry<Specifications, SourceProductPage>> prods, String a1, String a2) {
		if (prods.size() == 0) {
			throw new IllegalArgumentException("Cannot compute MI on empty pair of documents"); 
		}
		
		double mi = 0.0;

		List<List<String>> valueSets = getDistinctValues(prods, a1, a2);
		double[][] matrix = getJointProbDistr(prods, a1, a2, valueSets.get(0), valueSets.get(1));

		double[] margProb1, margProb2;
		margProb1 = getMarginalProbabilityDistribution(matrix, true);
		margProb2 = getMarginalProbabilityDistribution(matrix, false);

		for (int i = 0; i < margProb1.length; i++)
			for (int j = 0; j < margProb2.length; j++) {
				if (matrix[i][j] == 0)
					continue;
				double logArg = matrix[i][j] / (margProb1[i] * margProb2[j]);
				mi += matrix[i][j] * (Math.log(logArg) / Math.log(2));
			}

		return mi;
	}

	private static double[] getMarginalProbabilityDistribution(double[][] jointPD, boolean byRows) {
		int dim1 = byRows ? jointPD.length : jointPD[0].length;
		int dim2 = byRows ? jointPD[0].length : jointPD.length;
		double[] margProb = new double[dim1];

		/*
		 * iterates rows -> columns if byRows == true else columns -> rows
		 */
		for (int i = 0; i < dim1; i++) {
			double acc = 0.0;
			for (int j = 0; j < dim2; j++)
				if (byRows)
					acc += jointPD[i][j];
				else
					acc += jointPD[j][i];
			margProb[i] = acc;
		}

		return margProb;
	}

	private static double[][] getJointProbDistr(List<Entry<Specifications, SourceProductPage>> prods, String a1, String a2,
			List<String> distinctValues1, List<String> distinctValues2) {

		int n = 0;
		double[][] matrix = new double[distinctValues1.size()][distinctValues2.size()];
		for (Entry<Specifications, SourceProductPage> couple : prods) {
			String value1 = couple.getKey().get(a1);
			String value2 = couple.getValue().getSpecifications().get(a2);
			if (value1 == null || value2 == null) // shouldn't happen anyway
				continue;
			String[] values = value1.split("###", -1);
			for (String v : values) {
				int index1 = distinctValues1.indexOf(v);
				int index2 = distinctValues2.indexOf(value2);
				matrix[index1][index2]++;
				n++;
			}
		}

		for (int i = 0; i < distinctValues1.size(); i++)
			for (int j = 0; j < distinctValues2.size(); j++)
				matrix[i][j] /= n;

		return matrix;
	}

	private static List<List<String>> getDistinctValues(List<Entry<Specifications, SourceProductPage>> prods, String a1,
			String a2) {
		List<List<String>> distValues = new ArrayList<>();
		Set<String> values1 = new HashSet<>();
		Set<String> values2 = new HashSet<>();

		for (Entry<Specifications, SourceProductPage> couple : prods) {
			// The parameter -1 avoids having empty arrays if content is '###' (it can happen)
			String[] value1 = couple.getKey().get(a1).split("###", -1);
			String[] value2 = couple.getValue().getSpecifications().get(a2).split("###", -1);
			// if(a1.equals("Ethernet:") && a2.equals("Wi-Fi:"))
			// System.out.println(value1+"\t"+value2);
			values1.addAll(Arrays.asList(value1));
			values2.addAll(Arrays.asList(value2));
		}

		distValues.add(new ArrayList<>(values1));
		distValues.add(new ArrayList<>(values2));

		return distValues;
	}
}
