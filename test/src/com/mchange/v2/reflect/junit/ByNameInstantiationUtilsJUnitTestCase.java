package com.mchange.v2.reflect.junit;

import com.mchange.v2.cfg.SecurityRatchetTestSupport;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import junit.framework.TestCase;

import com.mchange.v2.cfg.WhitelistInfo;
import com.mchange.v2.reflect.ByNameInstantiationUtils;
import com.mchange.v2.reflect.InstantiationNotPermittedException;

/**
 *  The by-name instantiation guard: a whitelist of class names, enforced or merely warned
 *  about, gating reflective construction of classes whose names may have reached us from a
 *  deserialized or dereferenced object.
 *
 *  <p>These drive the guard through System properties with a null PropertiesConfig, which
 *  is both the simplest hermetic arrangement and an exercise of the null-pcfg path the
 *  class documents as supported. Every property touched is restored afterwards, because the
 *  suite shares a JVM.</p>
 */
public class ByNameInstantiationUtilsJUnitTestCase extends TestCase
{
    private final static String PFX       = "com.mchange.v2.reflect.byNameInstantiation";
    private final static String WHITELIST = PFX + ".whitelist";
    private final static String ENFORCE   = PFX + ".enforceWhitelist";
    private final static String OVERRIDE  = PFX + ".overrideWhitelist";

    private final static String MARKER = ByNameMarker.class.getName();
    private final static String OTHER  = "com.mchange.v2.reflect.junit.NotOnAnyWhitelist";

    private Properties saved;

    @Override
    public void setUp() throws Exception
    {
        // The gates these cases drive now latch at first lookup, so each case must begin
        // from the state an unstarted JVM would have. See SecurityRatchetTestSupport.
        SecurityRatchetTestSupport.resetAll( ByNameInstantiationUtils.class );

        saved = (Properties) System.getProperties().clone();
        clearOurKeys();
    }

    @Override
    public void tearDown() throws Exception
    {
        // The gates these cases drive now latch at first lookup, so each case must begin
        // from the state an unstarted JVM would have. See SecurityRatchetTestSupport.
        SecurityRatchetTestSupport.resetAll( ByNameInstantiationUtils.class );

        clearOurKeys();
        for ( String k : saved.stringPropertyNames() )
            if ( k.startsWith( PFX ) )
                System.setProperty( k, saved.getProperty( k ) );
    }

    private void clearOurKeys()
    {
        List<String> doomed = new ArrayList<String>();
        for ( String k : System.getProperties().stringPropertyNames() )
            if ( k.startsWith( PFX ) )
                doomed.add( k );
        for ( String k : doomed )
            System.clearProperty( k );
    }

    private boolean permitted( String fqcn )
    {
        try
        {
            ByNameInstantiationUtils.checkWarnThrowForInstantiateByNameGated( fqcn, null );
            return true;
        }
        catch ( InstantiationNotPermittedException e )
        { return false; }
    }

    // ---------- enforcement ----------

    /** With enforcement on, a name absent from the whitelist is refused. */
    public void testEnforcedWhitelistRefusesAbsentName()
    {
        System.setProperty( ENFORCE, "true" );
        System.setProperty( WHITELIST + ".layerOne", MARKER );

        assertTrue( "A whitelisted name must be permitted.", permitted( MARKER ) );
        assertFalse( "A name absent from an enforced whitelist must be refused.", permitted( OTHER ) );
    }

    /** and the refusal must be the documented type, since callers distinguish it. */
    public void testRefusalThrowsInstantiationNotPermittedException()
    {
        System.setProperty( ENFORCE, "true" );
        System.setProperty( WHITELIST + ".layerOne", MARKER );

        try
        {
            ByNameInstantiationUtils.instantiateByNameGated( OTHER, null );
            fail( "Expected refusal." );
        }
        catch ( InstantiationNotPermittedException expected )
        {}
        catch ( Exception e )
        { fail( "Expected InstantiationNotPermittedException, got " + e.getClass().getName() ); }
    }

    /** Default is to warn rather than refuse, so existing deployments keep working. */
    public void testUnenforcedWhitelistPermitsAbsentName()
    {
        System.setProperty( WHITELIST + ".layerOne", MARKER );
        // ENFORCE deliberately unset

        assertTrue( "With enforcement unset, an absent name must still be permitted.", permitted( OTHER ) );
    }

