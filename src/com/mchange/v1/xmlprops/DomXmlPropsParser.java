package com.mchange.v1.xmlprops;

import java.io.*;
import java.util.*;
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.w3c.dom.*;

import com.mchange.v1.xml.ResourceEntityResolver;
import com.mchange.v1.xml.StdErrErrorHandler;

/**
 *  This implementation supports full flexibility in entity resolution,
 *  which means it is not suitable for parsing untrusted XML.
 *
 *  Unrestricted entity resolution can provoke the loading of arbitrary
 *  files, loading of external URLs, or result in endless recursion and
 *  denial of service attacks.
 *
 *  Thanks to chennbnbnb on github for calling attention to this issue.
 *
 *  @deprecated we know of no current users, so are maintaining this very lightly
 */
@Deprecated
public class DomXmlPropsParser
{
    final static String XMLPROPS_NAMESPACE_URI = "http://www.mchange.com/namespaces/xmlprops";

    final static DocumentBuilderFactory _factory = DocumentBuilderFactory.newInstance();

    static
    {
	_factory.setNamespaceAware(true);

        // we haven't defined a DTD to validate against.
        // But note an external DTD and its entities will
        // still get resolved.
        //
  	//_factory.setValidating(true);
    }

    static synchronized DocumentBuilder newDocumentBuilder() throws ParserConfigurationException
    { return _factory.newDocumentBuilder(); }

    /**
     *  Not suitable for parsing untrusted XML. See the class documentation.
     */
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
		
		DocumentBuilder dbuilder = newDocumentBuilder();
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
		for (Iterator<Object> ii = props.keySet().iterator(); ii.hasNext(); )
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





