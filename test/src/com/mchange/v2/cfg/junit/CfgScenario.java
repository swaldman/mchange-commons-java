package com.mchange.v2.cfg.junit;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 *  A test harness that runs com.mchange.v2.cfg against a synthetic classpath.
 *
 *  The cfg package resolves every resource it reads through the ClassLoader that
 *  defined its own classes:
 *
 *    - ConfigUtils.class.getResourceAsStream(..)               (resource-path text files)
 *    - MultiPropertiesConfig.class.getResourceAsStream(..)     (properties resources)
 *    - BasicMultiPropertiesConfig.class.getResource(..)        (HOCON-absent probe)
 *    - HoconPropertiesConfigSource.class.getClassLoader()      (HOCON resources)
 *
 *  So loading the cfg classes afresh in a child ClassLoader whose first classpath
 *  entry is a scenario directory redirects ALL of that resolution into the scenario.
 *  As a side effect the package's process-wide statics -- MConfig.cache and
 *  ConfigUtils.canonicalDefaultConfig -- are reinitialized per scenario, which is
 *  otherwise very difficult to arrange.
 *
 *  It also lets us omit typesafe-config from the child classpath, exercising the
 *  "HOCON libraries not available" branch even though config is present on the
 *  ordinary test classpath as a compile-optional dependency.
 *
 *  Scenario directories live under test/resources/cfgscenarios/<name>/ and are
 *  copied onto the test classpath by the ordinary build; no build configuration is
 *  required. Each contains a marker file, which both locates the directory at
 *  runtime and keeps otherwise-empty scenarios tracked by git.
 *
 *  The parent ClassLoader is the platform loader, not null and not the application
 *  loader: the platform loader supplies the JDK (including java.sql and javax.naming,
 *  which are NOT visible through the bootstrap loader on Java 9+) while still hiding
 *  every com.mchange class, so the child is forced to define its own copies.
 */
public final class CfgScenario implements Closeable
{
    private final static String MARKER = "cfgscenario-marker.txt";

    private final static String CN_MCONFIG    = "com.mchange.v2.cfg.MConfig";
    private final static String CN_ASPROVIDED = "com.mchange.v2.cfg.MConfig$AsProvided";
    private final static String CN_TRADITIONAL =
        "com.mchange.v2.cfg.MConfig$WithTraditionalDefaultSources";
    private final static String CN_HOCON_CONFIG = "com.typesafe.config.Config";

    /** Reaches the cached readers, which are package-private. See shimRoot(). */
    private final static String CN_SHIM = "com.mchange.v2.cfg.MConfigCachedReadShim";

    /** No caller-supplied resources on this side of the read. */
    private final static String[] NO_PATHS = new String[0];

    // MT: guarded by this class' lock, via shimRoot()
    private static URL shimRoot = null;

    private final String          name;
    private final URLClassLoader  loader;

    public static CfgScenario open( String scenarioName, boolean withHocon )
    { return openWithRoots( scenarioName, withHocon ); }

    /** Convenience: scenarios that do not care about HOCON still get the library. */
    public static CfgScenario open( String scenarioName )
    { return open( scenarioName, true ); }

    /**
     *  Like open(..), but prepends caller-created directories to the scenario classpath.
     *
     *  Scenario directories under test/resources are fixed at build time, which is fine until
     *  a scenario's content must name something only known at runtime -- a resource-path text file
     *  pointing at a temp file, say. Such a directory is built by the test and passed here; being
     *  first on the classpath, it also shadows the named scenario.
     */
    public static CfgScenario openWithRoots( String scenarioName, boolean withHocon, java.io.File... extraRoots )
    {
        List<URL> urls = new ArrayList<URL>();
        for ( java.io.File root : extraRoots )
        {
            try
            { urls.add( root.toURI().toURL() ); }
            catch ( MalformedURLException e )
            { throw new RuntimeException( "Could not use '" + root + "' as a classpath root", e ); }
        }
        urls.add( scenarioRoot( scenarioName ) );
        urls.add( codeSourceOf( "com.mchange.v2.cfg.MConfig" ) );
        urls.add( shimRoot() );
        if ( withHocon )
            urls.add( codeSourceOf( CN_HOCON_CONFIG ) );

        return new CfgScenario( scenarioName,
                                new URLClassLoader( urls.toArray( new URL[ urls.size() ] ), platformClassLoader() ) );
    }

