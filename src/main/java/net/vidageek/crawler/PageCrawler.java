/**
 * 
 */
package net.vidageek.crawler;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net.vidageek.crawler.component.Downloader.Downloader;
import net.vidageek.crawler.component.ExecutorCounter;
import net.vidageek.crawler.component.LinkNormalizer.LinkNormalizer;
import net.vidageek.crawler.component.PageCrawlerExecutor;
import net.vidageek.crawler.config.CrawlerConfiguration;
import net.vidageek.crawler.exception.CrawlerException;
import net.vidageek.crawler.http.Url;
import net.vidageek.crawler.queue.DelayedBlockingQueue;
import net.vidageek.crawler.visitor.DoesNotFollowVisitedUrlVisitor;

import net.vidageek.crawler.visitor.PageVisitor;
import org.apache.log4j.Logger;

/**
 * @author jonasabreu
 * 
 */

/**     页面下载爬虫，里面包含一个记录器Logger，一个爬虫配置对象
CrawlerConfiguration config(Downloader,LinkNormalizer
在里面完成完成初始化的工作和内存分配)，一个线程池对象。
ThreadPoolExecutor executor（在构造线程池对象的时候初始化进程队列DelayedBlockQueue）
        一个计数器counter用来记录下载过的页面的当前的线程。
方法crawl中启动一个下载进程，在下载进程PageCrawlerExecutor中通过递归不断开启新的下载。
        该网络爬虫的核心思想：新建一个进程池，以一个下载地址作为参数从进程池分配一个线程
 并启动，从获取到的页面中提取出所有可用链接，对于每个链接都从进程池中分配一个进程进行管理。在
 对获取到的链接进行访问之前通过PageVisitor进行是否进行访问的判断符合访问条件即分配下载，
 不符合则不访问。在对进程池进行配置的时候需要CrawlerConfiguration对象，下载网页时需要
 Downloader工具，LinkNormalizer工具，UrlUtils处理工具，寻找网页过程中需要LinkFinder
 工具，保存访问到的网页需要一个Page对象，其有正常访问，错误访问，拒绝访问等不同类型的具体
 访问结果实现，需要一个PageFactory工厂类来管理。对于访问到的网页需要进行处理，在PageVisitor
 接口的实现类里对当前网页是否进一步访问以及访问到的内容处理，作者采用的PageVisitor的设计
 思路是可以多级嵌套，每一个PageVisitor的实现特有的是否继续访问的判断功能和对于访问到的
 内容的处理功能

 */
public class PageCrawler {

    /**
     * 记录器Logger用来输出调试信息和下载信息每秒输出一次完成的下载页面的数量及当前存活的下载进程
     */

	private final Logger log = Logger.getLogger(PageCrawler.class);

