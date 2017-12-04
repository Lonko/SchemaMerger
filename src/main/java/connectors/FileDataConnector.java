package connectors;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.cedarsoftware.util.io.JsonWriter;

public class FileDataConnector {
	private static final String DEFAULT_DATASET = "src/main/resources/specifications";
	private static final String DEFAULT_RL = "src/main/resources/id2category2urls.json";
	private static final String DEFAULT_TS_FOLDER = "src/main/resources/classification";
	private String datasetPath;
	private String rlPath;
	private String tsPath;

	public FileDataConnector(){
		this(DEFAULT_DATASET, DEFAULT_RL, DEFAULT_TS_FOLDER);
	}
	
	public FileDataConnector(String dataset, String rl, String ts){
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
	
	public void printSchema(String name, List<List<String>> schema){
		PrintWriter writer = null;
		JSONObject json = new JSONObject();
		int clusterID = 0;
		
		try { 
			writer = new PrintWriter(new File(this.tsPath + "/" + name + ".json"));
			
			for(List<String> cluster : schema){
				JSONArray jsonCluster = new JSONArray();
				jsonCluster.addAll(cluster);
				json.put(clusterID, jsonCluster);	
				clusterID++;
			}
			
			String jsonToPrint = JsonWriter.formatJson(json.toJSONString());
			writer.print(jsonToPrint);
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (writer != null)
				writer.close();
		}
	}
	
	private void printCSV(String name, List<String> trainingSet, String header){
		PrintWriter writer = null;
		
		try { 
			writer = new PrintWriter(new File(this.tsPath + "/" + name + ".csv"));
			writer.print(header);
			
			for(String row : trainingSet){
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
