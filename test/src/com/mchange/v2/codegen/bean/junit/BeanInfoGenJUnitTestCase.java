package com.mchange.v2.codegen.bean.junit;

import java.beans.BeanInfo;
import java.beans.EventSetDescriptor;
import java.beans.IndexedPropertyDescriptor;
import java.beans.MethodDescriptor;
import java.beans.PropertyChangeListener;
import java.beans.PropertyDescriptor;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import java.net.URI;

import java.security.CodeSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import junit.framework.TestCase;

import com.mchange.v2.codegen.bean.BeanInfoGen;

/**
 * Exercises {@link BeanInfoGen} end-to-end: it generates the source of an explicit BeanInfo for a
 * sample bean, compiles that source in-memory with the system Java compiler, loads and instantiates
 * the result, and asserts on the descriptors the generated BeanInfo reports.
 *
 * If no system Java compiler is available (i.e. the tests are running on a JRE rather than a JDK),
 * the compile-and-load assertions are skipped so the suite still passes.
 */
public class BeanInfoGenJUnitTestCase extends TestCase
{
    // ==========================================
    // Sample bean.
    //
    // Public + static so that it is reflectively accessible, and so that the canonical-name class
    // literal the generator emits ("...BeanInfoGenJUnitTestCase.Widget.class") is legal source.
    //
    // "tags" exercises indexed properties; the add/removePropertyChangeListener pair produces a
    // "propertyChange" event set; "secret" is the property we exclude in the exclusion tests.
    // ==========================================
    public static class Widget
    {
	private String   name;
	private int      size;
	private boolean  active;
	private String   secret;
	private String[] tags;
	private String[] labels;

	public String getName()                  { return name; }
	public void   setName( String n )        { this.name = n; }

	public int  getSize()                    { return size; }
	public void setSize( int s )             { this.size = s; }

	public boolean isActive()                { return active; }
	public void    setActive( boolean a )    { this.active = a; }

	public String getSecret()                { return secret; }
	public void   setSecret( String s )      { this.secret = s; }

	// indexed property defined by BOTH an array accessor and a single-index accessor
	public String[] getTags()                { return tags; }
	public void     setTags( String[] t )    { this.tags = t; }
	public String   getTags( int i )         { return tags[i]; }
	public void     setTags( int i, String v ) { tags[i] = v; }

	// indexed property defined by a single-index accessor ONLY (no array accessor)
	public String getLabels( int i )         { return labels[i]; }
	public void   setLabels( int i, String v ) { labels[i] = v; }

	public void doSomething( int count, String label ) {}

	public void addPropertyChangeListener( PropertyChangeListener pcl )    {}
	public void removePropertyChangeListener( PropertyChangeListener pcl ) {}
    }

    private final static String BEAN_INFO_FQCN = Widget.class.getPackage().getName() + ".WidgetBeanInfo";

    // ==========================================
    // Tests
    // ==========================================

    public void testExcludedPropertyIsNotAPropertyButRemainsAMethod() throws Exception
    {
	BeanInfo bi = beanInfoFor( exclusions( "secret" ) );
	if ( bi == null ) return; // no compiler; skip

	Set propNames = propertyNames( bi );
	assertFalse( "Excluded property 'secret' should not appear among property descriptors.", propNames.contains( "secret" ) );

	// the accessor methods of the excluded property must still be reported as ordinary methods
	Set methodNames = methodNames( bi );
	assertTrue( "getSecret should remain a reported method.", methodNames.contains( "getSecret" ) );
	assertTrue( "setSecret should remain a reported method.", methodNames.contains( "setSecret" ) );
    }

    public void testNonExcludedPropertiesArePresent() throws Exception
    {
	BeanInfo bi = beanInfoFor( exclusions( "secret" ) );
	if ( bi == null ) return;

	Set propNames = propertyNames( bi );
	assertTrue( propNames.contains( "name" ) );
	assertTrue( propNames.contains( "size" ) );
	assertTrue( propNames.contains( "active" ) );
	assertTrue( propNames.contains( "tags" ) );
	assertTrue( "Default introspection exposes a read-only 'class' property; full replication keeps it.",
		    propNames.contains( "class" ) );
    }

