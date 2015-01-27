package net.vidageek.crawler;

import net.vidageek.crawler.http.Status;
import org.junit.Assert;

import org.junit.Test;

public class StatusTest {

	@Test
	public void testToCheckNotFoundIsReturned() {
		Assert.assertEquals(Status.NOT_FOUND, Status.fromHttpCode(404));
	}
}
