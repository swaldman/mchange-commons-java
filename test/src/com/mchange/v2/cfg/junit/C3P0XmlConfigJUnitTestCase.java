package com.mchange.v2.cfg.junit;

import junit.framework.TestCase;

import com.mchange.v2.cfg.ConfigVetoedException;
import com.mchange.v2.cfg.DelayedLogItem;
import com.mchange.v2.cfg.MConfig;
import com.mchange.v2.cfg.MultiPropertiesConfig;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 *  <code>c3p0-xml:</code> identifiers: c3p0's XML configuration format read as properties.
 *
 *  <h3>What these cases are really about</h3>
 *
 *  <p>The source has no access to c3p0's list of configurable properties, so it cannot validate a
 *  name — it applies a shape heuristic instead. These cases are that heuristic, written out, plus
 *  the structural rules about where an element may appear.</p>
 *
 *  <p>The asymmetry between the two scopes is the heart of it, and is deliberate. The default
 *  scope shares a namespace with properties-style config, so a fully-qualified name written there
 *  means itself. A named config is a per-DataSource scope, while every reader of a
 *  <code>com.mchange.*</code> setting reads it statically with no DataSource in hand — so such a
 *  name cannot be scoped, and is refused rather than silently promoted to a global.</p>
 *
 *  <p>Read through <code>AsProvidedVetoable</code> throughout: the source honors
 *  <code>permissions=useronly</code> on its <code>file:</code> form, so it is vetoable, and plain
 *  <code>AsProvided</code> declines to read it at all.</p>
 */
public final class C3P0XmlConfigJUnitTestCase extends TestCase
{
    private final static String[] NONE = new String[0];

    private Path dir;

    @Override
    protected void setUp() throws Exception
    { dir = Files.createTempDirectory( "mchange-cfg-c3p0xml-" ).toRealPath(); }

    @Override
    protected void tearDown() throws Exception
    { deleteRecursively( dir ); }

    private static void deleteRecursively( Path p )
    {
        try
        {
            if ( Files.isDirectory( p ) )
                for ( Path child : Files.newDirectoryStream( p ) ) deleteRecursively( child );
            Files.deleteIfExists( p );
        }
        catch ( Exception e ) { /* best effort */ }
    }

    // ------------------------------------------------------------- fixture

    /** An XML document in a temp file, named by a c3p0-xml:file: identifier. */
    private String xml( String body, String query ) throws IOException
    {
        Path p = Files.createTempFile( dir, "c3p0-config-", ".xml" );
        PrintWriter pw = new PrintWriter( p.toFile(), "UTF-8" );
        try { pw.println( "<c3p0-config>" ); pw.println( body ); pw.println( "</c3p0-config>" ); }
        finally { pw.close(); }
        if ( posixSupported() ) Files.setPosixFilePermissions( p, PosixFilePermissions.fromString( "rw-------" ) );
        return "c3p0-xml:" + p.toUri() + (query == null ? "" : "?" + query);
    }

    private String xml( String body ) throws IOException
    { return xml( body, null ); }

    /** A document written verbatim, for the cases about the root element. */
    private String raw( String document ) throws IOException
    {
        Path p = Files.createTempFile( dir, "c3p0-raw-", ".xml" );
        PrintWriter pw = new PrintWriter( p.toFile(), "UTF-8" );
        try { pw.println( document ); }
        finally { pw.close(); }
        return "c3p0-xml:" + p.toUri();
    }

    private boolean posixSupported()
    { return dir.getFileSystem().supportedFileAttributeViews().contains( "posix" ); }

    @SuppressWarnings("unchecked")
    private static MultiPropertiesConfig read( String... identifiers ) throws ConfigVetoedException
    { return MConfig.AsProvidedVetoable.readUncachedResourceConfig( NONE, identifiers, new ArrayList() ); }

    private static Properties propsOf( MultiPropertiesConfig mpc )
    { return mpc.getPropertiesByPrefix( "" ); }

