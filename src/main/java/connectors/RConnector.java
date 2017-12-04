package connectors;

import java.lang.reflect.InvocationTargetException;

import model.DataFrame;

import org.rosuda.JRI.Rengine;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPDouble;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngine;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.REngineStdOutput;
import org.rosuda.REngine.RList;

public class RConnector {
	
	private String modelPath;
	private REngine eng = null;
	
	public RConnector(String modelPath){
		this.modelPath = modelPath;
	}
	
	public void start(){
		//start REngine
		try {
			/*for debugging
			this.eng = REngine.engineForClass("org.rosuda.REngine.JRI.JRIEngine", new String [] {"--vanilla"},
									new REngineStdOutput(), false);*/
			this.eng = REngine.engineForClass("org.rosuda.REngine.JRI.JRIEngine");
		    //load caret package
		    this.eng.parseAndEval("library(caret)");
		    //load classifier model
		    this.eng.parseAndEval("load('"+this.modelPath+"')");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}  catch (REngineException e) {
			e.printStackTrace();
		} catch (REXPMismatchException e) {
			e.printStackTrace();
		}
		
	}
	
	public void stop(){
		if(this.eng != null)
			this.eng.close();
	}
	
	public double[] classify(DataFrame df){
		double[] predictions = null;
		
	    try {
	    	//build dataframe columns
		    String[] colNames = {"JSDs", "JSDc", "JCs", "JCc", "MIs", "MIc"};
		    double[] colJSDs = df.getJSDs().stream().mapToDouble(Double::doubleValue).toArray();
		    double[] colJSDc = df.getJSDc().stream().mapToDouble(Double::doubleValue).toArray();
		    double[] colJCs = df.getJCs().stream().mapToDouble(Double::doubleValue).toArray();
		    double[] colJCc = df.getJCc().stream().mapToDouble(Double::doubleValue).toArray();
		    double[] colMIs = df.getMIs().stream().mapToDouble(Double::doubleValue).toArray();
		    double[] colMIc = df.getMIc().stream().mapToDouble(Double::doubleValue).toArray();
		    //create dataframe
		    REXP mydf = REXP
		    		.createDataFrame(new RList(
		    				new REXP[] {
		    						new REXPDouble(colJSDs),
		    						new REXPDouble(colJSDc),
		    						new REXPDouble(colJCs),
		    						new REXPDouble(colJCc),
		    						new REXPDouble(colMIs),
		    						new REXPDouble(colMIc)
		    				},
		    				colNames));
		    //pass dataframe to REngine
		    this.eng.assign("dataFrame", mydf);
		    //predict matches
		    this.eng.parseAndEval("predictions <- predict(modelN, dataFrame, type = 'prob')");
		    //System.out.println(eng.parseAndEval("print(predictions$true)"));
		    predictions = this.eng.parseAndEval("predictions$true").asDoubles();
		    
		} catch (REXPMismatchException e) {
			e.printStackTrace();
		} catch (REngineException e) {
			e.printStackTrace();
		}

	    return predictions;
	}
	/* @TODO
	public void train(){
		String columns = "AttributeSource, AttributeCatalog, JSDw, JCw, MIw, Match";
	    // new R-engine
	    Rengine re=new Rengine (new String [] {"--vanilla"}, false, null);
	    if (!re.waitForR())
	    {
	      System.out.println ("Cannot load R");
	      return;
	    }

	    // print a random number from uniform distribution
	    re.eval ("library(caret)");
	    re.eval ("load('my_model.rda')");
	    re.eval ("matchData <- read.csv('dataFrame.csv',header=TRUE)");
	    re.eval ("dataFrame <- subset(matchData, select=-c("+columns+"))");
	    re.eval ("predictions <- predict(model1, dataFrame, type = 'prob')");
	    re.eval ("newFrame <- cbind(matchData, predictions$true)");
	    re.eval ("colnames(newFrame)[13] <- 'prob'");
	    re.eval ("newFrame <- newFrame[(newFrame[,13]>=0.5),]");

	    // done...
	    re.end();
	}*/
	
}
