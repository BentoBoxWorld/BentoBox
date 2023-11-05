package world.bentobox.bentobox.database.objects;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.google.gson.annotations.Expose;

/**
 * Stores data on ranks
 */
@Table(name = "Ranks")
public class RankObject implements DataObject {
	
	public static final String ID = "BentoBox-Ranks";
	
	public RankObject(Map<String, Integer> rankReference) {
		super();
		this.rankReference = rankReference;
	}

	@Expose
	private Map<String, Integer> rankReference;

	@Override
	public String getUniqueId() {
		return ID;
	}

	@Override
	public void setUniqueId(String uniqueId) {
		// Nothing to do
	}

	public Map<String, Integer> getRankReference() {
		return Objects.requireNonNullElse(rankReference, new LinkedHashMap<>());
	}

	public void setRankReference(Map<String, Integer> rankReference) {
		this.rankReference = rankReference;
	}

	
	
}