    private CfgScenario( String name, URLClassLoader loader )
    {
        this.name   = name;
        this.loader = loader;
    }

    public String name()
    { return name; }

    public ClassLoader classLoader()
    { return loader; }

    // ---------------------------------------------------------------- classpath

    private static ClassLoader platformClassLoader()
    {
        // On Java 9+ this is the platform loader; on Java 8, the extension loader.
        // Either way it carries the JDK and no application classes. Reached this way
        // rather than via ClassLoader.getPlatformClassLoader() so the harness still
        // compiles under a Java 8 toolchain.
        return ClassLoader.getSystemClassLoader().getParent();
    }

    private static URL scenarioRoot( String scenarioName )
    {
        String markerPath = "/cfgscenarios/" + scenarioName + "/" + MARKER;
        URL markerUrl = CfgScenario.class.getResource( markerPath );
        if ( markerUrl == null )
            throw new IllegalArgumentException(
                String.format( "No cfg scenario named '%s': expected a marker resource at '%s'. " +
                               "Scenario directories live under test/resources/cfgscenarios/.",
                               scenarioName, markerPath ) );

        String s = markerUrl.toExternalForm();
        String root = s.substring( 0, s.length() - MARKER.length() ); // keeps the trailing '/'
        try
        { return new URL( root ); }
        catch ( MalformedURLException e )
        { throw new RuntimeException( "Could not derive scenario root URL from '" + s + "'", e ); }
    }

    /**
     *  A classpath root holding MConfigCachedReadShim and nothing else.
     *
     *  <p>The shim must be DEFINED BY the scenario ClassLoader, or its package-private calls
     *  into MConfig would resolve against the copy on the ordinary test classpath instead of
     *  the scenario's. But its code source is target/test-classes, whose root also holds
     *  /mchange-commons.properties and /logging.properties -- and /mchange-commons.properties
     *  is one of the hardcoded backstop paths, so putting that directory on a scenario's
     *  classpath would silently supply configuration the scenario is supposed to lack. The
     *  BackstopDefaults tests exist precisely to check what happens when it is absent.</p>
     *
     *  <p>So the one class file is copied out to a directory of its own, once per JVM.</p>
     */
    private synchronized static URL shimRoot()
    {
        if ( shimRoot == null )
        {
            String classResource = '/' + CN_SHIM.replace( '.', '/' ) + ".class";
            try
            {
                Path root = Files.createTempDirectory( "mchange-cfg-shim-" );

                Path dir = root.resolve( CN_SHIM.substring( 0, CN_SHIM.lastIndexOf( '.' ) ).replace( '.', '/' ) );
                Files.createDirectories( dir );

                Path classFile = dir.resolve( CN_SHIM.substring( CN_SHIM.lastIndexOf( '.' ) + 1 ) + ".class" );
                InputStream is = CfgScenario.class.getResourceAsStream( classResource );
                if ( is == null )
                    throw new IllegalStateException( "Expected " + classResource + " on the test classpath" );
                try
                { Files.copy( is, classFile ); }
                finally
                { is.close(); }

                // deleteOnExit removes in reverse order of registration, so the shallowest
                // must be registered first for the class file to go before its directories
                List<Path> chain = new ArrayList<Path>();
                for ( Path p = classFile; p != null && !p.equals( root.getParent() ); p = p.getParent() )
                    chain.add( p );
                Collections.reverse( chain );
                for ( Path p : chain )
                    p.toFile().deleteOnExit();

                shimRoot = root.toUri().toURL();
            }
            catch ( IOException e )
            { throw new RuntimeException( "Could not stage " + CN_SHIM + " for the scenario ClassLoader", e ); }
        }
        return shimRoot;
    }

    private static URL codeSourceOf( String className )
    {
        try
        {
            Class<?> c = Class.forName( className );
            URL loc = c.getProtectionDomain().getCodeSource().getLocation();
            if ( loc == null )
                throw new IllegalStateException( "No code source location for " + className );
            return loc;
        }
        catch ( ClassNotFoundException e )
        { throw new IllegalStateException( "Expected " + className + " on the test classpath", e ); }
    }

