package connectors;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import models.matcher.DataFrame;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPDouble;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngine;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.REngineStdOutput;
import org.rosuda.REngine.RList;

public class RConnector {
	
	private REngine eng = null;
	
	public RConnector(){
	}
	
	public void start(){
		//start REngine
		try {
			/*for debugging*/
			this.eng = REngine.engineForClass("org.rosuda.REngine.JRI.JRIEngine", new String [] {"--vanilla"},
									new REngineStdOutput(), false);
//			this.eng = REngine.engineForClass("org.rosuda.REngine.JRI.JRIEngine");
		    //load caret package
		    this.eng.parseAndEval("library(caret)");
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
	
	public void loadModel(String modelPath){
	    //load classifier model
	    try {
			this.eng.parseAndEval("load('"+modelPath+"')");
		} catch (REngineException | REXPMismatchException e) {
			e.printStackTrace();
		}
	}
	
	//generates classifier model
	public void train(String tsPath, String modelPath){
		try {
			//read training set
			this.eng.parseAndEval("data <- read.csv(\""+tsPath+"\",header=TRUE)");
			this.eng.parseAndEval("data$Match <- as.factor(data$Match)");
			this.eng.parseAndEval("levels(data$Match) <- c(\"false\", \"true\")");
			this.eng.parseAndEval("data$Match <- factor(data$Match, levels=c(\"true\",\"false\"))");
			//remove unnecessary columns
			this.eng.parseAndEval("dataSub <- subset(data, select=-c(Attribute1, Attribute2, Website1,"
					+ " Website2, Category, JSDw, JCw, MIw))");
			//trainControl setup
			this.eng.parseAndEval("tc <- trainControl(method=\"repeatedcv\", number=10, repeats=50, classProbs=TRUE,"
					+ " savePredictions=TRUE, summaryFunction=twoClassSummary)");
			//training: logistic regression with Area under ROC as metric
			this.eng.parseAndEval("modelClassifier <- train(Match~., data=dataSub, trControl=tc, method=\"glm\","
					+ " family=binomial(link=\"logit\"), metric=\"ROC\")");
			//save model to file to avoid retraining
			this.eng.parseAndEval("save(modelClassifier, file = \""+modelPath+"\")");
			
		} catch (REngineException | REXPMismatchException e) {
			e.printStackTrace();
		}
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
		    this.eng.parseAndEval("predictions <- predict(modelClassifier, dataFrame, type = 'prob')");
		    //System.out.println(eng.parseAndEval("print(predictions$true)"));
		    predictions = this.eng.parseAndEval("predictions$true").asDoubles();
		    
		} catch (REXPMismatchException e) {
			e.printStackTrace();
		} catch (REngineException e) {
			e.printStackTrace();
			try {
				System.in.read();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			System.out.println(df.getJSDs().toString());
		}

	    return predictions;
	}
	
}
