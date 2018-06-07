package models.generator;

import java.util.Scanner;

/**
 * Classes of attribute's tokens configuration (in the form "random-fixed")
 * @author federico
 *
 */
public class TokenClass {
	
	private static final String TOKEN_CLASS_FORMAT = "%d-%d";

	public TokenClass(int random, int fixed) {
		super();
		this.random = random;
		this.fixed = fixed;
	}	
	
	private int random;
	private int fixed;
	private String cachedToString;
	
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
	
	@Override
	public String toString() {
		if (this.cachedToString == null) {
			this.cachedToString = String.format(TOKEN_CLASS_FORMAT, this.random, this.fixed);
		}
		return this.cachedToString;
	}

	/**
	 * ToString inverse
	 * @param next
	 * @return
	 */
	public static TokenClass parseTokenClass(String stringRepr) {
		try (Scanner scanner = new Scanner(stringRepr)){
			scanner.useDelimiter("-");
			return new TokenClass(scanner.nextInt(), scanner.nextInt());
		}
	}
}
