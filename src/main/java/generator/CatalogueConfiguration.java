package generator;

import models.generator.ClassesPercentageConfiguration;
import models.generator.TokenClass;
import models.generator.CurveFunctionFactory.CurveFunctionType;

/**
 * Configuration of the catalogue generator
 * @author federico
 *
 */
public interface CatalogueConfiguration {

	public int getMaxPages();

	public int getMinPages();

	public int getSources();

	public int getMaxLinkage();

	public int getAttributes();

	public CurveFunctionType getSizeCurveType();

	public CurveFunctionType getProdCurveType();
	
	public ClassesPercentageConfiguration<Integer> getCardinalityClasses();

	public ClassesPercentageConfiguration<TokenClass> getTokenClasses();

}
