/**
 * 
 */
package net.vidageek.crawler.component.visitor;

import net.vidageek.crawler.component.visitor.impl.DomainVisitor;
import org.junit.Test;

/**
 * @author jonasabreu
 * 
 */
public class DomainVisitorTest {

    @Test(expected = IllegalArgumentException.class)
    public void testThatThrowsExceptionIfBaseUrlIsNull() {
        new DomainVisitor(null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThatThrowsExceptionIfBaseUrlIsEmpty() {
        new DomainVisitor("  \n   \t  ", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThatThrowsExceptionIfBaseUrlDoesntMatchPattern() {
        new DomainVisitor("htp://any.thing", null);
    }

}
