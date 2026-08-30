package com.mchange.v1.db.sql;

import java.sql.*;
import com.mchange.v1.util.*;

import com.mchange.util.CommandLineParser;
import com.mchange.util.impl.CommandLineParserImpl;

public class SchemaManager
{
    final static String[] VALID = new String[] {"create", "drop"};

    public static void main(String[] argv)
    {
	Connection con = null;
	try
	    {
		CommandLineParser clp = new CommandLineParserImpl(argv, VALID, null, null);
		boolean create = clp.checkSwitch("create");
		
		if (!clp.checkArgv()) usage();
		if (! (create ^ clp.checkSwitch("drop"))) usage();
		
		String[] unswitched = clp.findUnswitchedArgs();

		if (unswitched.length == 2)
		    con = DriverManager.getConnection(unswitched[0]);
		else if (unswitched.length == 4)
		    con = DriverManager.getConnection(unswitched[0], unswitched[1], unswitched[2]);
		else
		    usage();

		con.setAutoCommit(false);

		Schema s = (Schema) (Class.forName(unswitched[unswitched.length - 1]).newInstance());
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
	    {e.printStackTrace();}
	finally
	    {CleanupUtils.attemptClose(con);}
    }

    static void usage()
    {
	System.err.println("java -Djdbc.drivers=<driverclass> com.mchange.v1.db.sql.SchemaManager" +
			   " [-create | -drop] <jdbc_url> [<user> <password>] <schemaclass>");
	System.exit(-1);
    }
}
