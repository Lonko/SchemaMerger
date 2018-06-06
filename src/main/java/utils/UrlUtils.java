package utils;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Utils for URL of sources
 * 
 * @author federico
 *
 */
public class UrlUtils {
	
	public static String getDomain(String url) {
		String domain = null;
		url = url.replaceAll(" ", "%20").replaceAll("\\|", "%7C").replaceAll("\"", "%22");
		if (url.startsWith("http:/") || url.startsWith("https:/")) {
			if (!url.startsWith("http://") && !url.startsWith("https://")) {
				url = url.replace("http:/", "http://");
				url = url.replace("https:/", "https://");
			}
		} else
			url = "http://" + url;

		try {
			URI u = new URI(url);
			domain = u.getHost();
		} catch (URISyntaxException e) {
			System.err.println("Couldn't extract host from URL: " + url);
			e.printStackTrace();
		}
		domain = domain.replaceAll("%20", " ").replaceAll("%7C", "|");

		return domain;
	}
}
