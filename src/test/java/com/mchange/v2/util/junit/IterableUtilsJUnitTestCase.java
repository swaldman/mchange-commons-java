/*
 * Distributed as part of mchange-commons-java 0.2.11
 *
 * Copyright (C) 2015 Machinery For Change, Inc.
 *
 * Author: Steve Waldman <swaldman@mchange.com>
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of EITHER:
 *
 *     1) The GNU Lesser General Public License (LGPL), version 2.1, as
 *        published by the Free Software Foundation
 *
 * OR
 *
 *     2) The Eclipse Public License (EPL), version 1.0
 *
 * You may choose which license to accept if you wish to redistribute
 * or modify this work. You may offer derivatives of this work
 * under the license you have chosen, or you may provide the same
 * choice of license which you have been offered here.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * You should have received copies of both LGPL v2.1 and EPL v1.0
 * along with this software; see the files LICENSE-EPL and LICENSE-LGPL.
 * If not, the text of these licenses are currently available at
 *
 * LGPL v2.1: http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 *  EPL v1.0: http://www.eclipse.org/org/documents/epl-v10.php
 *
 */

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