    public void testNoExclusionsKeepsAllProperties() throws Exception
    {
	BeanInfo bi = beanInfoFor( Collections.EMPTY_SET );
	if ( bi == null ) return;

	assertTrue( "With no exclusions, 'secret' should be a property.", propertyNames( bi ).contains( "secret" ) );
    }

    public void testNullExcludedSetTreatedAsNoExclusions() throws Exception
    {
	BeanInfo bi = beanInfoFor( null );
	if ( bi == null ) return;

	assertTrue( "A null excluded set should behave like an empty one.", propertyNames( bi ).contains( "secret" ) );
    }

    public void testTagsIsIndexedProperty() throws Exception
    {
	BeanInfo bi = beanInfoFor( exclusions( "secret" ) );
	if ( bi == null ) return;

	PropertyDescriptor tags = propertyByName( bi, "tags" );
	assertNotNull( "Expected a 'tags' property.", tags );
	assertTrue( "'tags' should be an indexed property.", tags instanceof IndexedPropertyDescriptor );
    }

    public void testEventSetReplicated() throws Exception
    {
	BeanInfo bi = beanInfoFor( exclusions( "secret" ) );
	if ( bi == null ) return;

	boolean found = false;
	EventSetDescriptor[] esds = bi.getEventSetDescriptors();
	for ( int i = 0; i < esds.length; ++i )
	    if ( "propertyChange".equals( esds[i].getName() ) )
		found = true;
	assertTrue( "Expected the 'propertyChange' event set to be replicated.", found );
    }

    public void testBeanDescriptorReplicated() throws Exception
    {
	BeanInfo bi = beanInfoFor( exclusions( "secret" ) );
	if ( bi == null ) return;

	assertEquals( Widget.class, bi.getBeanDescriptor().getBeanClass() );
    }

    public void testPropertiesExcludedByType() throws Exception
    {
	// excluding String.class drops the String-typed scalar properties (name, secret) and, because an
	// indexed property's element type is considered, the String-element indexed properties as well
	BeanInfo bi = beanInfoFor( Collections.EMPTY_SET, typeExclusions( String.class ) );
	if ( bi == null ) return;

	Set propNames = propertyNames( bi );
	assertFalse( "String property 'name' should be excluded by type.", propNames.contains( "name" ) );
	assertFalse( "String property 'secret' should be excluded by type.", propNames.contains( "secret" ) );

	assertTrue( "int property 'size' should remain.", propNames.contains( "size" ) );
	assertTrue( "boolean property 'active' should remain.", propNames.contains( "active" ) );
	assertTrue( "Class property 'class' should remain.", propNames.contains( "class" ) );

	// as with name-based exclusion, the accessors of a type-excluded property remain ordinary methods
	Set methods = methodNames( bi );
	assertTrue( methods.contains( "getName" ) );
	assertTrue( methods.contains( "setName" ) );
    }

    public void testIndexedPropertiesExcludedByElementTypeRegardlessOfConvention() throws Exception
    {
	// 'tags' is declared with both an array accessor and a single-index accessor; 'labels' with only
	// a single-index accessor. Excluding the element type (String) must drop both, identically.
	BeanInfo bi = beanInfoFor( Collections.EMPTY_SET, typeExclusions( String.class ) );
	if ( bi == null ) return;

	Set propNames = propertyNames( bi );
	assertFalse( "Array-and-index indexed property 'tags' should be excluded by element type String.", propNames.contains( "tags" ) );
	assertFalse( "Index-only indexed property 'labels' should be excluded by element type String.", propNames.contains( "labels" ) );

	// and the indexed accessors survive as ordinary methods
	Set methods = methodNames( bi );
	assertTrue( methods.contains( "getTags" ) );
	assertTrue( methods.contains( "getLabels" ) );
    }

