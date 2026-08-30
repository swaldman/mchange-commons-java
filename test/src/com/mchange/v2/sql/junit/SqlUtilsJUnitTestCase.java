package com.mchange.v2.sql.junit;

import java.sql.*;
import junit.framework.*;
import com.mchange.v2.sql.SqlUtils;

public class SqlUtilsJUnitTestCase extends TestCase
{
    public void testGoodDebugLoggingOfNestedExceptions()
    {
	// this is only supposed to complete (in response to a bug where logging of 
	// nested Exceptions was an infinite loop.
	SQLException original = new SQLException("Original.");
	SQLWarning nestedWarning = new SQLWarning("Nested Warning.");
	original.setNextException( nestedWarning );
	SqlUtils.toSQLException( original );
    }

    public static void main(String[] argv)
    { 
	junit.textui.TestRunner.run( new TestSuite( SqlUtilsJUnitTestCase.class ) ); 
	//junit.swingui.TestRunner.run( SqlUtilsJUnitTestCase.class ); 
	//new SqlUtilsJUnitTestCase().testGoodDebugLoggingOfNestedExceptions();
    }
}
