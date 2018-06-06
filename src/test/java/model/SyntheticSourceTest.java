package model;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Test the {@link SyntheticSource} class
 * 
 * @author federico
 *
 */
public class SyntheticSourceTest {

	@Test
	public void testParseSourceH() {
		SyntheticSource parseSource = SyntheticSource.parseSource("www.prova--s--H--s--100--s--200--s--300.com");
		assertEquals("www.prova", parseSource.getDomain());
		assertEquals("com", parseSource.getExtension());
		assertEquals(HeadOrTail.H, parseSource.getHeadOrTail());
		assertEquals(0.1, parseSource.getValueErrorRate(), 0.001);
		assertEquals(0.2, parseSource.getLinkageErrorRate(), 0.001);
		assertEquals(0.3, parseSource.getLinkageMissingRate(), 0.001);
	}

	@Test
	public void testParseSourceT() {
		SyntheticSource parseSource = SyntheticSource.parseSource("www.prova--s--T--s--100--s--200--s--300.com");
		assertEquals("www.prova", parseSource.getDomain());
		assertEquals("com", parseSource.getExtension());
		assertEquals(HeadOrTail.T, parseSource.getHeadOrTail());
		assertEquals(0.1, parseSource.getValueErrorRate(), 0.001);
		assertEquals(0.2, parseSource.getLinkageErrorRate(), 0.001);
		assertEquals(0.3, parseSource.getLinkageMissingRate(), 0.001);
	}

	@Test
	public void testParseSourceNoWww() {
		SyntheticSource parseSource = SyntheticSource.parseSource("prova--s--H--s--100--s--200--s--300.com");
		assertEquals("prova", parseSource.getDomain());
		assertEquals("com", parseSource.getExtension());
		assertEquals(HeadOrTail.H, parseSource.getHeadOrTail());
		assertEquals(0.1, parseSource.getValueErrorRate(), 0.001);
		assertEquals(0.2, parseSource.getLinkageErrorRate(), 0.001);
		assertEquals(0.3, parseSource.getLinkageMissingRate(), 0.001);
	}

	@Test
	public void testToStringH() {
		SyntheticSource generateSource = new SyntheticSource("www.prova", "com", HeadOrTail.H, 0.1, 0.2, 0.3);
		assertEquals("www.prova--s--H--s--100--s--200--s--300.com", generateSource.toString());
	}
	
	@Test
	public void testToStringDoubleApproximation() {
		SyntheticSource generateSource = new SyntheticSource("www.prova", "com", HeadOrTail.H, 0.15555, 0.2, 0.3);
		assertEquals("www.prova--s--H--s--156--s--200--s--300.com", generateSource.toString());
	}

	@Test
	public void testToStringT() {
		SyntheticSource generateSource = new SyntheticSource("www.prova", "com", HeadOrTail.T, 0.1, 0.2, 0.3);
		assertEquals("www.prova--s--T--s--100--s--200--s--300.com", generateSource.toString());
	}

	@Test
	public void testToStringNoWww() {
		SyntheticSource generateSource = new SyntheticSource("prova", "com", HeadOrTail.H, 0.1, 0.2, 0.3);
		assertEquals("prova--s--H--s--100--s--200--s--300.com", generateSource.toString());
	}

}