    public void testIndexedPropertiesExcludedByArrayTypeRegardlessOfConvention() throws Exception
    {
	// Excluding the array type (String[]) must likewise drop both indexed properties, even 'labels',
	// whose descriptor reports no array type (we derive it from the element type).
	BeanInfo bi = beanInfoFor( Collections.EMPTY_SET, typeExclusions( String[].class ) );
	if ( bi == null ) return;

	Set propNames = propertyNames( bi );
	assertFalse( "Array-and-index indexed property 'tags' should be excluded by array type String[].", propNames.contains( "tags" ) );
	assertFalse( "Index-only indexed property 'labels' should be excluded by array type String[].", propNames.contains( "labels" ) );

	// scalar String properties are not String[], so they remain
	assertTrue( "Scalar String property 'name' should remain when excluding String[].", propNames.contains( "name" ) );
    }

    public void testPropertiesExcludedByAssignableSupertype() throws Exception
    {
	// every reference-typed property is assignable to Object; primitive-typed properties are not
	BeanInfo bi = beanInfoFor( Collections.EMPTY_SET, typeExclusions( Object.class ) );
	if ( bi == null ) return;

	Set propNames = propertyNames( bi );
	assertFalse( "Reference-typed 'name' should be excluded as assignable to Object.", propNames.contains( "name" ) );
	assertFalse( "Reference-typed 'secret' should be excluded as assignable to Object.", propNames.contains( "secret" ) );
	assertFalse( "Array-typed 'tags' should be excluded as assignable to Object.", propNames.contains( "tags" ) );
	assertFalse( "Reference-typed 'class' should be excluded as assignable to Object.", propNames.contains( "class" ) );

	assertTrue( "Primitive int 'size' is not assignable to Object and should remain.", propNames.contains( "size" ) );
	assertTrue( "Primitive boolean 'active' is not assignable to Object and should remain.", propNames.contains( "active" ) );
    }

    public void testNameAndTypeExclusionsCombine() throws Exception
    {
	// exclude 'active' by name and String-typed properties by type
	BeanInfo bi = beanInfoFor( exclusions( "active" ), typeExclusions( String.class ) );
	if ( bi == null ) return;

	Set propNames = propertyNames( bi );
	assertFalse( "'active' excluded by name.", propNames.contains( "active" ) );
	assertFalse( "'name' excluded by type.", propNames.contains( "name" ) );
	assertFalse( "'secret' excluded by type.", propNames.contains( "secret" ) );
	assertFalse( "String-element indexed property 'tags' excluded by type.", propNames.contains( "tags" ) );

	assertTrue( "Primitive int 'size' should remain.", propNames.contains( "size" ) );
    }

    // ==========================================
    // Descriptor caching (suppressDescriptorCaching flag)
    //
    // With caching enabled (the default), the generated BeanInfo computes each descriptor set once into
    // instance fields and hands back a defensive clone() of the arrays -- so the array objects differ
    // between calls, but the descriptor *elements* are shared. With caching suppressed, every accessor
    // rebuilds its descriptors fresh, so even the elements differ between calls. See the Javadoc on
    // BeanInfoGen.explicitBeanInfoClassSourceForBeanClass(Class,Set,Set,boolean,boolean).
    // ==========================================

    public void testBeanDescriptorCachingHonorsFlag() throws Exception
    {
	BeanInfo cached = beanInfoFor( exclusions( "secret" ), Collections.EMPTY_SET, false, false );
	if ( cached == null ) return; // no compiler; skip

	assertSame( "With caching enabled, getBeanDescriptor() should return the one cached instance.",
		    cached.getBeanDescriptor(), cached.getBeanDescriptor() );

	BeanInfo fresh = beanInfoFor( exclusions( "secret" ), Collections.EMPTY_SET, true, false );
	assertNotSame( "With caching suppressed, getBeanDescriptor() should build a new instance each call.",
		       fresh.getBeanDescriptor(), fresh.getBeanDescriptor() );
    }

