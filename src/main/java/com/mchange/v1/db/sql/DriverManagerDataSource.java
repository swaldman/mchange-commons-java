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

import java.io.*;
import java.util.*;
import java.sql.*;
import javax.naming.*;
import javax.naming.spi.ObjectFactory;
import javax.sql.DataSource;
import com.mchange.io.UnsupportedVersionException;

/**
 * @deprecated use com.mchange.v2.c3p0.DriverManagerDataSource
 */
public class DriverManagerDataSource implements DataSource, Serializable, Referenceable
{
    final static String REF_FACTORY_NAME = DmdsObjectFactory.class.getName();
    final static String REF_JDBC_URL     = "jdbcUrl";
    final static String REF_DFLT_USER    = "dfltUser";
    final static String REF_DFLT_PWD     = "dfltPassword";

    String jdbcUrl;
    String dfltUser;
    String dfltPassword;

    public DriverManagerDataSource(String jdbcUrl, String dfltUser, String dfltPassword)
    {
	this.jdbcUrl = jdbcUrl;
	this.dfltUser = dfltUser;
	this.dfltPassword = dfltPassword;
    }

    public DriverManagerDataSource(String jdbcUrl)
    { this( jdbcUrl, null, null ); }

    public Connection getConnection() throws SQLException
    { 
	//  				System.err.println( "user: " + dfltUser );
	//  				System.err.println( "pass: " + dfltPassword );
	return DriverManager.getConnection( jdbcUrl, createProps(null, null) ); 
    }

    public Connection getConnection(String username, String password) throws SQLException
    { 
	//  				System.err.println( "user: " + username );
	//  				System.err.println( "pass: " + password );
	return DriverManager.getConnection( jdbcUrl, createProps(username, password) ); 
    }

    public PrintWriter getLogWriter() throws SQLException
    { return DriverManager.getLogWriter(); }

    public void setLogWriter(PrintWriter out) throws SQLException
    { DriverManager.setLogWriter( out ); }

    public int getLoginTimeout() throws SQLException
    { return DriverManager.getLoginTimeout(); }

    public void setLoginTimeout(int seconds) throws SQLException
    { DriverManager.setLoginTimeout( seconds ); }

    // JDBC4 add-ons
    public boolean isWrapperFor(Class<?> iface) throws SQLException
    { return false; }

    public <T> T unwrap(Class<T> iface) throws SQLException
    { throw new SQLException( this.getClass().getName() + " is not a wrapper for an object implementing any interface." ); }


    public Reference getReference() throws NamingException
    {
	Reference out = new Reference(this.getClass().getName(),
				      REF_FACTORY_NAME,
				      null);
	out.add( new StringRefAddr( REF_JDBC_URL, jdbcUrl ) );
	out.add( new StringRefAddr( REF_DFLT_USER, dfltUser ) );
	out.add( new StringRefAddr( REF_DFLT_PWD, dfltPassword ) );

	return out;
    }

    // JDK7 add-on
    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException
    { throw new SQLFeatureNotSupportedException("javax.sql.DataSource.getParentLogger() is not currently supported by " + this.getClass().getName());}

    private Properties createProps(String user, String password)
    {
	Properties props = new Properties();
	if (user != null)
	    {
		props.put("user", user);
		props.put("password", password);
	    }
	else if (dfltUser != null)
	    {
		props.put("user", dfltUser);
		props.put("password", dfltPassword);
	    }
	return props;
    }

    //Serialization
    static final long serialVersionUID = 1; //override to take control of versioning
    private final static short VERSION = 0x0001;
    
    private void writeObject(ObjectOutputStream out) throws IOException
    {
	out.writeShort(VERSION);
	out.writeUTF(jdbcUrl);
	out.writeUTF(dfltUser);
	out.writeUTF(dfltPassword);
    }
    
    private void readObject(ObjectInputStream in) throws IOException
    {
	short version = in.readShort();
	switch (version)
	    {
	    case 0x0001:
		this.jdbcUrl = in.readUTF();
		this.dfltUser = in.readUTF();
		this.dfltPassword = in.readUTF();
		break;
	    default:
		throw new UnsupportedVersionException(this, version);
	    }
    }

    //ObjectFactory for JNDI referencing
    public static class DmdsObjectFactory implements ObjectFactory
    {
	public Object getObjectInstance(Object refObj, Name name, Context nameCtx, Hashtable env)
	    throws Exception
	{
	    Reference ref;
	    String className = DriverManagerDataSource.class.getName();
	    if (refObj instanceof Reference && 
		(ref = (Reference) refObj).getClassName().equals(className))
		{
		    return new DriverManagerDataSource((String) ref.get(REF_JDBC_URL).getContent(),
						       (String) ref.get(REF_DFLT_USER).getContent(),
						       (String) ref.get(REF_DFLT_PWD).getContent());
		} 
	    else
		return null;
	}
    }
}