    @SuppressWarnings("unchecked")
    private static boolean warns( MultiPropertiesConfig mpc, String... fragments )
    {
        for ( Object o : mpc.getDelayedLogItems() )
        {
            DelayedLogItem item = (DelayedLogItem) o;
            if (! DelayedLogItem.Level.WARNING.equals( item.getLevel() ) ) continue;
            String text = String.valueOf( item.getText() );
            boolean all = true;
            for ( String f : fragments ) if ( text.indexOf( f ) < 0 ) all = false;
            if ( all ) return true;
        }
        return false;
    }

    private static String assertVetoed( String... identifiers )
    {
        try
        {
            read( identifiers );
            fail( "expected a veto" );
            return null;
        }
        catch ( ConfigVetoedException expected )
        { return expected.getMessage(); }
    }

    // =================================================== the documented examples

    /**
     *  The shape the manual shows, end to end: a default config, a named config, user overrides
     *  and extensions together. Taken from doc/docsrc/index.html rather than invented, so the
     *  mapping is pinned against what deployments were actually told to write.
     */
    public void testTheDocumentedExampleMapsAsExpected() throws Exception
    {
        Properties p = propsOf( read( xml(
            "  <default-config>\n" +
            "    <property name=\"maxPoolSize\">30</property>\n" +
            "    <extensions>\n" +
            "      <property name=\"initSql\">SET SCHEMA 'default'</property>\n" +
            "    </extensions>\n" +
            "  </default-config>\n" +
            "  <named-config name=\"intergalactoApp\">\n" +
            "    <property name=\"maxPoolSize\">1000</property>\n" +
            "    <user-overrides user=\"steve\">\n" +
            "      <property name=\"maxPoolSize\">20</property>\n" +
            "    </user-overrides>\n" +
            "    <extensions>\n" +
            "      <property name=\"initSql\">SET SCHEMA 'intergalacto'</property>\n" +
            "    </extensions>\n" +
            "  </named-config>" ) ) );

        assertEquals( "30",   p.getProperty( "c3p0.maxPoolSize" ) );
        assertEquals( "SET SCHEMA 'default'", p.getProperty( "c3p0.extensions.initSql" ) );
        assertEquals( "1000", p.getProperty( "c3p0.named-configs.intergalactoApp.maxPoolSize" ) );
        assertEquals( "20",   p.getProperty( "c3p0.named-configs.intergalactoApp.user-overrides.steve.maxPoolSize" ) );
        assertEquals( "SET SCHEMA 'intergalacto'",
                      p.getProperty( "c3p0.named-configs.intergalactoApp.extensions.initSql" ) );
    }

    public void testDefaultScopeUserOverridesUseTheDocumentedKeys() throws Exception
    {
        Properties p = propsOf( read( xml(
            "  <default-config>\n" +
            "    <user-overrides user=\"steve\">\n" +
            "      <property name=\"maxPoolSize\">15</property>\n" +
            "      <property name=\"minPoolSize\">5</property>\n" +
            "    </user-overrides>\n" +
            "    <user-overrides user=\"ramona\">\n" +
            "      <property name=\"maxPoolSize\">50</property>\n" +
            "    </user-overrides>\n" +
            "  </default-config>" ) ) );

        assertEquals( "15", p.getProperty( "c3p0.user-overrides.steve.maxPoolSize" ) );
        assertEquals( "5",  p.getProperty( "c3p0.user-overrides.steve.minPoolSize" ) );
        assertEquals( "50", p.getProperty( "c3p0.user-overrides.ramona.maxPoolSize" ) );
    }

    // =================================================== the default scope's name rule

    /** A bare name is a c3p0 pool property and gets the prefix. */
    public void testABareNameInTheDefaultScopeGetsTheC3p0Prefix() throws Exception
    {
        Properties p = propsOf( read( xml( "  <default-config><property name=\"maxPoolSize\">30</property></default-config>" ) ) );
        assertEquals( "30", p.getProperty( "c3p0.maxPoolSize" ) );
    }

