package com.mchange.v2.codegen.bean;

import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.beans.EventSetDescriptor;
import java.beans.IndexedPropertyDescriptor;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.beans.PropertyDescriptor;

import java.io.IOException;
import java.io.StringWriter;

import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mchange.v2.io.IndentedWriter;

/**
 * Generates the Java source of an explicit <code>java.beans.BeanInfo</code> class for a bean class.
 *
 * By default the generated <code>BeanInfo</code> fully replicates the information that ordinary
 * introspection would have produced in the absence of any explicit <code>BeanInfo</code> for the
 * bean class -- its bean descriptor, property descriptors, event-set descriptors and method
 * descriptors.
 *
 * Property names supplied in <code>excludedProperties</code> are omitted from the generated
 * property descriptors. Because the generated <code>getPropertyDescriptors()</code> returns a
 * non-null array, the JavaBeans <code>Introspector</code> uses it verbatim rather than rediscovering
 * properties reflectively, so the accessor methods of an excluded property are no longer seen as
 * constituting a JavaBeans property. The methods themselves remain ordinary public methods and are
 * still reported among the method descriptors.
 */
public final class BeanInfoGen
{
    private final static String GENERATOR_NAME = BeanInfoGen.class.getName();

    public static String explicitBeanInfoClassSourceForBeanClass( Class beanClass, Set excludedProperties )
	throws IntrospectionException, IOException
    {
	if ( excludedProperties == null )
	    excludedProperties = Collections.EMPTY_SET;

	// IGNORE_IMMEDIATE_BEANINFO so that, on regeneration, we reproduce the information that would
	// have been introspected without the explicit BeanInfo we are (re)generating, while still
	// honoring any BeanInfo associated with superclasses (as ordinary introspection would).
	BeanInfo bi = Introspector.getBeanInfo( beanClass, Introspector.IGNORE_IMMEDIATE_BEANINFO );

	PropertyDescriptor[] allPds = bi.getPropertyDescriptors();
	List includedPds = new ArrayList( allPds.length );
	boolean hasIndexed = false;
	for ( int i = 0, len = allPds.length; i < len; ++i )
	    {
		PropertyDescriptor pd = allPds[i];
		if ( excludedProperties.contains( pd.getName() ) )
		    continue;
		includedPds.add( pd );
		if ( pd instanceof IndexedPropertyDescriptor )
		    hasIndexed = true;
	    }

	EventSetDescriptor[] esds = bi.getEventSetDescriptors();
	MethodDescriptor[]   mds  = bi.getMethodDescriptors();

	StringWriter sw = new StringWriter();
	IndentedWriter iw = new IndentedWriter( sw );

	String packageName       = packageNameForClass( beanClass );
	String beanInfoClassName  = beanClass.getSimpleName() + "BeanInfo";
	String beanClassLiteral   = beanClass.getCanonicalName();

	writeBannerComments( iw );
	iw.println();
	if ( packageName != null )
	    {
		iw.println("package " + packageName + ';');
		iw.println();
	    }
	writeImports( iw, hasIndexed );
	iw.println();
	writeClassJavaDocComment( iw, beanClass );
	iw.println("public class " + beanInfoClassName + " extends SimpleBeanInfo");
	iw.println('{');
	iw.upIndent();

	iw.println("private final static Class BEAN_CLASS = " + beanClassLiteral + ".class;");
	iw.println();

	writeGetBeanDescriptor( iw );
	iw.println();
	writeGetPropertyDescriptors( iw, includedPds );
	iw.println();
	writeGetEventSetDescriptors( iw, esds );
	iw.println();
	writeGetMethodDescriptors( iw, mds );

	iw.downIndent();
	iw.println('}');

	iw.flush();
	return sw.toString();
    }

    private static void writeGetBeanDescriptor( IndentedWriter iw ) throws IOException
    {
	iw.println("public BeanDescriptor getBeanDescriptor()");
	iw.println("{ return new BeanDescriptor( BEAN_CLASS ); }");
    }

