package com.mchange.v2.naming.junit;

import java.lang.reflect.InvocationTargetException;

import junit.framework.TestCase;

import com.mchange.v2.naming.JavaBeanObjectFactory;

/**
 *  Class.newInstance() is deprecated in favor of
 *  getDeclaredConstructor().newInstance(). The two are not equivalent in one respect
 *  that this codebase depends on: Class.newInstance() propagates whatever the
 *  constructor threw, while the reflective form wraps it in an
 *  InvocationTargetException.
 *
 *  Call sites that merely log inside catch(Exception) were migrated as is. Sites that
 *  let a constructor's exception escape, or that test the exception's type, unwrap so
 *  that the old behavior is preserved. This pins that unwrapping, since a later
 *  simplification back to the naive form would compile and pass every other test.
 */
public class ReflectiveConstructionJUnitTestCase extends TestCase
{
    public static class Ordinary
    {
	public Ordinary() {}
    }

    public static class ThrowsChecked
    {
	public ThrowsChecked() throws Exception
	{ throw new java.io.IOException("checked from constructor"); }
    }

    public static class ThrowsUnchecked
    {
	public ThrowsUnchecked()
	{ throw new IllegalStateException("unchecked from constructor"); }
    }

    /** createBlankInstance is protected, so reach it through a subclass. */
    static class ExposedFactory extends JavaBeanObjectFactory
    {
	public Object create(Class c) throws Exception
	{ return createBlankInstance(c); }
    }

    public void testOrdinaryBeanIsConstructed() throws Exception
    {
	Object o = new ExposedFactory().create( Ordinary.class );
	assertNotNull("an ordinary bean should be constructed", o);
	assertTrue("of the requested type", o instanceof Ordinary);
    }

    public void testCheckedConstructorExceptionPropagatesUnwrapped()
    {
	try
	    {
		new ExposedFactory().create( ThrowsChecked.class );
		fail("the constructor's exception should have propagated");
	    }
	catch (Exception e)
	    {
		assertFalse("the constructor's exception should not arrive wrapped, "
			    + "as Class.newInstance() did not wrap it",
			    e instanceof InvocationTargetException);
		assertTrue("the original checked exception should propagate, got " + e,
			   e instanceof java.io.IOException);
		assertEquals("checked from constructor", e.getMessage());
	    }
    }

    public void testUncheckedConstructorExceptionPropagatesUnwrapped()
    {
	try
	    {
		new ExposedFactory().create( ThrowsUnchecked.class );
		fail("the constructor's exception should have propagated");
	    }
	catch (Exception e)
	    {
		assertFalse("the constructor's exception should not arrive wrapped",
			    e instanceof InvocationTargetException);
		assertTrue("the original runtime exception should propagate, got " + e,
			   e instanceof IllegalStateException);
		assertEquals("unchecked from constructor", e.getMessage());
	    }
    }

    /**
     *  A class with no accessible no-arg constructor must still fail, rather than
     *  being quietly constructed by some other route.
     */
    public void testMissingNoArgConstructorFails()
    {
	try
	    {
		new ExposedFactory().create( Integer.class );
		fail("Integer has no no-arg constructor; construction should fail");
	    }
	catch (Exception e)
	    { /* expected */ }
    }
}