    /**
     *  A compound name in the default scope means itself. Two of them, because the two cases look
     *  different and are the same rule: an already-prefixed pool property is already correct, and
     *  a fully-qualified library setting belongs to the namespace it names.
     */
    public void testACompoundNameInTheDefaultScopeIsKeptAsWritten() throws Exception
    {
        Properties p = propsOf( read( xml(
            "  <default-config>\n" +
            "    <property name=\"c3p0.minPoolSize\">10</property>\n" +
            "    <property name=\"com.mchange.v2.log.freezeConfig\">true</property>\n" +
            "  </default-config>" ) ) );

        assertEquals( "10",   p.getProperty( "c3p0.minPoolSize" ) );
        assertEquals( "true", p.getProperty( "com.mchange.v2.log.freezeConfig" ) );
        assertNull( "it must not be prefixed a second time", p.getProperty( "c3p0.c3p0.minPoolSize" ) );
    }

    // =================================================== the named scope's name rule

    public void testANamedScopePrefixesBareAndC3p0PrefixedNamesAlike() throws Exception
    {
        Properties p = propsOf( read( xml(
            "  <named-config name=\"X\">\n" +
            "    <property name=\"maxPoolSize\">1000</property>\n" +
            "    <property name=\"c3p0.acquireIncrement\">50</property>\n" +
            "  </named-config>" ) ) );

        assertEquals( "1000", p.getProperty( "c3p0.named-configs.X.maxPoolSize" ) );
        assertEquals( "50",   p.getProperty( "c3p0.named-configs.X.acquireIncrement" ) );
        assertNull( "the c3p0. prefix must be stripped, not kept",
                    p.getProperty( "c3p0.named-configs.X.c3p0.acquireIncrement" ) );
    }

    /**
     *  THE ASYMMETRY. A fully-qualified name has no meaning in a per-DataSource scope, so it is
     *  refused and said so — where the very same name in the default scope is kept. Promoting it
     *  to a global would be the dangerous reading: an unrelated named config could then change a
     *  library-wide setting.
     */
    public void testANamedScopeRefusesAFullyQualifiedName() throws Exception
    {
        MultiPropertiesConfig mpc = read( xml(
            "  <named-config name=\"X\">\n" +
            "    <property name=\"com.mchange.v2.log.freezeConfig\">true</property>\n" +
            "  </named-config>" ) );

        Properties p = propsOf( mpc );
        assertNull( p.getProperty( "com.mchange.v2.log.freezeConfig" ) );
        assertNull( p.getProperty( "c3p0.named-configs.X.com.mchange.v2.log.freezeConfig" ) );
        assertTrue( warns( mpc, "com.mchange.v2.log.freezeConfig", "named config 'X'" ) );
    }

    /** And a c3p0-prefixed name of more than two parts is equally unusable there. */
    public void testANamedScopeRefusesALongerC3p0PrefixedName() throws Exception
    {
        MultiPropertiesConfig mpc = read( xml(
            "  <named-config name=\"X\"><property name=\"c3p0.a.b\">v</property></named-config>" ) );

        assertTrue( propsOf( mpc ).isEmpty() );
        assertTrue( warns( mpc, "c3p0.a.b", "named config 'X'" ) );
    }

    /** A user override obeys the named-scope rule too, in either scope. */
    public void testAUserOverrideRefusesAFullyQualifiedName() throws Exception
    {
        MultiPropertiesConfig mpc = read( xml(
            "  <default-config>\n" +
            "    <user-overrides user=\"steve\">\n" +
            "      <property name=\"maxPoolSize\">15</property>\n" +
            "      <property name=\"com.mchange.v2.anything\">nope</property>\n" +
            "    </user-overrides>\n" +
            "  </default-config>" ) );

        assertEquals( "15", propsOf( mpc ).getProperty( "c3p0.user-overrides.steve.maxPoolSize" ) );
        assertNull( propsOf( mpc ).getProperty( "com.mchange.v2.anything" ) );
        assertTrue( warns( mpc, "com.mchange.v2.anything", "user override for 'steve'" ) );
    }

