package generator;

import java.util.Random;

public class RandomStringGenerator implements StringGenerator {
	
	private int sourceLength;
	private int attributeLength;
	private int valueLength;
	private String allowedChars = "abcdefghijklmnopqrstuvwxyz"
									+"ABCDEFGHIJKLMNOPQRSTUVWXYZ"
									+"0123456789";
	private char[] chars;
	
	public RandomStringGenerator(int sourceLength, int attributeLength, int valueLength){
		this.sourceLength = sourceLength;
		this.attributeLength = attributeLength;
		this.valueLength = valueLength;
		this.chars = allowedChars.toCharArray();
	}

	@Override
	public String generateSourceName() {
		StringBuilder sb = new StringBuilder(this.sourceLength);
		Random random = new Random();
		for (int i = 0; i < this.sourceLength; i++) {
			char c = this.chars[random.nextInt(this.chars.length)];
			sb.append(c);
		}

		return sb.toString();
	}

	@Override
	public String generateAttributeName() {
		StringBuilder sb = new StringBuilder(this.attributeLength);
		Random random = new Random();
		for (int i = 0; i < this.attributeLength; i++) {
			char c = this.chars[random.nextInt(this.chars.length)];
			sb.append(c);
		}

		return sb.toString();
	}

	@Override
	public String generateAttributeValue() {
		StringBuilder sb = new StringBuilder(this.valueLength);
		Random random = new Random();
		for (int i = 0; i < this.valueLength; i++) {
			char c = this.chars[random.nextInt(this.chars.length)];
			sb.append(c);
		}

		return sb.toString();
	}

	public int getSourceLength() {
		return sourceLength;
	}

	public void setSourceLength(int sourceLength) {
		this.sourceLength = sourceLength;
	}

	public int getAttributeLength() {
		return attributeLength;
	}

	public void setAttributeLength(int attributeLength) {
		this.attributeLength = attributeLength;
	}

	public int getValueLength() {
		return valueLength;
	}

	public void setValueLength(int valueLength) {
		this.valueLength = valueLength;
	}

	public String getAllowedChars() {
		return allowedChars;
	}

	public void setAllowedChars(String allowedChars) {
		this.allowedChars = allowedChars;
		this.chars = allowedChars.toCharArray();
	}

}
