package net.vidageek.crawler.link;

import java.util.List;

import net.vidageek.crawler.component.LinkFinder.impl.IframeLinkFinder;
import org.junit.Assert;
import org.junit.Test;

public class IframeLinkFinderTest {

	@Test(expected = IllegalArgumentException.class)
	public void testIllegalArgumentException() {
		new IframeLinkFinder(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testEmptyIllegalArgumentException() {
		new IframeLinkFinder("");
	}

	@Test
	public void testCanRecoverLinks() {
		List<String> links = new IframeLinkFinder(
				"<br /> <iframe id=\"link1\" href=\"test.page1\"></iframe><br />  <iframe id=\"link2\" href=\"test.page2\"></iframe>")
				.getLinks();
		Assert.assertEquals(2, links.size());
		Assert.assertEquals("test.page1", links.get(0));
		Assert.assertEquals("test.page2", links.get(1));
	}
}
