package matcher;

import static org.junit.Assert.assertEquals;
import static testutils.TestUtils.entry;
import static testutils.TestUtils.spec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import org.junit.Test;

import model.AbstractProductPage.Specifications;
import model.SourceProductPage;
import models.matcher.BagsOfWordsManager;
import testutils.TestUtils;

/**
 * Test on the {@link FeatureExtractor} class
 * @author federico
 *
 */
public class FeatureExtractorTest {
	
	@Test
	public void testSameTwoValues() {
		List<Entry<Specifications, SourceProductPage>> docs = Arrays.asList(
				entry(spec(entry("a1", "A")), TestUtils.spp(entry("a2", "A"))),
				entry(spec(entry("a1", "B")), TestUtils.spp(entry("a2", "B"))));
		assertFeatures(docs, "a1", "a2", 1.0, 1.0, 0.0);
	}
	
	@Test
	public void testSameOneValue() {
		List<Entry<Specifications, SourceProductPage>> docs = Arrays.asList(
				entry(spec(entry("a1", "A")), TestUtils.spp(entry("a2", "A"))),
				entry(spec(entry("a1", "B")), TestUtils.spp(entry("a2", "B"))));
		assertFeatures(docs, "a1", "a2", 1.0, 1.0, 0.0);
	}

	@Test
	public void testComplex() {
		List<Entry<Specifications, SourceProductPage>> docs = Arrays.asList(
				entry(spec(entry("a1", "A")), TestUtils.spp(entry("a2", "A"))),
				entry(spec(entry("a1", "A")), TestUtils.spp(entry("a2", "B"))),
				entry(spec(entry("a1", "C")), TestUtils.spp(entry("a2", "C"))),
				entry(spec(entry("a1", "C")), TestUtils.spp(entry("a2", "D"))),
				entry(spec(entry("a1", "E")), TestUtils.spp(entry("a2", "E"))),
				entry(spec(entry("a1", "E###F")), TestUtils.spp(entry("a2", "F")))
				);
		assertFeatures(docs, "a1", "a2", 1.664, 0.667, 0.198);
	}
	
	@Test
	public void testEmptyValueStrings() {
		List<Entry<Specifications, SourceProductPage>> docs = Arrays.asList(
				entry(spec(entry("a1", "###")), TestUtils.spp(entry("a2", "A"))),
				entry(spec(entry("a1", "###")), TestUtils.spp(entry("a2", "")))
				);
		assertFeatures(docs, "a1", "a2", 0.0, 0.5, 0.948);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testEmptyDocuments() {
		List<Entry<Specifications, SourceProductPage>> docs = new ArrayList<>();
		assertFeatures(docs, "a1", "a2", 0.0, 0.0, 0.0);
	}

	/**
	 * Compute all features and compare with expected
	 * @param docs
	 * @param a1
	 * @param a2
	 * @param expectedMi
	 * @param expectedJC
	 * @param expectedJSD
	 */
	private void assertFeatures(List<Entry<Specifications, SourceProductPage>> docs, String a1,
			String a2, double expectedMi, double expectedJC, double expectedJSD) {
		BagsOfWordsManager sBags = new BagsOfWordsManager(a1, a2, docs);
		assertEquals(expectedMi, FeatureExtractor.getMI(docs, a1, a2), 0.001);
		assertEquals(expectedJC,  FeatureExtractor.getJC(sBags), 0.001);
		assertEquals(expectedJSD,  FeatureExtractor.getJSD(sBags),0.001);
	}
}
