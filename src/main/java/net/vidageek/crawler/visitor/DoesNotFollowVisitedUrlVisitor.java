package net.vidageek.crawler.visitor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.vidageek.crawler.http.Page;
import net.vidageek.crawler.http.Status;
import net.vidageek.crawler.http.Url;

/**
 * @author jonasabreu
 * 
 */
final public class DoesNotFollowVisitedUrlVisitor implements PageVisitor {

    //Attention!!!!!!
    //这个PageVisitor visitor是用户自定义的visitor，通过在里面完成visit（Page）
    // 和onError（Url，Status）两个方法，实现对于获取到的web页面的操作，在这两个方法中对
    //页面进行具体的处理
    private final PageVisitor visitor;
    // Using map since jdk 1.5 does not provide a good concurrent set
    // implementation
    private final Map<Url, String> visitedUrls = new ConcurrentHashMap<Url, String>();

    public DoesNotFollowVisitedUrlVisitor(final String beginUrl, final PageVisitor visitor) {
        this.visitor = visitor;
        visitedUrls.put(new Url(beginUrl, 0), "");
    }

    public boolean followUrl(final Url url) {
        if (visitedUrls.get(url) != null) {
            return false;
        }
        visitedUrls.put(url, "");

        //在经过不重复访问Visitor DoesNotFollowVisitedUrlVisitor的不重复访问判断后，
        // 由用户自定义的PageVisitor作进一步判断，是否对这个页面进行访问。
        //可以PageVisitor里面套其他类型的PageVisitor作进步一判断
        return visitor.followUrl(url);
    }

    public void onError(final Url url, final Status statusError) {
        visitor.onError(url, statusError);

    }

    public void visit(final Page page) {
        visitor.visit(page);
    }

    /**
     * @author jonasabreu
     *
     */
    public static interface ContentVisitor {

        void visit(Page page);

        void onError(Url errorUrl, Status statusError);

    }
}