    public void testPropertyDescriptorCachingHonorsFlag() throws Exception
    {
	BeanInfo cached = beanInfoFor( exclusions( "secret" ), Collections.EMPTY_SET, false, false );
	if ( cached == null ) return;

	// a defensive clone of the array each call ...
	assertNotSame( "Cached getter should hand back a defensive array clone, not the cached array itself.",
		       cached.getPropertyDescriptors(), cached.getPropertyDescriptors() );
	// ... but the descriptor elements within it are shared
	assertSame( "With caching enabled, property descriptor instances should be shared across calls.",
		    propertyByName( cached, "name" ), propertyByName( cached, "name" ) );

	BeanInfo fresh = beanInfoFor( exclusions( "secret" ), Collections.EMPTY_SET, true, false );
	assertNotSame( "With caching suppressed, each call should build fresh property descriptor instances.",
		       propertyByName( fresh, "name" ), propertyByName( fresh, "name" ) );
    }

    public void testMethodAndEventDescriptorCachingHonorsFlag() throws Exception
    {
	BeanInfo cached = beanInfoFor( exclusions( "secret" ), Collections.EMPTY_SET, false, false );
	if ( cached == null ) return;

	assertNotSame( "Cached method-descriptor getter should clone its array.",
		       cached.getMethodDescriptors(), cached.getMethodDescriptors() );
	assertNotSame( "Cached event-set getter should clone its array.",
		       cached.getEventSetDescriptors(), cached.getEventSetDescriptors() );
	assertSame( "With caching enabled, method descriptor instances should be shared across calls.",
		    methodByName( cached, "getName" ), methodByName( cached, "getName" ) );
	assertSame( "With caching enabled, event-set descriptor instances should be shared across calls.",
		    eventSetByName( cached, "propertyChange" ), eventSetByName( cached, "propertyChange" ) );

	BeanInfo fresh = beanInfoFor( exclusions( "secret" ), Collections.EMPTY_SET, true, false );
	assertNotSame( "With caching suppressed, each call should build fresh method descriptor instances.",
		       methodByName( fresh, "getName" ), methodByName( fresh, "getName" ) );
	assertNotSame( "With caching suppressed, each call should build fresh event-set descriptor instances.",
		       eventSetByName( fresh, "propertyChange" ), eventSetByName( fresh, "propertyChange" ) );
    }

    public void testCachedDescriptorMutationVisibleAcrossCalls() throws Exception
    {
	// the documented hazard: cached descriptors are shared mutable objects, so a mutation by one
	// caller is visible to every later caller (and, via the Introspector's own BeanInfo cache, persists)
	BeanInfo bi = beanInfoFor( exclusions( "secret" ), Collections.EMPTY_SET, false, false );
	if ( bi == null ) return;

	propertyByName( bi, "name" ).setShortDescription( "MUTATED" );
	assertEquals( "With caching enabled, a mutation to a shared descriptor is visible to later callers.",
		      "MUTATED", propertyByName( bi, "name" ).getShortDescription() );
    }

    public void testSuppressedDescriptorMutationIsolatedAcrossCalls() throws Exception
    {
	// the reason the escape hatch exists: with caching suppressed, fresh descriptors isolate callers
	BeanInfo bi = beanInfoFor( exclusions( "secret" ), Collections.EMPTY_SET, true, false );
	if ( bi == null ) return;

	propertyByName( bi, "name" ).setShortDescription( "MUTATED" );
	assertFalse( "With caching suppressed, a mutation must not leak to a freshly built descriptor.",
		     "MUTATED".equals( propertyByName( bi, "name" ).getShortDescription() ) );
    }

    // ==========================================
    // Resilience logging (includeMLogging flag)
    // ==========================================

    public void testMLoggingPlumbingEmittedOnlyWhenRequested() throws Exception
    {
	String withLogging    = sourceFor( false, true );
	String withoutLogging = sourceFor( false, false );

	assertTrue( "mlogging source should declare an MLogger.",         withLogging.indexOf( "MLogger" ) >= 0 );
	assertTrue( "mlogging source should obtain it via MLog.",         withLogging.indexOf( "MLog.getLogger" ) >= 0 );
	assertTrue( "mlogging source should log skips at WARNING.",       withLogging.indexOf( "MLevel.WARNING" ) >= 0 );
	assertTrue( "mlogging source should import the mchange log API.", withLogging.indexOf( "com.mchange.v2.log" ) >= 0 );

	assertTrue( "non-mlogging source should not mention MLogger.",        withoutLogging.indexOf( "MLogger" ) < 0 );
	assertTrue( "non-mlogging source should not import the log API.",      withoutLogging.indexOf( "com.mchange.v2.log" ) < 0 );
    }

