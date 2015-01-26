/**
 * 
 */
package net.vidageek.crawler.component;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;
import net.vidageek.crawler.Page;
import net.vidageek.crawler.Status;
import net.vidageek.crawler.config.http.Cookie;
import net.vidageek.crawler.exception.CrawlerException;
import net.vidageek.crawler.page.DefaultPageFactory;
import net.vidageek.crawler.page.PageFactory;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author jonasabreu
 * 
 */

/**
 * WebDownloader这个类在运行过程中是多线程同时访问的，其保存数据的容器为了
 * 支持多线程的并发访问使用了ConcurrentLinkedQueue，同时考虑到对于这两个容器
 * 的访问基本采用顺序遍历，采用链表的具体实现。为了防止被其他线程修改变量，其变
 * 量都定义为了final。类当中的方法都是静态的，可以让多个线程同时访问，类成员变量
 * 是常量，访问时不做修改，同样支持多线程操作。故WebDownloader这个类完全支持
 * 多线程操作，若干个下载线程同时使用这个WebDownloader来下载网页
 */
public class WebDownloader implements Downloader {

    /**
     * 声明一个记录器Logger用来记录cookie创建，请求url request的操作
     */
    private final Logger log = Logger.getLogger(WebDownloader.class);

    /**
     * 定义两个支持并发操作的容器，mimeTypesToInclude用来保存支持的网页类型，当获取到的
     * 网页类型在这个列表中则做进一步的处理，否则放弃。cookies用来记录网站cookie，
     * """"需要手工保存""""，讲每个保存的cookie都与当前操作的URL访问同时提交
     */

	private final ConcurrentLinkedQueue<String> mimeTypesToInclude;

	private final ConcurrentLinkedQueue<Cookie> cookies;

    /**
     * 页面的工厂函数，根据网站的访问结果生成不同的对象包括OkPage，ErrorPage，
     * RejectedMineTypePage,用相同的Page指向PageFactory生成的不同的页面类
     */
	private final PageFactory pageFactory;

    /**
     * WebDownloader的最简单的构造函数，需要一个保存有支持网页类型的List类型的列表，
     * 同时新生成一个空的cookie的列表调用有三个参数的构造函数
     * @param mimeTypesToInclude
     */
	public WebDownloader(final List<String> mimeTypesToInclude) {
		this(mimeTypesToInclude, new ArrayList<Cookie>(), new DefaultPageFactory());
	}

    /**
     *
     * @param mimeTypesToInclude 支持的网页类型，需要一个List
     * @param cookies 保存的cookie，""""需要手工输入，不会自动产生""""，需要一个List
     * @param pageFactory 页面的工厂函数，根据访问结果产生不同网页
     */
	public WebDownloader(final List<String> mimeTypesToInclude,
						 final List<Cookie> cookies,
						 final PageFactory pageFactory) {
		
		this.cookies = new ConcurrentLinkedQueue<Cookie>(cookies);
		this.mimeTypesToInclude = new ConcurrentLinkedQueue<String>(mimeTypesToInclude);
		this.pageFactory = pageFactory;
	}

    /**
     * 默认没有参数的构造函数，只支持“text/html”类型的网页内容
     */
	public WebDownloader() {
		this(Arrays.asList("text/html"));
	}

    /**
     * 获取网页的函数，产生OkPage，ErrorPage，RejectedMimeTypePage三种网页
     * 中的一种，用他们的公共接口Page指向其中可能产生的任意一种
     * @param url 用来访问网站的url
     * @return 用Page接口指向返回的一种页面对象
     */
	public Page get(final String url) {
        /**
         * 创建一个http客户端
         */
		DefaultHttpClient client = new DefaultHttpClient();
        /**
         * 将cookies列表里面保存的每个cookie都和当前的这个url同时发送给服务器
         * 这是一种最简单的方法，可以考虑判断cookie是否是当前url路径下的以减少访问服务器
         * 的次数，但是无法减少循环次数即计算量
         */
		for (Cookie cookie : cookies) {
            /**
             * 保存当前cookie的name值
             */
			String name = cookie.name();
            /**
             * 保存当前cookie的value值
             */
			String value = cookie.value();
			log.debug("Creating cookie [" + name + " = " + value + "] " + cookie.domain());
            /**
             * 产生一个cookie对象BasicClientCookie，并将这个cookie对象添加到http
             * 客户端中，准备发送给服务器
             */
			BasicClientCookie clientCookie = new BasicClientCookie(name, value);
			clientCookie.setPath(cookie.path());
			clientCookie.setDomain(cookie.domain());
			client.getCookieStore().addCookie(clientCookie);
		}
        /**
         *设置socket的超时时间15s
         */
		client.getParams().setIntParameter("http.socket.timeout", 15000);
        /**
         *调用两个参数的get方法
         */
		return get(client, url);
	}

