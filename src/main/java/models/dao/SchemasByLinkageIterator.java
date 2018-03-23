package models.dao;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.bson.Document;

import connectors.MongoDBConnector;

/**
 * Iterate over dataset to get sources with most linkages with preceding ones (iteratively)
 * @author federico
 *
 */
public class SchemasByLinkageIterator implements Iterator<String> {
	
	private MongoDBConnector connector;
	private boolean firstSource;
	private Set<String> allIds;
	private long size;
	
	public SchemasByLinkageIterator(MongoDBConnector connector) {
		super();
		this.connector = connector;
		this.firstSource = true;
		
		connector.copySchema();
		this.size = connector.getTempSize();
		this.allIds = new HashSet<>();
	}

	@Override
	public boolean hasNext() {
		return this.size > 0;
	}

	@SuppressWarnings("unchecked")
	@Override
	public String next() {
		Document d;
		if (this.firstSource) {
			this.firstSource = false;
			d = connector.getTopSource();
		} else {
			d = connector.getTopLinkage(new LinkedList<>(this.allIds));
		}
		List<?> linkages = d.get("attributes", List.class);
		String result = d.getString("website");
		if (linkages != null) {
			allIds.addAll((List<? extends String>) linkages);
		}
		this.size = connector.removeSourceTempSendSize(d);
		return result;
	}

}
