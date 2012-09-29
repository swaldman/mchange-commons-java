/*
 * Distributed as part of mchange-commons-java v.0.2.3
 *
 * Copyright (C) 2012 Machinery For Change, Inc.
 *
 * Author: Steve Waldman <swaldman@mchange.com>
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License version 2.1, as 
 * published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; see the file LICENSE.  If not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307, USA.
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
