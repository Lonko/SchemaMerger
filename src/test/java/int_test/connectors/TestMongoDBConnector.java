package int_test.connectors;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.bson.Document;
import org.junit.BeforeClass;
import org.junit.Test;

import connectors.MongoDBConnector;

public class TestMongoDBConnector {
	
	private static MongoDBConnector connector;
	
	@BeforeClass
	public static void connect() {
		connector = new MongoDBConnector(null); 
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testTopLinkage() {
		connector.copySchema();
		Document d = connector.getTopSource();
		List<?> linkages = d.get("attributes", List.class);
		String website = d.getString("website");
		List<String> results = new LinkedList<>();
		results.add(website);
		
		Set<String> allIds = new HashSet<String>((List<? extends String>) linkages);
		
		
		long size = connector.removeSourceTempSendSize(d);
		while (size > 0) {
			d = connector.getTopLinkage(new LinkedList<>(allIds));
			linkages = d.get("attributes", List.class);
			website = d.getString("website");
			results.add(website);
			allIds.addAll((List<? extends String>) linkages);
			size = (int) connector.removeSourceTempSendSize(d);
		}
		System.out.println(results);
	}

}
