/*
 * Distributed as part of mchange-commons-java v.0.2.1
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


package com.mchange.v1.db.sql.xmlpropsschema;

import java.sql.*;
import org.xml.sax.*;
import com.mchange.v1.xmlprops.*;
import java.io.InputStream;
import java.util.Properties;
import java.util.StringTokenizer;

public class XmlPropsSchemaHelper
{
    Properties props;

    public XmlPropsSchemaHelper(InputStream is)
	throws XmlPropsException
    {
	DomXmlPropsParser parser = new DomXmlPropsParser();
	props = parser.parseXmlProps( is );
    }

    public PreparedStatement prepareXmlStatement(Connection con, String key)
	throws SQLException
    { return con.prepareStatement( getKey(key) ); }

    public void executeViaStatement(Statement stmt, String key) throws SQLException
    { stmt.executeUpdate(getKey(key)); }

    public StringTokenizer getItems(String key)
    {
	String rawItems = getKey(key);
	return new StringTokenizer(rawItems, ", \t\r\n");
    }

    public String getKey(String key)
    { return props.getProperty( key ).trim(); }
}

