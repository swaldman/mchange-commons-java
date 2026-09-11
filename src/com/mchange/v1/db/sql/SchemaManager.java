package com.mchange.v1.db.sql;

import java.sql.*;
import java.lang.reflect.InvocationTargetException;

import com.mchange.v2.cmdline.BadCommandLineException;
import com.mchange.v2.cmdline.CommandLineUtils;
import com.mchange.v2.cmdline.ParsedCommandLine;

import static com.mchange.v2.reflect.ByNameInstantiationUtils.instantiateByNameUnguarded;

public class SchemaManager
{
    final static String[] VALID = new String[] {"create", "drop"};

    // no switch here takes an argument. CommandLineUtils.parse(...) documents null
    // as meaning "none", but NPEs on it, so pass the empty array it really wants.
    final static String[] NO_ARG_SWITCHES = new String[0];

    public static void main(String[] argv)
    {
	Connection con = null;
	try
	    {
		ParsedCommandLine pcl = null;
		try
		    { pcl = CommandLineUtils.parse(argv, "-", VALID, null, NO_ARG_SWITCHES); }
		catch (BadCommandLineException e)
		    { usage(); }

		boolean create = pcl.includesSwitch("create");

		if (! (create ^ pcl.includesSwitch("drop"))) usage();

		String[] unswitched = pcl.getUnswitchedArgs();

		if (unswitched.length == 2)
		    con = DriverManager.getConnection(unswitched[0]);
		else if (unswitched.length == 4)
		    con = DriverManager.getConnection(unswitched[0], unswitched[1], unswitched[2]);
		else
		    usage();

		con.setAutoCommit(false);

                // unguarded because this is intended to be invoked as an explicit command line argument,
                // there should be no surprise in it
		Schema s = (Schema) instantiateByNameUnguarded(unswitched[unswitched.length - 1]);
		if (create)
		    {
			s.createSchema(con);
			System.out.println("Schema created.");
		    }
		else
		    {
			s.dropSchema(con);
			System.out.println("Schema dropped.");
		    }
	    }
	catch (Exception e)
	    {
		// reflective construction wraps whatever the constructor threw; report the cause
		Throwable t = ( e instanceof InvocationTargetException ? e.getCause() : e );
		t.printStackTrace();
	    }
	finally
	    {
		try
		    { if (con != null) con.close(); }
		catch (SQLException e)
		    { e.printStackTrace(); }
	    }
    }

    static void usage()
    {
	System.err.println("java -Djdbc.drivers=<driverclass> com.mchange.v1.db.sql.SchemaManager" +
			   " [-create | -drop] <jdbc_url> [<user> <password>] <schemaclass>");
	System.exit(-1);
    }
}
