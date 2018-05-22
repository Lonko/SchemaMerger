package models.generator;

import java.util.Arrays;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Defines a configuration to use while launching the script, each element has a
 * corresponding configuration file
 * 
 * @author federico
 * 
 */
public enum LaunchConfiguration {

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

	private static LaunchConfiguration defaultConf = TEST;
	private String configFile;

	private LaunchConfiguration(String configFile) {
		this.configFile = configFile;
	}

	public String getConfigFile() {
		return configFile;
	}

	public static LaunchConfiguration getConfigurationFromArgs(String[] args) {
		LaunchConfiguration res;
		if (args.length >= 1 && StringUtils.isNotBlank(args[0])) {
			if (EnumUtils.isValidEnum(LaunchConfiguration.class, args[0])) {
				res = LaunchConfiguration.valueOf(args[0]);
			} else {
				throw new IllegalArgumentException(
						"Invalid launch configuration. Valid ones are " + Arrays.asList(LaunchConfiguration.values()));
			}
		} else {
			res = defaultConf;
		}
		return res;
	}

	/**
	 * Test function
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println(getConfigurationFromArgs(new String[] { "PROD" }).toString());
		System.out.println(getConfigurationFromArgs(new String[] { "TEST" }).toString());
		System.out.println(getConfigurationFromArgs(new String[] { "CUSTOM", "opop" }).toString());
		System.out.println(getConfigurationFromArgs(new String[] { "" }).toString());
		System.out.println(getConfigurationFromArgs(new String[] {}).toString());
		System.out.println(getConfigurationFromArgs(new String[] { "fgsdfgdf" }).toString());
	}
}