    /**
     *      页面爬虫PageCrawler的配置文件，里面提供了爬虫使用的下载器Downloader和链接处理器LinkNormalizer
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
     *      配置文件CrawlerConfiguration所需的所有参数，包括初始字符串beginUrl，下载器Downloader，
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
     *      爬虫的核心抓取函数，在该线程中启动一个用来下载的其他线程，在其他下载线程中每找到
     * 一个下载链接启动一个新的线程来下载，在该主线程中输出下载信息。
     * @param visitor 用户自定义页面处理器，是一个实现PageVisitor接口的处理器，建议继承
     *                作者定义的PageVisitor的风格，可以实现PageVisitor的嵌套调用
     */
	public void crawl(final PageVisitor visitor) {
		if (visitor == null) {
			throw new IllegalArgumentException("visitor cannot be null");
		}


        /**
         *      定义一个进程池对象executor，指定进程池对象的核心进程数，最大进程数，空闲进程
         * 存活时间，时间单位以及进程池的缓冲队列。进程池的缓冲队列常用的是
         * LinkedBlockingQueue<Runnable>，ArrayBlockingQueue<Runnable>，这里使用了
         * LinkedBlockingQueue<Runnable>队列，由于需要控制进程访问服务器的速率，在
         * LinkedBlockingQueue<Runnable>队列的基础上重新定义了一个叫做DelayedBlockQueue
         * 的队列，里面添加里最后用一次成功获取进程对象时间和获取进程对象间隔两个变量，并重写了
         * poll，take，remove的方法，使得获取一个进程的时间不少于一个延时时间。
         *
         *      通过进程池对象的execute方法从进程池中新建一个进程，execute（Runnable）方法里面需要
         * 一个实现了Runnable接口的类PageCrawlerExecutor，新建进程后执行Runnable接口
         * 的run（）方法，这样会新建一个进程执行PageCrawlerExecutor里面的run（）方法。在
         * PageCrawlerExecutor的run()方法里面实现对一个url地址网页的访问，提取网页中的所有链接，
         * 对提取到的所有链接用PageVisitor来进行判断，如果符合PageVisitor的访问规则则生成对应的
         * 网页对象，在并将这个网页对象Page交给用户自定义的PageVisitor进行处理。如果不符合
         * DoesNotFollowVisitedUrlVisitor或者用户自定义的PageVisitor的访问规则则不对这个页面
         * 进行任何处理。
         *      执行线程PageCrawlerExecutor里面的visitor默认首先指向DoesNotFollowVisitedUrlVisitor，
         * 然后在DoesNotFollowVisitedUrlVisitor中的visitor指向用户自定义的PageVisitor。即
         * 按照嵌套的顺序，首先用DoesNotFollowVisitedUrlVisitor的visitor对Page对象进行处理，
         * 然后调用DoesNotFollowVisitedUrlVisitor中的用户自定义PageVisitor对象对Page对象进行
         * 处理。可以在完成了不访问重复链接DoesNotFollowVisitedUrlVisitor处理后，在用户自定义
         * PageVisitor中完成对Page对象的全部处理。也可以在用户自定义PageVisitor中再次定义一个
         * PageVisitor对象再次对Page对象进行处理，这样的嵌套可以一直持续下去。
         *      作者还定义了DomainVisitor和RejectAtDepthVisitor两个访问器给我们使用。DomainVisitor
         * 用来检验访问的网址是否在同一个域名下，生成DomainVisitor时构造函数需要一个域名以及另外
         * 一个PageVisitor。RejectAtDepthVisitor用来检查访问的深度不大于一个定义的值，当访问的
         * 页面深度大于定义值则不再做操作。
         *      其余配置在CrawlerConfiguration的配置对象config中都已经定义好，直接使用
         *
         *
         */
		ThreadPoolExecutor executor =
                new ThreadPoolExecutor(config.minPoolSize(),
                                       config.maxPoolSize(),
                                       config.keepAliveMilliseconds(),
                                       TimeUnit.MILLISECONDS,
                                       new DelayedBlockingQueue(config.requestDelayMilliseconds()));

        /**
         * 定义一个进程计数器，在运行过程中记录已执行的任务即已经下载的网页和当前的活动进程
         */
		final ExecutorCounter counter = new ExecutorCounter();

        /**
         *      启动初始进程，这个进程回去访问给定的网址，并在给定的网址里面寻找其他网址，对于找到
         * 的网址一次进行访问。给定的网址的深度定义为0。在这个进程中，对于找到的每一个网址，调用
         * 进程池对象executor分配一个新的进程用来访问对应的网页，并将进程池对象executor分配的这个
         * 进程保存到进程缓存队列中，当满足队列的读取条件时自动从进程队列中启动这个进程，进程的启动
         * 条件和设定的核心进程数有关，此处最大因为进程缓存队列没有设定长度所以进程队列的长度是无限的，
         * 进程池的最大进程数没有作用。
         */
		try {
            /**
             *      给进程池执行一个PageCrawlerExecutor对象，PageCrawlerExecutor对象实现了
             * Runnable接口，新建的进程会自动执行PageCrawlerExecutor对象中的run（）方法
             */
			executor.execute(
                    new PageCrawlerExecutor(new Url(config.beginUrl(), 0),
                                            executor,
                                            counter,
                                            config.downloader(),
                                            config.normalizer(),
                                            new DoesNotFollowVisitedUrlVisitor(config.beginUrl(),visitor)));

            /**
             *      在进程执行期间，如果进程数不为0，则每个1s中输出一个完成的访问网页数和当前
             * 进程数。sleep（）函数包装了Thread.sleep()函数，睡眠1s。当前进程是在主线程中，
             * 负责下载页面的都是启动的其他线程，每一个下载任务对应一个下载线程，在主线程中输出
             * 当前完成的下载任务和当前存活的线程数
             */
			while (counter.value() != 0) {
				log.debug("executors that finished: " + executor.getCompletedTaskCount());
				log.debug("Number of Executors alive: " + counter.value());
				sleep();
			}
		} finally {
			executor.shutdown();
		}
	}

    /**
     *      辅助函数，包装Thread.sleep()，线程停止1s，当主线程出错时输出CrawlerException
     * 异常。
     */
	private void sleep() {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			throw new CrawlerException("main thread died. ", e);
		}

	}
}
