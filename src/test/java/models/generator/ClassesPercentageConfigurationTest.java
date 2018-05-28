package models.generator;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

/**
 * Tester for {@link ClassesPercentageConfiguration}
 * 
 * @author federico
 *
 */
public class ClassesPercentageConfigurationTest {

	@Test
	public void testAssignClassesStandard() {
		ClassesPercentageConfiguration<Integer> classes = generateClasses(Arrays.asList(1, 2, 3, 4), 9.9, 50.1, 20.,
				20.);
		Map<String, Integer> assigned = classes
				.assignClasses(Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j"));
		Collection<Integer> allValues = assigned.values();
		assertEquals(1, Collections.frequency(allValues, 1));
		assertEquals(5, Collections.frequency(allValues, 2));
		assertEquals(2, Collections.frequency(allValues, 3));
		assertEquals(2, Collections.frequency(allValues, 4));
	}

	@Test
	/**
	 * Here last class, 4, will have 1 attribute affected even if its percentage is
	 * too low (because we have to assign a class to the last attribute, isolated
	 * because of approximations)
	 */
	public void testAssignClassesTooManyAttributes() {
		ClassesPercentageConfiguration<Integer> classes = generateClasses(Arrays.asList(1, 2, 3, 4), 80., 9.9, 9.9,
				0.2);
		Map<String, Integer> assigned = classes.assignClasses(Arrays.asList("a", "b", "c", "d", "e"));
		Collection<Integer> allValues = assigned.values();
		assertEquals(4, Collections.frequency(allValues, 1));
		// Note that one attribute is assigned to last class, as we need to fill last
		// element
		assertEquals(1, Collections.frequency(allValues, 4));
	}

	@Test
	/**
	 * Here the last class remains without attributes even if its percentage would
	 * be enough (still because of approximations)
	 */
	public void testAssignClassesTooManyClasses() {
		ClassesPercentageConfiguration<Integer> classes = generateClasses(Arrays.asList(1, 2, 3), 50.1, 30.1, 19.8);
		Map<String, Integer> assigned = classes.assignClasses(Arrays.asList("a", "b", "c", "d", "e"));
		Collection<Integer> allValues = assigned.values();

		assertEquals(3, Collections.frequency(allValues, 1));
		assertEquals(2, Collections.frequency(allValues, 2));
	}

	@Test(expected = IllegalArgumentException.class)
	/**
	 * Here percentages are wrong
	 */
	public void testPercentagesTooHigh() {
		generateClasses(Arrays.asList(1, 2, 3), 50.1, 30.1, 20.1);
	}

	@Test(expected = IllegalArgumentException.class)
	/**
	 * Here percentages are wrong
	 */
	public void testPercentagesTooLow() {
		generateClasses(Arrays.asList(1, 2, 3), 49.9, 29.9, 19.9);
	}

	private static <T> ClassesPercentageConfiguration<T> generateClasses(List<T> classes, Double... elements) {
		Map<T, Double> result = new HashMap<>();
		for (int i = 0; i < classes.size(); i++) {
			result.put(classes.get(i), elements[i]);
		}
		return new ClassesPercentageConfiguration<>(result);
	}

}