    // =================================================== extensions are never touched

    /**
     *  Extension names are a deployment's own, read back through getExtensions(..), so there is no
     *  vocabulary to correct them against and none is applied — not even to a name that looks
     *  exactly like a c3p0 property.
     */
    public void testExtensionNamesArePassedThroughVerbatim() throws Exception
    {
        Properties p = propsOf( read( xml(
            "  <default-config><extensions>\n" +
            "    <property name=\"initSql\">SET SCHEMA 'foo'</property>\n" +
            "    <property name=\"c3p0.looksLikeOurs\">kept</property>\n" +
            "    <property name=\"com.example.deeply.qualified\">kept</property>\n" +
            "  </extensions></default-config>" ) ) );

        assertEquals( "SET SCHEMA 'foo'", p.getProperty( "c3p0.extensions.initSql" ) );
        assertEquals( "kept", p.getProperty( "c3p0.extensions.c3p0.looksLikeOurs" ) );
        assertEquals( "kept", p.getProperty( "c3p0.extensions.com.example.deeply.qualified" ) );
    }

    public void testNamedScopeExtensionsAreScopedAndVerbatim() throws Exception
    {
        Properties p = propsOf( read( xml(
            "  <named-config name=\"X\"><extensions><property name=\"c3p0.odd\">v</property></extensions></named-config>" ) ) );

        assertEquals( "v", p.getProperty( "c3p0.named-configs.X.extensions.c3p0.odd" ) );
    }

    /**
     *  Extensions are per-config, not per-user. c3p0 offers no way to read them per user, so
     *  accepting them here would accept configuration that could never be seen — worse than
     *  refusing it, because nothing would ever say so.
     */
    public void testExtensionsBeneathAUserOverrideAreRefused() throws Exception
    {
        MultiPropertiesConfig mpc = read( xml(
            "  <default-config>\n" +
            "    <user-overrides user=\"steve\">\n" +
            "      <property name=\"maxPoolSize\">15</property>\n" +
            "      <extensions><property name=\"initSql\">nope</property></extensions>\n" +
            "    </user-overrides>\n" +
            "  </default-config>" ) );

        assertEquals( "the override's own properties still load",
                      "15", propsOf( mpc ).getProperty( "c3p0.user-overrides.steve.maxPoolSize" ) );
        for ( Object k : propsOf( mpc ).keySet() )
            assertFalse( "no extension key may be produced: " + k, String.valueOf( k ).contains( "extensions" ) );
        assertTrue( warns( mpc, "extensions", "user-overrides", "steve" ) );
    }

    // =================================================== attributes and structure

    /** A since-corrected error in the manual showed 'name' here, so it is accepted — and nagged. */
    public void testUserOverridesAcceptsNameInPlaceOfUserButPrefersUser() throws Exception
    {
        MultiPropertiesConfig mpc = read( xml(
            "  <default-config><user-overrides name=\"ramona\">\n" +
            "    <property name=\"maxPoolSize\">50</property>\n" +
            "  </user-overrides></default-config>" ) );

        assertEquals( "50", propsOf( mpc ).getProperty( "c3p0.user-overrides.ramona.maxPoolSize" ) );
        assertTrue( warns( mpc, "'name' attribute", "'user' is the documented and preferred" ) );
    }

    public void testAUserOverrideWithNoUserAtAllIsRefused() throws Exception
    {
        MultiPropertiesConfig mpc = read( xml(
            "  <default-config><user-overrides><property name=\"maxPoolSize\">50</property></user-overrides></default-config>" ) );

        assertTrue( propsOf( mpc ).isEmpty() );
        assertTrue( warns( mpc, "neither a 'user' nor a 'name'" ) );
    }