    public void testDescriptorCachingFieldsEmittedOnlyWhenCaching() throws Exception
    {
	String cached     = sourceFor( false, false );
	String suppressed = sourceFor( true,  false );

	assertTrue( "cached source should initialize descriptor cache fields.",
		    cached.indexOf( "_propertyDescriptors = _getPropertyDescriptors()" ) >= 0 );
	assertTrue( "cached source should hand back defensive clones.",
		    cached.indexOf( "_propertyDescriptors.clone()" ) >= 0 );

	assertTrue( "caching-suppressed source should not declare cache fields.",
		    suppressed.indexOf( "_propertyDescriptors = _getPropertyDescriptors()" ) < 0 );
	assertTrue( "caching-suppressed source should not clone (it returns fresh descriptors).",
		    suppressed.indexOf( ".clone()" ) < 0 );
    }

    public void testMLoggingVariantProducesEquivalentWorkingBeanInfo() throws Exception
    {
	BeanInfo logging = beanInfoFor( exclusions( "secret" ), Collections.EMPTY_SET, false, true );
	if ( logging == null ) return;
	BeanInfo nonLogging = beanInfoFor( exclusions( "secret" ), Collections.EMPTY_SET, false, false );

	assertEquals( "Enabling mlogging must not change which properties are reported.",
		      propertyNames( nonLogging ), propertyNames( logging ) );
	assertEquals( "Enabling mlogging must not change which methods are reported.",
		      methodNames( nonLogging ), methodNames( logging ) );
	assertEquals( "Enabling mlogging must not change the bean class.",
		      Widget.class, logging.getBeanDescriptor().getBeanClass() );
    }

    // ==========================================
    // Breadth: every flag combination compiles and reports the same descriptors
    // ==========================================

    public void testAllFlagCombinationsReportConsistentDescriptors() throws Exception
    {
	boolean[] options = { false, true };
	Set referenceProps   = null;
	Set referenceMethods = null;
	for ( int s = 0; s < options.length; ++s )
	    for ( int l = 0; l < options.length; ++l )
		{
		    BeanInfo bi = beanInfoFor( exclusions( "secret" ), Collections.EMPTY_SET, options[s], options[l] );
		    if ( bi == null ) return; // no compiler

		    String combo = "suppressDescriptorCaching=" + options[s] + ", includeMLogging=" + options[l];
		    Set props = propertyNames( bi );
		    assertTrue(  combo + ": expected property 'name'.",                      props.contains( "name" ) );
		    assertTrue(  combo + ": expected indexed property 'tags'.",              props.contains( "tags" ) );
		    assertFalse( combo + ": excluded property 'secret' must be absent.",     props.contains( "secret" ) );
		    assertTrue(  combo + ": excluded accessor 'getSecret' must remain a method.", methodNames( bi ).contains( "getSecret" ) );

		    if ( referenceProps == null )
			{
			    referenceProps   = props;
			    referenceMethods = methodNames( bi );
			}
		    else
			{
			    assertEquals( combo + ": properties should be identical across flag combinations.", referenceProps, props );
			    assertEquals( combo + ": methods should be identical across flag combinations.",    referenceMethods, methodNames( bi ) );
			}
		}
    }

    // ==========================================
    // Helpers: generate -> compile -> load -> instantiate
    // ==========================================

    /**
     * @return an instance of the generated BeanInfo for Widget, or null if no system compiler is available.
     */
    private static BeanInfo beanInfoFor( Set excludedPropertyNames ) throws Exception
    { return beanInfoFor( excludedPropertyNames, Collections.EMPTY_SET ); }

