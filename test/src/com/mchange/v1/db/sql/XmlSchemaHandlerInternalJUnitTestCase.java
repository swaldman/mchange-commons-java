package com.mchange.v1.db.sql;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import junit.framework.TestCase;

import org.xml.sax.InputSource;

/**
 *  XmlSchema's SAX handler moved from the deprecated org.xml.sax.HandlerBase, whose
 *  callbacks take the raw element name and an AttributeList, to
 *  org.xml.sax.helpers.DefaultHandler, whose callbacks take namespace triples and an
 *  Attributes. The migration relies on SAXParserFactory being namespace unaware by
 *  default, so that qName carries the raw name HandlerBase used to supply --
 *  reading localName instead would silently yield empty strings.
 *
 *  Lives in com.mchange.v1.db.sql rather than a junit subpackage because MySaxHandler
 *  and the fields it populates are package private.
 *
 *  Note this drives the handler directly. XmlSchema.parse(InputStream) cannot be
 *  called: it dereferences getResource("schema.dtd"), and no such resource is in the
 *  tree, so it throws NullPointerException. That is long standing and unrelated.
 */
public class XmlSchemaHandlerInternalJUnitTestCase extends TestCase
{
    final static String DOC =
	"<schema>" +
	"  <create>" +
	"    <statement>CREATE TABLE foo (a int)</statement>" +
	"    <comment>this comment must not be collected</comment>" +
	"    <statement>CREATE TABLE bar (b int)</statement>" +
	"  </create>" +
	"  <drop>" +
	"    <statement>DROP TABLE foo</statement>" +
	"  </drop>" +
	"  <application name=\"myapp\">" +
	"    <statement name=\"lookup\">SELECT * FROM foo</statement>" +
	"    <statement name=\"purge\">DELETE FROM foo</statement>" +
	"  </application>" +
	"</schema>";

    private XmlSchema parseWithHandler(String doc) throws Exception
    {
	XmlSchema xs = new XmlSchema();
	xs.createStmts = new ArrayList();
	xs.dropStmts   = new ArrayList();
	xs.appMap      = new HashMap();

	SAXParser sp = SAXParserFactory.newInstance().newSAXParser();
	sp.parse( new InputSource( new StringReader( doc ) ), xs.new MySaxHandler() );
	return xs;
    }

    /** Element names must reach the handler, which they do not if localName is read. */
    public void testCreateAndDropStatementsAreCollectedInOrder() throws Exception
    {
	XmlSchema xs = parseWithHandler(DOC);

	assertEquals("two create statements", 2, xs.createStmts.size());
	assertEquals("CREATE TABLE foo (a int)", xs.createStmts.get(0));
	assertEquals("CREATE TABLE bar (b int)", xs.createStmts.get(1));

	assertEquals("one drop statement", 1, xs.dropStmts.size());
	assertEquals("DROP TABLE foo", xs.dropStmts.get(0));
    }

    public void testCommentsAreNotCollectedAsStatements() throws Exception
    {
	XmlSchema xs = parseWithHandler(DOC);
	for (int i = 0, len = xs.createStmts.size(); i < len; ++i)
	    assertTrue("comment text should not appear among statements",
		       ((String) xs.createStmts.get(i)).indexOf("must not be collected") < 0);
    }

    /** Attribute names must reach the handler, i.e. getQName and not getLocalName. */
    public void testApplicationStatementsAreKeyedByNameAttribute() throws Exception
    {
	XmlSchema xs = parseWithHandler(DOC);

	assertEquals("SELECT * FROM foo", xs.getStatementText("myapp", "lookup"));
	assertEquals("DELETE FROM foo", xs.getStatementText("myapp", "purge"));
	assertNull("an unknown application should have no statement",
		   xs.getStatementText("nosuchapp", "lookup"));
	assertNull("an unknown statement should be absent",
		   xs.getStatementText("myapp", "nosuchstatement"));
    }

    public void testApplicationStatementsAreNotAlsoCreateStatements() throws Exception
    {
	XmlSchema xs = parseWithHandler(DOC);
	assertFalse("an application statement is not a create statement",
		    xs.createStmts.contains("SELECT * FROM foo"));
	assertFalse("an application statement is not a drop statement",
		    xs.dropStmts.contains("SELECT * FROM foo"));
    }

    public void testEmptySchemaYieldsNoStatements() throws Exception
    {
	XmlSchema xs = parseWithHandler("<schema/>");
	assertTrue("no create statements", xs.createStmts.isEmpty());
	assertTrue("no drop statements", xs.dropStmts.isEmpty());
    }

    /**
     *  Pins the parser contract the handler is written against, so the reason it reads
     *  qName rather than localName does not have to be rediscovered.
     *
     *  HandlerBase.startElement supplied the raw element name. Under DefaultHandler
     *  that name arrives as qName; with a namespace unaware factory -- which is the
     *  default, and what this code uses -- localName is empty. Attribute names are
     *  not affected: for unprefixed attributes both accessors agree.
     */
    public void testDefaultParserSuppliesElementNamesAsQName() throws Exception
    {
	SAXParserFactory factory = SAXParserFactory.newInstance();
	assertFalse("the default factory should be namespace unaware", factory.isNamespaceAware());

	final StringBuffer seen = new StringBuffer();
	factory.newSAXParser().parse(
	    new InputSource( new StringReader( "<application name=\"myapp\"/>" ) ),
	    new org.xml.sax.helpers.DefaultHandler()
	    {
		public void startElement(String uri, String localName, String qName,
					 org.xml.sax.Attributes attrs)
		{
		    seen.append("element localName=[").append(localName)
			.append("] qName=[").append(qName).append("]");
		    seen.append(" attr localName=[").append(attrs.getLocalName(0))
			.append("] qName=[").append(attrs.getQName(0)).append("]");
		}
	    });

	assertEquals("element name should arrive as qName only, attribute name as either",
		     "element localName=[] qName=[application]"
		     + " attr localName=[name] qName=[name]",
		     seen.toString());
    }
}
