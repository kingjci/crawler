package net.vidageek.crawler.component.visitor;

import net.vidageek.crawler.http.Page;
import net.vidageek.crawler.http.Status;
import net.vidageek.crawler.http.Url;

/**
 * Created by Administrator on 15-1-27.
 */
public interface ContentVisitor {

    public void onError(final Url url, final Status statusError);

    public void visit(final Page page);
}
