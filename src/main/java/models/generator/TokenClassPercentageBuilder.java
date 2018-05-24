package models.generator;

/**
 * Builder for percentage classes of type {@link TokenClass}
 * @author federico
 *
 */
public class TokenClassPercentageBuilder extends AbstractClassesPercentageBuilder<TokenClass> {

	@Override
	protected TokenClass convertStringToClass(String classString) {
		String[] split = classString.split("-");
		return new TokenClass(Integer.valueOf(split[0]), Integer.valueOf(split[1]));
	}

	@Override
	public String name() {
		return "Token classes";
	}

}
