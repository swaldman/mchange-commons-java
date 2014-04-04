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

package com.mchange.v1.xmlprops;

import java.io.*;
import java.sql.*;
import java.util.*;
import org.xml.sax.*;

import com.mchange.v1.xml.StdErrErrorHandler;
import com.mchange.v1.util.StringTokenizerUtils;

public class SaxXmlPropsParser
{
    final static String DEFAULT_XML_READER = "org.apache.xerces.parsers.SAXParser";
    final static String XMLPROPS_NAMESPACE_URI = "http://www.mchange.com/namespaces/xmlprops";

    public static Properties parseXmlProps(InputStream istr) throws XmlPropsException
    {
	try
	    {
		//TODO: let a system property or somesuch determine the
		//      XMLReader class...
		String readerClass = DEFAULT_XML_READER;
		XMLReader reader = (XMLReader) Class.forName( readerClass ).newInstance();
		InputSource is = new InputSource( istr );
		return parseXmlProps( is, reader, null, null);
	    }
	catch (XmlPropsException e)
	    { throw e; }
	catch (Exception e)
	     {
		 e.printStackTrace();
		 throw new XmlPropsException("Exception while instantiating XMLReader.", e);
	     }
    } 

     private static Properties parseXmlProps(InputSource is, XMLReader saxy, 
					    EntityResolver eresolv, ErrorHandler grrr) 
	 throws XmlPropsException
     {
	 try
	     {
		 if (eresolv != null)
		     saxy.setEntityResolver( eresolv );
		 if (grrr == null)
		     grrr = new StdErrErrorHandler();
		 saxy.setErrorHandler( grrr );
		 XmlPropsContentHandler fsch = new XmlPropsContentHandler(); 
		 saxy.setContentHandler( fsch );
		 saxy.parse(is);
		 return fsch.getLastProperties();
	     }
	 catch (Exception e)
	     {
		 if (e instanceof SAXException)
		     {
			 ((SAXException) e).getException().printStackTrace();
		     }
		 e.printStackTrace();
		 throw new XmlPropsException(e);
	     }
     }

    //we presume that the xml is being validated by the parser
    //here, and do not double-check constraints defined by the DTD
    static class XmlPropsContentHandler implements ContentHandler 
    {
	Locator locator;

	Properties props;
	String name;
	StringBuffer valueBuf;

	public void setDocumentLocator(Locator locator)
	{ this.locator = locator; }

	public void startDocument() throws SAXException
	{ 
	    props  = new Properties();
	}

	public void startElement(String namespaceURI, String localName, String qName, Attributes atts)
	{
	    System.err.println("--> startElement( " + namespaceURI + ", " + localName + ", "  + atts + ")");
	    if (!namespaceURI.equals("") && !namespaceURI.equals( XMLPROPS_NAMESPACE_URI ))
		return;
	    
	    if (localName.equals( "property" ))
		{
		    name = atts.getValue( namespaceURI, "name" );
		    valueBuf = new StringBuffer();
		}
	}

	public void characters(char[] ch, int start, int length) throws SAXException
	{
	    if (valueBuf != null)
		valueBuf.append(ch, start, length); 
	}

	public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException
	{ 
	    if (valueBuf != null)
		valueBuf.append(ch, start, length); 
	}

	public void endElement(String namespaceURI, String localName, String qName) throws SAXException
	{
	    if (!namespaceURI.equals("") && !namespaceURI.equals( XMLPROPS_NAMESPACE_URI ))
		return;
	    
	    if ( localName.equals( "property" ) )
		{
		    System.err.println("NAME: " + name);
		    props.put(name, valueBuf.toString());
		    valueBuf = null;
		}
	}

	public void endDocument() throws SAXException
	{}

	public void startPrefixMapping(String prefix, String uri) throws SAXException
	{}



	public void endPrefixMapping(String prefix) throws SAXException
	{}

	public void processingInstruction(String target, String data) throws SAXException
	{}

	public void skippedEntity(String name) throws SAXException
	{}

	public Properties getLastProperties()
	{ return props; }
    }

    public static void main(String[] argv)
    {
	try
	    {
		InputStream is = new BufferedInputStream( new FileInputStream( argv[0] ) );
		SaxXmlPropsParser parser = new SaxXmlPropsParser();
		Properties props = parser.parseXmlProps( is );
  		for (Iterator ii = props.keySet().iterator(); ii.hasNext(); )
		    {
			String key = (String) ii.next();
			String value = props.getProperty(key);
			System.err.println(key + '=' + value);
		    }
	    }
	catch (Exception e)
	    { e.printStackTrace(); }
    }
}





