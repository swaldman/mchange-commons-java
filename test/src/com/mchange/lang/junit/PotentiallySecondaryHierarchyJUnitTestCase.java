package com.mchange.lang.junit;

import junit.framework.TestCase;

import com.mchange.lang.PotentiallySecondary;
import com.mchange.lang.PotentiallySecondaryException;
import com.mchange.v1.util.UnreliableIteratorException;
import com.mchange.v1.xmlprops.XmlPropsException;
import com.mchange.v2.csv.MalformedCsvException;

/**
 *  com.mchange.lang.PotentiallySecondaryException is deprecated: it predates the
 *  cause field Throwable acquired in jdk 1.4. The obvious cleanup is to lift its
 *  three subclasses out of it and extend Exception directly, and the deprecation
 *  warnings invite exactly that.
 *
 *  That would be a breaking change. A client may well catch
 *  PotentiallySecondaryException, or read getNestedThrowable(), and would silently
 *  stop matching. The superclass was therefore kept and the warnings suppressed.
 *
 *  This pins that decision, so removing the superclass fails here rather than in
 *  somebody's application.
 */
@SuppressWarnings("deprecation")
public class PotentiallySecondaryHierarchyJUnitTestCase extends TestCase
{
    public void testSubclassesRemainCatchableAsPotentiallySecondaryException()
    {
	assertCatchable( new XmlPropsException("boom") );
	assertCatchable( new UnreliableIteratorException("boom") );
	assertCatchable( new MalformedCsvException("boom") );
    }

    private void assertCatchable(Exception thrown)
    {
	String name = thrown.getClass().getName();
	try
	    { throw thrown; }
	catch (PotentiallySecondaryException e)
	    { assertSame(name + " should arrive as itself", thrown, e); }
	catch (Exception e)
	    { fail(name + " should still be catchable as PotentiallySecondaryException"); }
    }

    public void testSubclassesRemainPotentiallySecondary()
    {
	assertTrue("XmlPropsException should implement PotentiallySecondary",
		   new XmlPropsException("boom") instanceof PotentiallySecondary);
	assertTrue("UnreliableIteratorException should implement PotentiallySecondary",
		   new UnreliableIteratorException("boom") instanceof PotentiallySecondary);
	assertTrue("MalformedCsvException should implement PotentiallySecondary",
		   new MalformedCsvException("boom") instanceof PotentiallySecondary);
    }

    /**
     *  Both the legacy accessor and the standard one must report the cause, since
     *  either may be what a client reads.
     */
    public void testNestedThrowableAndCauseAgree()
    {
	Throwable cause = new IllegalStateException("root");

	assertCauseReported( new XmlPropsException("boom", cause), cause );
	assertCauseReported( new UnreliableIteratorException("boom", cause), cause );
	assertCauseReported( new MalformedCsvException("boom", cause), cause );
    }

    private void assertCauseReported(PotentiallySecondaryException e, Throwable cause)
    {
	String name = e.getClass().getName();
	assertSame(name + " getNestedThrowable() should report the cause", cause, e.getNestedThrowable());
	assertSame(name + " getCause() should report the cause", cause, e.getCause());
	assertEquals(name + " should keep its message", "boom", e.getMessage());
    }

    /** The no-cause constructors must stay usable and report no cause. */
    public void testConstructorsWithoutCause()
    {
	XmlPropsException e = new XmlPropsException();
	assertNull("an unchained exception should report no nested throwable", e.getNestedThrowable());
	assertNull("an unchained exception should report no cause", e.getCause());

	MalformedCsvException m = new MalformedCsvException("just a message");
	assertEquals("just a message", m.getMessage());
	assertNull("a message-only exception should report no cause", m.getCause());
    }
}
