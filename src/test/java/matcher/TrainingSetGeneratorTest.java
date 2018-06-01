package matcher;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import connectors.dao.AlignmentDaoMock;
import junit.framework.Assert;
import models.matcher.Features;
import models.matcher.Tuple;

public class TrainingSetGeneratorTest {

	private static final String A2 = "a2";
	private static final String A2_2 = "a2_2";
	private static final String A1_2 = "a1_2";
	private static final String A1 = "a1";

	private static final String WEBSITE_TEST_1 = "web1.com";
	private static final String WEBSITE_TEST_2 = "web2_linked.com";
	private static final String CATEGORY = "category";
	private TrainingSetGenerator trainingSetGenerator;
	List<Tuple> pExamples;
	List<Tuple> nExamples;

	@Before
	public void setUp() throws Exception {
		Map<String, List<String>> clSources = new HashMap<>();
		clSources.put("aaa", Arrays.asList("a", "b", "c"));
		trainingSetGenerator = new TrainingSetGenerator(
				new AlignmentDaoMock(), new FeatureExtractor(), clSources);
		this.pExamples = Arrays.asList(new Tuple(A1, A1, WEBSITE_TEST_1, WEBSITE_TEST_2, CATEGORY),
				new Tuple(A1_2, A2_2, WEBSITE_TEST_1, WEBSITE_TEST_2, CATEGORY),
				new Tuple(A1, A1, WEBSITE_TEST_1, AlignmentDaoMock.MOCKED_WEBSITE1, CATEGORY),
				new Tuple(A1_2, A2_2, WEBSITE_TEST_1, AlignmentDaoMock.MOCKED_WEBSITE1, CATEGORY),
				new Tuple(A1, A1, AlignmentDaoMock.MOCKED_WEBSITES.get(1), AlignmentDaoMock.MOCKED_WEBSITE1, CATEGORY),
				new Tuple(A1_2, A2_2, AlignmentDaoMock.MOCKED_WEBSITES.get(1), AlignmentDaoMock.MOCKED_WEBSITE1,
						CATEGORY)

		);
		this.nExamples = Arrays.asList(new Tuple(A1, A2, WEBSITE_TEST_1, WEBSITE_TEST_2, CATEGORY),
				new Tuple(A1_2, A2_2, WEBSITE_TEST_1, WEBSITE_TEST_2, CATEGORY));
	}

	@Test
	@Ignore
	/** Da sistemare per eventualmente usarlo */
	public void testGetTrainingSet() {
		System.out.println("Input: " + pExamples + "\n\nNEG: " + nExamples);
		List<Features> res = this.trainingSetGenerator.computeFeaturesOnTrainingSet(pExamples, nExamples, CATEGORY);
		Assert.assertEquals(8, res.size());
		System.out.println(res);
	}

}
