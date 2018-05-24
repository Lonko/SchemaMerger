package models.generator;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

/**
 * Tester for {@link TokenClassPercentageBuilder}.
 * @author federico
 *
 */
public class TokenClassPercentageBuilderTest {
	
	private TokenClassPercentageBuilder builder;

	@Before
	public void setUp() throws Exception {
		this.builder = new TokenClassPercentageBuilder();
	}

	@Test
	public void testGenerateClassesPercentage() {
		ClassesPercentageConfiguration<TokenClass> generateClassesPercentage = this.builder.generateClassesPercentage("1-2/3-4/5-6", "20.0/70.0/10");
		assertToken(1,2,20, generateClassesPercentage);
		assertToken(3,4,70, generateClassesPercentage);
		assertToken(5,6,10, generateClassesPercentage);
	}

	/**
	 * Check the token classes
	 * @param random
	 * @param fixed
	 * @param percentageExpected
	 * @param generateClassesPercentage
	 */
	private void assertToken(int random , int fixed, double percentageExpected,
			ClassesPercentageConfiguration<TokenClass> generateClassesPercentage) {
		double percentageActual = generateClassesPercentage.getClass2percentage().get(new TokenClass(random, fixed));
		assertEquals(percentageExpected, percentageActual, 0.01);
	}

}