    public void testExplicitlyDisabledEnforcementPermitsAbsentName()
    {
        System.setProperty( ENFORCE, "false" );
        System.setProperty( WHITELIST + ".layerOne", MARKER );

        assertTrue( permitted( OTHER ) );
    }

    // ---------- the wildcard ----------

    /** '*' alone disables the whitelist. */
    public void testLoneWildcardPermitsAnything()
    {
        System.setProperty( ENFORCE, "true" );
        System.setProperty( WHITELIST + ".layerOne", "*" );

        assertTrue( permitted( OTHER ) );
        assertTrue( permitted( "com.example.Anything" ) );
    }

    /**
     *  but a '*' that shares the whitelist with a real name is NOT a wildcard. Layered
     *  whitelists compose by union, so one layer's '*' must not silently open the gate that
     *  another layer narrowed; the conservative reading wins, and the class logs a warning
     *  saying so.
     */
    public void testWildcardAmongOtherEntriesIsNotAWildcard()
    {
        System.setProperty( ENFORCE, "true" );
        System.setProperty( WHITELIST + ".layerOne", "*" );
        System.setProperty( WHITELIST + ".layerTwo", MARKER );

        assertTrue( "The explicitly named class is still permitted.", permitted( MARKER ) );
        assertFalse( "A '*' sharing the whitelist with other entries must not act as a wildcard.",
                     permitted( OTHER ) );
    }

    // ---------- layering and override ----------

    /** Subkeys union, so independent layers can each contribute. */
    public void testWhitelistSubkeysUnion()
    {
        System.setProperty( ENFORCE, "true" );
        System.setProperty( WHITELIST + ".layerOne", MARKER );
        System.setProperty( WHITELIST + ".layerTwo", "com.example.Beta" );

        assertTrue( permitted( MARKER ) );
        assertTrue( permitted( "com.example.Beta" ) );
        assertFalse( permitted( OTHER ) );
    }

    /** A single key may carry a comma-separated list. */
    public void testCommaSeparatedListInOneKey()
    {
        System.setProperty( ENFORCE, "true" );
        System.setProperty( WHITELIST + ".layerOne", MARKER + " , com.example.Beta" );

        assertTrue( permitted( MARKER ) );
        assertTrue( permitted( "com.example.Beta" ) );
        assertFalse( permitted( OTHER ) );
    }

    /** The bare whitelist key, with no subkey, must count. */
    public void testBareWhitelistKeyCounts()
    {
        System.setProperty( ENFORCE, "true" );
        System.setProperty( WHITELIST, MARKER );

        assertTrue( "A whitelist set on the bare key must be honored.", permitted( MARKER ) );
        assertFalse( permitted( OTHER ) );
    }

    /** The override replaces the union outright rather than adding to it. */
    public void testOverrideReplacesTheUnion()
    {
        System.setProperty( ENFORCE, "true" );
        System.setProperty( WHITELIST + ".layerOne", MARKER );
        System.setProperty( OVERRIDE, "com.example.OnlyThis" );

        assertTrue( "The override's own entry is permitted.", permitted( "com.example.OnlyThis" ) );
        assertFalse( "An entry only in the overridden union must no longer be permitted.",
                     permitted( MARKER ) );
    }

    /** which is how a deployment gets a working wildcard despite a layered '*' being ignored. */
    public void testOverrideCanSupplyAWorkingWildcard()
    {
        System.setProperty( ENFORCE, "true" );
        System.setProperty( WHITELIST + ".layerOne", MARKER );
        System.setProperty( WHITELIST + ".layerTwo", "*" );
        System.setProperty( OVERRIDE, "*" );

        assertTrue( permitted( OTHER ) );
    }

    /** A blank value contributes nothing, rather than an empty-string entry. */
    public void testBlankWhitelistValueContributesNothing()
    {
        System.setProperty( ENFORCE, "true" );
        System.setProperty( WHITELIST + ".layerOne", "   " );
        System.setProperty( WHITELIST + ".layerTwo", "*" );

        assertTrue( "A blank entry must not defeat a lone '*'.", permitted( OTHER ) );
    }

    // ---------- instantiation ----------