    public void testANamedConfigWithoutANameIsRefused() throws Exception
    {
        MultiPropertiesConfig mpc = read( xml(
            "  <named-config><property name=\"maxPoolSize\">9</property></named-config>" ) );

        assertTrue( propsOf( mpc ).isEmpty() );
        assertTrue( warns( mpc, "'named-config' element has no 'name'" ) );
    }

    public void testADocumentWithTheWrongRootElementIsRefused() throws Exception
    {
        MultiPropertiesConfig mpc = read( raw( "<not-c3p0-config><default-config/></not-c3p0-config>" ) );

        assertEquals( "the source must contribute nothing", Arrays.asList(), Arrays.asList( mpc.getPropertiesResourcePaths() ) );
        assertTrue( warns( mpc, "root element", "should be 'c3p0-config'" ) );
    }

    public void testUnparseableXmlIsRefused() throws Exception
    {
        MultiPropertiesConfig mpc = read( raw( "<c3p0-config><default-config></c3p0-config>" ) );

        assertEquals( Arrays.asList(), Arrays.asList( mpc.getPropertiesResourcePaths() ) );
        assertTrue( warns( mpc, "could not be parsed as XML" ) );
    }

    public void testDuplicateScopesMergeLaterWinningAndSayTheyDid() throws Exception
    {
        MultiPropertiesConfig mpc = read( xml(
            "  <default-config><property name=\"maxPoolSize\">1</property></default-config>\n" +
            "  <default-config><property name=\"maxPoolSize\">2</property></default-config>\n" +
            "  <named-config name=\"X\"><property name=\"minPoolSize\">3</property></named-config>\n" +
            "  <named-config name=\"X\"><property name=\"minPoolSize\">4</property></named-config>" ) );

        assertEquals( "2", propsOf( mpc ).getProperty( "c3p0.maxPoolSize" ) );
        assertEquals( "4", propsOf( mpc ).getProperty( "c3p0.named-configs.X.minPoolSize" ) );
        assertTrue( warns( mpc, "'default-config' elements" ) );
        assertTrue( warns( mpc, "name 'X' appears more than once" ) );
    }

    /**
     *  A misplaced element is REPORTED, not merely skipped. Being silently ignored is how a
     *  nesting mistake becomes a puzzle, and a count is cheap enough to buy the ability to say
     *  "you have one of these where it will never be seen".
     */
    public void testAMisplacedElementIsReportedRatherThanSilentlySkipped() throws Exception
    {
        MultiPropertiesConfig mpc = read( xml(
            "  <default-config>\n" +
            "    <property name=\"maxPoolSize\">30</property>\n" +
            "    <wrapper>\n" +
            "      <user-overrides user=\"steve\"><property name=\"maxPoolSize\">15</property></user-overrides>\n" +
            "    </wrapper>\n" +
            "  </default-config>" ) );

        assertEquals( "30", propsOf( mpc ).getProperty( "c3p0.maxPoolSize" ) );
        assertNull( "a buried user-override contributes nothing",
                    propsOf( mpc ).getProperty( "c3p0.user-overrides.steve.maxPoolSize" ) );
        assertTrue( warns( mpc, "'user-overrides' element(s) are not" ) );
    }

    public void testANamedConfigNotDirectlyBeneathTheRootIsReported() throws Exception
    {
        MultiPropertiesConfig mpc = read( xml(
            "  <default-config>\n" +
            "    <named-config name=\"buried\"><property name=\"maxPoolSize\">9</property></named-config>\n" +
            "  </default-config>" ) );

        assertNull( propsOf( mpc ).getProperty( "c3p0.named-configs.buried.maxPoolSize" ) );
        assertTrue( warns( mpc, "'named-config' element(s) are not" ) );
    }

    // =================================================== the identifier forms