    /**
     * @return an instance of the generated BeanInfo for Widget, or null if no system compiler is available.
     */
    private static BeanInfo beanInfoFor( Set excludedPropertyNames, Set excludedPropertyTypes ) throws Exception
    { return beanInfoFor( excludedPropertyNames, excludedPropertyTypes, false, false ); }

    /**
     * @return an instance of the generated BeanInfo for Widget, generated with the given caching and
     *         mlogging flags, or null if no system compiler is available.
     */
    private static BeanInfo beanInfoFor( Set excludedPropertyNames, Set excludedPropertyTypes, boolean suppressDescriptorCaching, boolean includeMLogging ) throws Exception
    {
	String source = BeanInfoGen.explicitBeanInfoClassSourceForBeanClass( Widget.class, excludedPropertyNames, excludedPropertyTypes, suppressDescriptorCaching, includeMLogging );
	Class beanInfoClass = compileAndLoad( BEAN_INFO_FQCN, source );
	if ( beanInfoClass == null )
	    return null;
	return (BeanInfo) beanInfoClass.newInstance();
    }

    /**
     * The generated BeanInfo source (excluding 'secret') for the given caching and mlogging flags, for
     * tests that assert on the emitted source itself rather than on a compiled-and-loaded instance.
     */
    private static String sourceFor( boolean suppressDescriptorCaching, boolean includeMLogging ) throws Exception
    { return BeanInfoGen.explicitBeanInfoClassSourceForBeanClass( Widget.class, exclusions( "secret" ), Collections.EMPTY_SET, suppressDescriptorCaching, includeMLogging ); }

    private static Class compileAndLoad( String fqClassName, final String source ) throws Exception
    {
	JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
	if ( compiler == null )
	    return null; // running on a JRE, not a JDK

	DiagnosticCollector diagnostics = new DiagnosticCollector();
	StandardJavaFileManager standardFileManager = compiler.getStandardFileManager( diagnostics, null, null );

	// captures the bytecode the compiler emits, keyed by binary class name
	final Map classBytes = new HashMap();

	JavaFileManager fileManager = new ForwardingJavaFileManager( standardFileManager )
	{
	    public JavaFileObject getJavaFileForOutput( Location location, final String className, JavaFileObject.Kind kind, FileObject sibling )
		throws IOException
	    {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		classBytes.put( className, baos );
		URI uri = URI.create( "byte:///" + className.replace( '.', '/' ) + kind.extension );
		return new SimpleJavaFileObject( uri, kind )
		{
		    public OutputStream openOutputStream()
		    { return baos; }
		};
	    }
	};

	URI sourceUri = URI.create( "string:///" + fqClassName.replace( '.', '/' ) + JavaFileObject.Kind.SOURCE.extension );
	JavaFileObject sourceObject = new SimpleJavaFileObject( sourceUri, JavaFileObject.Kind.SOURCE )
	{
	    public CharSequence getCharContent( boolean ignoreEncodingErrors )
	    { return source; }
	};

	// compile against a classpath that can resolve the sample bean (see compilationClasspath)
	List options = Arrays.asList( "-classpath", compilationClasspath() );

	boolean ok = compiler.getTask( null, fileManager, diagnostics, options, null, Collections.singletonList( sourceObject ) ).call().booleanValue();
	if (! ok )
	    {
		StringBuffer sb = new StringBuffer( "In-memory compilation of generated BeanInfo failed:\n" );
		List diags = diagnostics.getDiagnostics();
		for ( int i = 0, len = diags.size(); i < len; ++i )
		    sb.append( diags.get(i) ).append( '\n' );
		sb.append( "\n--- generated source ---\n" ).append( source );
		fail( sb.toString() );
	    }

	final Map classDefs = new HashMap();
	for ( Iterator ii = classBytes.entrySet().iterator(); ii.hasNext(); )
	    {
		Map.Entry e = (Map.Entry) ii.next();
		classDefs.put( e.getKey(), ((ByteArrayOutputStream) e.getValue()).toByteArray() );
	    }

	ClassLoader loader = new ClassLoader( BeanInfoGenJUnitTestCase.class.getClassLoader() )
	{
	    protected Class findClass( String name ) throws ClassNotFoundException
	    {
		byte[] bytes = (byte[]) classDefs.get( name );
		if ( bytes == null )
		    throw new ClassNotFoundException( name );
		return defineClass( name, bytes, 0, bytes.length );
	    }
	};
	return loader.loadClass( fqClassName );
    }

