package com.mchange.v1.xml;

import org.xml.sax.*;

public class StdErrErrorHandler implements ErrorHandler
{
    public void warning(SAXParseException e) 
    {
	System.err.println("[Warning]");
	showExceptionInformation(e);
	e.printStackTrace();
    }
		
    public void error(SAXParseException e) 
    {
	System.err.println("[Error]");
	showExceptionInformation(e);
	e.printStackTrace();
    }
		
    public void fatalError(SAXParseException e) throws SAXException 
    {
	System.err.println("[Fatal Error]");
	showExceptionInformation(e);
	e.printStackTrace();
	throw e;
    }

    private void showExceptionInformation(SAXParseException e)
    {
	System.err.println("[\tLine Number: " + e.getLineNumber() + ']');
	System.err.println("[\tColumn Number: " + e.getColumnNumber() + ']');
	System.err.println("[\tPublic ID: " + e.getPublicId() + ']');
	System.err.println("[\tSystem ID: " + e.getSystemId() + ']');
    }
}


