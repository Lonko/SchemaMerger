package models.generator;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.Validate;

/**
 * Converts an input from a configuration to a
 * {@link ClassesPercentageConfiguration} file.
 * <p>
 * The expected input are 2 strings (config parameter) of this form:
 * <p>
 * <code>
 * xxxClasses=class1/class2/class3<br/>
 * xxxPercentages=70.0/25.0/5.0</code>
 * <p>
 * Where class1... must respect a specific format, according to subclass
 * 
 * @author federico
 *
 */
public abstract class AbstractClassesPercentageBuilder<T> {

	public ClassesPercentageConfiguration<T> generateClassesPercentage(String classes, String percentages) {
		String[] classArray = classes.split("/");
		String[] percentageArray = percentages.split("/");
		Validate.inclusiveBetween(classArray.length, classArray.length, percentageArray.length,
				"Classes and percentages don't match for configuration " + name());
		Map<T, Double> mapOfElements = new HashMap<>();
		
		try {
			for (int i = 0; i < classArray.length; i++) {
				T element = convertStringToClass(classArray[i]);
				double percentage = Double.valueOf(percentageArray[i]);
				mapOfElements.put(element, percentage);
			}
			return new ClassesPercentageConfiguration<>(mapOfElements);
		} catch (Exception e) {
			//TODO more fine-grained controls?
			throw new IllegalArgumentException("Malformed values in parameters for "+name()+", error: "+e.getMessage(), e);
		}
	}

	protected abstract T convertStringToClass(String classString);

	public abstract String name();
}
