package matcher;

import static testutils.TestUtils.entry;
import static testutils.TestUtils.spec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import connectors.dao.AlignmentDaoMock;
import junit.framework.Assert;
import model.AbstractProductPage.Specifications;
import model.SourceProductPage;
import models.matcher.Features;
import models.matcher.Tuple;

/**
 * Test on {@link TrainingSetGenerator}
 * @author federico
 *
 */
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
	private FeaturesBuilder fb;
	private AlignmentDaoMock dao;

	@Before
	public void setUp() throws Exception {
		Map<String, List<String>> clSources = new HashMap<>();
		clSources.put("aaa", Arrays.asList("a", "b", "c"));
		this.fb = Mockito.mock(FeaturesBuilder.class);
		dao = new AlignmentDaoMock();
		trainingSetGenerator = new TrainingSetGenerator(fb, dao, clSources);
		this.pExamples = Arrays.asList(new Tuple(A1, A1, WEBSITE_TEST_1, WEBSITE_TEST_2, CATEGORY),
				new Tuple(A1_2, A2_2, WEBSITE_TEST_1, WEBSITE_TEST_2, CATEGORY),
				new Tuple(A1, A1, WEBSITE_TEST_1, AlignmentDaoMock.MOCKED_WEBSITE1, CATEGORY),
				new Tuple(A1_2, A2_2, WEBSITE_TEST_1, AlignmentDaoMock.MOCKED_WEBSITE1, CATEGORY),
				new Tuple(A1, A1, AlignmentDaoMock.MOCKED_WEBSITES.get(1), AlignmentDaoMock.MOCKED_WEBSITE1, CATEGORY),
				new Tuple(A1_2, A2_2, AlignmentDaoMock.MOCKED_WEBSITES.get(1), AlignmentDaoMock.MOCKED_WEBSITE1,
						CATEGORY)

		);
		this.nExamples = Arrays.asList(new Tuple(A1, A2, WEBSITE_TEST_1, WEBSITE_TEST_2, CATEGORY),
				new Tuple(A1, A2, AlignmentDaoMock.MOCKED_WEBSITES.get(2), WEBSITE_TEST_2, CATEGORY));
	}

	/* Risultati attesi:
	 * cList: [a1/mocks --> a1/web2_linked] ; slist:[]
	 * cList: [a1_2/mocks --> a2_2 --> web2_linked] ; slist:[]
	 * cList: [a1/mocks --> a1 --> mock1] ; slist:[]
	 * cList: [a1_2/mocks --> a2_2 --> mock1] ; slist:[]
	 * cList: [a1_2/mocks --> a2_2 --> mock1] ; slist:[]
	 * cList: [a1/mocks --> a1 --> mock2] ; slist:[a1/mock1 --> a1/mock2]
	 * cList: [a1_2/mocks --> a2_2 --> mock2] ; slist:[a1_2/mock1 --> a2_2/mock2]
	 */
	
	/**
	 * We expect to obtain, for each tuple T1:<ul>
	 * <li>Pair of pages a1[w1] ---> w2, a2
	 * <li>Pair of pages a1[any website] ---> w2, a2
	 * </ul>
	 * Note that w1 is not provided in output
	 */
	@Test
	public void testGetTrainingSet() {
		System.out.println("Input: " + pExamples + "\n\nNEG: " + nExamples);
		List<Features> res = this.trainingSetGenerator.computeFeaturesOnTrainingSet(pExamples, nExamples, CATEGORY);
		
		//Pos examples
		
		Mockito.verify(this.fb).computeFeatures(
				new ArrayList<>(), 
				fullCList(A1, A1, WEBSITE_TEST_2),A1, A1, 1.0d);
		
		Mockito.verify(this.fb).computeFeatures(
				new ArrayList<>(), 
				fullCList(A1_2, A2_2, WEBSITE_TEST_2),A1_2, A2_2, 1.0d);	
		
		Mockito.verify(this.fb).computeFeatures(
				new ArrayList<>(), 
				fullCList(A1, A1, AlignmentDaoMock.MOCKED_WEBSITE1),A1, A1, 1.0d);
		
		Mockito.verify(this.fb).computeFeatures(
				new ArrayList<>(), 
				fullCList(A1_2, A2_2, AlignmentDaoMock.MOCKED_WEBSITE1),A1_2, A2_2, 1.0d);			
		
		Mockito.verify(this.fb).computeFeatures(
				Arrays.asList(pair(spec(entry(A1, "1")), 1, AlignmentDaoMock.MOCKED_WEBSITE1, spec(entry(A1, "0")))),
				fullCList(A1, A1, AlignmentDaoMock.MOCKED_WEBSITE1),A1, A1, 1.0d);
		
		Mockito.verify(this.fb).computeFeatures(
				Arrays.asList(pair(spec(entry(A1_2, "1")), 1, AlignmentDaoMock.MOCKED_WEBSITE1, spec(entry(A2_2, "0")))),
				fullCList(A1_2, A2_2, AlignmentDaoMock.MOCKED_WEBSITE1),A1_2, A2_2, 1.0d);
		
		//Neg examples
		
		Mockito.verify(this.fb).computeFeatures(
				new ArrayList<>(), 
				fullCList(A1, A2, WEBSITE_TEST_2),A1, A2, 0.0d);
		
		Mockito.verify(this.fb).computeFeatures(
				Arrays.asList(pair(spec(entry(A1, "2")), 2, WEBSITE_TEST_2, spec(entry(A2, "0")))),
				fullCList(A1, A2, WEBSITE_TEST_2),A1, A2, 0.0d);
		

		
		Assert.assertEquals(8, res.size());
		System.out.println(res);
	}
	
	public List<Entry<Specifications, SourceProductPage>> fullCList(String a1, String a2, String website2) {
		return Arrays.asList(
				pair(spec(entry(a1, "0")), 0, website2, spec(entry(a2, "0"))),
				pair(spec(entry(a1, "1")), 1, website2, spec(entry(a2, "1"))),
				pair(spec(entry(a1, "2")), 2, website2, spec(entry(a2, "2"))),
				pair(spec(entry(a1, "3")), 3, website2, spec(entry(a2, "3"))),
				pair(spec(entry(a1, "4")), 4, website2, spec(entry(a2, "4"))));
	}
	
	public Entry<Specifications, SourceProductPage> pair(Specifications s1, int i, 
			String website2, Specifications spec2) {
		SourceProductPage value = new SourceProductPage(CATEGORY, dao.buildUrl(website2, i), website2);
		value.getSpecifications().putAll(spec2);
		return entry(s1, value);
	}

}
