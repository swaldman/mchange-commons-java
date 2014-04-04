/*
 * Distributed as part of mchange-commons-java 0.2.7
 *
 * Copyright (C) 2014 Machinery For Change, Inc.
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

package com.mchange.v2.lang.junit;

import junit.framework.TestCase;
import com.mchange.v2.lang.VersionUtils;

public class VersionUtilsJUnitTestCase extends TestCase
{
    final static int[]  SAMPLE_ARRAY1 = {1, 6, 0, 2};
    final static String SAMPLE_VERSION_STRING1 = "1.6.0_02-ea";
    final static int[]  SAMPLE_ARRAY2 = {1, 7, 0, 147};
    final static String SAMPLE_VERSION_STRING2 = "1.7.0_147-icedtea";
    
    public void testNonIntegralVersionComponents()
    {
	_testNonIntegralVersionComponents(SAMPLE_VERSION_STRING1, SAMPLE_ARRAY1);
	_testNonIntegralVersionComponents(SAMPLE_VERSION_STRING2, SAMPLE_ARRAY2);
    }


    private void _testNonIntegralVersionComponents(String versionString, int[] knownArray)
    {
        int[] versionArray = VersionUtils.extractVersionNumberArray(versionString);
        assertEquals( versionArray.length, knownArray.length );
        for (int i = 0, len = versionArray.length; i < len; ++i)
            assertEquals(versionArray[i], knownArray[i]);
    }
}
