package models.generator;

/**
 * Builder for classes of attribute's cardinalities (Ex: 2/3/5/10/50/200)
 * @author federico
 *
 */
public class CardinalitiesClassBuilder extends AbstractClassesPercentageBuilder<Integer> {

	@Override
	protected Integer convertStringToClass(String classString) {
		return Integer.valueOf(classString);
	}

	@Override
	public String name() {
		return "Attribute cardinalities";
	}

}