    // ------------------------------------------------------------- entry points

    /** The child's copy of MConfig. Distinct from the caller's MConfig.class. */
    public Class<?> mconfigClass()
    { return load( CN_MCONFIG ); }

    /** The child's copy of the cached-reader shim. See shimRoot(). */
    public Class<?> shimClass()
    { return load( CN_SHIM ); }

    public Object asProvidedCached( String[] resourcePaths, List delayedLogItemsOut )
    { return asProvidedCached( NO_PATHS, resourcePaths, delayedLogItemsOut ); }

    public Object asProvidedCached( String[] resourcePaths )
    { return asProvidedCached( resourcePaths, null ); }

    public Object asProvidedCached( String[] defaults, String[] preempts, List delayedLogItemsOut )
    {
        return call( load( CN_SHIM ), "asProvidedCached",
                     new Class[] { String[].class, String[].class, List.class },
                     new Object[] { defaults, preempts, delayedLogItemsOut } );
    }

    public Object asProvidedUncached( String[] resourcePaths, List delayedLogItemsOut )
    { return asProvidedUncached( NO_PATHS, resourcePaths, delayedLogItemsOut ); }

    public Object asProvidedUncached( String[] resourcePaths )
    { return asProvidedUncached( resourcePaths, null ); }

    public Object asProvidedUncached( String[] defaults, String[] preempts, List delayedLogItemsOut )
    {
        return call( load( CN_ASPROVIDED ), "readUncachedClassloaderResourceConfig",
                     new Class[] { String[].class, String[].class, List.class },
                     new Object[] { defaults, preempts, delayedLogItemsOut } );
    }

    public Object traditionalCached( String[] defaults, String[] preempts, List delayedLogItemsOut )
    {
        return call( load( CN_SHIM ), "traditionalCached",
                     new Class[] { String[].class, String[].class, List.class },
                     new Object[] { defaults, preempts, delayedLogItemsOut } );
    }

    public Object traditionalCached( String[] defaults, String[] preempts )
    { return traditionalCached( defaults, preempts, null ); }

    /** The zero-argument form: no caller-supplied paths at all. */
    public Object traditionalCached()
    {
        return call( load( CN_TRADITIONAL ), "readCachedClassloaderResourceConfig",
                     new Class[] {}, new Object[] {} );
    }

    public Object traditionalUncached( String[] defaults, String[] preempts, List delayedLogItemsOut )
    {
        return call( load( CN_TRADITIONAL ), "readUncachedClassloaderResourceConfig",
                     new Class[] { String[].class, String[].class, List.class },
                     new Object[] { defaults, preempts, delayedLogItemsOut } );
    }

    public Object traditionalUncached( String[] defaults, String[] preempts )
    { return traditionalUncached( defaults, preempts, null ); }

    // --------------------------------------------------- MultiPropertiesConfig

    public String getProperty( Object mpc, String key )
    { return (String) invoke( mpc, "getProperty", new Class[] { String.class }, new Object[] { key } ); }

    public Properties getPropertiesByPrefix( Object mpc, String pfx )
    { return (Properties) invoke( mpc, "getPropertiesByPrefix", new Class[] { String.class }, new Object[] { pfx } ); }

    public Properties getPropertiesByResourcePath( Object mpc, String path )
    { return (Properties) invoke( mpc, "getPropertiesByResourcePath", new Class[] { String.class }, new Object[] { path } ); }

    public String[] getPropertiesResourcePaths( Object mpc )
    { return (String[]) invoke( mpc, "getPropertiesResourcePaths", new Class[] {}, new Object[] {} ); }

    // ------------------------------------------------------- DelayedLogItem

    /**
     *  Renders a List of child-loaded DelayedLogItems as {level, text, exceptionClassName}
     *  triples, so nothing crosses the ClassLoader boundary as an incompatible type.
     *  exceptionClassName is null when the item carries no Throwable.
     */
    public List<String[]> renderLogItems( List items )
    {
        List<String[]> out = new ArrayList<String[]>();
        if ( items == null ) return out;
        for ( Object item : items )
            out.add( renderLogItem( item ) );
        return out;
    }

