package matcher;

import java.util.List;
import java.util.Map.Entry;

import model.SourceProductPage;
import model.AbstractProductPage.Specifications;
import models.matcher.BagsOfWordsManager;
import models.matcher.Features;

/**
 * Build an object {@link Features} provided attributes source comparison and category comparison 
 * @author federico
 *
 */
public class FeaturesBuilder {
	
	/**
	 * Compute features for classification
	 * @param sList
	 * @param cList
	 * @param a1
	 * @param a2
	 * @param useMI
	 * @return
	 */
	public Features computeFeatures(List<Entry<Specifications, SourceProductPage>> sList,
			List<Entry<Specifications, SourceProductPage>> cList, String a1, String a2, boolean useMI) {

		Features features = new Features();
		BagsOfWordsManager sBags = new BagsOfWordsManager(a1, a2, sList);
		BagsOfWordsManager cBags = new BagsOfWordsManager(a1, a2, cList);

		features.setSourceJSD(FeatureExtractor.getJSD(sBags));
		features.setCategoryJSD(FeatureExtractor.getJSD(cBags));
		features.setSourceJC(FeatureExtractor.getJC(sBags));
		features.setCategoryJC(FeatureExtractor.getJC(cBags));
		if (useMI) {
			features.setSourceMI(FeatureExtractor.getMI(sList, a1, a2));
			features.setCategoryMI(FeatureExtractor.getMI(cList, a1, a2));
		}
		if (features.hasNan())
			throw new ArithmeticException("feature value is NaN");

		return features;
	}
	
	/**
	 * Compute features for training
	 * @param sList coppia di pagine in linkage appartenenti alle sorgenti della tupla
	 * @param cList coppia di pagine in linkage mantenendo come s2 la sorgente della tupla, e cercando tutte le possibili s1 nella categoria
	 * @param a1
	 * @param a2
	 * @param type
	 * @return
	 */
	public Features computeFeatures(List<Entry<Specifications, SourceProductPage>> sList,
			List<Entry<Specifications, SourceProductPage>> cList, String a1, String a2, double type) {

		Features features = computeFeatures(sList, cList, a1, a2, true);
		features.setMatch(type);
		return features;
	}

}
