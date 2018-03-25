package matcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import models.matcher.BagsOfWordsManager;
import models.matcher.DataFrame;
import models.matcher.Features;
import models.matcher.InvertedIndexesManager;
import models.matcher.Match;
import models.matcher.Schema;

import org.bson.Document;

import connectors.MongoDBConnector;
import connectors.RConnector;

public class CategoryMatcher {

    private MongoDBConnector mdbc;
    private FeatureExtractor fe;
    private RConnector r;

    public CategoryMatcher(MongoDBConnector conn) {
        this.mdbc = conn;
        this.fe = new FeatureExtractor();
    }

    public CategoryMatcher(MongoDBConnector conn, RConnector r) {
        this.mdbc = conn;
        this.fe = new FeatureExtractor();
        this.r = r;
    }

    public boolean getMatch(List<String> websites, String category, int cardinality, Schema schemaMatch,
            boolean useMI, boolean matchToOne) {
        boolean matched = false;
        // last website is the one to be matched with the catalog
        String newSource = websites.remove(websites.size() - 1);
        // linked page -> pages in catalog
        Map<Document, List<Document>> linkageMap = this.mdbc.getProdsInRL(websites, category);
        // check if new source is matchable
        if (checkIfValidSource(newSource, linkageMap.keySet())) {
            // pages in catalog(merged) -> linked page
            List<Document[]> linkedProds = setupCatalog(linkageMap, schemaMatch);

            // Inverted indexes (attribute name -> indexes of prods)
            InvertedIndexesManager invIndexes = getInvertedIndexes(linkedProds, newSource);

            Map<String, Integer> attributesLinkage = new HashMap<>();
            DataFrame dataFrame = computeAttributesFeatures(linkedProds, invIndexes, cardinality, newSource,
                    attributesLinkage, useMI);
            try {
                double[] predictions = r.classify(dataFrame);
                Match match = filterMatch(dataFrame, predictions);

                updateSchema(schemaMatch, invIndexes, match, attributesLinkage);

                matched = true;
            } catch (Exception e) {
                System.out.println("CON LINKAGE -> " + checkIfValidSource(newSource, linkageMap.keySet()));
                System.out.println("(df di lunghezza : " + dataFrame.getAttrCatalog().size() + ")");
            }
        }

        // if false the match was skipped
        return matched;
    }

    // check if among the linked pages there's at least one belonging to the new
    // source
    boolean checkIfValidSource(String source, Set<Document> linkedPages) {
        boolean foundSourcePage = false;

        for (Document page : linkedPages) {
            if (page.getString("url").contains(source)) {
                foundSourcePage = true;
                break;
            }
        }

        return foundSourcePage;
    }

    // update all products pages' attribute names and merges catalog prods in linkageS
    private List<Document[]> setupCatalog(Map<Document, List<Document>> prods, Schema schema) {
       return setupCatalog(prods, schema, "");
    }
    
    private List<Document[]> setupCatalog(Map<Document, List<Document>> prods, Schema schema, String reference) {
        List<Document[]> updatedList = new ArrayList<>();

        for (Map.Entry<Document, List<Document>> linkage : prods.entrySet()) {
            Document sourcePage = linkage.getKey();
            Document catalogPage = mergeProducts(linkage.getValue(), schema, reference);
            updateSpecs(sourcePage, schema);
            updatedList.add(new Document[] { catalogPage, sourcePage });
        }

        return updatedList;
    }

    private Document mergeProducts(List<Document> prods, Schema schema, String reference) {
        // update the attribute names according to the existing schema
        prods.forEach(p -> updateSpecs(p, schema));
        Map<String, Object> newSpecs = new HashMap<>();

        for (Document p : prods)
            for (String attr : p.get("spec", Document.class).keySet()) {
                /* if reference is an empty string add all attributes
                 * if it contains a source name, only add the attributes that can be found in it
                 */
                if(reference.equals("") || attr.contains(reference)){
                    String oldValue = (String) newSpecs.getOrDefault(attr, "");
                    String newValue = p.get("spec", Document.class).getString(attr);
                    if (oldValue.length() > 0) // there was already a value for that attr
                        newSpecs.put(attr, oldValue + "###" + newValue);
                    else
                        newSpecs.put(attr, newValue);
                }
            }
        
        Document specs = new Document(newSpecs);
        return new Document("spec", specs);
    }