    public List<String[]> getDelayedLogItems( Object mpc )
    { return renderLogItems( (List) invoke( mpc, "getDelayedLogItems", new Class[] {}, new Object[] {} ) ); }

    private String[] renderLogItem( Object item )
    {
        Object level = invoke( item, "getLevel", new Class[] {}, new Object[] {} );
        Object text  = invoke( item, "getText",  new Class[] {}, new Object[] {} );
        Object exc   = invoke( item, "getException", new Class[] {}, new Object[] {} );
        return new String[] {
            level == null ? null : level.toString(),
            (String) text,
            exc == null ? null : exc.getClass().getName()
        };
    }

    // ------------------------------------------------------------- assertions

    /** True if some rendered log item is at {@code level} and its text contains {@code fragment}. */
    public static boolean hasLogItem( List<String[]> rendered, String level, String fragment )
    {
        for ( String[] r : rendered )
            if ( level.equals( r[0] ) && r[1] != null && r[1].contains( fragment ) )
                return true;
        return false;
    }

    public static String describe( List<String[]> rendered )
    {
        StringBuilder sb = new StringBuilder();
        for ( String[] r : rendered )
            sb.append( "\n    [" ).append( r[0] ).append( "] " ).append( r[1] )
              .append( r[2] == null ? "" : "  (" + r[2] + ")" );
        return sb.toString();
    }

    // ------------------------------------------------------------- reflection

    private Class<?> load( String cn )
    {
        try
        { return Class.forName( cn, true, loader ); }
        catch ( ClassNotFoundException e )
        { throw new RuntimeException( "Scenario '" + name + "' could not load " + cn, e ); }
    }

    private static Object call( Class<?> c, String method, Class[] sig, Object[] args )
    {
        try
        {
            Method m = c.getMethod( method, sig );
            m.setAccessible( true );
            return m.invoke( null, args );
        }
        catch ( java.lang.reflect.InvocationTargetException e )
        { throw asUnchecked( e.getCause() ); }
        catch ( Exception e )
        { throw new RuntimeException( "Failed to invoke static " + c.getName() + "." + method, e ); }
    }

    private static Object invoke( Object target, String method, Class[] sig, Object[] args )
    {
        try
        {
            Method m = target.getClass().getMethod( method, sig );
            m.setAccessible( true );
            return m.invoke( target, args );
        }
        catch ( java.lang.reflect.InvocationTargetException e )
        { throw asUnchecked( e.getCause() ); }
        catch ( Exception e )
        { throw new RuntimeException( "Failed to invoke " + method + " on " + target.getClass().getName(), e ); }
    }

    /**
     *  Rethrows a Throwable raised inside the scenario. Note that the Throwable's class
     *  was loaded by the child, so callers must compare by class NAME, not by identity
     *  or instanceof, for any com.mchange type.
     */
    private static RuntimeException asUnchecked( Throwable t )
    {
        if ( t instanceof RuntimeException ) return (RuntimeException) t;
        if ( t instanceof Error ) throw (Error) t;
        return new RuntimeException( t );
    }

    /**
     *  Deliberately does NOT call URLClassLoader.close().
     *
     *  MLog.<clinit> starts an unjoined "MLog-Init-Reporter" thread (MLog.java:162,168)
     *  which keeps resolving classes -- DelayedLogItem.hashCode reaching for
     *  com.mchange.v2.lang.ObjectUtils, among others -- well after the call that
     *  triggered it has returned. Closing the loader wins that race often enough to
     *  spray NoClassDefFoundError across unrelated tests. A closed URLClassLoader also
     *  refuses only NEW class loads, so closing buys nothing for classes already defined.
     *
     *  So a scenario's loader is simply left to the garbage collector. Each scenario
     *  defines a few dozen small classes; across a whole suite run that is negligible,
     *  and it is the price of per-scenario static isolation.
     */
    public void close() throws IOException
    {}

    /** Symmetry with close(), for use in finally blocks without a checked exception. */
    public void closeQuietly()
    {}
}
