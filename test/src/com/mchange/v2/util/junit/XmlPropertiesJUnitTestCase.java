package com.mchange.v2.util.junit;

import junit.framework.TestCase;

import com.mchange.v2.util.XmlProperties;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.xml.sax.SAXException;

/**
 *  XmlProperties is deprecated and has no in-tree callers, so this is short on purpose. It
 *  exists because the class's XML parser configuration is a balance that is easy to tip by
 *  accident in either direction, and nothing else here would notice.
 *
 *  <p>The format requires a DOCTYPE -- it validates against a bundled DTD -- so the blunt
 *  mitigation, <code>disallow-doctype-decl</code>, makes the class unable to read documents it
 *  wrote itself. It instead permits the DOCTYPE and refuses every external fetch:
 *  external general entities, external parameter entities and external DTD loading are all
 *  disabled, and an EntityResolver serves the bundled DTD locally while returning an empty
 *  source for anything else. Returning <code>null</code> there would mean "resolve it yourself",
 *  which is the hole this arrangement closes.</p>
 *
 *  <p>So there are two ways to break it and one test for each: tighten it and the round trip
 *  stops working, loosen it and a document can read local files. Internal entity expansion
 *  stays possible, since a DOCTYPE is allowed, and is bounded by the JDK's own
 *  entityExpansionLimit rather than by anything here.</p>
 */
@SuppressWarnings("deprecation")
public final class XmlPropertiesJUnitTestCase extends TestCase
{
    /** Tightening the parser breaks this: the written document carries a DOCTYPE. */
    public void testWrittenDocumentCanBeReadBack() throws Exception
    {
        XmlProperties out = new XmlProperties();
        out.setProperty( "alpha", "one" );

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        out.storeXml( baos, "round trip" );
        byte[] doc = baos.toByteArray();

        XmlProperties in = new XmlProperties();
        in.loadXml( new ByteArrayInputStream( doc ) );

        assertEquals( "a document this class wrote must be one it can read", "one", in.getProperty( "alpha" ) );
    }

    /**
     *  Loosening it breaks this. The contract is that the file's content must not reach the
     *  parsed value; whether the parse fails or merely expands the entity to nothing is left
     *  open, since failing closed is equally acceptable.
     */
    public void testAnExternalEntityCannotReadALocalFile() throws Exception
    {
        Path secret = Files.createTempFile( "mchange-xxe-probe-", ".txt" );
        try
        {
            Files.write( secret, "SENTINEL-c0ffee-MUST-NOT-APPEAR".getBytes( "8859_1" ) );

            String attack = "<?xml version=\"1.0\"?>"
                          + "<!DOCTYPE xml-properties [<!ENTITY xxe SYSTEM \"" + secret.toUri() + "\">]>"
                          + "<xml-properties><property name=\"a\">&xxe;</property></xml-properties>";

            String value;
            try
            {
                XmlProperties in = new XmlProperties();
                in.loadXml( new ByteArrayInputStream( attack.getBytes( "8859_1" ) ) );
                value = String.valueOf( in.getProperty( "a" ) );
            }
            catch ( SAXException failedClosed )
            { return; }   // refusing the document outright is a fine answer

            assertFalse( "an external entity disclosed a local file: " + value,
                         value.contains( "SENTINEL" ) );
        }
        finally
        { deleteQuietly( secret ); }
    }

    /**
     *  The resolver can only serve the DTD locally if it ships. It did not for most of this
     *  class's life -- the lookup returned null, the resolver handed the parser an InputSource
     *  with no stream, and validation could never have worked.
     */
    public void testTheBundledDtdIsOnTheClasspath()
    {
        assertNotNull( "expected /com/mchange/v2/util/dtd/xml-properties.dtd to be packaged",
                       XmlProperties.class.getResource( "dtd/xml-properties.dtd" ) );
    }

    private static void deleteQuietly( Path p )
    { try { Files.deleteIfExists( p ); } catch ( IOException e ) { /* best effort */ } }
}
