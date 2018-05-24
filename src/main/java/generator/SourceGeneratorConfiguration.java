package generator;

import java.util.List;

import models.generator.Configurations.RecordLinkageErrorType;
import models.generator.CurveFunctionFactory.CurveFunctionType;

/**
 * Configuration for {@link SourcesGenerator}
 * 
 * @author federico
 *
 */
public interface SourceGeneratorConfiguration {

	public CurveFunctionType getAttrCurveType();

	public 	int getSources();

	public int getAttributes();

	public double getRandomErrorChance();

	public double getDifferentFormatChance();

	public double getDifferentRepresentationChance();

	public double getMissingLinkageChance();

	public double getLinkageErrorChance();

	public RecordLinkageErrorType getRlErrorType();

	public List<String> getCategories();

}