    /**
     * The classpath to compile the generated source against.
     *
     * We cannot rely solely on "java.class.path": under sbt's in-process (unforked) test runner the
     * project's compiled test classes -- which is where our sample bean lives -- are loaded through
     * an sbt-managed classloader and are not on the JVM system classpath, so the generated source's
     * reference to the bean would not resolve. We therefore also add the code-source locations of the
     * classes the generated source actually needs, which is reliable regardless of how the tests are
     * launched.
     */
    private static String compilationClasspath()
    {
	Set entries = new LinkedHashSet();

	String jcp = System.getProperty( "java.class.path" );
	if ( jcp != null )
	    {
		String[] parts = jcp.split( File.pathSeparator );
		for ( int i = 0, len = parts.length; i < len; ++i )
		    if ( parts[i].length() > 0 )
			entries.add( parts[i] );
	    }

	addCodeSourceLocation( entries, BeanInfoGenJUnitTestCase.class ); // location of the sample bean
	addCodeSourceLocation( entries, BeanInfoGen.class );              // location of the generator

	StringBuffer sb = new StringBuffer();
	for ( Iterator ii = entries.iterator(); ii.hasNext(); )
	    {
		if ( sb.length() > 0 )
		    sb.append( File.pathSeparatorChar );
		sb.append( (String) ii.next() );
	    }
	return sb.toString();
    }

    private static void addCodeSourceLocation( Set entries, Class cl )
    {
	try
	    {
		CodeSource cs = cl.getProtectionDomain().getCodeSource();
		if ( cs != null && cs.getLocation() != null )
		    entries.add( new File( cs.getLocation().toURI() ).getAbsolutePath() );
	    }
	catch ( Exception e )
	    { /* best effort; fall back to whatever else is on the path */ }
    }

    // ==========================================
    // Helpers: small conveniences
    // ==========================================

    private static Set exclusions( String name )
    {
	Set out = new HashSet();
	out.add( name );
	return out;
    }

    private static Set typeExclusions( Class type )
    {
	Set out = new HashSet();
	out.add( type );
	return out;
    }

    private static Set propertyNames( BeanInfo bi )
    {
	Set out = new HashSet();
	PropertyDescriptor[] pds = bi.getPropertyDescriptors();
	for ( int i = 0, len = pds.length; i < len; ++i )
	    out.add( pds[i].getName() );
	return out;
    }

    private static PropertyDescriptor propertyByName( BeanInfo bi, String name )
    {
	PropertyDescriptor[] pds = bi.getPropertyDescriptors();
	for ( int i = 0, len = pds.length; i < len; ++i )
	    if ( name.equals( pds[i].getName() ) )
		return pds[i];
	return null;
    }

    private static Set methodNames( BeanInfo bi )
    {
	Set out = new HashSet();
	MethodDescriptor[] mds = bi.getMethodDescriptors();
	for ( int i = 0, len = mds.length; i < len; ++i )
	    out.add( mds[i].getName() );
	return out;
    }

    private static MethodDescriptor methodByName( BeanInfo bi, String name )
    {
	MethodDescriptor[] mds = bi.getMethodDescriptors();
	for ( int i = 0, len = mds.length; i < len; ++i )
	    if ( name.equals( mds[i].getName() ) )
		return mds[i];
	return null;
    }

    private static EventSetDescriptor eventSetByName( BeanInfo bi, String name )
    {
	EventSetDescriptor[] esds = bi.getEventSetDescriptors();
	for ( int i = 0, len = esds.length; i < len; ++i )
	    if ( name.equals( esds[i].getName() ) )
		return esds[i];
	return null;
    }
}
