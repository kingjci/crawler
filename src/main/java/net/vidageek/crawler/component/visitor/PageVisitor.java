/**
 * 
 */
package net.vidageek.crawler.component.visitor;

import net.vidageek.crawler.http.Url;

/**
 * @author jonasabreu
 * 
 */
public interface PageVisitor extends ContentVisitor {

    boolean followUrl(Url url);

}
