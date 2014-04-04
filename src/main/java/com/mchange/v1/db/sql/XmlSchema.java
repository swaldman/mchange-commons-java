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
import java.net.*;
import java.sql.*;
import java.util.*;
import org.xml.sax.*;
import javax.xml.parsers.*;

public class XmlSchema implements Schema
{
    private final static int CREATE = 0;
    private final static int DROP   = 1;

    List createStmts;
    List dropStmts;
    Map  appMap;

    public XmlSchema(URL xmlSchema) throws SAXException, IOException, ParserConfigurationException
    {parse(xmlSchema.openStream());}

    public XmlSchema(InputStream xmlStream) throws SAXException, IOException, ParserConfigurationException
    {parse(xmlStream);}

    public XmlSchema()
    {}

    public void parse(InputStream is) throws SAXException, IOException, ParserConfigurationException
    {
	createStmts = new ArrayList();
	dropStmts   = new ArrayList();
	appMap      = new HashMap();

	InputSource isrc = new InputSource();
	isrc.setByteStream(is);

	/*
	 * This doesn't seem to matter. How do we validate???
	 */
	isrc.setSystemId(XmlSchema.class.getResource("schema.dtd").toExternalForm());

	SAXParser sp = SAXParserFactory.newInstance().newSAXParser();
	MySaxHandler testHandler = new MySaxHandler();

	// apparently the finalized interface no
	// longer includes these functions..
	// everything is set, I hope, in parse...

	//sp.setDocumentHandler(testHandler);
	//sp.setErrorHandler(testHandler);

	sp.parse(isrc, testHandler);
    }

    private void doStatementList(List stmtList, Connection con)
	throws SQLException
    {
	if (stmtList != null)
	    {
		Statement stmt = null;
		try
		    {
			stmt = con.createStatement();
			for (Iterator ii = stmtList.iterator(); ii.hasNext();)
			    stmt.executeUpdate((String) ii.next());
			con.commit();
		    }
		catch (SQLException e)
		    {
			ConnectionUtils.attemptRollback(con);
			e.fillInStackTrace();
			throw e;
		    }
		finally
		    {StatementUtils.attemptClose(stmt);}
	    }
    }

    public String getStatementText(String appName, String stmtName)
    {
	SqlApp app = (SqlApp) appMap.get(appName);
	String out = null;
	if (app != null)
	    out = app.getStatementText(stmtName);
	return out;
    }

    public void createSchema(Connection con) throws SQLException
    {doStatementList(createStmts, con);}

    public void dropSchema(Connection con) throws SQLException
    {doStatementList(dropStmts, con);}


    public static void main(String[] argv)
    {
	try
	    {
		Schema s = new XmlSchema(XmlSchema.class.getResource("/com/mchange/v1/hjug/hjugschema.xml"));
		
	    }
	catch (Exception e)
	    {e.printStackTrace();}

    }

    class MySaxHandler extends HandlerBase
    {
	int          state = -1;             //-1 unless we are in drop or create
	boolean      in_statement    = false;
	boolean      in_comment      = false;
	StringBuffer charBuff        = null;
	SqlApp       currentApp      = null; //non-null iff we are in an application
	String       currentStmtName = null; //non-null iff we are in a named (application) stmt

	public void startElement(String name, AttributeList attributes)
	{
	    if (name.equals("create"))
		state = CREATE;
	    else if (name.equals("drop"))
		state = DROP;
	    else if (name.equals("statement"))
		{
		    in_statement = true;
		    charBuff = new StringBuffer();
		    if (currentApp != null)
			{
			    for (int i = 0, len = attributes.getLength(); i < len; ++i)
				{
				    String attr = attributes.getName(i);
				    if (attr.equals("name"))
					{
					    currentStmtName = attributes.getValue(i);
					    break;
					}
				}
			}
		}
	    else if (name.equals("comment"))
		in_comment = true;
	    else if (name.equals("application"))
		{
		    for (int i = 0, len = attributes.getLength(); i < len; ++i)
			{
			    String attr = attributes.getName(i);
			    if (attr.equals("name"))
				{
				    String appName = attributes.getValue(i);
				    currentApp     = (SqlApp) appMap.get(appName);
				    if (currentApp == null)
					{
					    currentApp = new SqlApp();
					    appMap.put(appName.intern(), currentApp);
					}
				    break;
				}
			}
		}
	}

	public void characters(char[] ch, int start, int length)
	    throws SAXException
	{
	    if (!in_comment)
		{
		    if (in_statement)
			charBuff.append(ch, start, length);
		}
	}

	public void endElement(String name)
	{
	    if (name.equals("statement"))
		{
		    String stmtStr = charBuff.toString().trim();
		    if (state == CREATE)
			createStmts.add(stmtStr);
		    else if (state == DROP)
			dropStmts.add(stmtStr);
		    //System.out.println("Statement: " + stmtStr);
		    else if (currentApp != null && currentStmtName != null) //named app stmt
			currentApp.setStatementText(currentStmtName, stmtStr);
		}
	    else if (name.equals("create") || name.equals("drop"))
		state = -1;
	    else if (name.equals("comment"))
		in_comment = false;
	    else if (name.equals("application"))
		currentApp = null;
	}

	//from IBM examples,.,.
	/** Warning. */
	public void warning(SAXParseException ex) {
	    System.err.println("[Warning] "+
			       //getLocationString(ex)+": "+
			       ex.getMessage());
	}
		
	/** Error. */
	public void error(SAXParseException ex) {
	    System.err.println("[Error] "+
			       //getLocationString(ex)+": "+
			       ex.getMessage());
	}
		
	/** Fatal error. */
	public void fatalError(SAXParseException ex) throws SAXException {
	    System.err.println("[Fatal Error] "+
			       //getLocationString(ex)+": "+
			       ex.getMessage());
	    throw ex;
	}
    }

    class SqlApp
    {
	Map stmtMap = new HashMap();

	public void setStatementText(String name, String sql)
	{stmtMap.put(name, sql);}

	public String getStatementText(String name)
	{return (String) stmtMap.get(name);}
    }
}
