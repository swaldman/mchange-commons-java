package com.mchange.v2.cfg.junit;

import java.io.FileNotFoundException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import junit.framework.TestCase;

import com.mchange.v2.cfg.BasicPropertiesConfigSource;
import com.mchange.v2.cfg.PropertiesConfigSource;

/**
 *  BasicPropertiesConfigSource is the source behind every plain classpath properties path in an
 *  MConfig resource-path list, which makes it the most-used PropertiesConfigSource there is --
 *  and it had no tests. These pin what it reads, how it reports an absence, and the parts of the
 *  PropertiesConfigSource contract it is obliged to satisfy.
 *
 *  <p>Two of those obligations are worth stating, because nothing in the type system enforces
 *  either. The interface requires a public no-argument constructor, since MConfig selects a
 *  source by class name and instantiates it reflectively. And it requires implementations to be
 *  stateless and safe for concurrent use, because one instance is shared by every read across
 *  every identifier and every thread.</p>
 *
 *  <p>Resources live under test/resources/com/mchange/v2/cfg/junit/ with a bpcs- prefix, kept
 *  separate from a.properties and b.properties, which several other cfg suites depend on.</p>
 */
public class BasicPropertiesConfigSourceJUnitTestCase extends TestCase
{
    private final static String BASIC_ABS = "/com/mchange/v2/cfg/junit/bpcs-basic.properties";
    private final static String EMPTY_ABS = "/com/mchange/v2/cfg/junit/bpcs-empty.properties";
    private final static String MISSING   = "/com/mchange/v2/cfg/junit/bpcs-no-such-file.properties";
    private final static String MALFORMED = "/com/mchange/v2/cfg/junit/bpcs-malformed-escape.properties";

    private PropertiesConfigSource source;

    @Override
    protected void setUp() throws Exception
    { source = new BasicPropertiesConfigSource(); }

    private Properties propsFrom( String identifier ) throws Exception
    { return source.propertiesFromSource( identifier ).getProperties(); }

    // ==================== what it reads ====================

    public void testItReadsAClasspathPropertiesResource() throws Exception
    {
        Properties p = propsFrom( BASIC_ABS );

        assertEquals( "one", p.getProperty( "alpha" ) );
        assertEquals( "whitespace around the separator is not part of the value",
                      "two", p.getProperty( "beta" ) );
        assertEquals( "a colon separates as well as an equals sign",
                      "three", p.getProperty( "gamma" ) );
    }

    /**
     *  Properties-file semantics, which this source gets from java.util.Properties rather than
     *  implementing itself. Pinned because the source could plausibly be rewritten to parse by
     *  hand, or to pre-process the stream, and these are the behaviours a deployment's existing
     *  files depend on.
     */
    public void testItHonorsPropertiesFileSemantics() throws Exception
    {
        Properties p = propsFrom( BASIC_ABS );

        assertEquals( "a key with no separator has an empty value", "", p.getProperty( "delta" ) );
        assertEquals( "trailing whitespace is part of the value",
                      "value with trailing space   ", p.getProperty( "trailing" ) );
        assertEquals( "a backslash continues a line, and leading whitespace on the next is dropped",
                      "first second", p.getProperty( "continued" ) );
        assertEquals( "escapes are interpreted", "a\tb\nc", p.getProperty( "escaped" ) );
        assertEquals( "and so are unicode escapes", "café", p.getProperty( "unicode" ) );
        assertEquals( "a repeated key takes its last value", "last", p.getProperty( "dup" ) );
        assertNull( "comments contribute nothing", p.getProperty( "# a comment" ) );
        assertEquals( "exactly the keys above, and nothing from the comments or the blank line",
                      9, p.size() );
    }

    /** An empty file is a successful read of no properties, not a failure. */
    public void testAnEmptyResourceParsesToNoProperties() throws Exception
    {
        Properties p = propsFrom( EMPTY_ABS );

        assertNotNull( p );
        assertTrue( "" + p, p.isEmpty() );
    }

    /**
     *  Resolution goes through Class.getResourceAsStream rather than the ClassLoader's, so a
     *  relative identifier resolves against the package of com.mchange.v2.cfg.MultiPropertiesConfig
     *  and an absolute one against the classpath root. Both reach the same file here. Worth
     *  pinning: switching to ClassLoader.getResourceAsStream would quietly break every relative
     *  identifier, and it is the kind of substitution that looks like a cleanup.
     */
    public void testARelativeIdentifierResolvesAgainstTheCfgPackage() throws Exception
    {
        Properties viaAbsolute = propsFrom( BASIC_ABS );
        Properties viaRelative = propsFrom( "junit/bpcs-basic.properties" );

        assertEquals( viaAbsolute, viaRelative );
        assertFalse( "Precondition: this is a real read, not two empty ones.", viaAbsolute.isEmpty() );
    }

    // ==================== how it reports an absence ====================

    /**
     *  Nothing at the identifier is a FileNotFoundException, which is the signal
     *  BasicMultiPropertiesConfig reads as "drop this path, keep the others" and reports at FINE.
     *  Any other exception type would be reported at WARNING instead, so the type carries meaning
     *  beyond the message.
     */
    public void testAMissingResourceIsAFileNotFoundException() throws Exception
    {
        try
        {
            source.propertiesFromSource( MISSING );
            fail( "Expected FileNotFoundException for an identifier with nothing behind it." );
        }
        catch ( FileNotFoundException e )
        {
            assertTrue( "and the message should name the path that was not found: " + e.getMessage(),
                        e.getMessage().indexOf( MISSING ) >= 0 );
        }
    }

