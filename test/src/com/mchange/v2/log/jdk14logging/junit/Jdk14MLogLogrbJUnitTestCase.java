package com.mchange.v2.log.jdk14logging.junit;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import junit.framework.TestCase;

import com.mchange.v2.log.MLevel;
import com.mchange.v2.log.MLog;
import com.mchange.v2.log.MLogger;

/**
 *  MLogger.logrb takes the resource bundle by name, and Jdk14MLog delegates to
 *  java.util.logging.Logger's name-taking logrb, which jdk 9 deprecated in favor of
 *  an overload taking a ResourceBundle instance. Those calls are suppressed rather
 *  than migrated.
 *
 *  The reason is behavioral, so it is pinned here. Resolving the name locally --
 *  what a migration would have to do -- changes two things: ResourceBundle.getBundle
 *  throws on an unknown or null name, where logging one is a non-event today; and it
 *  resolves against the caller's ClassLoader rather than the thread context loader
 *  jdk logging consults, which loses bundles in a container.
 *
 *  This covers the first, which is testable without a container. A rewrite that
 *  resolves the bundle itself and does not swallow MissingResourceException, or that
 *  does not guard null, fails here.
 */
public class Jdk14MLogLogrbJUnitTestCase extends TestCase
{
    final static String LOGGER_NAME = "com.mchange.test.logrb";

    private Logger      julLogger;
    private Handler     captured;
    private List        records;
    private boolean     savedUseParentHandlers;

    @Override
    public void setUp()
    {
	records = new ArrayList();
	julLogger = Logger.getLogger( LOGGER_NAME );
	savedUseParentHandlers = julLogger.getUseParentHandlers();
	julLogger.setUseParentHandlers( false );
	julLogger.setLevel( Level.ALL );
	captured = new Handler()
	{
	    @Override
	    public void publish(LogRecord r) { records.add(r); }
	    @Override
	    public void flush() {}
	    @Override
	    public void close() {}
	};
	julLogger.addHandler( captured );
    }

    @Override
    public void tearDown()
    {
	if (julLogger != null)
	    {
		julLogger.removeHandler( captured );
		julLogger.setUseParentHandlers( savedUseParentHandlers );
		julLogger.setLevel( null );
	    }
    }

    private MLogger logger()
    {
	MLog mlog = MLog.findByClassnames(
	    new String[] { "com.mchange.v2.log.jdk14logging.Jdk14MLog" }, false );
	assertNotNull( "Jdk14MLog should be loadable", mlog );
	return mlog.getLogger( LOGGER_NAME );
    }

    /**
     *  A bundle name that does not resolve must still log, unlocalized, rather than
     *  throwing out of the logging call.
     */
    public void testUnresolvableBundleNameDoesNotThrow()
    {
	MLogger l = logger();

	l.logrb( MLevel.INFO, "C", "m", "NoSuchBundleAnywhere", "plain" );
	l.logrb( MLevel.INFO, "C", "m", "NoSuchBundleAnywhere", "with-param", "p" );
	l.logrb( MLevel.INFO, "C", "m", "NoSuchBundleAnywhere", "with-params",
		 new Object[] { "p", "q" } );
	l.logrb( MLevel.INFO, "C", "m", "NoSuchBundleAnywhere", "with-thrown",
		 new Exception("boom") );

	assertEquals( "all four overloads should have logged", 4, records.size() );
    }

    /** A null bundle name is likewise tolerated; getBundle(null) would NPE. */
    public void testNullBundleNameDoesNotThrow()
    {
	MLogger l = logger();

	l.logrb( MLevel.INFO, "C", "m", null, "plain" );
	l.logrb( MLevel.INFO, "C", "m", null, "with-param", "p" );
	l.logrb( MLevel.INFO, "C", "m", null, "with-params", new Object[] { "p", "q" } );
	l.logrb( MLevel.INFO, "C", "m", null, "with-thrown", new Exception("boom") );

	assertEquals( "all four overloads should have logged", 4, records.size() );
    }

    /**
     *  Keeps the counts above meaningful: the messages must actually reach the
     *  records, so a logger that silently dropped everything could not pass.
     */
    public void testMessagesReachTheRecords()
    {
	logger().logrb( MLevel.INFO, "C", "m", "NoSuchBundleAnywhere", "a-distinctive-message" );

	assertEquals( 1, records.size() );
	LogRecord r = (LogRecord) records.get(0);
	assertEquals( "the raw message should survive", "a-distinctive-message", r.getMessage() );
	assertEquals( "the bundle name should be carried on the record",
		      "NoSuchBundleAnywhere", r.getResourceBundleName() );
	assertNull( "an unresolvable bundle should simply be absent", r.getResourceBundle() );
    }
}