    // update attribute names of a single product page
    private void updateSpecs(Document p, Schema schema) {
        Document specs = p.get("spec", Document.class);
        String website = p.getString("website");
        Document newSpecs = new Document();
        // update attribute names to the format "attribute###website"
        specs.keySet()
                .forEach(
                        attr -> {
                            if (!attr.contains("###")) {
                                String value = specs.getString(attr);
                                newSpecs.append(
                                        schema.getAttributesMap().getOrDefault(attr + "###" + website,
                                                attr + "###" + website), value);
                            } else 
                                /* In case of wrong linkage a page could appear twice in the original linkageMap;
                                 * when this happens, its specs get updated twice, but the second time they need 
                                 * to be added without changes.
                                 */
                                newSpecs.append(attr, specs.getString(attr));
                        });
        p.put("spec", newSpecs);
    }

    private InvertedIndexesManager getInvertedIndexes(List<Document[]> prods, String website) {
        Map<String, Set<Integer>> invIndCatalog = new HashMap<>();
        Map<String, Set<Integer>> invIndLinked = new HashMap<>();
        Map<String, Set<Integer>> invIndSource = new HashMap<>();

        // check all linked product pages
        for (int i = 0; i < prods.size(); i++) {
            Document[] pair = prods.get(i);
            // get the attributes present in those 2 pages
            Set<String> attrsCatalog = pair[0].get("spec", Document.class).keySet();
            Set<String> attrsLinked = pair[1].get("spec", Document.class).keySet();
            // check the website of the linked page
            boolean isInSource = pair[1].getString("website").equals(website);

            // add the attributes in the catalog's index
            for (String attrC : attrsCatalog) {
                Set<Integer> indexes = invIndCatalog.getOrDefault(attrC, new HashSet<Integer>());
                indexes.add(i);
                invIndCatalog.put(attrC, indexes);
            }
            // add the attributes in the linked index...
            for (String attrL : attrsLinked) {
                Set<Integer> indexesL = invIndLinked.getOrDefault(attrL, new HashSet<Integer>());
                indexesL.add(i);
                invIndLinked.put(attrL, indexesL);

                // ...and the source index if the page belongs to the source to
                // be matched
                if (isInSource) {
                    Set<Integer> indexesS = invIndSource.getOrDefault(attrL, new HashSet<Integer>());
                    indexesS.add(i);
                    invIndSource.put(attrL, indexesS);
                }
            }
        }

        InvertedIndexesManager invIndexes = new InvertedIndexesManager();
        invIndexes.setCatalogIndex(invIndCatalog);
        invIndexes.setLinkedIndex(invIndLinked);
        invIndexes.setSourceIndex(invIndSource);

        return invIndexes;
    }

    private DataFrame computeAttributesFeatures(List<Document[]> linkedProds,
            InvertedIndexesManager invIndexes, int cardinality, String website,
            Map<String, Integer> attributesLinkage, boolean useMI) {

        DataFrame df = new DataFrame();
        // Set<String> attrSet = new TreeSet<>();

        // scorri il prod cartesiano di attributiS x attributiC
        for (Map.Entry<String, Set<Integer>> attrS : invIndexes.getSourceIndex().entrySet())
            for (Map.Entry<String, Set<Integer>> attrCatalog : invIndexes.getCatalogIndex().entrySet()) {
                String aCatalog = attrCatalog.getKey();
                String aSource = attrS.getKey();

                // prods in linkage between S and Catalog with the required
                // attributes
                Set<Integer> commonProdsS = new HashSet<>(attrS.getValue());
                commonProdsS.retainAll(attrCatalog.getValue());
                if (commonProdsS.size() < 1)
                    continue;
                // prods in linkage between the whole category and Catalog with
                // the required attributes
                Set<Integer> commonProdsL = new HashSet<>(invIndexes.getLinkedIndex().get(aSource));
                commonProdsL.retainAll(attrCatalog.getValue());

                List<Document[]> linkageS = new ArrayList<>();
                commonProdsS.forEach(i -> linkageS.add(linkedProds.get(i)));
                // <------>
                // DA SISTEMARE ORA CHE GLI ATTRIBUTI SONO GESTITI
                // DIFFERENTEMENTE !!!!!!
                // if(cardinality > 0 || checkSourceDomain(linkedProds, aSource,
                // website, cardinality) ||
                // hasSmallDomain(linkageS, aCatalog, false, cardinality))
                // continue;
                // if(aSource.equals(aCatalog))
                // attrSet.add(aSource);
                // <------>
                List<Document[]> linkageL = new ArrayList<>();
                commonProdsL.forEach(i -> linkageL.add(linkedProds.get(i)));

                attributesLinkage.put(aSource + aCatalog, linkageS.size());
                Features features = computeFeatures(linkageS, linkageL, aCatalog, aSource, useMI);
                df.addRow(features, aCatalog, aSource);
            }

        // System.out.println("Considerati: " + attrSet.size());
        // System.out.println(attrSet.toString());

        return df;
    }

