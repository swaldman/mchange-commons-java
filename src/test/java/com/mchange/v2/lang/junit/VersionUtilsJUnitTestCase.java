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
