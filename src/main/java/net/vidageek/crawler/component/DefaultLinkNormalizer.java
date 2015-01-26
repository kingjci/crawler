package net.vidageek.crawler.component;

/**
 * @author jonasabreu
 */
public class DefaultLinkNormalizer implements LinkNormalizer {

	private final String beginUrl;

	public DefaultLinkNormalizer(final String beginUrl) {
		if ((beginUrl == null) || (beginUrl.trim().length() == 0)) {
			throw new IllegalArgumentException("beginUrl cannot be null or empty");
		}
		this.beginUrl = beginUrl;
	}

    /**
     * normalize用来将处理相对路径的问题，如果链接中使用了相对路径，则将相对路径和
     * 域名组合成为一个完整的url，如果链接中使用的是绝对路径，则任然输出绝对路径。beginUrl
     * 是DefaultLinkNormalizer构造函数的一个参数，一个LinkNormalizer对应一个域名并处理这个
     * 域名下面的所有路径
     * @param url 待处理的路径，相对域名或者是绝对域名
     * @return 返回的域名字符串
     */
	public String normalize(String url) {

        /**
         * 将转移字符&amp;转换为&
         */
		url = url.replaceAll("&amp;", "&");

        /**
         * UrlUtils.resolveUrl(beginUrl, url)用来将域名beginUrl和相对路径url
         * 合成一个完整的url路径提供给下面的操作访问
         */
		return UrlUtils.resolveUrl(beginUrl, url);
	}

}