    public void testTheClassloaderFormReadsAResource() throws Exception
    {
        // shipped under test/resources, so it is on the test classpath
        Properties p = propsOf( read( "c3p0-xml:/com/mchange/v2/cfg/junit/c3p0-config-probe.xml" ) );

        assertEquals( "42", p.getProperty( "c3p0.maxPoolSize" ) );
        assertEquals( "from-probe", p.getProperty( "c3p0.extensions.probeKey" ) );
    }

    public void testAnAbsentClassloaderResourceIsOrdinaryAbsence() throws Exception
    {
        MultiPropertiesConfig mpc = read( "c3p0-xml:/no/such/c3p0-config.xml" );
        assertTrue( propsOf( mpc ).isEmpty() );
    }

    public void testASlashAloneIsRefused() throws Exception
    {
        MultiPropertiesConfig mpc = read( "c3p0-xml:/" );
        assertTrue( warns( mpc, "names no resource" ) );
    }

    /**
     *  A query string on the classloader form would be read as part of the resource path, and the
     *  resource would simply not be found — a directive that silently does nothing. Refuse it and
     *  point at the form that does support directives.
     */
    public void testAQueryStringOnTheClassloaderFormIsRefused() throws Exception
    {
        MultiPropertiesConfig mpc = read( "c3p0-xml:/com/mchange/v2/cfg/junit/c3p0-config-probe.xml?required=true" );

        assertTrue( propsOf( mpc ).isEmpty() );
        assertTrue( warns( mpc, "takes no query string" ) );
    }

    public void testAnIdentifierThatIsNeitherFormIsRefused() throws Exception
    {
        MultiPropertiesConfig mpc = read( "c3p0-xml:relative/path.xml" );
        assertTrue( warns( mpc, "must be either a classloader resource path" ) );
    }

    // =================================================== file: directives still apply

    public void testRequiredTrueOnAnAbsentFileVetoes() throws Exception
    {
        String absent = "c3p0-xml:" + dir.resolve( "definitely-absent.xml" ).toUri() + "?required=true";

        String msg = assertVetoed( absent );
        assertTrue( "the veto should name the identifier: " + msg, msg.contains( "definitely-absent" ) );
    }

    public void testAnAbsentFileWithoutRequiredIsSkipped() throws Exception
    {
        String absent = "c3p0-xml:" + dir.resolve( "definitely-absent.xml" ).toUri();
        assertTrue( propsOf( read( absent ) ).isEmpty() );
    }

    public void testUserOnlyPermissionsAreEnforcedOnTheFileForm() throws Exception
    {
        if ( !posixSupported() ) return;

        String secure = xml( "  <default-config><property name=\"maxPoolSize\">30</property></default-config>",
                             "permissions=useronly" );
        assertEquals( "a 0600 document passes", "30", propsOf( read( secure ) ).getProperty( "c3p0.maxPoolSize" ) );

        // now the same document, world-readable
        Path exposed = Files.createTempFile( dir, "c3p0-exposed-", ".xml" );
        PrintWriter pw = new PrintWriter( exposed.toFile(), "UTF-8" );
        try { pw.println( "<c3p0-config><default-config><property name=\"maxPoolSize\">30</property></default-config></c3p0-config>" ); }
        finally { pw.close(); }
        Files.setPosixFilePermissions( exposed, PosixFilePermissions.fromString( "rw-r--r--" ) );

        String msg = assertVetoed( "c3p0-xml:" + exposed.toUri() + "?permissions=useronly" );
        assertTrue( "the veto should still be about permissions: " + msg, msg.contains( "permissions" ) );
    }

    /** An unsupported query key is a veto, not a silently skipped directive — as for file:. */
    public void testAMisspelledQueryKeyIsVetoed() throws Exception
    {
        String id = xml( "  <default-config><property name=\"maxPoolSize\">30</property></default-config>",
                         "Permissions=useronly" );
        assertVetoed( id );
    }
}