	public Page get(final HttpClient client, final String url) {
		try {
            /**
             *将url用utf-8的方式进行编码
             */
			String encodedUrl = encode(url);
			log.debug("Requesting url: [" + encodedUrl + "]");
            /**
             *用get方式访问服务器，采用经过utf-8编码后的url
             */
			HttpGet method = new HttpGet(encodedUrl);

			try {
                /**
                 *http客户端连接服务器
                 */
				HttpResponse response = client.execute(method);
                /**
                 *返回连接服务器的状态码，马上用来判断连接成功与否，
                 * 决定产生何种类型的网页对象
                 */
				Status status = Status.fromHttpCode(response.getStatusLine().getStatusCode());

                /**
                 *调用acceptsMimeType函数，判断当前服务器方位的内容类型
                 * 是否在mimeTypesToInclude列表中，如果不在则返回RejectedMimeTypePage，
                 * 表示被拒绝的网页类型
                 */
				if (!acceptsMimeType(response.getLastHeader("Content-Type"))) {
					return pageFactory.buildRejectedMimeTypePage(
							url, status, response.getLastHeader("Content-Type").getValue());
				}

                /**
                 *如果状态码在200-299之间，则访问服务器成功，服务器返回一个InputStream
                 * 调用read函数从服务器返回的这个InputStream输入流中读取返回数据
                 * 并判断返回数据的编码方式并将其转换为utf-8的编码方式，保存到字符串
                 * utf8Content中，用url和返回内容utf8Content产生一个OkPage
                 */
				if (Status.OK.equals(status)) {
					CharsetDetector detector = new CharsetDetector();
					detector.setText(read(response.getEntity().getContent()));
					CharsetMatch match = detector.detect();

					log.debug("Detected charset: " + match.getName());

					String content = match.getString();
					CharBuffer buffer = CharBuffer.wrap(content.toCharArray());
					Charset utf8Charset = Charset.forName("UTF-8");
					String utf8Content = new String(utf8Charset.encode(buffer).array(), "UTF-8");

					return pageFactory.buildOkPage(url, utf8Content);
				}
                /**
                 *如果返回的状态码不再200-299之间则产生一个错误页面，将url和错误码保存到ErrorPage
                 * 对象中并返回
                 */
				return pageFactory.buildErrorPage(url, status);
			} finally {
				method.abort();
			}

		} catch (IOException e) {
			throw new CrawlerException("Could not retrieve data from " + url, e);
		}
	}

    /**
     * acceptsMimeType函数用来判断返回的网页类型是否在网页支持类型列表mimeTypesToInclude
     * 中，如果在返回true，不再或者没有有限的Header则返回false
     * @param header 返回数据的头文件
     * @return 判断的返回值是否接受这个从服务器返回的输入流
     */
	private boolean acceptsMimeType(final Header header) {
		final String value = header.getValue();
        /**
         *Header中没有包含有效值，返回false
         */
		if (value == null) {
			return false;
		}

        /**
         *遍历mimeTypesToInclude列表，判断value是否在这个列表中，如果找到则
         * 返回true
         */
		for (String mimeType : mimeTypesToInclude) {
			if (value.contains(mimeType)) {
				return true;
			}
		}
        /**
         *遍历mimeTypesToInclude列表完毕，没有发现value，返回false
         */
		return false;
	}

    /**
     * read函数从服务器返回的InputStream流中读取返回数据，并将其保存到
     * 一个byte数组中，以便在探测出其编码方式后将其转化为utf-8方式的字符串
     * @param inputStream 从服务器范围的输入流
     * @return 将服务器返回的流保存到一个二进制数组中并返回
     */
	private byte[] read(final InputStream inputStream) {
        /**
         * 预声明一个1000个字符的二进制数组，下面如果使用中不够用的话将会声明一个
         * 1.5倍的数组，并将原来的内容复制到其中
         */
		byte[] bytes = new byte[1000];
		int i = 0;
		int b;
		try {
            /**
             *一直读到流结束
             */
			while ((b = inputStream.read()) != -1) {
				bytes[i++] = (byte) b;
				if (bytes.length == i) {
					byte[] newBytes = new byte[(bytes.length * 3) / 2 + 1];
					for (int j = 0; j < bytes.length; j++) {
						newBytes[j] = bytes[j];
					}
					bytes = newBytes;
				}
			}
		} catch (IOException e) {
			throw new CrawlerException("There was a problem reading stream.", e);
		}

        /**
         *将bytes中的内容复制到一块和其大小一样的内容中
         */
		byte[] copy = Arrays.copyOf(bytes, i);

		return copy;
	}

    /**
     * 对url中的特殊字符重新用utf-8编码，防止在传输过程中出现乱码
     * @param url
     * @return
     */
	private String encode(final String url) {
		String res = "";
        /**
         * 对url中的每个字符做判断，如果其含有:/.?&#=中的一种，则用utf-8
         * 对这个字符重新编码
         */
		for (char c : url.toCharArray()) {
			if (!":/.?&#=".contains("" + c)) {
				try {
					res += URLEncoder.encode("" + c, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					throw new CrawlerException(
							"There is something really wrong with your JVM. It could not find UTF-8 encoding.", e);
				}
			} else {
				res += c;
			}
		}

		return res;
	}

}