package model;

import java.util.ArrayList;
import java.util.List;

public class Match {
	
	private List<String> sourceAttributes = new ArrayList<>();
	private List<String> catalogAttributes = new ArrayList<>();
	private List<Double> matchProbabilities = new ArrayList<>();
	private int nRows = 0;
	
	public Match(){
		
	}
	
	public void addRow(String aCatalog, String aSource, double prob){
		this.catalogAttributes.add(aCatalog);
		this.sourceAttributes.add(aSource);
		this.matchProbabilities.add(prob);
		this.nRows += 1;
	}

	public List<String> getSourceAttributes() {
		return sourceAttributes;
	}

	public void setSourceAttributes(List<String> sourceAttributes) {
		this.sourceAttributes = sourceAttributes;
	}

	public List<String> getCatalogAttributes() {
		return catalogAttributes;
	}

	public void setCatalogAttributes(List<String> catalogAttributes) {
		this.catalogAttributes = catalogAttributes;
	}

	public List<Double> getMatchProbabilities() {
		return matchProbabilities;
	}

	public void setMatchProbabilities(List<Double> matchProbabilities) {
		this.matchProbabilities = matchProbabilities;
	}

	public int getnRows() {
		return nRows;
	}

	public void setnRows(int nRows) {
		this.nRows = nRows;
	}
	
	public List<String[]> getMatchedAttributes(){
		List<String[]> attrs = new ArrayList<>();
		
		for(int i = 0; i < this.nRows; i++){
			String[] attributesCouple = new String[]{
										this.sourceAttributes.get(i),
										this.catalogAttributes.get(i)
										};
			attrs.add(attributesCouple);
		}
		
		return attrs;
	}
	
	public List<String> toCSVFormat(){
		List<String> rows = new ArrayList<>();
		for(int i = 0; i < this.nRows; i++){
			rows.add(this.sourceAttributes.get(i).replace(",", "#;#")
						+","+this.catalogAttributes.get(i).replace(",", "#;#")
						+","+this.matchProbabilities.get(i));
		}
		return rows;
	}

}
