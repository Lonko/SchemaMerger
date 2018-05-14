package launchers;

import java.util.Scanner;

import connectors.FileDataConnector;
import connectors.MongoDbConnectionFactory;
import connectors.RConnector;
import connectors.dao.AlignmentDao;
import connectors.dao.MongoAlignmentDao;
import models.generator.Configurations;
import models.generator.LaunchConfiguration;

/**
 * Launch controller for Agrawal
 * 
 * @author federico
 *
 */
public class Cohordinator {


    public static void main(String[] args) {
        System.out.println("UTILIZZARE DATASET SINTETICO? (S/N)");
        boolean useSynthDataset = false;
        try(Scanner scanner = new Scanner(System.in)){
        	useSynthDataset = Character.toLowerCase(scanner.next().charAt(0)) == 's';
        }

        // Setup
        System.out.println("INIZIO SETUP");
        LaunchConfiguration lc = LaunchConfiguration.getConfigurationFromArgs(args);
        FileDataConnector fdc = new FileDataConnector(lc.getConfigFile());
        Configurations config = new Configurations(fdc.readConfig());
        fdc.setDatasetPath(config.getDatasetPath());
        fdc.setRlPath(config.getRecordLinkagePath());
        fdc.setTsPath(config.getTrainingSetPath());
        RConnector r = new RConnector(config.getModelPath());
        System.out.println("FINE SETUP");
        AlignmentDao connector = getDao(config);
        DatasetAlignmentAlgorithm algorithm = new DatasetAlignmentAlgorithm(connector, fdc, r, config);
        
        //SCHEMA ALIGNMENT
        if(useSynthDataset)
        	algorithm.alignmentSyntheticDataset();
        else
        	algorithm.alignmentRealDataset();
    }

    /**
     * Returns the DAO for algorithm
     * TODO use proper dependency injection
     * 
     * @param config
     * @return
     */
	private static AlignmentDao getDao(Configurations config) {
		MongoDbConnectionFactory factory = MongoDbConnectionFactory.getMongoInstance(config.getMongoURI(), config.getDatabaseName());
        AlignmentDao connector = new MongoAlignmentDao(factory);
		return connector;
	}
}
