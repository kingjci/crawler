package net.vidageek.crawler.http;

import net.vidageek.crawler.http.Status;

import java.util.List;

/**
 * @author jonasabreu
 * 
 */
public interface Page {

    public List<String> getLinks();

    public String getUrl();

    public String getContent();

    public Status getStatusCode();

    public String getCharset();

}
