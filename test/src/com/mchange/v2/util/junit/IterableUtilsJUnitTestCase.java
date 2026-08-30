package com.mchange.v2.util.junit;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;

import junit.framework.TestCase;
import com.mchange.v2.util.IterableUtils;

public class IterableUtilsJUnitTestCase extends TestCase
{
    public void testJoinMultipleElements()
    {
	assertEquals(
	    "a, b, c",
	    IterableUtils.joinAsString(", ", Arrays.asList("a", "b", "c"))
	);
    }

    public void testJoinSingleElement()
    {
	assertEquals(
	    "only",
	    IterableUtils.joinAsString(", ", Collections.singletonList("only"))
	);
    }

    public void testJoinEmptyIterable()
    {
	assertEquals(
	    "",
	    IterableUtils.joinAsString(", ", Collections.emptyList())
	);
    }

    public void testJoinWithEmptyDelimiter()
    {
	assertEquals(
	    "abc",
	    IterableUtils.joinAsString("", Arrays.asList("a", "b", "c"))
	);
    }

    public void testJoinWithMultiCharDelimiter()
    {
	assertEquals(
	    "x -> y -> z",
	    IterableUtils.joinAsString(" -> ", Arrays.asList("x", "y", "z"))
	);
    }

    public void testJoinWithNonStringElements()
    {
	assertEquals(
	    "1, 2, 3",
	    IterableUtils.joinAsString(", ", Arrays.asList(Integer.valueOf(1), Integer.valueOf(2), Integer.valueOf(3)))
	);
    }

    public void testJoinWithMixedTypes()
    {
	ArrayList list = new ArrayList();
	list.add("hello");
	list.add(Integer.valueOf(42));
	list.add(Boolean.TRUE);
	assertEquals(
	    "hello|42|true",
	    IterableUtils.joinAsString("|", list)
	);
    }

    public void testJoinWithNullElement()
    {
	ArrayList list = new ArrayList();
	list.add("a");
	list.add(null);
	list.add("c");
	assertEquals(
	    "a, null, c",
	    IterableUtils.joinAsString(", ", list)
	);
    }

    public void testJoinTwoElements()
    {
	assertEquals(
	    "first, second",
	    IterableUtils.joinAsString(", ", Arrays.asList("first", "second"))
	);
    }

    public void testJoinWithEmptyStringElements()
    {
	assertEquals(
	    ", , ",
	    IterableUtils.joinAsString(", ", Arrays.asList("", "", ""))
	);
    }
}
