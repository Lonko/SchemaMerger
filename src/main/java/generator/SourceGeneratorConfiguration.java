package generator;

import java.util.List;

import models.generator.ClassesPercentageConfiguration;
import models.generator.Configurations.RecordLinkageErrorType;

/**
 * Configuration for {@link SourcesGenerator}
 * 
 * @author federico
 *
 */
public interface SourceGeneratorConfiguration {

	public 	int getSources();

	public int getAttributes();

	public double getDifferentFormatChance();

	public double getDifferentRepresentationChance();
	
	public ClassesPercentageConfiguration<Double> getValueErrorChanceClasses();

	public ClassesPercentageConfiguration<Double> getLinkageErrorChanceClasses();
	
	public ClassesPercentageConfiguration<Double> getMissingLinkageChanceClasses();	

	public RecordLinkageErrorType getRlErrorType();

	public List<String> getCategories();

}
