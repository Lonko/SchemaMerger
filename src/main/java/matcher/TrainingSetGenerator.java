package matcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import connectors.dao.AlignmentDao;
import me.tongfei.progressbar.ProgressBar;
import model.AbstractProductPage.Specifications;
import model.Source;
import model.SourceProductPage;
import models.matcher.BagsOfWordsManager;
import models.matcher.Features;
import models.matcher.Tuple;

/**
 * Generator of training sets, ie sets of pairs of attributes, with features
 * computed AND match/mismatch
 * 
 * @author marco
 * @see Features
 *
 */
public class TrainingSetGenerator {

	private AlignmentDao dao;
	private FeatureExtractor fe;
	private Map<String, List<String>> clonedSources;

	public TrainingSetGenerator(AlignmentDao dao, FeatureExtractor fe, Map<String, List<String>> clSources) {
		this.dao = dao;
		this.fe = fe;
		this.clonedSources = clSources;
	}

	/**
	 * @param sampleSize number of original pages, from which we will look for linked pages and compare them 
	 * @param setSize expected number of examples ({@link Tuple}), i.e. pairs of neg and pos atts
	 * @param useWebsite
	 * @param addTuples add the information on the tuple to the row containing the features
	 * @param ratio
	 * @param category
	 * @return
	 */
	public List<String> getTrainingSetWithTuples(int sampleSize, int setSize, boolean addTuples,
			double ratio, String category) {

		Map<String, List<Tuple>> examples = new HashMap<>();
		boolean hasEnoughExamples = false;
		int newSizeP, newSizeN, sizeP = 0, sizeN = 0, tentatives = 0;

		/*
		 *  We loop until we have a reasonable number of examples, this number can variate depending on the number of attributes and linked pages
		 *  of the randomly picked pages. 
		 *  Note that we keep anyway the biggest set of examples found until now, so that if we make too many tentatives, we eventually use this biggest
		 *  set as the good one even if it is not big enough 
		 */
		do {
			List<SourceProductPage> sample = this.dao.getSamplePagesFromCategory(sampleSize, category);
			Map<String, List<Tuple>> newExamples = getExamples(sample, ratio);
			newSizeP = newExamples.get("positives").size();
			newSizeN = newExamples.get("negatives").size();
			System.out.println(newSizeP + " + " + newSizeN + " = " + (newSizeP + newSizeN));
			hasEnoughExamples = ((setSize * 0.95) <= (newSizeP + newSizeN))
					&& ((newSizeP + newSizeN) <= (setSize * 1.05));
			tentatives++;
			if ((sizeP + sizeN) < (newSizeP + newSizeN)) {
				examples = newExamples;
				sizeP = newSizeP;
				sizeN = newSizeN;
			}
		} while (!hasEnoughExamples && tentatives < 10); // there must be enough examples, however don't loop too many
															// times

		// if not enough examples were found, return an empty list
		if (examples.size() == 0) {
			System.err.println("NON ABBASTANZA ESEMPI");
			return new ArrayList<String>();
		}

		System.out.println(examples.get("positives").size() + " esempi positivi\t" + examples.get("negatives").size()+" esempi negativi");

		List<Features> trainingSet = computeFeaturesOnTrainingSet(examples.get("positives"), examples.get("negatives"), category);

		// Training set is computed. Now, adapt it to the format required (a ~csv used by R).
		List<String> tsRows = new ArrayList<>();

		for (int i = 0; i < trainingSet.size(); i++) {
			String row = "";
			if (addTuples) {
				Tuple t;
				if (i < sizeP)
					t = examples.get("positives").get(i);
				else
					t = examples.get("negatives").get(i % sizeP);
				row = t.toRowString();
			}
			Features f = trainingSet.get(i);
			row += f.toString() + "," + f.getMatch();
			tsRows.add(row);
		}

		return tsRows;
	}