    public void testGatedInstantiationReturnsAnInstance() throws Exception
    {
        System.setProperty( ENFORCE, "true" );
        System.setProperty( WHITELIST + ".layerOne", MARKER );

        Object o = ByNameInstantiationUtils.instantiateByNameGated( MARKER, null );
        assertTrue( o instanceof ByNameMarker );
    }

    public void testUngatedInstantiationIgnoresTheWhitelist() throws Exception
    {
        System.setProperty( ENFORCE, "true" );
        System.setProperty( WHITELIST + ".layerOne", "com.example.SomethingElse" );

        Object o = ByNameInstantiationUtils.instantiateByNameUngated( MARKER );
        assertTrue( "The ungated entry point is the documented bypass.", o instanceof ByNameMarker );
    }

    /**
     *  The preloaded-Class overload must instantiate the Class it is given, not re-resolve
     *  the name.
     *
     *  <p>This is why: c3p0's DriverManagerDataSource.loadDriverClass falls back to the
     *  thread context ClassLoader when Class.forName fails, so the Class it hands over may
     *  be one that a second Class.forName cannot find at all. An implementation that
     *  re-resolved would throw ClassNotFoundException on exactly the deployments the
     *  fallback exists for -- and would pass every test that does not involve a second
     *  ClassLoader, which is why this one builds one.</p>
     */
    public void testPreloadedClassIsUsedRatherThanReResolved() throws Exception
    {
        ClassLoader shadowing = new ShadowingClassLoader( getClass().getClassLoader(), MARKER );
        Class<?> shadow = shadowing.loadClass( MARKER );

        assertEquals( "Precondition: same name.", MARKER, shadow.getName() );
        assertNotSame( "Precondition: a genuinely distinct Class object.", ByNameMarker.class, shadow );

        Object o = ByNameInstantiationUtils.instantiateByNameUngated( MARKER, shadow );

        assertSame( "The instance must come from the Class supplied, not from a re-resolution of its name.",
                    shadow, o.getClass() );
        assertNotSame( ByNameMarker.class, o.getClass() );
    }

    /** and it must reject a Class whose name disagrees with the one given. */
    public void testPreloadedClassMustMatchTheName() throws Exception
    {
        try
        {
            ByNameInstantiationUtils.instantiateByNameUngated( "com.example.Mismatch", ByNameMarker.class );
            fail( "A Class whose name differs from the fqcn must be rejected." );
        }
        catch ( IllegalArgumentException expected )
        {}
    }


    // ---------- what the whitelist reports about itself ----------

    /**
     *  An unconfigured whitelist is MISSING, not merely empty. The distinction is the
     *  point: a deployment that configured a deny-all list meant it, while one that
     *  configured nothing has simply not been told yet, and callers that must insist on
     *  configuration -- ReferenceableUtils.findMandatoryObjectFactoryWhitelist is the one
     *  this exists for -- can tell those two apart only through the source.
     */
    public void testUnconfiguredWhitelistReportsSourceMissing()
    {
        WhitelistInfo info = ByNameInstantiationUtils.currentWhitelistInfo( null );

        assertEquals( "Nothing configured must report MISSING.",
                      WhitelistInfo.Source.MISSING, info.getSource() );
        assertTrue( "A MISSING whitelist must still carry an empty Set, never null.",
                    info.getWhitelist().isEmpty() );
    }

    /** An explicitly empty whitelist is a configured deny-all, and says so. */
    public void testExplicitlyEmptyWhitelistReportsItsKeyAsSource()
    {
        System.setProperty( WHITELIST + ".layerOne", "" );

        WhitelistInfo info = ByNameInstantiationUtils.currentWhitelistInfo( null );

        assertEquals( "A configured-but-empty whitelist is MAIN_WHITELIST, not MISSING.",
                      WhitelistInfo.Source.MAIN_WHITELIST, info.getSource() );
        assertTrue( info.getWhitelist().isEmpty() );
    }

    /** Both empty cases deny, however they are labelled. */
    public void testBothEmptyAndMissingDenyUnderEnforcement()
    {
        System.setProperty( ENFORCE, "true" );
        assertFalse( "A MISSING whitelist must permit nothing under enforcement.", permitted( OTHER ) );

        System.setProperty( WHITELIST + ".layerOne", "" );
        assertFalse( "A configured empty whitelist must permit nothing either.", permitted( OTHER ) );
    }

