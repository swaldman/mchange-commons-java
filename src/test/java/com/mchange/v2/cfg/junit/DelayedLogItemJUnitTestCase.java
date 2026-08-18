package com.mchange.v2.cfg.junit;

import junit.framework.TestCase;

import com.mchange.v2.cfg.DelayedLogItem;
import com.mchange.v2.cfg.MConfig;
import com.mchange.v2.log.MLevel;
import com.mchange.v2.log.MLog;
import com.mchange.v2.log.MLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *  DelayedLogItem's value semantics are load-bearing: MLogConfig.logDelayedItems
 *  de-duplicates accumulated items through a HashSet before replaying them, so a
 *  broken equals/hashCode would produce duplicated or lost startup diagnostics.
 */
public final class DelayedLogItemJUnitTestCase extends TestCase
{
    public void testEqualItemsAreEqualAndHashAlike()
    {
        DelayedLogItem a = new DelayedLogItem( DelayedLogItem.Level.FINE, "same text" );
        DelayedLogItem b = new DelayedLogItem( DelayedLogItem.Level.FINE, "same text" );

        assertEquals( a, b );
        assertEquals( a.hashCode(), b.hashCode() );
    }

    public void testLevelAndTextAndExceptionAllDistinguish()
    {
        DelayedLogItem base = new DelayedLogItem( DelayedLogItem.Level.FINE, "text" );

        assertFalse( "level should distinguish",
                     base.equals( new DelayedLogItem( DelayedLogItem.Level.WARNING, "text" ) ) );
        assertFalse( "text should distinguish",
                     base.equals( new DelayedLogItem( DelayedLogItem.Level.FINE, "other" ) ) );
        assertFalse( "exception should distinguish",
                     base.equals( new DelayedLogItem( DelayedLogItem.Level.FINE, "text", new RuntimeException( "boom" ) ) ) );
    }

    public void testNotEqualToOtherTypesOrNull()
    {
        DelayedLogItem item = new DelayedLogItem( DelayedLogItem.Level.FINE, "text" );
        assertFalse( item.equals( "text" ) );
        assertFalse( item.equals( null ) );
    }

    /** The same Throwable instance on both sides compares equal. */
    public void testSameExceptionInstanceComparesEqual()
    {
        RuntimeException e = new RuntimeException( "boom" );
        assertEquals( new DelayedLogItem( DelayedLogItem.Level.WARNING, "text", e ),
                      new DelayedLogItem( DelayedLogItem.Level.WARNING, "text", e ) );
    }

    /** This is the property MLogConfig.logDelayedItems actually depends on. */
    public void testDuplicatesCollapseInAHashSet()
    {
        List<DelayedLogItem> items = new ArrayList<DelayedLogItem>();
        items.add( new DelayedLogItem( DelayedLogItem.Level.FINE, "duplicated" ) );
        items.add( new DelayedLogItem( DelayedLogItem.Level.FINE, "duplicated" ) );
        items.add( new DelayedLogItem( DelayedLogItem.Level.FINE, "distinct" ) );

        Set<DelayedLogItem> uniquerizer = new HashSet<DelayedLogItem>();
        uniquerizer.addAll( items );

        assertEquals( 2, uniquerizer.size() );
    }

    public void testToStringMentionsLevelAndText()
    {
        String s = new DelayedLogItem( DelayedLogItem.Level.WARNING, "the message" ).toString();
        assertTrue( "toString should mention the level: " + s, s.contains( "WARNING" ) );
        assertTrue( "toString should mention the text: " + s,  s.contains( "the message" ) );
    }

    /**
     *  MConfig maps every DelayedLogItem.Level onto an MLevel by reflective field lookup
     *  in a static initializer. A Level with no matching MLevel field would take the whole
     *  class down at load time, so verify the correspondence is total.
     */
    public void testEveryLevelMapsToAnMLevel() throws Exception
    {
        for ( DelayedLogItem.Level level : DelayedLogItem.Level.values() )
        {
            Object mlevel = MLevel.class.getField( level.toString() ).get( null );
            assertNotNull( "no MLevel for DelayedLogItem.Level." + level, mlevel );
            assertTrue( "MLevel." + level + " should be an MLevel", mlevel instanceof MLevel );
        }
    }

    /** dumpToLogger must accept every level, with and without an exception. */
    public void testDumpToLoggerAcceptsEveryLevel()
    {
        MLogger logger = MLog.getLogger( DelayedLogItemJUnitTestCase.class );

        List<DelayedLogItem> items = new ArrayList<DelayedLogItem>();
        for ( DelayedLogItem.Level level : DelayedLogItem.Level.values() )
        {
            items.add( new DelayedLogItem( level, "test item at " + level ) );
            items.add( new DelayedLogItem( level, "test item at " + level + " with exception",
                                           new RuntimeException( "expected, part of the test" ) ) );
        }

        MConfig.dumpToLogger( items, logger ); // must not throw
    }
}