    /**
     *  A resource java.util.Properties cannot parse is a failure of the whole read, not a
     *  partial one. That matters because load() populates as it goes and only then throws, so
     *  the Properties it was filling holds everything before the bad line -- here, one of three
     *  keys. Handing that back would be configuration silently truncated at the first
     *  typo, which is worse than refusing it.
     *
     *  <p>This is also the one case that can tell apart assigning the Properties before the
     *  load from assigning it only on success, so it pins an ordering that is otherwise
     *  invisible.</p>
     */
    public void testAnUnparseableResourceFailsRatherThanYieldingPartialProperties() throws Exception
    {
        try
        {
            source.propertiesFromSource( MALFORMED );
            fail( "Expected the malformed escape to fail the read." );
        }
        catch ( IllegalArgumentException e )
        {
            assertTrue( "java.util.Properties reports a malformed escape this way: " + e.getMessage(),
                        String.valueOf( e.getMessage() ).indexOf( "Malformed" ) >= 0 );
        }
    }

    /** A FileNotFoundException means absent; an unparseable resource must not look absent. */
    public void testAnUnparseableResourceIsNotReportedAsAbsent() throws Exception
    {
        try
        {
            source.propertiesFromSource( MALFORMED );
            fail( "Expected a failure." );
        }
        catch ( FileNotFoundException e )
        { fail( "An unparseable resource is present but bad; reporting it as not-found would drop "
                + "the path at FINE and hide a broken configuration file." ); }
        catch ( Exception e ) { /* anything else is the right shape */ }
    }

    // ==================== what it reports alongside a successful read ====================

    /**
     *  A Parse is the only way this source can say anything, and on an ordinary read it has
     *  nothing to say. The list must still be present rather than null: every caller does
     *  addAll on it unconditionally.
     */
    public void testASuccessfulReadCarriesAnEmptyMessageList() throws Exception
    {
        List<?> items = source.propertiesFromSource( BASIC_ABS ).getDelayedLogItems();

        assertNotNull( "callers addAll this without checking", items );
        assertTrue( "an ordinary read has nothing to report: " + items, items.isEmpty() );
    }

    // ==================== the contract the interface imposes ====================

    /**
     *  MConfig selects a source by class name and instantiates it reflectively, so the public
     *  no-argument constructor the interface requires is load-bearing. Nothing in the type
     *  system enforces it.
     */
    public void testItHasThePublicNoArgConstructorTheInterfaceRequires() throws Exception
    {
        Constructor<?> ctor = BasicPropertiesConfigSource.class.getConstructor();

        assertTrue( "the constructor must be public", Modifier.isPublic( ctor.getModifiers() ) );
        assertTrue( "and the class too, or it cannot be reached by name",
                    Modifier.isPublic( BasicPropertiesConfigSource.class.getModifiers() ) );
        assertTrue( ctor.newInstance() instanceof PropertiesConfigSource );
    }

    /**
     *  Each read must hand back its own Properties. One source instance is shared by every read,
     *  so a shared or cached Properties would let one caller's mutation reach another's
     *  configuration.
     */
    public void testEachReadReturnsItsOwnProperties() throws Exception
    {
        Properties first  = propsFrom( BASIC_ABS );
        Properties second = propsFrom( BASIC_ABS );

        assertNotSame( "distinct objects", first, second );
        assertEquals( "with equal content", first, second );

        first.setProperty( "alpha", "mutated" );
        assertEquals( "and mutating one must not reach the other", "one", second.getProperty( "alpha" ) );
    }

    /**
     *  The interface requires implementations to be stateless and safe for concurrent use,
     *  because one instance is shared across every identifier and every thread. A source that
     *  kept per-read state in a field would see it corrupted by unrelated reads -- so this reads
     *  two different identifiers at once, repeatedly, and insists each answer is its own.
     */
    public void testConcurrentReadsOfDifferentIdentifiersDoNotInterfere() throws Exception
    {
        final int rounds = 200;
        ExecutorService pool = Executors.newFixedThreadPool( 4 );
        try
        {
            List<Callable<String>> work = new ArrayList<Callable<String>>();
            for ( int i = 0; i < rounds; ++i )
            {
                work.add( new Callable<String>()
                {
                    @Override
                    public String call() throws Exception
                    { return "basic:" + propsFrom( BASIC_ABS ).getProperty( "alpha" ); }
                } );
                work.add( new Callable<String>()
                {
                    @Override
                    public String call() throws Exception
                    { return "empty:" + propsFrom( EMPTY_ABS ).size(); }
                } );
            }

            int basics = 0, empties = 0;
            for ( Future<String> f : pool.invokeAll( work ) )
            {
                String got = f.get();
                if ( "basic:one".equals( got ) )    ++basics;
                else if ( "empty:0".equals( got ) ) ++empties;
                else fail( "A concurrent read returned something from another read: " + got );
            }
            assertEquals( rounds, basics );
            assertEquals( rounds, empties );
        }
        finally { pool.shutdownNow(); }
    }
}