    private static void writeGetPropertyDescriptors( IndentedWriter iw, List pds ) throws IOException
    {
	iw.println("public PropertyDescriptor[] getPropertyDescriptors()");
	iw.println('{');
	iw.upIndent();

	int n = pds.size();
	if ( n == 0 )
	    iw.println("return new PropertyDescriptor[0];");
	else
	    {
		iw.println("try");
		iw.println('{');
		iw.upIndent();

		iw.println("PropertyDescriptor[] pds = new PropertyDescriptor[ " + n + " ];");
		for ( int i = 0; i < n; ++i )
		    iw.println("pds[" + i + "] = " + propertyDescriptorExpression( (PropertyDescriptor) pds.get(i) ) + ';');
		iw.println("return pds;");

		iw.downIndent();
		iw.println('}');
		iw.println("catch ( IntrospectionException e )");
		iw.println("{ throw new RuntimeException( \"Could not construct PropertyDescriptors for explicit BeanInfo of \" + BEAN_CLASS.getName() + \".\", e ); }");
	    }

	iw.downIndent();
	iw.println('}');
    }

    private static String propertyDescriptorExpression( PropertyDescriptor pd )
    {
	String readName  = methodNameArgument( pd.getReadMethod() );
	String writeName = methodNameArgument( pd.getWriteMethod() );
	if ( pd instanceof IndexedPropertyDescriptor )
	    {
		IndexedPropertyDescriptor ipd = (IndexedPropertyDescriptor) pd;
		return "new IndexedPropertyDescriptor( " + quote( pd.getName() ) + ", BEAN_CLASS, " +
		    readName + ", " + writeName + ", " +
		    methodNameArgument( ipd.getIndexedReadMethod() ) + ", " + methodNameArgument( ipd.getIndexedWriteMethod() ) + " )";
	    }
	else
	    return "new PropertyDescriptor( " + quote( pd.getName() ) + ", BEAN_CLASS, " + readName + ", " + writeName + " )";
    }

    private static void writeGetEventSetDescriptors( IndentedWriter iw, EventSetDescriptor[] esds ) throws IOException
    {
	iw.println("public EventSetDescriptor[] getEventSetDescriptors()");
	iw.println('{');
	iw.upIndent();

	if ( esds.length == 0 )
	    iw.println("return new EventSetDescriptor[0];");
	else
	    {
		iw.println("try");
		iw.println('{');
		iw.upIndent();

		iw.println("EventSetDescriptor[] esds = new EventSetDescriptor[ " + esds.length + " ];");
		for ( int i = 0, len = esds.length; i < len; ++i )
		    writeEventSetDescriptorAssignment( iw, esds[i], i );
		iw.println("return esds;");

		iw.downIndent();
		iw.println('}');
		iw.println("catch ( IntrospectionException e )");
		iw.println("{ throw new RuntimeException( \"Could not construct EventSetDescriptors for explicit BeanInfo of \" + BEAN_CLASS.getName() + \".\", e ); }");
	    }

	iw.downIndent();
	iw.println('}');
    }

    private static void writeEventSetDescriptorAssignment( IndentedWriter iw, EventSetDescriptor esd, int index ) throws IOException
    {
	String ctor = "new EventSetDescriptor( BEAN_CLASS, " + quote( esd.getName() ) + ", " +
	    classLiteral( esd.getListenerType() ) + ", " +
	    stringArrayLiteral( methodNames( esd.getListenerMethods() ) ) + ", " +
	    methodNameArgument( esd.getAddListenerMethod() ) + ", " +
	    methodNameArgument( esd.getRemoveListenerMethod() ) + ", " +
	    methodNameArgument( esd.getGetListenerMethod() ) + " )";

	boolean unicast      = esd.isUnicast();
	boolean notInDefault = ! esd.isInDefaultEventSet();

	if ( unicast || notInDefault )
	    {
		String var = "esd" + index;
		iw.println("EventSetDescriptor " + var + " = " + ctor + ';');
		if ( unicast )
		    iw.println( var + ".setUnicast( true );" );
		if ( notInDefault )
		    iw.println( var + ".setInDefaultEventSet( false );" );
		iw.println("esds[" + index + "] = " + var + ';');
	    }
	else
	    iw.println("esds[" + index + "] = " + ctor + ';');
    }

    private static void writeGetMethodDescriptors( IndentedWriter iw, MethodDescriptor[] mds ) throws IOException
    {
	iw.println("public MethodDescriptor[] getMethodDescriptors()");
	iw.println('{');
	iw.upIndent();

	if ( mds.length == 0 )
	    iw.println("return new MethodDescriptor[0];");
	else
	    {
		iw.println("try");
		iw.println('{');
		iw.upIndent();

		iw.println("MethodDescriptor[] mds = new MethodDescriptor[ " + mds.length + " ];");
		for ( int i = 0, len = mds.length; i < len; ++i )
		    {
			Method m = mds[i].getMethod();
			iw.println("mds[" + i + "] = new MethodDescriptor( BEAN_CLASS.getMethod( " +
				   quote( m.getName() ) + ", " + classArrayLiteral( m.getParameterTypes() ) + " ) );");
		    }
		iw.println("return mds;");

		iw.downIndent();
		iw.println('}');
		iw.println("catch ( NoSuchMethodException e )");
		iw.println("{ throw new RuntimeException( \"Could not reflectively find a method while constructing MethodDescriptors for explicit BeanInfo of \" + BEAN_CLASS.getName() + \".\", e ); }");
	    }

	iw.downIndent();
	iw.println('}');
    }

