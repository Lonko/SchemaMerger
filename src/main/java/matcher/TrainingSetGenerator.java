package matcher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import model.BagsOfWordsManager;
import model.Features;
import model.Tuple;

import org.bson.Document;

import connectors.MongoDBConnector;

public class TrainingSetGenerator {

	private MongoDBConnector mdbc;
	private FeatureExtractor fe;
	private Map<String, List<String>> clonedSources;
	
	public TrainingSetGenerator(MongoDBConnector conn, FeatureExtractor fe, Map<String, List<String>> clSources){
		this.mdbc = conn;
		this.fe = new FeatureExtractor();	
		this.clonedSources = clSources;
	}
	
	public List<String> getTrainingSetWithTuples(int sampleSize, int setSize, boolean useWebsite, 
													boolean addTuples, double ratio, String category){
		
		Map<String, List<Tuple>> examples = new HashMap<>();
		boolean hasEnoughNegatives = false, hasEnoughExamples = false;
		int newSizeP, newSizeN, sizeP = 0, sizeN = 0, tentatives = 0;
		
		do{
			List<Document> sample = this.mdbc.getRLSample(sampleSize, category);
			Map<String, List<Tuple>> newExamples = getExamples(sample, setSize, ratio);
			newSizeP = newExamples.get("positives").size(); 
			newSizeN = newExamples.get("negatives").size();
			System.out.println(newSizeP+" + "+newSizeN+" = "+(newSizeP+newSizeN));
			hasEnoughExamples = ((setSize * 0.95) <= (newSizeP + newSizeN)) && ((newSizeP + newSizeN) <= (setSize * 1.05));
			hasEnoughNegatives = ( newSizeP / (double) (newSizeP + newSizeN) == ratio );
			tentatives++;
			if((sizeP+sizeN) < (newSizeP+newSizeN) && hasEnoughNegatives){
				examples = newExamples;
				sizeP = newSizeP;
				sizeN = newSizeN;
			}
			//don't loop too many times
			if(tentatives == 10)
				break;
		} while (! (hasEnoughExamples));  				     //there must be enough examples
		
		//if not enough examples were found, return an empty list
		if(examples.size() == 0) 
			return new ArrayList<String>();
		
		System.out.println(examples.get("positives").size()
				+" coppie di prodotti prese in considerazione per il training set");
		System.out.println(examples.get("positives").size()+"\t"+examples.get("negatives").size());
		
		List<Features> trainingSet = getTrainingSet(
										examples.get("positives"), examples.get("negatives"), useWebsite);
		List<String> tsRows = new ArrayList<>();
		
		for(int i = 0; i < trainingSet.size(); i++){
			String row = "";
			if(addTuples){
				Tuple t;
				if(i < sizeP)
					t = examples.get("positives").get(i);
				else 
					t = examples.get("negatives").get(i % sizeP);
				row = t.toRowString();
			}
			Features f = trainingSet.get(i);
			row += f.toString()+","+f.getMatch();
			tsRows.add(row);
		}
		
		return tsRows;
	}
	
