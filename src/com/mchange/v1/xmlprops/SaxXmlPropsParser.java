package com.mchange.v1.xmlprops;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
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
		XMLReader reader = (XMLReader) Class.forName( readerClass ).getDeclaredConstructor().newInstance();
		InputSource is = new InputSource( istr );
		return parseXmlProps( is, reader, null, null);
	    }
	catch (XmlPropsException e)
	    { throw e; }
	catch (Exception e)
	     {
		 // reflective construction wraps whatever the constructor threw; report the cause
		 Throwable t = ( e instanceof InvocationTargetException ? e.getCause() : e );

		 if ( t instanceof XmlPropsException ) throw (XmlPropsException) t;

		 t.printStackTrace();
		 throw new XmlPropsException("Exception while instantiating XMLReader.", t);
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

	@Override
	public void setDocumentLocator(Locator locator)
	{ this.locator = locator; }

	@Override
	public void startDocument() throws SAXException
	{ 
	    props  = new Properties();
	}

	@Override
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

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException
	{
	    if (valueBuf != null)
		valueBuf.append(ch, start, length); 
	}

	@Override
	public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException
	{ 
	    if (valueBuf != null)
		valueBuf.append(ch, start, length); 
	}

	@Override
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

	@Override
	public void endDocument() throws SAXException
	{}

	@Override
	public void startPrefixMapping(String prefix, String uri) throws SAXException
	{}



	@Override
	public void endPrefixMapping(String prefix) throws SAXException
	{}

	@Override
	public void processingInstruction(String target, String data) throws SAXException
	{}

	@Override
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





