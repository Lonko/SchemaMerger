package launchers;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import model.SyntheticAttribute;
import model.SyntheticSource;

/**
 * Class that provides some stats on results of synthetic dataset generator, as
 * well as some information on dataset not present in Mongo (TODO should we fix
 * it?)
 * 
 * @author federico
 *
 */
public class SyntheticDataOutputStat {

	private List<SyntheticSource> sourcesByLinkage;
	private Map<SyntheticAttribute, Integer> attrLinkage;
	private long catalogueTime;
	private long datasetTime;
	private long totalTime;
	private int catalogueSize;
	private int datasetSize;
	
	public SyntheticDataOutputStat(List<SyntheticSource> sourcesByLinkage, Map<SyntheticAttribute, Integer> attrLinkage, long catalogueTime,
			long datasetTime, long totalTime, int catalogueSize, int datasetSize) {
		super();
		this.sourcesByLinkage = sourcesByLinkage;
		this.attrLinkage = attrLinkage;
		this.catalogueTime = catalogueTime;
		this.datasetTime = datasetTime;
		this.totalTime = totalTime;
		this.catalogueSize = catalogueSize;
		this.datasetSize = datasetSize;
	}

	protected long getCatalogueTime() {
		return catalogueTime;
	}

	protected long getDatasetTime() {
		return datasetTime;
	}

	protected long getTotalTime() {
		return totalTime;
	}

	protected int getCatalogueSize() {
		return catalogueSize;
	}

	protected int getDatasetSize() {
		return datasetSize;
	}

	public List<String> getSourcesNamesByLinkage() {
		return sourcesByLinkage.stream().map(source -> source.toString()).collect(Collectors.toList());
	}
	
	public Map<SyntheticAttribute, Integer> getAttrLinkage() {
		return attrLinkage;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		//TODO move outside
		sb.append("Prodotti nel catalogo: " + getCatalogueSize());
		sb.append("\nProdotti nel dataset: " + getDatasetSize());
		sb.append("Tempo di generazione del Catalogo: " + (catalogueTime / 60) + " min "
				+ (catalogueTime % 60) + " sec");
		sb.append(
				"Tempo di generazione del Dataset: " + (datasetTime / 60) + " min " + (datasetTime % 60) + " s ");
		sb.append("Tempo di esecuzione totale: " + (totalTime / 60) + " min " + (totalTime % 60) + " s ");
		return sb.toString();
	}
}
