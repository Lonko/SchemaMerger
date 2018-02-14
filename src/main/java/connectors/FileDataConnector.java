package connectors;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.stream.Collectors;

import models.matcher.Schema;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.cedarsoftware.util.io.JsonWriter;

public class FileDataConnector {
	
	private static final String DEFAULT_CONFIG = "src/main/resources/config.properties";
	private static final String DEFAULT_DATASET = "src/main/resources/specifications";
	private static final String DEFAULT_RL = "src/main/resources/id2category2urls.json";
	private static final String DEFAULT_TS_FOLDER = "src/main/resources/classification";
	private String configPath;
	private String datasetPath;
	private String rlPath;
	private String tsPath;

	public FileDataConnector(){
		this(DEFAULT_DATASET, DEFAULT_RL, DEFAULT_TS_FOLDER, DEFAULT_CONFIG);
	}
	
	//when using the synthetic dataset
	public FileDataConnector(String ts, String config){
		this.tsPath = ts;
		this.configPath = config;
	}
	
	public FileDataConnector(String dataset, String rl, String ts, String config){
		this.configPath = config;
		this.datasetPath = dataset;
		this.rlPath = rl;
		this.tsPath = ts;
	}
	
	public Map<String, String> getAllWebsites(){
		File[] directories = new File(this.datasetPath).listFiles(File::isDirectory);
		Map<String, String> websites = new TreeMap<>();
		
		for(File dir : directories)
			websites.put(dir.getName(), dir.getAbsolutePath());
		
		return websites;
	}
	
	public List<File> getWebsiteCategories(String path){
		File[] sources = new File(path).listFiles(File::isFile);
		return Arrays.asList(sources);
	}
	
	public JSONArray readSource(String path){
		return readJSONArray(path);
	}
	
	public JSONObject readRecordLinkage(){
		return ((JSONObject) readJSONArray(this.rlPath).get(0));
	}
	
	public Map<String, List<String>> readClonedSources(String path){
		Scanner inputStream = null;
		File f = new File(path);
		Map<String, List<String>> clonedSources = new HashMap<>();
		
		try{
			inputStream = new Scanner(f);
			//skip header
			if(inputStream.hasNext()) inputStream.next();
			//read rows
			while(inputStream.hasNext()){
				String line = inputStream.next();
				String[] sources = line.split(",");
				List<String> clones = clonedSources.getOrDefault(sources[0], new ArrayList<>());
				clones.add(sources[1]);
				clonedSources.put(sources[0], clones);
			}
		} catch (FileNotFoundException e) {
            e.printStackTrace();
		} finally {
			if (inputStream != null)
				inputStream.close();
		}
		
		return clonedSources;
	}
	
	public double[][] readTrainingSet(String tsName){
		Scanner inputStream = null;
		File tsFile = new File(this.tsPath + "/" + tsName + ".csv");
		List<double[]> trainingSet = new ArrayList<>();
		
		try{
			inputStream = new Scanner(tsFile);
			//skip header
			if(inputStream.hasNext()) inputStream.next();
			//read rows
			while(inputStream.hasNext()){
	             String line = inputStream.next();
	             String[] values = line.split(",");
	             double[] row = new double[10];
	             for(int i = 0; i < 10; i++)
	            	 row[i] = Double.valueOf(values[i]);
			}
		} catch (FileNotFoundException e) {
            e.printStackTrace();
		} finally {
			if (inputStream != null)
				inputStream.close();
		}
		
		return trainingSet.toArray(new double[][]{});
	}
	