	/**
	 * From example pages, find all pages in linkage, then generate pairs of
	 * attributes for training set. Tries to respect pos-neg proportion (ratio) : if
	 * it is not respected, try again (max 10 tentatives)
	 * 
	 * @param sample
	 * @param ratio
	 *            pos-neg proportion
	 * @return
	 */
	private Map<String, List<Tuple>> getExamples(List<SourceProductPage> sample, double ratio) {
		List<Tuple> posExamples = new ArrayList<>();
		List<Tuple> negExamples = new ArrayList<>();

		for (SourceProductPage doc1 : sample) {
			for (String url : doc1.getLinkage()) {
				SourceProductPage doc2 = this.dao.getPageFromUrlIfExistsInDataset(url);

				if (doc2 != null) {

					// check if the two pages belong to cloned sources
					Source source1 = doc1.getSource();
					Source source2 = doc2.getSource();
					if ((!this.clonedSources.containsKey(source1.toString())
							|| !this.clonedSources.get(source1.toString()).contains(source2.toString()))
							&& !source1.getWebsite().equals(source2.getWebsite())) {
						Set<String> schemaIntersection = new HashSet<>(doc1.getSpecifications().keySet());
						schemaIntersection.retainAll(doc2.getSpecifications().keySet());

						// generates positive examples
						List<Tuple> allTmpPosEx = new ArrayList<>();
						schemaIntersection.stream().forEach(a -> {
							Tuple t = new Tuple(a, a, source1.getWebsite(), source2.getWebsite(),
									source1.getCategory());
							allTmpPosEx.add(t);
						});
						Collections.shuffle(allTmpPosEx);
						// get max 10 examples from the same couple (to avoid biases from very similar pages with a lot of attributes)
						List<Tuple> tmpPosEx = allTmpPosEx.subList(0, Math.min(10, allTmpPosEx.size()));
						posExamples.addAll(tmpPosEx);

						// generates negative examples
						for (int i = 0; i < tmpPosEx.size() - 1; i++) {
							for (int j = i + 1; j < tmpPosEx.size(); j++) {
								Tuple t1 = tmpPosEx.get(i);
								Tuple t2 = tmpPosEx.get(j);
								negExamples.add(t1.getMixedTuple(t2));
							}
						}
					}
				}
			}
		}

		posExamples = posExamples.stream().distinct().collect(Collectors.toList());
		negExamples = negExamples.stream().distinct().collect(Collectors.toList());
		Collections.shuffle(posExamples);
		Collections.shuffle(negExamples);
		//'ratio' is the ratio of positive examples on total examples (p + n = Total; p = ratio * Total)
		//We want now the ratio between pos and neg --> p = r / (1-r) n
		double ratioPosNeg = ratio / (1-ratio);
		
		int posSize = Math.min(posExamples.size(), (int) (negExamples.size() * ratioPosNeg));
		int negSize = (int) (posSize / ratioPosNeg);
		System.out.println(
				"posExamples size = " + posExamples.size() + " --- posSize = " + posSize + " --- negSize = " + negSize);
		if (posExamples.size() > posSize)
			posExamples = posExamples.subList(0, posSize);
		if (negExamples.size() > negSize)
			negExamples = negExamples.subList(0, negSize);

		Map<String, List<Tuple>> allExamples = new HashMap<>();
		allExamples.put("positives", posExamples);
		allExamples.put("negatives", negExamples);

		return allExamples;
	}

	public List<Features> computeFeaturesOnTrainingSet(List<Tuple> pExamples, List<Tuple> nExamples, String category) {
		List<Features> examples = new ArrayList<>();
		System.out.println("Positive examples");
		examples.addAll(getAllFeatures(pExamples, category, 1));
		System.out.println("Negative examples");
		examples.addAll(getAllFeatures(nExamples, category, 0));

		return examples;
	}

