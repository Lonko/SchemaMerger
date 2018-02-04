package models.matcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class DataFrame {
	
	private List<String> attrCatalog = new ArrayList<>();
	private List<String> attrSource = new ArrayList<>();
	private List<Double> JSDs = new ArrayList<>();
	private List<Double> JSDw = new ArrayList<>();
	private List<Double> JSDc = new ArrayList<>();
	private List<Double> JCs = new ArrayList<>();
	private List<Double> JCw = new ArrayList<>();
	private List<Double> JCc = new ArrayList<>();
	private List<Double> MIs = new ArrayList<>();
	private List<Double> MIw = new ArrayList<>();
	private List<Double> MIc = new ArrayList<>();
	private List<Double> Match = new ArrayList<>();
	private int nRows = 0;

	public DataFrame(){
	}
	
	public DataFrame(DataFrame df){
		Collections.copy(this.attrCatalog, df.getAttrCatalog());
		Collections.copy(this.attrSource, df.getAttrSource());
		Collections.copy(this.JSDs, df.getJSDs());
		Collections.copy(this.JSDw, df.getJSDw());
		Collections.copy(this.JSDc, df.getJSDc());
		Collections.copy(this.JCs, df.getJCs());
		Collections.copy(this.JCw, df.getJCw());
		Collections.copy(this.JCc, df.getJCc());
		Collections.copy(this.MIs, df.getMIs());
		Collections.copy(this.MIw, df.getMIw());
		Collections.copy(this.MIc, df.getMIc());
		Collections.copy(this.Match, df.getMatch());
	}
	
	public void addRow(Features features, String aCatalog, String aSource){
		this.attrCatalog.add(aCatalog);
		this.attrSource.add(aSource);
		
		this.JSDs.add(features.getSourceJSD());
		this.JSDw.add(features.getWebsiteJSD());
		this.JSDc.add(features.getCategoryJSD());

		this.JCs.add(features.getSourceJC());
		this.JCw.add(features.getWebsiteJC());
		this.JCc.add(features.getCategoryJC());

		this.MIs.add(features.getSourceMI());
		this.MIw.add(features.getWebsiteMI());
		this.MIc.add(features.getCategoryMI());
		
		this.Match.add(features.getMatch());
		
		this.nRows += 1;
	}
	
	public String getStringAtIndex(String field, int index){
		String value = null;
		if(field.equals("attrCatalog"))
			value = this.attrCatalog.get(index);
		if(field.equals("attrSource"))
			value = this.attrSource.get(index);
		return value;
	}
	
	public void removeByIndexes(List<Integer> indexes){
		for(int index : indexes)
			removeByIndex(index);
	}
	
	public List<Integer> getSourceRanges(){
		List<Integer> ranges = new ArrayList<>();
		String attr = ""; 
		
		for(int i = 0; i < this.attrSource.size(); i++){
			if(!this.attrSource.get(i).equals(attr)){
				ranges.add(i);
				attr = this.attrSource.get(i);
			}
		}
		ranges.add(this.attrSource.size());
		
		return ranges;
	}
	
	public Set<Integer> getIndexesByValue(String field, String value){
		Set<Integer> indexes = new TreeSet<>();
		
		if(field.equals("attrCatalog")){
			for(int i = 0; i < this.attrCatalog.size(); i++)
				if(this.attrCatalog.get(i).equals(value)) indexes.add(i);
		}
		if(field.equals("attrSource")){
			for(int i = 0; i < this.attrSource.size(); i++)
				if(this.attrSource.get(i).equals(value)) indexes.add(i);
		}
		
		return indexes;
	}
	
	public void removeByIndex(int index){
		this.attrCatalog.remove(index);
		this.attrSource.remove(index);
		this.JSDs.remove(index);
		this.JSDw.remove(index);
		this.JSDc.remove(index);
		this.JCs.remove(index);
		this.JCw.remove(index);
		this.JCc.remove(index);
		this.MIs.remove(index);
		this.MIw.remove(index);
		this.MIc.remove(index);
		this.Match.remove(index);
		this.nRows -= 1;
	}
	
	public void updateMatchProbabilities(double[] probs){
		List<Double> newProbs = new ArrayList<>();
		for(double p : probs)
			newProbs.add(p);
		this.Match = newProbs;
	}

	public List<String> getAttrCatalog() {
		return attrCatalog;
	}

	public List<String> getAttrSource() {
		return attrSource;
	}

	public List<Double> getJSDs() {
		return JSDs;
	}

	public List<Double> getJSDw() {
		return JSDw;
	}

	public List<Double> getJSDc() {
		return JSDc;
	}

	public List<Double> getJCs() {
		return JCs;
	}

	public List<Double> getJCw() {
		return JCw;
	}

	public List<Double> getJCc() {
		return JCc;
	}

	public List<Double> getMIs() {
		return MIs;
	}

	public List<Double> getMIw() {
		return MIw;
	}

	public List<Double> getMIc() {
		return MIc;
	}

	public List<Double> getMatch() {
		return Match;
	}
	
	public List<String> toCSVFormat(){
		List<String> rows = new ArrayList<>();
		for(int i = 0; i < this.nRows; i++){
			rows.add(this.attrSource.get(i).replace(",", "#;#")
						+","+this.attrCatalog.get(i).replace(",", "#;#")
						+","+this.JSDs.get(i)
						+","+this.JSDw.get(i)
						+","+this.JSDc.get(i)
						+","+this.JCs.get(i)
						+","+this.JCw.get(i)
						+","+this.JCc.get(i)
						+","+this.MIs.get(i)
						+","+this.MIw.get(i)
						+","+this.MIc.get(i)
						+","+this.Match.get(i));
		}
		return rows;
	}
	
	public List<String> toCSVFormatSlim(){
		List<String> rows = new ArrayList<>();
		for(int i = 0; i < this.nRows; i++){
			rows.add(this.attrSource.get(i).replace(",", "#;#")
						+","+this.attrCatalog.get(i).replace(",", "#;#")
						+","+this.Match.get(i));
		}
		return rows;
	}
}