	public Properties readConfig(){
		Properties prop = new Properties();
		InputStream input = null;
		
		try {
			input = new FileInputStream(this.configPath);
			prop.load(input);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		return prop;
	}
	
	public void printTrainingSet(String tsName, double[][] trainingSet){
		String header = "JSDs,JSDw,JSDc,JCs,JCw,JCc,MIs,MIw,MIc,Match";
		List<String> lines = new ArrayList<>();

		for(double[] row : trainingSet){
			String line = Arrays.stream(row)
					.mapToObj(String::valueOf)
					.collect(Collectors.joining(","));
			lines.add(line);
		}
		
		printCSV(tsName, lines, header);
	}
	
	public void printAllTrainingSets(Map<String, List<String>> trainingSets){
		for(String cat : trainingSets.keySet())
			printTrainingSet("1"+cat+"_ts", trainingSets.get(cat));
	}
	
	public void printTrainingSet(String tsName, List<String> trainingSet){
		String header = "Attribute1,Attribute2,Website1,Website2,Category,"
	   					+ "JSDs,JSDw,JSDc,JCs,JCw,JCc,MIs,MIw,MIc,Match";
		printCSV(tsName, trainingSet, header);
	}
	
	public void printDataFrame(String dfName, List<String> dataFrame){
		String header = "AttributeSource,AttributeCatalog,JSDs,JSDw,JSDc,JCs,JCw,JCc,MIs,MIw,MIc,Match";
		printCSV(dfName, dataFrame, header);
	}
	
	public void printMatch(String source, List<String> match){
		String header = "AttributeSource,AttributeCatalog,Match";
		printCSV(source, match, header);
	}
	
	public void printClonedSources(String name, Map<String, List<String>> sources){
		String header = "Source1,Source2";
		List<String> rows = new ArrayList<>();
		
		for(String source : sources.keySet()){
			List<String> clones = sources.get(source);
			for(String clone : clones)
				rows.add(source+","+clone);
		}
		
		printCSV(name, rows, header);
	}
	
	//prints attributes' clusters
	public void printMatchSchema(String name, Schema schema){
		PrintWriter writer = null;
		JSONObject json = new JSONObject();
		int clusterID = 0;
		String header = "Attribute,MatchLinkage,TotalLinkage";
		List<String> csvRows = new ArrayList<>();
		
		try { 
			writer = new PrintWriter(new File(this.tsPath + "/" + name + ".json"));
			
			for(List<String> cluster : schema.schema2Clusters()){
				JSONArray jsonCluster = new JSONArray();
				jsonCluster.addAll(cluster);
				json.put(clusterID, jsonCluster);	
				clusterID++;
				//add rows for the linkage CSV
				for(String attr : cluster){
					if(schema.getMatchLinkage().containsKey(attr) ||
					   schema.getTotalLinkage().containsKey(attr)){
						//linkage on source
						int matchLinkage = schema.getMatchLinkage().getOrDefault(attr, 0);
						//linkage on category
						int totLinkage = schema.getTotalLinkage().get(attr);
						String row = attr.replaceAll(",", "#;#")+","+matchLinkage+","+totLinkage;
						csvRows.add(row);
					}
				}
			}
			
			String jsonToPrint = JsonWriter.formatJson(json.toJSONString());
			writer.print(jsonToPrint);
			printCSV(name+"_linkage", csvRows, header);
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (writer != null)
				writer.close();
		}
	}
	

	/*
	 * Not used
	 * 
	 * 
	public void printSyntheticDatasetLinkage(Map<String, Integer> linkageInfo){
		String header = "Attribute,Linkage";
		List<String> csvRows = new ArrayList<>();
		
		for(String attribute : linkageInfo.keySet()){
			int linkage = linkageInfo.get(attribute);
			csvRows.add(attribute+","+linkage);
		}
		
		printCSV("attribute_linkage.csv", csvRows, header);
	}
	
	public Map<String, Integer> readSyntheticDatasetLinkage(){
		Scanner inputStream = null;
		String path = this.tsPath+"/attribute_linkage.csv";
		File linkageFile = new File(path);
		Map<String, Integer> linkage = new HashMap<>();
		
		try{
			inputStream = new Scanner(linkageFile);
			//skip header
			if(inputStream.hasNext()) inputStream.next();
			//read rows
			while(inputStream.hasNext()){
	             String[] line = inputStream.next().split(",");
	             linkage.put(line[0], Integer.valueOf(line[1]));
			}
		} catch (FileNotFoundException e) {
            e.printStackTrace();
		} finally {
			if (inputStream != null)
				inputStream.close();
		}
		
		return linkage;
	}
	*
	*
	*/
	
	private void printCSV(String name, List<String> rows, String header){
		PrintWriter writer = null;
		
		try { 
			writer = new PrintWriter(new File(this.tsPath + "/" + name + ".csv"));
			writer.print(header);
			
			for(String row : rows){
				writer.println();
				writer.print(row);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			if (writer != null)
				writer.close();
		}
	}

	@SuppressWarnings("unchecked")
	private JSONArray readJSONArray(String path){
		JSONParser parser = new JSONParser();
		JSONArray jsonArray = new JSONArray();

		try {
			Object json = parser.parse(new FileReader(path));
			if(json instanceof JSONArray)
				jsonArray = (JSONArray) json;
			else if(json instanceof JSONObject)
				jsonArray.add((JSONObject) json);
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
		
		return jsonArray;
	}
}
