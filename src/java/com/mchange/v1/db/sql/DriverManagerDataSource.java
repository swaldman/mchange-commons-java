/*
 * Distributed as part of mchange-commons-java v.0.2.4
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

