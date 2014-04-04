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