    public void testConfiguredWhitelistReportsItsContents()
    {
        System.setProperty( WHITELIST + ".layerOne", MARKER );

        WhitelistInfo info = ByNameInstantiationUtils.currentWhitelistInfo( null );

        assertEquals( WhitelistInfo.Source.MAIN_WHITELIST, info.getSource() );
        assertEquals( Collections.singleton( MARKER ), info.getWhitelist() );
    }

    /**
     *  An override in force must report itself as the source. Diagnostics name the key
     *  that decided, and naming the subkeys here would send a reader to edit a key whose
     *  contents were discarded.
     */
    public void testOverrideReportsSourceOverride()
    {
        System.setProperty( WHITELIST + ".layerOne", MARKER );
        System.setProperty( OVERRIDE, "com.example.OnlyThis" );

        WhitelistInfo info = ByNameInstantiationUtils.currentWhitelistInfo( null );

        assertEquals( WhitelistInfo.Source.OVERRIDE, info.getSource() );
        assertEquals( Collections.singleton( "com.example.OnlyThis" ), info.getWhitelist() );
    }

    /**
     *  currentWhitelistInfo(...) is a snapshot, not a view: a later resolution does not reach
     *  back and alter one already handed out.
     *
     *  <p>Demonstrated by <i>narrowing</i>, since the gate now ratchets and a widening would
     *  simply be ignored -- which would make the test pass for the wrong reason, proving only
     *  that nothing had changed anywhere.</p>
     */
    public void testWhitelistInfoIsASnapshot()
    {
        System.setProperty( WHITELIST + ".layerOne", MARKER + ",com.example.Beta" );
        WhitelistInfo before = ByNameInstantiationUtils.currentWhitelistInfo( null );
        assertEquals( "Precondition: two entries to begin with.", 2, before.getWhitelist().size() );

        System.setProperty( WHITELIST + ".layerOne", MARKER );
        WhitelistInfo after = ByNameInstantiationUtils.currentWhitelistInfo( null );

        assertEquals( "The narrowing must take effect in the new reading.",
                      Collections.singleton( MARKER ), after.getWhitelist() );
        assertEquals( "but must not reach back into the earlier one.",
                      2, before.getWhitelist().size() );
    }

    /**
     *  toString is what lands in c3p0's SQLException message when a driver class is refused,
     *  so it must name the source and the contents, and must not fall back to Object's
     *  identity form.
     */
    public void testWhitelistInfoToStringIsLegible() throws Exception
    {
        // Three separate deployments, not three phases of one: the gate latches at its first
        // lookup, so each configuration needs the state an unstarted JVM would have.
        String missing = ByNameInstantiationUtils.currentWhitelistInfo( null ).toString();
        assertFalse( "toString must be overridden.", missing.contains( "@" ) );
        assertTrue( "A MISSING whitelist must say so: " + missing, missing.contains( "missing" ) );

        SecurityRatchetTestSupport.resetAll( ByNameInstantiationUtils.class );
        System.setProperty( WHITELIST + ".layerOne", MARKER );
        String main = ByNameInstantiationUtils.currentWhitelistInfo( null ).toString();
        assertTrue( "The contents belong in the message: " + main, main.contains( MARKER ) );

        SecurityRatchetTestSupport.resetAll( ByNameInstantiationUtils.class );
        System.setProperty( OVERRIDE, "com.example.OnlyThis" );
        String override = ByNameInstantiationUtils.currentWhitelistInfo( null ).toString();
        assertTrue( "An override must identify itself: " + override, override.contains( "override" ) );
        assertFalse( "and must not report the entries it discarded: " + override,
                     override.contains( MARKER ) );
    }

