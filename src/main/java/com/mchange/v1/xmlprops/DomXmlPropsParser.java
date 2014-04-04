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
import java.util.*;
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.w3c.dom.*;

import com.mchange.v1.xml.ResourceEntityResolver;
import com.mchange.v1.xml.StdErrErrorHandler;

public class DomXmlPropsParser
{
    final static String XMLPROPS_NAMESPACE_URI = "http://www.mchange.com/namespaces/xmlprops";

    static DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

    static
    {
	factory.setNamespaceAware(true);
  	factory.setValidating(true);
  	//factory.setValidating(false);
    }

    public Properties parseXmlProps(InputStream istr) throws XmlPropsException
    {
	return parseXmlProps( new InputSource(istr), 
			      new ResourceEntityResolver(this.getClass()), 
			      new StdErrErrorHandler());
    } 

    private Properties parseXmlProps(InputSource isrc, EntityResolver eresolv, ErrorHandler grrr) 
	throws XmlPropsException
    {
	try
	    {
		Properties props = new Properties();
		
		DocumentBuilder dbuilder = factory.newDocumentBuilder();
		dbuilder.setEntityResolver( eresolv );
		dbuilder.setErrorHandler( grrr );
		
		Document doc = dbuilder.parse( isrc );
		Element docElem = doc.getDocumentElement();
		//NodeList propertiesNL = docElem.getElementsByTagNameNS(XMLPROPS_NAMESPACE_URI, "property");
		NodeList propertiesNL = docElem.getElementsByTagName("property");
		for (int i = 0, len = propertiesNL.getLength(); i < len; ++i)
		    {
			Element propElem = (Element) propertiesNL.item(i);
			//String name  = propElem.getAttributeNS(XMLPROPS_NAMESPACE_URI, "name");
			String name  = propElem.getAttribute("name");
			StringBuffer valueBuf = new StringBuffer();
			NodeList peNL = propElem.getChildNodes();
			for (int j = 0, plen = peNL.getLength(); j < plen; ++j)
			    {
				Node node = peNL.item(j);
				if (node.getNodeType() == Node.TEXT_NODE)
				    valueBuf.append(node.getNodeValue());
			    }
//  			System.err.println("NAME: " + name);
//  			System.err.println("VALUE: " + valueBuf);
			props.put(name, valueBuf.toString());
		    }
		
		return props;
	    }
	catch (Exception e)
	    {
		e.printStackTrace();
		throw new XmlPropsException(e); 
	    }
    }
    
    public static void main(String[] argv)
    {
	try
	    {
		InputStream is = new BufferedInputStream( new FileInputStream( argv[0] ) );
		DomXmlPropsParser parser = new DomXmlPropsParser();
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





