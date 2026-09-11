package com.mchange.v2.reflect.junit;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;

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
    private final static String PFX       = "com.mchange.v2.reflect.by-name-instantiation";
    private final static String WHITELIST = PFX + ".whitelist";
    private final static String ENFORCE   = PFX + ".enforce-whitelist";
    private final static String OVERRIDE  = PFX + ".override-whitelist";

    private final static String MARKER = ByNameMarker.class.getName();
    private final static String OTHER  = "com.mchange.v2.reflect.junit.NotOnAnyWhitelist";

    private Properties saved;

    @Override
    public void setUp()
    {
        saved = (Properties) System.getProperties().clone();
        clearOurKeys();
    }

    @Override
    public void tearDown()
    {
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