    private static void writeImports( IndentedWriter iw, boolean hasIndexed ) throws IOException
    {
	iw.println("import java.beans.BeanDescriptor;");
	iw.println("import java.beans.EventSetDescriptor;");
	if ( hasIndexed )
	    iw.println("import java.beans.IndexedPropertyDescriptor;");
	iw.println("import java.beans.IntrospectionException;");
	iw.println("import java.beans.MethodDescriptor;");
	iw.println("import java.beans.PropertyDescriptor;");
	iw.println("import java.beans.SimpleBeanInfo;");
    }

    private static void writeClassJavaDocComment( IndentedWriter iw, Class beanClass ) throws IOException
    {
	iw.println("/**");
	iw.println(" * Explicit BeanInfo for " + beanClass.getName() + ", generated by " + GENERATOR_NAME + '.');
	iw.println(" */");
    }

    private static void writeBannerComments( IndentedWriter iw ) throws IOException
    {
	// support deterministic builds, see https://reproducible-builds.org/docs/source-date-epoch/
	String sde = System.getenv("SOURCE_DATE_EPOCH");
	Date timestamp;
	if ( sde == null )
	    timestamp = new Date();
	else
	    timestamp = new Date( Long.parseLong( sde ) * 1000 );

	iw.println("/*");
	iw.println(" * This class autogenerated by " + GENERATOR_NAME + '.');
	iw.println(" * " + timestamp);
	iw.println(" * DO NOT HAND EDIT!");
	iw.println(" */");
    }

    private static String methodNameArgument( Method m )
    { return m == null ? "null" : quote( m.getName() ); }

    private static String[] methodNames( Method[] methods )
    {
	if ( methods == null )
	    return new String[0];
	String[] out = new String[ methods.length ];
	for ( int i = 0, len = methods.length; i < len; ++i )
	    out[i] = methods[i].getName();
	return out;
    }

    private static String classArrayLiteral( Class[] classes )
    {
	if ( classes.length == 0 )
	    return "new Class[0]";
	StringBuffer sb = new StringBuffer(128);
	sb.append("new Class[] { ");
	for ( int i = 0, len = classes.length; i < len; ++i )
	    {
		if ( i != 0 )
		    sb.append(", ");
		sb.append( classLiteral( classes[i] ) );
	    }
	sb.append(" }");
	return sb.toString();
    }

    private static String stringArrayLiteral( String[] strings )
    {
	if ( strings.length == 0 )
	    return "new String[0]";
	StringBuffer sb = new StringBuffer(128);
	sb.append("new String[] { ");
	for ( int i = 0, len = strings.length; i < len; ++i )
	    {
		if ( i != 0 )
		    sb.append(", ");
		sb.append( quote( strings[i] ) );
	    }
	sb.append(" }");
	return sb.toString();
    }

    private static String classLiteral( Class cl )
    { return cl.getCanonicalName() + ".class"; }

    private static String quote( String s )
    { return '"' + s + '"'; }

    private static String packageNameForClass( Class cl )
    {
	Package pkg = cl.getPackage();
	if ( pkg != null )
	    return pkg.getName();

	// fall back to parsing the class name, in case the package is not available from the loader
	String fqcn = cl.getName();
	int lastDot = fqcn.lastIndexOf('.');
	return lastDot < 0 ? null : fqcn.substring( 0, lastDot );
    }

    public static void main( String[] argv )
    {
	try
	    {
		if ( argv.length < 1 )
		    {
			System.err.println("Usage: java " + GENERATOR_NAME + " <bean-class-name> [excluded-property-name]...");
			return;
		    }

		Class beanClass = Class.forName( argv[0] );
		Set excludedProperties = new HashSet();
		for ( int i = 1; i < argv.length; ++i )
		    excludedProperties.add( argv[i] );

		System.out.println( explicitBeanInfoClassSourceForBeanClass( beanClass, excludedProperties ) );
	    }
	catch ( Exception e )
	    { e.printStackTrace(); }
    }

    private BeanInfoGen()
    {}
}
