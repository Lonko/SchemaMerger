package connectors.dao;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import model.Source;
import model.SourceProductPage;

public class AlignmentDaoMock implements AlignmentDao {
	
	public static String MOCKED_WEBSITE1 = "mock1.com";
	public static List<String> MOCKED_WEBSITES = Arrays.asList(MOCKED_WEBSITE1, "mock2.com","mock3.com",  "mock4.com");
	private int val = 0; //value of attributes, not constant but deterministic

	@Override
	public List<SourceProductPage> getRLSample(int size, String category) {
		System.out.printf("Called getRLSample with size %d and category %s\n",size, category);
		return buildProdList(size, category, MOCKED_WEBSITE1, null, null);
	}
	
	private List<SourceProductPage> buildProdList(int size, String category, String website, String website2_link, String att1) {
		website = StringUtils.isNotEmpty(website) ?  website: null;
		List<SourceProductPage> prods = new LinkedList<>();
		for (int i = 0; i < size; i++) {
			String websiteCurrent = ObjectUtils.firstNonNull(website, MOCKED_WEBSITES.get(i % MOCKED_WEBSITES.size()));
			SourceProductPage prodPage = new SourceProductPage(category, buildUrl(websiteCurrent, i), websiteCurrent);
			if (website2_link != null) {
				prodPage.getLinkage().add(buildUrl(website2_link, i));
			}
			if (att1 != null) {
				prodPage.addAttributeValue(att1, String.valueOf(i));
			}
			prods.add(prodPage);
		}
		return prods;
	}

	private String buildUrl(String website, int num) {
		return website+"/"+num;
	}

	@Override
	public Map<Source, List<String>> getSchemas(List<String> categories) {
		System.out.printf("Called getSchemas with categories %s\n",categories.toString());
		return null;
	}

	@Override
	public List<String> getSingleSchema(Source source) {
		System.out.printf("Called getSingleSchema with source %s\n",source);
		return null;
	}

	@Override
	public Map<SourceProductPage, List<SourceProductPage>> getProdsInRL(List<String> websites, String category) {
		System.out.printf("Called getProdsInRL with websites %d and category %s\n",websites.toString(), category);
		return null;
	}

	@Override
	public List<SourceProductPage> getProds(String category, String website1, String website2, String attribute1) {
		System.out.printf("Called getProds with category %s and website1 %s and website2 %s and attribute1 %s\n",category, website1, website2, attribute1);
		return buildProdList(5, category, website1, website2, attribute1);
	}

	@Override
	public List<Entry<SourceProductPage, SourceProductPage>> getProdsInRL(List<SourceProductPage> cList1,
			String website2, String attribute2) {
		System.out.printf("Called getProdsInRL with cList1 %s, website2 %s and attribute2 %s\n",cList1.toString(), website2, attribute2);

		List<Entry<SourceProductPage, SourceProductPage>> results = new LinkedList<>();
		for (SourceProductPage spp : cList1) {
			for (String url: spp.getLinkage()) {
				if (url.contains(website2)) {
					SourceProductPage spp_link = new SourceProductPage(spp.getSource().getCategory(), url, website2);
					spp_link.addAttributeValue(attribute2, String.valueOf(val));
					results.add(new AbstractMap.SimpleEntry<>(spp, spp_link));
				}
				val++;
			}
		}
		return results;
	}

	@Override
	public SourceProductPage getIfValid(String url) {
		System.out.printf("Called getIfValid with url %s\n",url);
		return null;
	}

}
