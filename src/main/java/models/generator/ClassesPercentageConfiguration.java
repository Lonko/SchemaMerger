package models.generator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiConsumer;

import org.apache.commons.lang3.Validate;

/**
 * Configuration that provide different classes and the percentage for each class.<p>
 * The sum of percentages must be equal to 100
 * 
 * @author federico
 *
 * @param <T> type of attribute in class
 */
public class ClassesPercentageConfiguration<T> {
	private Map<T, Double> class2percentage;

	public ClassesPercentageConfiguration(Map<T, Double> class2percentage) {
		double sum = class2percentage.values().stream().mapToDouble(Double::doubleValue).sum();
		Validate.inclusiveBetween(100, 100, sum);
		this.class2percentage = class2percentage;
	}
	
	/**
	 * Assigns to each attribute a class, keeping the percentages provided<p>
	 * E.g:
	 * <ul>
	 * <li>Classes are A-40%, B-40%, C-20%<br/>
	 * <li>Attributes are aa,bb,cc,dd,ee
	 * <li>Output: aa-A, bb-A, cc-B, dd-B, ee-C
	 * </ul>
	 * Note that there are some approximation issues that are addressed 
	 * 
	 * @param attrs
	 * 
	 * @return 
	 */
	public <Q> void assignClasses(List<Q> attrs, BiConsumer<Q, T> action) {
		
		List<Q> shuffled = new ArrayList<>(attrs);
		Collections.shuffle(shuffled);
		Iterator<Q> attributeIterator = shuffled.iterator();
		Iterator<Entry<T, Double>> classesIterator = this.class2percentage.entrySet().iterator();
		while (classesIterator.hasNext()) {
			Entry<T, Double> currentClass = classesIterator.next();
			long numberOfElementsInThisClass = Math.round(currentClass.getValue() * shuffled.size() / 100);
			
			
			//Last class may have  not enough attributes to affect (because of approximations), we then check the attribute iterator...
			for (int i = 0; i < numberOfElementsInThisClass && attributeIterator.hasNext(); i++) {
				action.accept(attributeIterator.next(), currentClass.getKey());
			}
			
			//...or there may be some attributes still to assign, we affect them to last class
			if (!classesIterator.hasNext()) {
				while(attributeIterator.hasNext()) {
					action.accept(attributeIterator.next(), currentClass.getKey());
				}
			}
		}
	}
	
	/**
	 * Assign attribute to classes using an external map
	 * @param attrs
	 * @return
	 */
	public <Q> Map<Q, T> assignClasses(List<Q> attrs) {
		Map<Q, T> result = new HashMap<>();
		assignClasses(attrs, (attr, val) ->  result.put(attr, val) );
		return result;
	}

	public Set<T> getClasses() {
		return this.class2percentage.keySet();
	}

	//For tests
	protected Map<T, Double> getClass2percentage() {
		return class2percentage;
	}	
}
