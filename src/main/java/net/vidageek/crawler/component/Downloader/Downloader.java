/**
 * 
 */
package net.vidageek.crawler.component.Downloader;

import net.vidageek.crawler.http.Page;

/**
 * @author jonasabreu
 * 
 */
public interface Downloader {

    Page get(String url);

}
