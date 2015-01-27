package net.vidageek.crawler.link;

import java.util.List;

import net.vidageek.crawler.component.LinkFinder.impl.DefaultLinkFinder;
import org.junit.Assert;
import org.junit.Test;

public class DefaultLinkFinderTest {

	@Test(expected = IllegalArgumentException.class)
	public void testIllegalArgumentException() {
		new DefaultLinkFinder(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testEmptyIllegalArgumentException() {
		new DefaultLinkFinder("");
	}

	@Test
	public void testCanRecoverLinks() {
		List<String> links = new DefaultLinkFinder(
				"<br /> <a id=\"link1\" href=\"test.page1\"></a><br />  <a id=\"link2\" href=\"test.page2\"></a>")
				.getLinks();
		Assert.assertEquals(2, links.size());
		Assert.assertEquals("test.page1", links.get(0));
		Assert.assertEquals("test.page2", links.get(1));
	}

	@Test
	public void testCanRecoverLinksThatUsesJsessionid() {
		List<String> links = new DefaultLinkFinder(
				"<br /> <a id=\"link1\" href=\"test.page1\"></a><br />  <a id=\"link2\" href=\"test.page2;jsessionid=20\"></a>")
				.getLinks();
		Assert.assertEquals(2, links.size());
		Assert.assertEquals("test.page1", links.get(0));
		Assert.assertEquals("test.page2;jsessionid=20", links.get(1));
	}
}