	/**
	 * Retrieve all features for tuples
	 * <p>
	 * For a given tuple, needs to retrieve pairs OF PROVIDED CATEGORY, of those 2
	 * types:
	 * <ul>
	 * <li>[w1, contains(a1)] --[linked with]--> [w2, contains(a2)]
	 * <li>[not(w1), contains(a1)] --[linked with]--> [w2, contains(a2)]
	 * </ul>
	 * Here we try to do it efficiently, limiting the number of call to mongo
	 * 
	 * @param tuples
	 * @param candidateType
	 * @return
	 */
	private List<Features> getAllFeatures(List<Tuple> tuples, String category, double candidateType) {
		List<Features> features = new ArrayList<>();
		Map<String, Map<String, Map<String, List<Tuple>>>> a1_s2_a2_tuple = tuples.stream()
				.collect(Collectors.groupingBy(Tuple::getAttribute1,
						Collectors.groupingBy(Tuple::getWebsite2, Collectors.groupingBy(Tuple::getAttribute2))));

		// TODO ProgressBar does not work currently (says cannot create system terminal), TODO investigate and fix 
		int counter = 0;
		for (Entry<String, Map<String, Map<String, List<Tuple>>>> a1_s2_a2_tuple_entry : a1_s2_a2_tuple.entrySet()) {
			String attribute1 = a1_s2_a2_tuple_entry.getKey();
			Map<String, Map<String, List<Tuple>>> s2_a2_tuple = a1_s2_a2_tuple_entry.getValue();
			for (Entry<String, Map<String, List<Tuple>>> s2_a2_tuple_entry : s2_a2_tuple.entrySet()) {
				String source2 = s2_a2_tuple_entry.getKey();
				List<SourceProductPage> pagesFromAllSourcesInLinkageS2 = this.dao.getPagesOutsideCatalogInLinkageWithPagesInside(category, "", source2,
						attribute1);
				Map<String, List<SourceProductPage>> w1_pagesLinkageS2 = pagesFromAllSourcesInLinkageS2.stream()
						.collect(Collectors.groupingBy(prodPage -> prodPage.getSource().getWebsite()));
				for (Entry<String, List<Tuple>> a2_tuple : s2_a2_tuple_entry.getValue().entrySet()) {
					getFeatures(candidateType, features, null, attribute1,
							source2, pagesFromAllSourcesInLinkageS2, w1_pagesLinkageS2, a2_tuple.getKey(), a2_tuple.getValue());
				}
			}
			System.out.println("Done " + ++counter / (double) a1_s2_a2_tuple.size() * 100 +"%");
		}
		return features;
	}

	private void getFeatures(double candidateType, List<Features> features, ProgressBar pb,
			String attribute1, String website2, List<SourceProductPage> pagesInLinkageS2,
			Map<String, List<SourceProductPage>> w1_pagesLinkageS2, String attribute2, List<Tuple> tuplesFromS2withA1A2) {
		// Here we deal with tuple for a specific a1, a2 and w2, and all possible W1s.
		List<Entry<Specifications, SourceProductPage>> cList2 = this.dao.getPairsOfPagesInLinkage(pagesInLinkageS2, website2,
				attribute2);
		List<String> websites1 = tuplesFromS2withA1A2.stream().map(t -> t.getWebsite1()).distinct()
				.collect(Collectors.toList());
		for (String website1 : websites1) {
			// pb.step();
			Features feature = new Features();
			List<SourceProductPage> subProds_of_w1 = w1_pagesLinkageS2.getOrDefault(website1, new ArrayList<>());
			List<Entry<Specifications, SourceProductPage>> sList2 = this.dao.getPairsOfPagesInLinkage(subProds_of_w1, website2,
					attribute2);
			
			feature = computeFeatures(sList2, new ArrayList<Entry<Specifications, SourceProductPage>>(), cList2,
					attribute1, attribute2, candidateType);
			features.add(feature);
		}
	}

	/**
	 * 
	 * @param sList coppia di pagine in linkage appartenenti alle sorgenti della tupla
	 * @param wList TODO eliminare --> stesso website diversa categoria
	 * @param cList coppia di pagine in linkage mantenendo come s2 la sorgente della tupla, e cercando tutte le possibili s1 nella categoria
	 * @param a1
	 * @param a2
	 * @param type
	 * @return
	 */
	private Features computeFeatures(List<Entry<Specifications, SourceProductPage>> sList,
			List<Entry<Specifications, SourceProductPage>> wList,
			List<Entry<Specifications, SourceProductPage>> cList, String a1, String a2, double type) {

		Features features = new Features();
		BagsOfWordsManager sBags = new BagsOfWordsManager(a1, a2, sList);
		BagsOfWordsManager cBags = new BagsOfWordsManager(a1, a2, cList);

		features.setSourceJSD(this.fe.getJSD(sBags));
		features.setCategoryJSD(this.fe.getJSD(cBags));
		features.setSourceJC(this.fe.getJC(sBags));
		features.setCategoryJC(this.fe.getJC(cBags));
		features.setSourceMI(this.fe.getMI(sList, a1, a2));
		features.setCategoryMI(this.fe.getMI(cList, a1, a2));

		features.setMatch(type);
		if (features.hasNan())
			throw new ArithmeticException("feature value is NaN");

		return features;
	}

}
