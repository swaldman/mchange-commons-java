package com.mchange.v2.cfg.junit;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.List;

/**
 *  Covers discovery of the resource-path text files that traditionally define where
 *  config may live, per the MConfig class documentation:
 *
 *    /com/mchange/v2/cfg/vmConfigResourcePaths.txt
 *    /com/mchange/v2/cfg/defaultConfigResourcePaths.txt
 *    /mchange-config-resource-paths.txt
 *
 *  read in that order, with all non-blank, non-'#' lines treated as resource paths.
 */
public final class PathFileDiscoveryJUnitTestCase extends TestCase
{
    private static List<String> readPaths( CfgScenario s, Object mpc )
    { return Arrays.asList( s.getPropertiesResourcePaths( mpc ) ); }

    /** A path file at the legacy vmConfig location supplies paths. */
    public void testVmConfigPathFileIsRead()
    {
        CfgScenario s = CfgScenario.open( "vmconfig-only" );
        try
        {
            Object mpc = s.traditionalCached();
            assertEquals( "vmconfig", s.getProperty( mpc, "which.pathfile" ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /** defaultConfigResourcePaths.txt is the recent addition to the list, and is read. */
    public void testDefaultConfigPathFileIsRead()
    {
        CfgScenario s = CfgScenario.open( "defaultconfig-only" );
        try
        {
            Object mpc = s.traditionalCached();
            assertEquals( "defaultconfig", s.getProperty( mpc, "which.pathfile" ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /** The unqualified, root-level path file is read. */
    public void testMchangeConfigPathFileIsRead()
    {
        CfgScenario s = CfgScenario.open( "mchangeconfig-only" );
        try
        {
            Object mpc = s.traditionalCached();
            assertEquals( "mchangeconfig", s.getProperty( mpc, "which.pathfile" ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /**
     *  All three path files are read and CONCATENATED in declaration order. This is
     *  what makes the MConfig javadoc's early.properties/late.properties example hold:
     *  paths contributed by a later file end up later in the list, so they win.
     */
    public void testAllThreePathFilesConcatenateInOrder()
    {
        CfgScenario s = CfgScenario.open( "all-three-pathfiles" );
        try
        {
            Object mpc = s.traditionalCached();

            assertEquals( "paths should be ordered vmConfig, defaultConfig, mchange-config",
                          Arrays.asList( "/early.properties", "/middle.properties", "/late.properties" ),
                          readPaths( s, mpc ) );

            assertEquals( "the last path file's contribution should win",
                          "late", s.getProperty( mpc, "ordered.key" ) );

            // every file still contributes its non-conflicting keys
            assertEquals( "yes", s.getProperty( mpc, "only.early" ) );
            assertEquals( "yes", s.getProperty( mpc, "only.middle" ) );
            assertEquals( "yes", s.getProperty( mpc, "only.late" ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /** Blank lines, '#' comments, and surrounding whitespace are ignored/trimmed. */
    public void testCommentsBlanksAndWhitespaceAreIgnored()
    {
        CfgScenario s = CfgScenario.open( "pathfile-comments-blanks" );
        try
        {
            Object mpc = s.traditionalCached();

            assertEquals( "only the two real, trimmed paths should survive",
                          Arrays.asList( "/padded.properties", "/second.properties" ),
                          readPaths( s, mpc ) );

            assertEquals( "yes", s.getProperty( mpc, "padded" ) );
            assertEquals( "yes", s.getProperty( mpc, "second" ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /**
     *  When path files DO supply paths, the hardcoded backstop defaults are not used --
     *  even though /mchange-commons.properties exists in this scenario. This is the
     *  documented way to keep the library out of the default locations entirely.
     */
    public void testSuppliedPathsSuppressHardcodedDefaults()
    {
        CfgScenario s = CfgScenario.open( "vmconfig-only" );
        try
        {
            Object mpc = s.traditionalCached();

            assertNull( "/mchange-commons.properties should not have been consulted",
                        s.getProperty( mpc, "backstop.source" ) );
            assertFalse( "backstop paths should be absent from the resolved list: " + readPaths( s, mpc ),
                         readPaths( s, mpc ).contains( "/mchange-commons.properties" ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /**
     *  A path file that EXISTS but parses to zero paths still falls back to the hardcoded
     *  defaults -- the test is on the number of paths yielded, not on file existence.
     *  This is the javadoc's "or no paths are parsed from them" clause.
     */
    public void testEmptyPathFileFallsBackToHardcodedDefaults()
    {
        CfgScenario s = CfgScenario.open( "pathfile-empty" );
        try
        {
            Object mpc = s.traditionalCached();
            assertEquals( "backstop should be in force when a path file yields nothing",
                          "mchange-commons", s.getProperty( mpc, "backstop.source" ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /** With no path files at all, the hardcoded backstop is in force. */
    public void testNoPathFilesUsesHardcodedDefaults()
    {
        CfgScenario s = CfgScenario.open( "no-pathfiles" );
        try
        {
            Object mpc = s.traditionalCached();
            assertEquals( "mchange-commons", s.getProperty( mpc, "backstop.source" ) );
            assertTrue( "the backstop's /mchange-commons.properties should have been read: " + readPaths( s, mpc ),
                        readPaths( s, mpc ).contains( "/mchange-commons.properties" ) );
        }
        finally
        { s.closeQuietly(); }
    }
}
