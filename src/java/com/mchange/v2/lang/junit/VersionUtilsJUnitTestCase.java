/*
 * Distributed as part of mchange-commonslib v.0.2
 *
 * Copyright (C) 2010 Machinery For Change, Inc.
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
    final static int[]  SAMPLE_ARRAY = {1, 6, 0};
    final static String SAMPLE_VERSION_STRING = "1.6.0_02-ea";
    
    public void testNonIntegralVersionComponents()
    {
        int[] versionArray = VersionUtils.extractVersionNumberArray(SAMPLE_VERSION_STRING);
        assertEquals( versionArray.length, SAMPLE_ARRAY.length);
        for (int i = 0, len = versionArray.length; i < len; ++i)
            assertEquals(versionArray[i], SAMPLE_ARRAY[i]);
    }
}
