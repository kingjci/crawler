package net.vidageek.crawler.http.page;

import net.vidageek.crawler.http.Page;
import net.vidageek.crawler.http.Status;

/**
 * Contract for {@link net.vidageek.crawler.http.Page}s factory.
 */
public interface PageFactory {
	
	Page buildOkPage(String url, String content);

	Page buildErrorPage(String url, Status status);

	Page buildRejectedMimeTypePage(String url, Status status, String mimeType);
}
