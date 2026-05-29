package com.mchange.v2.codegen.bean.junit;

import java.beans.BeanInfo;
import java.beans.EventSetDescriptor;
import java.beans.IndexedPropertyDescriptor;
import java.beans.MethodDescriptor;
import java.beans.PropertyChangeListener;
import java.beans.PropertyDescriptor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.net.URI;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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

	public String getName()                  { return name; }
	public void   setName( String n )        { this.name = n; }

	public int  getSize()                    { return size; }
	public void setSize( int s )             { this.size = s; }

	public boolean isActive()                { return active; }
	public void    setActive( boolean a )    { this.active = a; }

	public String getSecret()                { return secret; }
	public void   setSecret( String s )      { this.secret = s; }

	public String[] getTags()                { return tags; }
	public void     setTags( String[] t )    { this.tags = t; }
	public String   getTags( int i )         { return tags[i]; }
	public void     setTags( int i, String v ) { tags[i] = v; }

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

    // ==========================================
    // Helpers: generate -> compile -> load -> instantiate
    // ==========================================

    /**
     * @return an instance of the generated BeanInfo for Widget, or null if no system compiler is available.
     */
    private static BeanInfo beanInfoFor( Set excludedProperties ) throws Exception
    {
	String source = BeanInfoGen.explicitBeanInfoClassSourceForBeanClass( Widget.class, excludedProperties );
	Class beanInfoClass = compileAndLoad( BEAN_INFO_FQCN, source );
	if ( beanInfoClass == null )
	    return null;
	return (BeanInfo) beanInfoClass.newInstance();
    }

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

	// compile against the same classpath this test is running on, so Widget and java.beans resolve
	List options = Arrays.asList( "-classpath", System.getProperty( "java.class.path" ) );

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

    // ==========================================
    // Helpers: small conveniences
    // ==========================================

    private static Set exclusions( String name )
    {
	Set out = new HashSet();
	out.add( name );
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
}
