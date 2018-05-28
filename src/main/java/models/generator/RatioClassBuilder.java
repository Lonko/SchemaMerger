package models.generator;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.Validate;

/**
 * Builder for {@link ClassesPercentageConfiguration} for ratio
 * @author federico
 *
 */
public class RatioClassBuilder extends AbstractClassesPercentageBuilder<Double> {

	@Override
	protected Double convertStringToClass(String classString) {
		double ratio = Double.valueOf(classString);
		Validate.inclusiveBetween(0.0, 1.0, ratio);
		return ratio;
	}

	@Override
	public String name() {
		return "RatioClassBuilder" ;
	}
	
	/**
	 * Helper to build a fixed ratio percentage, ignoring configuration
	 * @param ratio
	 * @return
	 */
	public static ClassesPercentageConfiguration<Double> buildFixedRatio(double ratio){
		Map<Double, Double> class2percentage = new HashMap<>();
		class2percentage.put(ratio, 100.);
		ClassesPercentageConfiguration<Double> res = new ClassesPercentageConfiguration<>(class2percentage);
		return res;
	}

}
