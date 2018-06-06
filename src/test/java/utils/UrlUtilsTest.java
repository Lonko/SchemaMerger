package utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class UrlUtilsTest {

	@Test
	public void testGetDomain() {
		assertEquals("www.provaT.com", UrlUtils.getDomain("www.provaT.com"));
	}
	
	@Test
	public void testGetDomainWithSpecialChars() {
		assertEquals("www.prova--s--T.com", UrlUtils.getDomain("www.prova--s--T.com"));
	}

}
