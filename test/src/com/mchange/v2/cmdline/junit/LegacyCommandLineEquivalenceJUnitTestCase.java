package com.mchange.v2.cmdline.junit;

import java.util.Arrays;

import junit.framework.TestCase;

import com.mchange.util.CommandLineParser;
import com.mchange.util.impl.CommandLineParserImpl;
import com.mchange.v2.cmdline.BadCommandLineException;
import com.mchange.v2.cmdline.CommandLineUtils;
import com.mchange.v2.cmdline.ParsedCommandLine;

/**
 *  com.mchange.util.CommandLineParser is deprecated in favor of
 *  com.mchange.v2.cmdline.CommandLineUtils, and SchemaManager was moved across.
 *  Both implementations still exist, so the presumption that they agree can be
 *  checked rather than assumed -- for the shape of command line the deprecation
 *  note tells callers to migrate.
 *
 *  Scope: single character '-' prefix, a fixed set of valid switches, and no switch
 *  taking an argument. The two parsers are NOT equivalent in general; the v2 parser
 *  additionally understands "--switch=value" and a configurable prefix.
 */
public class LegacyCommandLineEquivalenceJUnitTestCase extends TestCase
{
    final static String[] VALID = { "create", "drop" };

    //
    // CommandLineUtils.parse documents null as "no switch takes an argument", but
    // ParsedCommandLineImpl.contains() does not null guard that parameter and throws
    // NullPointerException. Callers (SchemaManager among them) must pass the empty
    // array instead, which is also what CommandLineParserImpl substitutes for null.
    //
    final static String[] NO_ARG_SWITCHES = new String[0];

    final static String[][] COMMAND_LINES = {
	{ "-create", "jdbc:test:db", "com.example.Schema" },
	{ "-drop", "jdbc:test:db", "com.example.Schema" },
	{ "-create", "jdbc:test:db", "user", "secret", "com.example.Schema" },
	{ "-create", "-drop", "jdbc:test:db", "com.example.Schema" },
	{ "jdbc:test:db", "com.example.Schema" },
	{ "-bogus", "jdbc:test:db", "com.example.Schema" },
	{ "-create" },
	{},
    };

    /**
     *  Renders whatever SchemaManager actually asks of a parser, so that a drift in
     *  either implementation shows up as a mismatched description.
     */
    @SuppressWarnings("deprecation")
    private String describeLegacy(String[] argv)
    {
	CommandLineParser clp = new CommandLineParserImpl(argv, VALID, null, null);
	if (! clp.checkArgv())
	    return "rejected";
	return "create=" + clp.checkSwitch("create")
	    + " drop=" + clp.checkSwitch("drop")
	    + " unswitched=" + Arrays.toString(clp.findUnswitchedArgs());
    }

    private String describeCurrent(String[] argv)
    {
	ParsedCommandLine pcl;
	try
	    { pcl = CommandLineUtils.parse(argv, "-", VALID, null, NO_ARG_SWITCHES); }
	catch (BadCommandLineException e)
	    { return "rejected"; }
	return "create=" + pcl.includesSwitch("create")
	    + " drop=" + pcl.includesSwitch("drop")
	    + " unswitched=" + Arrays.toString(pcl.getUnswitchedArgs());
    }

    public void testParsersAgreeOnSchemaManagerStyleCommandLines()
    {
	for (int i = 0; i < COMMAND_LINES.length; ++i)
	    {
		String[] argv = COMMAND_LINES[i];
		assertEquals("parsers should agree on " + Arrays.toString(argv),
			     describeLegacy(argv), describeCurrent(argv));
	    }
    }

    /**
     *  Guards the descriptions above against becoming vacuous: if both parsers
     *  degenerated to "rejected" (or to empty results) the equivalence test would
     *  still pass while checking nothing.
     */
    public void testDescriptionsAreDiscriminating()
    {
	String create = describeCurrent(new String[] { "-create", "jdbc:test:db", "com.example.Schema" });
	String drop   = describeCurrent(new String[] { "-drop", "jdbc:test:db", "com.example.Schema" });
	String bogus  = describeCurrent(new String[] { "-bogus", "jdbc:test:db", "com.example.Schema" });

	assertFalse("a valid command line should not be rejected", "rejected".equals(create));
	assertFalse("-create and -drop should describe differently", create.equals(drop));
	assertEquals("an unknown switch should be rejected", "rejected", bogus);
	assertTrue("unswitched args should be reported",
		   create.indexOf("jdbc:test:db") >= 0 && create.indexOf("com.example.Schema") >= 0);
    }

    /**
     *  Pins the workaround SchemaManager depends on. If parse() ever stops needing
     *  the empty array, this still passes; it only fails if the empty array breaks.
     */
    public void testEmptyArgSwitchesIsAccepted() throws Exception
    {
	ParsedCommandLine pcl = CommandLineUtils.parse(
	    new String[] { "-create", "jdbc:test:db" }, "-", VALID, null, NO_ARG_SWITCHES);
	assertTrue("-create should be seen", pcl.includesSwitch("create"));
	assertFalse("-drop was not given", pcl.includesSwitch("drop"));
	assertEquals("one unswitched arg", 1, pcl.getUnswitchedArgs().length);
	assertEquals("jdbc:test:db", pcl.getUnswitchedArgs()[0]);
    }
}