    // public void printTemp(List<Document[]> l, String a){
    // for(Document[] d : l){
    // String value1 = d[0].get("spec", Document.class).getString(a);
    // String value2 = d[1].get("spec", Document.class).getString(a);
    // if(!value1.equals(value2))
    // System.out.println(value1+"----->"+value2);
    // }
    // }

    public Features computeFeatures(List<Document[]> sList, List<Document[]> cList, String a1, String a2,
            boolean useMI) {

        Features features = new Features();
        BagsOfWordsManager sBags = new BagsOfWordsManager(a1, a2, sList);
        BagsOfWordsManager cBags = new BagsOfWordsManager(a1, a2, cList);

        features.setSourceJSD(this.fe.getJSD(sBags));
        features.setCategoryJSD(this.fe.getJSD(cBags));
        features.setSourceJC(this.fe.getJC(sBags));
        features.setCategoryJC(this.fe.getJC(cBags));
        if (useMI) {
            features.setSourceMI(this.fe.getMI(sList, a1, a2));
            features.setCategoryMI(this.fe.getMI(cList, a1, a2));
        }
        if (features.hasNan())
            throw new ArithmeticException("feature value is NaN");

        // if(a1.equals("Apps Platform") && a2.equals("Apps Platform")){
        // printTemp(cList, a1);
        // System.out.println(features.toString());
        // }

        return features;
    }

    // calls hasSmallDomain using all the products pages in website with the
    // relevant attribute
    private boolean checkSourceDomain(List<Document[]> prods, String attribute, String website,
            int cardinality) {
        List<Document[]> sourceList = prods
                .stream()
                // NEL CATALOGO WEBSITE POTREBBE
                // NON ESSERE PIù
                // RILEVANTE !!!!!
                .filter(couple -> couple[1].getString("website").equals(website)
                        && couple[1].get("spec", Document.class).getString(attribute) != null)
                .collect(Collectors.toList());
        return hasSmallDomain(sourceList, attribute, true, cardinality);
    }

    private boolean hasSmallDomain(List<Document[]> prods, String attribute, boolean isInSource,
            int cardinality) {
        Set<String> values = new HashSet<>();

        for (int i = 0; i < prods.size(); i++) {
            Document[] pair = prods.get(i);
            String value;
            if (isInSource)
                value = pair[1].get("spec", Document.class).getString(attribute).toLowerCase();
            else
                value = pair[0].get("spec", Document.class).getString(attribute).toLowerCase();
            values.add(value);
            if (values.size() > cardinality)
                return false;
        }
        return true;
    }

    private Match filterMatch(DataFrame df, double[] predictions) {
        Match match = new Match();
        List<Double> filteredPredictions = new ArrayList<>();

        // df.updateMatchProbabilities(predictions);
        // remove all matches where the probability is below 0.5
        filteredPredictions = filterFalse(df, predictions);

        // FileDataConnector fdc = new FileDataConnector();
        // fdc.printDataFrame("dataFrame(card5)", df.toCSVFormatSlim());

        // get only the best (unique) matches
        boolean repeat = false;
        do {
            repeat = getMaxMatches(df, match, filteredPredictions);
        } while (repeat);

        return match;
    }

    private List<Double> filterFalse(DataFrame df, double[] predictions) {
        List<Double> predictionsList = new ArrayList<>();
        List<Integer> indexes = IntStream.range(0, predictions.length).mapToObj(Integer.class::cast)
                .collect(Collectors.toList());

        predictionsList = IntStream.range(0, predictions.length).filter(i -> predictions[i] >= 0.5)
                .mapToObj(i -> {
                    indexes.remove(indexes.indexOf(i));
                    return predictions[i];
                }).collect(Collectors.toList());

        // indexes.stream().forEach(i -> {
        // if(df.getAttrCatalog().get(i).equals(df.getAttrSource().get(i)))
        // System.out.println(df.getAttrSource().get(i)+"---->"+df.getAttrCatalog().get(i)
        // +"-----_>"+predictions[i]);
        // });
        Collections.reverse(indexes);
        df.removeByIndexes(indexes);

        return predictionsList;
    }

