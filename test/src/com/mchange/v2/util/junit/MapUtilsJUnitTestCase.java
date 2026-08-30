package com.mchange.v2.util.junit;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collections;
import java.util.AbstractMap;

import junit.framework.TestCase;
import com.mchange.v2.util.MapUtils;

public class MapUtilsJUnitTestCase extends TestCase
{
    // --- entryAsString tests ---

    public void testEntryAsStringNoQuotes()
    {
	Map.Entry entry = new AbstractMap.SimpleEntry("host", "localhost");
	assertEquals(
	    "host=localhost",
	    MapUtils.entryAsString(false, false, "=", entry)
	);
    }

    public void testEntryAsStringQuoteBoth()
    {
	Map.Entry entry = new AbstractMap.SimpleEntry("host", "localhost");
	assertEquals(
	    "\"host\"=\"localhost\"",
	    MapUtils.entryAsString(true, true, "=", entry)
	);
    }

    public void testEntryAsStringQuoteKeyOnly()
    {
	Map.Entry entry = new AbstractMap.SimpleEntry("host", "localhost");
	assertEquals(
	    "\"host\"=localhost",
	    MapUtils.entryAsString(true, false, "=", entry)
	);
    }

    public void testEntryAsStringQuoteValueOnly()
    {
	Map.Entry entry = new AbstractMap.SimpleEntry("host", "localhost");
	assertEquals(
	    "host=\"localhost\"",
	    MapUtils.entryAsString(false, true, "=", entry)
	);
    }

    public void testEntryAsStringCustomConnector()
    {
	Map.Entry entry = new AbstractMap.SimpleEntry("key", "value");
	assertEquals(
	    "key -> value",
	    MapUtils.entryAsString(false, false, " -> ", entry)
	);
    }

    public void testEntryAsStringWithNonStringTypes()
    {
	Map.Entry entry = new AbstractMap.SimpleEntry(Integer.valueOf(1), Boolean.TRUE);
	assertEquals(
	    "1: true",
	    MapUtils.entryAsString(false, false, ": ", entry)
	);
    }

    // --- joinEntriesIntoString tests ---

    public void testJoinSingleEntry()
    {
	Map map = new LinkedHashMap();
	map.put("a", "1");
	assertEquals(
	    "a=1",
	    MapUtils.joinEntriesIntoString(false, false, "=", ", ", map)
	);
    }

    public void testJoinMultipleEntries()
    {
	Map map = new LinkedHashMap();
	map.put("a", "1");
	map.put("b", "2");
	map.put("c", "3");
	assertEquals(
	    "a=1, b=2, c=3",
	    MapUtils.joinEntriesIntoString(false, false, "=", ", ", map)
	);
    }

    public void testJoinEmptyMap()
    {
	assertEquals(
	    "",
	    MapUtils.joinEntriesIntoString(false, false, "=", ", ", Collections.emptyMap())
	);
    }

    public void testJoinWithQuotedKeysAndValues()
    {
	Map map = new LinkedHashMap();
	map.put("host", "localhost");
	map.put("port", "8080");
	assertEquals(
	    "\"host\"=\"localhost\", \"port\"=\"8080\"",
	    MapUtils.joinEntriesIntoString(true, true, "=", ", ", map)
	);
    }

    public void testJoinWithCustomConnectorAndDelimiter()
    {
	Map map = new LinkedHashMap();
	map.put("x", "1");
	map.put("y", "2");
	assertEquals(
	    "x -> 1 | y -> 2",
	    MapUtils.joinEntriesIntoString(false, false, " -> ", " | ", map)
	);
    }

    public void testJoinNoDelimiterBetweenEntries()
    {
	Map map = new LinkedHashMap();
	map.put("a", "1");
	map.put("b", "2");
	assertEquals(
	    "a=1b=2",
	    MapUtils.joinEntriesIntoString(false, false, "=", "", map)
	);
    }
}
