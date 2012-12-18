/*
 * Distributed as part of mchange-commons-java v.0.2.3.3
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


package com.mchange.v2.util;

import java.io.*;
import java.util.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.w3c.dom.*;
import org.xml.sax.*;

//Example Document:
//
//  <?xml version="1.0" encoding="UTF-8"?>
//
//  <!DOCTYPE xml-properties SYSTEM "http://www.mchange.com/dtd/xml-properties.dtd">
//
//  <xml-properties>
//     <property name="key1">This is the first value.</property>
//     <property name="key2" trim="false">Spaced Label: </property>
//     <property name="key3" trim="true"><![CDATA[
//           <h1>Whatever.</h1>
//     ]]></property>
//  </xml-properties>

public class XmlProperties extends Properties
{
    final static String DTD_SYSTEM_ID = "http://www.mchange.com/dtd/xml-properties.dtd";
    final static String DTD_RSRC_PATH  = "dtd/xml-properties.dtd";

    //MT: members not thread-safe, but access protected by this' lock
    DocumentBuilder docBuilder;
    Transformer identityTransformer;

    public XmlProperties() throws ParserConfigurationException, TransformerConfigurationException
    {
	EntityResolver er = new EntityResolver()
	    {
		public InputSource resolveEntity (String publicId, String systemId)
		{
		    if (DTD_SYSTEM_ID.equals( systemId )) 
			{
			    InputStream is = XmlProperties.class.getResourceAsStream(DTD_RSRC_PATH);
			    return new InputSource(is);
			} 
		    else return null;
		}
	    };

	ErrorHandler eh = new ErrorHandler()
	    {
		public void warning(SAXParseException e) throws SAXException
		{ System.err.println("[Warning] " + e.toString()); }

		public void error(SAXParseException e) throws SAXException
		{ System.err.println("[Error] " + e.toString()); }

		public void fatalError(SAXParseException e) throws SAXException
		{ System.err.println("[Fatal Error] " + e.toString()); }
	    };


	DocumentBuilderFactory dbFact = DocumentBuilderFactory.newInstance();
	dbFact.setValidating( true );
	dbFact.setCoalescing( true );
	dbFact.setIgnoringComments( true );
	this.docBuilder = dbFact.newDocumentBuilder();
	this.docBuilder.setEntityResolver( er );
	this.docBuilder.setErrorHandler( eh );

	TransformerFactory tFact = TransformerFactory.newInstance();
	this.identityTransformer = tFact.newTransformer();
	this.identityTransformer.setOutputProperty("indent", "yes");
	this.identityTransformer.setOutputProperty("doctype-system", DTD_SYSTEM_ID);
    }

    public synchronized void loadXml(InputStream is) throws IOException, SAXException
    {
	Document doc = docBuilder.parse( is );
	NodeList nl = doc.getElementsByTagName("property");
	for( int i = 0, len = nl.getLength(); i < len; ++i)
	    extractProperty( nl.item( i ) );
    }

    /*
     * This method presumes validation by our DTD and a coalescing,
     * comment-ignoring parser!
     */
    private void extractProperty( Node propNode )
    {
	Element propElem = (Element) propNode;
	String key = propElem.getAttribute( "name" );
	boolean trim = Boolean.valueOf( propElem.getAttribute("trim") ).booleanValue();
	NodeList nl = propElem.getChildNodes();
	int len = nl.getLength();

	assert (len >= 0 && len <= 1) : "Bad number of children of property element: " + len;

	String val = ( len == 0 ? "" : ((Text) nl.item(0)).getNodeValue() );
	if (trim)
	    val = val.trim();

	this.put( key, val );
    }

    public synchronized void saveXml(OutputStream os)
	throws IOException, TransformerException
    { storeXml( os, null ); }

    public synchronized void storeXml(OutputStream os, String header) 
	throws IOException, TransformerException
    {
	Document doc = docBuilder.newDocument();
	if (header != null)
	    {
		Comment comment = doc.createComment( header );
		doc.appendChild( comment );
	    }

	Element xmlPropsElem = doc.createElement("xml-properties");
	
	for( Iterator ii = this.keySet().iterator(); ii.hasNext(); )
	    {
		Element propsElem = doc.createElement("property");
		String key = (String) ii.next();
		String val = (String) this.get(key);
		propsElem.setAttribute( "name", key );
		Text valNode = doc.createTextNode(val);
		propsElem.appendChild( valNode );
		xmlPropsElem.appendChild( propsElem );
	    }
	doc.appendChild( xmlPropsElem );
	
	identityTransformer.transform( new DOMSource( doc ), new StreamResult( os ) );
    }

    public static void main( String[] argv )
    {
	InputStream is  = null;
	OutputStream os = null;
	try
	    {
		is = new BufferedInputStream( new FileInputStream( argv[0] ) );
		os = new BufferedOutputStream( new FileOutputStream( argv[1] ) );
		XmlProperties xmlProps = new XmlProperties();
		xmlProps.loadXml( is );
		xmlProps.list( System.out );
		xmlProps.storeXml( os, "This is the resaved test document." );
		os.flush();
	    }
	catch ( Exception e )
	    { e.printStackTrace(); }
	finally
	    { 
		try { if (is != null) is.close(); } catch (Exception e) { e.printStackTrace(); }
		try { if (os != null) os.close(); } catch (Exception e) { e.printStackTrace(); }
	    }
    }
}
