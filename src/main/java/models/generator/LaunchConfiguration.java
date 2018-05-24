package models.generator;

import java.util.Arrays;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;

import connectors.FileDataConnector;

/**
 * Defines a configuration to use while launching the script, each element has a
 * corresponding configuration file
 * 
 * @author federico
 * 
 */
public class LaunchConfiguration {
	
	private Configurations conf;
	private FileDataConnector fdc;
	
	private LaunchConfiguration(Configurations conf, FileDataConnector fdc) {
		super();
		this.conf = conf;
		this.fdc = fdc;
	}

	public Configurations getConf() {
		return conf;
	}

	public FileDataConnector getFdc() {
		return fdc;
	}
	
	
	public static LaunchConfiguration setupConfiguration(String[] args) {
		// Setup
		System.out.println("INIZIO SETUP");
		ConfigurationVersions cv = ConfigurationVersions.getConfigurationFromArgs(args);
		FileDataConnector fdc = new FileDataConnector(cv.getConfigFile());
		Configurations config = new Configurations(fdc.readConfig());
		fdc.setDatasetPath(config.getDatasetPath());
		fdc.setRlPath(config.getRecordLinkagePath());
		fdc.setTsPath(config.getTrainingSetPath());
		LaunchConfiguration lc = new LaunchConfiguration(config, fdc);
		System.out.println("FINE SETUP");
		return lc;
	}
	
	public enum ConfigurationVersions {
		/**
		 * Generate full dataset
		 */
		PROD("src/main/resources/config_prod.properties"),
	
		/**
		 * Used to test, in particular it produces a little dataset
		 */
		TEST("src/main/resources/config_test.properties"),
	
		/**
		 * Customizable file that is ignored in git
		 */
		CUSTOM("src/main/resources/config_custom.properties");
	
		private static ConfigurationVersions defaultConf = TEST;
		private String configFile;
	
		private ConfigurationVersions(String configFile) {
			this.configFile = configFile;
		}
	
		public String getConfigFile() {
			return configFile;
		}
	
		public static ConfigurationVersions getConfigurationFromArgs(String[] args) {
			ConfigurationVersions res;
			if (args.length >= 1 && StringUtils.isNotBlank(args[0])) {
				if (EnumUtils.isValidEnum(ConfigurationVersions.class, args[0])) {
					res = ConfigurationVersions.valueOf(args[0]);
				} else {
					throw new IllegalArgumentException(
							"Invalid launch configuration. Valid ones are " + Arrays.asList(ConfigurationVersions.values()));
				}
			} else {
				res = defaultConf;
			}
			return res;
		}
	}
}