    /**
     *  The same whitelist reached by different routes is not the same WhitelistInfo. Identity
     *  spans all three components, because the keys a whitelist came from are load-bearing:
     *  EarliestOrNarrowestWhitelistManager re-resolves against exactly those keys, so two infos
     *  with identical contents but different origins would behave differently on the next
     *  lookup.
     */
    public void testWhitelistInfoEqualityIncludesSourceAndKeys()
    {
        Set<String> keys      = Collections.singleton( "a.key" );
        Set<String> otherKeys = Collections.singleton( "another.key" );

        WhitelistInfo viaMain = new WhitelistInfo( Collections.singleton( MARKER ),
                                                   keys, WhitelistInfo.Source.MAIN_WHITELIST );
        WhitelistInfo viaMainAgain = new WhitelistInfo( Collections.singleton( MARKER ),
                                                        keys, WhitelistInfo.Source.MAIN_WHITELIST );
        WhitelistInfo viaOverride = new WhitelistInfo( Collections.singleton( MARKER ),
                                                       keys, WhitelistInfo.Source.OVERRIDE );
        WhitelistInfo viaOtherKeys = new WhitelistInfo( Collections.singleton( MARKER ),
                                                        otherKeys, WhitelistInfo.Source.MAIN_WHITELIST );

        assertEquals( viaMain, viaMainAgain );
        assertEquals( viaMain.hashCode(), viaMainAgain.hashCode() );
        assertFalse( "Source participates in identity.", viaMain.equals( viaOverride ) );
        assertFalse( "and so do the keys the whitelist was computed from.",
                     viaMain.equals( viaOtherKeys ) );
    }

    /** The value object defends its own invariants: nothing handed out may be mutated. */
    public void testWhitelistInfoIsImmutable()
    {
        Set<String> mutableWhitelist = new HashSet<String>( Collections.singleton( MARKER ) );
        Set<String> mutableKeys      = new HashSet<String>( Collections.singleton( "a.key" ) );

        WhitelistInfo info = new WhitelistInfo( mutableWhitelist, mutableKeys,
                                                WhitelistInfo.Source.MAIN_WHITELIST );
        try
        {
            info.getWhitelist().add( "com.example.Sneaky" );
            fail( "A caller must not be able to widen a whitelist in place." );
        }
        catch ( UnsupportedOperationException expected )
        {}

        try
        {
            info.getFromKeys().add( "sneaky.key" );
            fail( "nor add a key the whitelist will later be re-resolved against." );
        }
        catch ( UnsupportedOperationException expected )
        {}
    }

    public void testWhitelistInfoRejectsNulls()
    {
        try
        {
            new WhitelistInfo( null, Collections.<String>emptySet(), WhitelistInfo.Source.MISSING );
            fail( "A null whitelist must be rejected." );
        }
        catch ( IllegalArgumentException expected )
        {}

        try
        {
            new WhitelistInfo( Collections.<String>emptySet(), null, WhitelistInfo.Source.MISSING );
            fail( "A null key set must be rejected." );
        }
        catch ( IllegalArgumentException expected )
        {}

        try
        {
            new WhitelistInfo( Collections.<String>emptySet(), Collections.<String>emptySet(), null );
            fail( "A null source must be rejected." );
        }
        catch ( IllegalArgumentException expected )
        {}
    }

    /**
     *  Defines one named class from its own bytes rather than delegating, yielding a Class
     *  object distinct from the parent's for the same name.
     */
    private static class ShadowingClassLoader extends ClassLoader
    {
        private final String target;

        ShadowingClassLoader( ClassLoader parent, String target )
        {
            super( parent );
            this.target = target;
        }

        @Override
        protected Class<?> loadClass( String name, boolean resolve ) throws ClassNotFoundException
        {
            if ( ! target.equals( name ) )
                return super.loadClass( name, resolve );

            Class<?> already = findLoadedClass( name );
            if ( already != null )
                return already;

            byte[] bytes = readBytes( name );
            Class<?> defined = defineClass( name, bytes, 0, bytes.length );
            if ( resolve )
                resolveClass( defined );
            return defined;
        }

        private byte[] readBytes( String name ) throws ClassNotFoundException
        {
            String path = name.replace( '.', '/' ) + ".class";
            InputStream is = getParent().getResourceAsStream( path );
            if ( is == null )
                throw new ClassNotFoundException( name );
            try
            {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                for ( int n = is.read( buf ); n >= 0; n = is.read( buf ) )
                    baos.write( buf, 0, n );
                return baos.toByteArray();
            }
            catch ( IOException e )
            { throw new ClassNotFoundException( name, e ); }
            finally
            { try { is.close(); } catch ( IOException e ) {} }
        }
    }
}
