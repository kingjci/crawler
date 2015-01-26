/**
 * 
 */
package net.vidageek.crawler;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net.vidageek.crawler.component.Downloader;
import net.vidageek.crawler.component.ExecutorCounter;
import net.vidageek.crawler.component.LinkNormalizer;
import net.vidageek.crawler.component.PageCrawlerExecutor;
import net.vidageek.crawler.config.CrawlerConfiguration;
import net.vidageek.crawler.exception.CrawlerException;
import net.vidageek.crawler.queue.DelayedBlockingQueue;
import net.vidageek.crawler.visitor.DoesNotFollowVisitedUrlVisitor;

import org.apache.log4j.Logger;

/**
 * @author jonasabreu
 * 
 */

/**页面下载爬虫，里面包含一个记录器Logger，一个爬虫配置对象
CrawlerConfiguration config(Downloader,LinkNormalizer
在里面完成完成初始化的工作和内存分配)，一个线程池对象
ThreadPoolExecutor executor（在构造线程池对象的时候初始化进程队列DelayedBlockQueue）
 一个计数器counter用来记录下载过的页面的当前的线程
方法crawl中启动一个下载进程，在下载进程PageCrawlerExecutor中通过递归不断开启新的下载
 */
public class PageCrawler {

    /**
     * 记录器Logger用来输出调试信息和下载信息每秒输出一次完成的下载页面的数量及当前存活的下载进程
     */

	private final Logger log = Logger.getLogger(PageCrawler.class);

    /**
     * 页面爬虫PageCrawler的配置文件，里面提供了爬虫使用的下载器Downloader和链接处理器LinkNormalizer
     并在里面定义下载线程数，最大下载线程数，线程空闲时间，请求延迟时间，CrawlerConfiguration的构造
     函数可以只有一个初始下载链接字符串
     */
	private final CrawlerConfiguration config;

    /**
     * 构造函数
     * @param config 网页爬虫PageCrawler的配置文件，里面包含起始地址beginUrl
     *               网页下载器WebDownloader，链接处理器LinkNormalizer，核心进程数
     *               最大进程数，进程空闲时间，进程访问延时时间
     */
	public PageCrawler(final CrawlerConfiguration config) {
		this.config = config;
	}

    /**
     * 未使用的一种构造函数
     * @param beginUrl
     */
	public PageCrawler(final String beginUrl) {
		this(CrawlerConfiguration.forStartPoint(beginUrl).build());
	}

    /**
     * 配置文件CrawlerConfiguration所需的所有参数，包括初始字符串beginUrl，下载器Downloader，
     *链接处理器LinkNormalizer。核心线程数，最大线程数，线程空闲存活时间，请求延迟时间在CrawlerConfiguration
     *类中有默认值
     * @param beginUrl 爬虫的起始地址，一般是域名
     * @param downloader 下载网页使用的下载器Downloader，这是一个接口对象其核心方法是个get（），
     *                   这里是用的是默认的实现类WebDownloader
     *
     * @param normalizer 网页链接处理器，用来替换转义字符并且处理可能的相对路径，返回绝对路径
     */
	public PageCrawler(final String beginUrl, final Downloader downloader, final LinkNormalizer normalizer) {
		config = CrawlerConfiguration.forStartPoint(beginUrl).withDownloader(downloader).withLinkNormalizer(normalizer)
				.build();
	}

    /**
     * 爬虫的核心抓取函数
     * @param visitor
     */
	public void crawl(final PageVisitor visitor) {
		if (visitor == null) {
			throw new IllegalArgumentException("visitor cannot be null");
		}

		ThreadPoolExecutor executor =
                new ThreadPoolExecutor(config.minPoolSize(),
                                       config.maxPoolSize(),
                                       config.keepAliveMilliseconds(),
                                       TimeUnit.MILLISECONDS,
                                       new DelayedBlockingQueue(config.requestDelayMilliseconds()));

		final ExecutorCounter counter = new ExecutorCounter();

		try {
			executor.execute(
                    new PageCrawlerExecutor(new Url(config.beginUrl(), 0),
                                            executor,
                                            counter,
                                            config.downloader(),
                                            config.normalizer(),
                                            new DoesNotFollowVisitedUrlVisitor(config.beginUrl(),visitor)));

			while (counter.value() != 0) {
				log.debug("executors that finished: " + executor.getCompletedTaskCount());
				log.debug("Number of Executors alive: " + counter.value());
				sleep();
			}
		} finally {
			executor.shutdown();
		}
	}

	private void sleep() {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			throw new CrawlerException("main thread died. ", e);
		}

	}
}
