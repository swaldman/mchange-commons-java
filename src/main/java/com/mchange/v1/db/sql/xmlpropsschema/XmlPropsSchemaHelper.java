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

