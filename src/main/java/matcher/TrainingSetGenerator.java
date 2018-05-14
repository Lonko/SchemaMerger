package matcher;

import java.io.IOException;
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
import model.Source;
import model.SourceProductPage;
import models.matcher.BagsOfWordsManager;
import models.matcher.Features;
import models.matcher.Tuple;

/**
 * Generator of training sets, ie sets of pairs of attributes, with features computed AND match/mismatch
 * @author marco
 * @see Features
 *
 */
public class TrainingSetGenerator {

    private AlignmentDao dao;
    private FeatureExtractor fe;
    private Map<String, List<String>> clonedSources;

    public TrainingSetGenerator(AlignmentDao dao, FeatureExtractor fe,
            Map<String, List<String>> clSources) {
        this.dao = dao;
        this.fe = new FeatureExtractor();
        this.clonedSources = clSources;
    }

    public List<String> getTrainingSetWithTuples(int sampleSize, int setSize, boolean useWebsite,
            boolean addTuples, double ratio, String category) {

        Map<String, List<Tuple>> examples = new HashMap<>();
        boolean hasEnoughExamples = false;
        int newSizeP, newSizeN, sizeP = 0, sizeN = 0, tentatives = 0;

        do {
            List<SourceProductPage> sample = this.dao.getRLSample(sampleSize, category);
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
        } while (!hasEnoughExamples && tentatives < 10); // there must be enough examples, however don't loop too many times

        // if not enough examples were found, return an empty list
        if (examples.size() == 0) {
            System.err.println("NON ABBASTANZA ESEMPI");
            return new ArrayList<String>();
        }

        System.out.println(examples.get("positives").size()
                + " coppie di prodotti prese in considerazione per il training set");
        System.out.println(examples.get("positives").size() + "\t" + examples.get("negatives").size());

        List<Features> trainingSet = getTrainingSet(examples.get("positives"), examples.get("negatives"),
                useWebsite);
        
        //Training set is computed. Now, adapt it to the format required.
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
     * From example pages, find all pages in linkage, then generate pairs of attributes for training set.
     * Tries to respect pos-neg proportion (ratio) : if it is not respected, try again (max 10 tentatives)
     * 
     * @param sample
     * @param ratio pos-neg proportion
     * @return
     */
    private Map<String, List<Tuple>> getExamples(List<SourceProductPage> sample, double ratio) {
        List<Tuple> posExamples = new ArrayList<>();
        List<Tuple> negExamples = new ArrayList<>();

        for (SourceProductPage doc1 : sample) {
        	Set<String> schema1 = doc1.getSpecifications().keySet();
            for (String url : doc1.getLinkage()) {
                SourceProductPage doc2 = this.dao.getIfValid(url);

                if (doc2 != null) {

                    // check if the two pages belong to cloned sources
                    Source source1 = doc1.getSource();
                    Source source2 = doc2.getSource();
					if ((!this.clonedSources.containsKey(source1.toString())
                            || !this.clonedSources.get(source1.toString()).contains(source2.toString()))
                    		&& !source1.getWebsite().equals(source2.getWebsite())) {
						Set<String> aSet1 = new HashSet<>(schema1);
                        aSet1.retainAll(doc2.getSpecifications().keySet());

                        // generates positive examples
                        List<Tuple> allTmpPosEx = new ArrayList<>();
                        aSet1.stream().forEach(a -> {
                            Tuple t = new Tuple(a, a, doc1.getSource().getWebsite(), doc2.getSource().getWebsite(), doc1.getSource().getCategory());
                            allTmpPosEx.add(t);
                        });
                        Collections.shuffle(allTmpPosEx);
                        int endIndex = (11 < allTmpPosEx.size()) ? 11 : allTmpPosEx.size();
                        // get max 10 examples from the same couple
                        List<Tuple> tmpPosEx = allTmpPosEx.subList(0, endIndex);
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
        int posSize = Math.min(posExamples.size(), (int) (negExamples.size() * ratio));
        int negSize = (int) (posSize / ratio);
//        int posSize = (int) (posExamples.size() * ratio);
//        int negSize = posExamples.size() - posSize;
        System.out.println("posExamples size = " + posExamples.size() + " --- posSize = " + posSize
                + " --- negSize = " + negSize);
        if (posExamples.size() > posSize)
            posExamples = posExamples.subList(0, posSize);
        if (negExamples.size() > negSize)
            negExamples = negExamples.subList(0, negSize);

        Map<String, List<Tuple>> allExamples = new HashMap<>();
        allExamples.put("positives", posExamples);
        allExamples.put("negatives", negExamples);

        return allExamples;
    }

    public List<Features> getTrainingSet(List<Tuple> pExamples, List<Tuple> nExamples, boolean useWebsite) {
        List<Features> examples = new ArrayList<>();
        examples.addAll(getAllFeatures(pExamples, 1, useWebsite));
        examples.addAll(getAllFeatures(nExamples, 0, useWebsite));

        return examples;
    }

    private List<Features> getAllFeatures(List<Tuple> tuples, double candidateType, boolean useWebsite) {
        List<Features> features = new ArrayList<>();

        for (int i = 0; i < tuples.size(); i++) {
            Tuple t = tuples.get(i);
            float percent = 100 * (i + 1) / (float) tuples.size();
            String type = (candidateType == 1) ? "positivi" : "negativi";
            System.out.println("\t\t!!!!!" + percent + "% di " + tuples.size() + " " + type + "!!!!!");
            features.add(getFeatures(t, candidateType, useWebsite));
        }

        return features;
    }

    private Features getFeatures(Tuple t, double candidateType, boolean useWebsite) {
        Features features = new Features();

        String website1 = t.getSchema1();
        String website2 = t.getSchema2();
        String attribute1 = t.getAttribute1();
        String attribute2 = t.getAttribute2();
        String category = t.getCategory();
        List<SourceProductPage> sList1 = this.dao.getProds(category, website1, website2, attribute1);
        // List<Document> wList1 = this.dao.getProds("", website1, attribute1);
        List<SourceProductPage> cList1 = this.dao.getProds(category, "", website2, attribute1);

        List<Entry<SourceProductPage, SourceProductPage>> sList2 = this.dao.getProdsInRL(sList1, website2, attribute2);
        // List<Document[]> wList2 = this.dao.getProdsInRL(wList1, website2,
        // attribute2);
        List<Entry<SourceProductPage, SourceProductPage>> cList2 = this.dao.getProdsInRL(cList1, website2, attribute2);

        try {
            features = computeFeatures(sList2, new ArrayList<Entry<SourceProductPage, SourceProductPage>>(), cList2, attribute1, attribute2,
                    candidateType, useWebsite);
        } catch (ArithmeticException e) {
            System.err.println(t.toString());
            try {
                System.in.read();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        System.out.println(sList1.size() + "-->" + sList2.size());
        // System.out.println(wList1.size()+"-->"+wList2.size());
        System.out.println(cList1.size() + "-->" + cList2.size());
        System.out.println(features.toString());

        return features;
    }

    private Features computeFeatures(List<Entry<SourceProductPage, SourceProductPage>> sList, List<Entry<SourceProductPage, SourceProductPage>> wList, 
    		List<Entry<SourceProductPage, SourceProductPage>> cList,
            String a1, String a2, double type, boolean useWebsite) {

        Features features = new Features();
        BagsOfWordsManager sBags = new BagsOfWordsManager(a1, a2, sList);
        BagsOfWordsManager wBags = new BagsOfWordsManager(a1, a2, wList);
        BagsOfWordsManager cBags = new BagsOfWordsManager(a1, a2, cList);

        features.setSourceJSD(this.fe.getJSD(sBags));
        features.setCategoryJSD(this.fe.getJSD(cBags));
        features.setSourceJC(this.fe.getJC(sBags));
        features.setCategoryJC(this.fe.getJC(cBags));
        try {
            features.setSourceMI(this.fe.getMI(sList, a1, a2));
            features.setCategoryMI(this.fe.getMI(cList, a1, a2));
        } catch (Exception e) {
        	//FIXME !!!!!!!
            // System.out.println("DIMENSIONI S: "+sList.size());
            // for(Document[] d: sList){
            // System.out.println(d[0].getString(a1)+"\t"+ d[1].getString(a2));
            // }
            //
            // System.out.println("DIMENSIONI C: "+sList.size());
            // for(Document[] d: cList){
            // System.out.println(d[0].getString(a1)+"\t"+ d[1].getString(a2));
            // }
        }
        if (useWebsite) {
            features.setWebsiteJSD(this.fe.getJSD(wBags));
            features.setWebsiteJC(this.fe.getJC(wBags));
            features.setWebsiteMI(this.fe.getMI(wList, a1, a2));
        }
        features.setMatch(type);
        if (features.hasNan())
            throw new ArithmeticException("feature value is NaN");

        return features;
    }

}
