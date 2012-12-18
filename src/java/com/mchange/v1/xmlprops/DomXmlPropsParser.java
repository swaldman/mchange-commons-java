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





