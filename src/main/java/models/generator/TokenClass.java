package models.generator;

/**
 * Classes of attribute's tokens configuration (in the form "random-fixed")
 * @author federico
 *
 */
public class TokenClass {
	
	public TokenClass(int random, int fixed) {
		super();
		this.random = random;
		this.fixed = fixed;
	}	
	
	private int random;
	
	private int fixed;
	
	public int getRandom() {
		return random;
	}
	
	public int getFixed() {
		return fixed;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + fixed;
		result = prime * result + random;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TokenClass other = (TokenClass) obj;
		if (fixed != other.fixed)
			return false;
		if (random != other.random)
			return false;
		return true;
	}
}
