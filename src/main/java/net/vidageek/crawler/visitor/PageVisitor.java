/**
 * 
 */
package net.vidageek.crawler.visitor;

import net.vidageek.crawler.http.Url;

/**
 * @author jonasabreu
 * 
 */
public interface PageVisitor extends DoesNotFollowVisitedUrlVisitor.ContentVisitor {

    boolean followUrl(Url url);

}