    private boolean getMaxMatches(DataFrame df, Match match, List<Double> predictions) {
        Map<String, Integer> indexes = new HashMap<>();
        List<Integer> ranges = df.getSourceRanges();

        for (int i = 0; i < ranges.size() - 1; i++) {
            // select only the probability of matches for the specific Source
            // Attribute
            List<Double> probs = predictions.subList(ranges.get(i), ranges.get(i + 1));
            // get the best match probability
            double max = Collections.max(probs);
            boolean isMultiple = Collections.frequency(probs, max) > 1;
            // check if there aren't more than one optimal match and if so,
            // accept it
            if (!isMultiple) {
                int index = ranges.get(i) + probs.indexOf(max);
                String catalogAttribute = df.getStringAtIndex("attrCatalog", index);
                // check that there isn't already a Source Attribute matched to
                // the same Catalog Attribute
                if (predictions.get(index) >= predictions.get(indexes.getOrDefault(catalogAttribute, index)))
                    indexes.put(catalogAttribute, index);
            }
        }

        TreeSet<Integer> rowsToRemove = new TreeSet<>();
        for (int index : indexes.values()) {
            String sourceAttr = df.getStringAtIndex("attrSource", index);
            String catalogAttr = df.getStringAtIndex("attrCatalog", index);
            double p = predictions.get(index);
            // add Match
            match.addRow(catalogAttr, sourceAttr, p);
            rowsToRemove.addAll(df.getIndexesByValue("attrSource", sourceAttr));
            rowsToRemove.addAll(df.getIndexesByValue("attrCatalog", catalogAttr));
        }

        rowsToRemove = (TreeSet) rowsToRemove.descendingSet();
        // remove all rows in the dataframe containing one of the two attributes
        df.removeByIndexes(new ArrayList<>(rowsToRemove));
        // also remove the probabilities associated to those rows
        rowsToRemove.stream().forEach(i -> predictions.remove(i));

        return indexes.values().size() > 0;
    }

    /*
     * The update consists in adding every attribute found in the matched source
     * (even those not matched) and new attributes from the catalog (if there
     * are)
     */
    public void updateSchema(Schema schema, InvertedIndexesManager invIndexes, Match match,
            Map<String, Integer> attributesLinkage) {
        // add new catalog's attribute
        for (String catAttr : invIndexes.getCatalogIndex().keySet())
            if (!schema.getAttributesMap().containsKey(catAttr))
                schema.getAttributesMap().put(catAttr, catAttr);
        // add matched attributes
        for (String[] couple : match.getMatchedAttributes()) {
            schema.getAttributesMap().put(couple[0], couple[1]);
            // update linkage count for the matched attributes
            int counter = attributesLinkage.get(couple[0] + couple[1]);
            schema.getMatchLinkage().put(couple[0], counter);
        }
        // add non matched attributes
        for (String sourceAttr : invIndexes.getSourceIndex().keySet())
            if (!schema.getAttributesMap().containsKey(sourceAttr))
                schema.getAttributesMap().put(sourceAttr, sourceAttr);
    }

    public static void main(String[] args) {
        // FileDataConnector fdc = new FileDataConnector();
        // MongoDBConnector mdbc = new MongoDBConnector(fdc);
        // CategoryMatcher cm = new CategoryMatcher(mdbc);
        // Map<String, String> schema = new HashMap<>();
        // schema.put("giorno###prova", "notte###prova");
        // schema.put("lunedi###zappa", "notte###prova");
        // Document spec = new Document().append("bo", "nulla");
        // spec.append("giorno", "1");
        // Document spec2 = new Document().append("lunedi", "cc");
        // spec2.append("terzo", "valore");
        // Document prod = new Document().append("spec", spec);
        // Document prod2 = new Document().append("spec", spec2);
        // prod.append("website", "prova");
        // prod2.append("website", "zappa");
        // cm.updateSpecs(prod, schema);
        // ArrayList<Document> list = new ArrayList<>();
        // list.add(prod);
        // list.add(prod2);
        // System.out.println(cm.mergeProducts(list, schema).get("spec",
        // Document.class));
    }
}