	private Map<String, List<Tuple>> getExamples(List<Document> sample, int nExamples, double ratio){
		List<Tuple> posExamples = new ArrayList<>();
		List<Tuple> negExamples = new ArrayList<>();
		
		for(Document doc1 : sample){
			@SuppressWarnings("unchecked")
			List<String> rlUrls = doc1.get("linkage", List.class);
			
			for(String url : rlUrls){
				Document doc2 = this.mdbc.getIfValid(url);
				
				if(doc2 != null){
					String website1 = doc1.getString("website");
					String website2 = doc2.getString("website");
					String category1 = doc1.getString("category");
					String category2 = doc2.getString("category");
					String source1 = category1+"###"+website1;
					String source2 = category2+"###"+website2;
					
					//check if the two pages belong to cloned sources
					if(this.clonedSources.containsKey(source1) && 
					   this.clonedSources.get(source1).contains(source2))
						continue;
					
					if(!website1.equals(website2)){
						Document attributes1 = doc1.get("spec", Document.class);
						Document attributes2 = doc2.get("spec", Document.class);
						Set<String> aSet1 = attributes1.keySet();
						Set<String> aSet2 = attributes2.keySet();
						aSet1.retainAll(aSet2);
						
						//generates positive examples
						List<Tuple> tmpPosEx = new ArrayList<>();
						aSet1.stream().forEach(a -> {
							Tuple t = new Tuple(a, a, website1, website2, category1);
							tmpPosEx.add(t);
						});
						posExamples.addAll(tmpPosEx);
						
						//generates negative examples
						for(int i = 0; i < tmpPosEx.size()-1; i++){
							for(int j = i+1; j < tmpPosEx.size(); j++){
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
		int posSize = (int)(posExamples.size() *  ratio);
		int negSize = posExamples.size() - posSize;
		System.out.println("posExamples size = "+posExamples.size()+" --- posSize = "+posSize+" --- negSize = "+negSize);
		if(posExamples.size() > posSize)
			posExamples = posExamples.subList(0, posSize);
		if(negExamples.size() > negSize)
			negExamples = negExamples.subList(0, negSize);
		
		Map<String, List<Tuple>> allExamples = new HashMap<>();
		allExamples.put("positives", posExamples);
		allExamples.put("negatives", negExamples);
		
		return allExamples;
	}

	public List<Features> getTrainingSet(List<Tuple> pExamples, List<Tuple> nExamples, boolean useWebsite){
		List<Features> examples = new ArrayList<>();
		examples.addAll(getAllFeatures(pExamples, 1, useWebsite));
		examples.addAll(getAllFeatures(nExamples, 0, useWebsite));
		
		return examples;
	}
	
	private List<Features> getAllFeatures(List<Tuple> tuples, double candidateType, boolean useWebsite){
		List<Features> features = new ArrayList<>();
		
		for(int i = 0; i < tuples.size(); i++){
			Tuple t = tuples.get(i);
			float percent = 100*(i+1)/(float)tuples.size();
			String type = (candidateType == 1) ? "positivi" : "negativi";
			System.out.println("\t\t!!!!!"+percent+"% di "+tuples.size()+" "+type+"!!!!!");
			features.add(getFeatures(t, candidateType, useWebsite));
		}
		
		return features;
	}
	
	private Features getFeatures(Tuple t, double candidateType, boolean useWebsite){
		Features features = new Features();

		String website1 = t.getSchema1();
		String website2 = t.getSchema2();
		String attribute1 = t.getAttribute1();
		String attribute2 = t.getAttribute2();
		String category = t.getCategory();
		List<Document> sList1 = this.mdbc.getProds(website1, category, attribute1);
		List<Document> wList1 = this.mdbc.getProds(website1, "", attribute1);
		List<Document> cList1 = this.mdbc.getProds("", category, attribute1);
		List<Document[]> sList2 = this.mdbc.getProdsInRL(sList1, website2, attribute2);
		List<Document[]> wList2 = this.mdbc.getProdsInRL(wList1, website2, attribute2);
		List<Document[]> cList2 = this.mdbc.getProdsInRL(cList1, website2, attribute2);
		try{
			features = computeFeatures(sList2, wList2, cList2, attribute1, attribute2, candidateType, useWebsite);
		} catch (ArithmeticException e){
			System.err.println(t.toString());
			try {
				System.in.read();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}

		System.out.println(sList1.size()+"-->"+sList2.size());
		System.out.println(wList1.size()+"-->"+wList2.size());
		System.out.println(cList1.size()+"-->"+cList2.size());
		System.out.println(features.toString());

		return features;
	}
	
	private Features computeFeatures(List<Document[]> sList, List<Document[]> wList, List<Document[]> cList,
									 String a1, String a2, double type, boolean useWebsite){
		
		Features features = new Features();
		BagsOfWordsManager sBags = new BagsOfWordsManager(a1, a2, sList);
		BagsOfWordsManager wBags = new BagsOfWordsManager(a1, a2, wList);
		BagsOfWordsManager cBags = new BagsOfWordsManager(a1, a2, cList);

		features.setSourceJSD(this.fe.getJSD(sBags));
		features.setCategoryJSD(this.fe.getJSD(cBags));
		features.setSourceJC(this.fe.getJC(sBags));
		features.setCategoryJC(this.fe.getJC(cBags));
		features.setSourceMI(this.fe.getMI(sList, a1, a2));
		features.setCategoryMI(this.fe.getMI(cList, a1, a2));
		if(useWebsite){
			features.setWebsiteJSD(this.fe.getJSD(wBags));
			features.setWebsiteJC(this.fe.getJC(wBags));
			features.setWebsiteMI(this.fe.getMI(wList, a1, a2));
		}
		features.setMatch(type);
		if(features.hasNan())
			throw new ArithmeticException("feature value is NaN");
		
		return features;
	}

}
