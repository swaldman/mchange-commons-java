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

import java.lang.reflect.Array;
import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
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
 * Properties may be excluded from the generated property descriptors in two ways: by name (any
 * property whose name appears in <code>excludedPropertyNames</code>) and by type (any property whose
 * type is assignable to one of the <code>Class</code> objects in <code>excludedPropertyTypes</code>).
 *
 * For an indexed property, exclusion by type considers <em>both</em> the array type and the element
 * type, independent of which accessor convention defined the property (an array-valued accessor such
 * as <code>String[] getTags()</code>, a single-index accessor such as <code>String getTags(int)</code>,
 * or both). Whichever of the two types the property descriptor does not directly report is derived
 * from the other, and the property is excluded if either is assignable to an excluded type. Thus an
 * indexed property with element type <code>String</code> is excluded whether <code>String.class</code>
 * or <code>String[].class</code> is given, and identically regardless of how it was declared.
 *
 * Because the generated <code>getPropertyDescriptors()</code> returns a non-null array, the JavaBeans
 * <code>Introspector</code> uses it verbatim rather than rediscovering properties reflectively, so
 * the accessor methods of an excluded property are no longer seen as constituting a JavaBeans
 * property. The methods themselves remain ordinary public methods and are still reported among the
 * method descriptors.
 */
public final class BeanInfoGen
{
    private final static String GENERATOR_NAME = BeanInfoGen.class.getName();

    /**
     * Convenience overload equivalent to {@link #explicitBeanInfoClassSourceForBeanClass(Class, Set, Set, boolean, boolean)}
     * with {@code suppressDescriptorCaching} and {@code includeMLogging} both {@code false}. The generated
     * <code>BeanInfo</code> therefore caches its descriptors (see the multi-argument overload for the
     * trade-offs that implies) and silently omits any descriptor that proves invalid at runtime.
     */
    public static String explicitBeanInfoClassSourceForBeanClass( Class beanClass, Set excludedPropertyNames, Set excludedPropertyTypes )
	throws IntrospectionException, IOException
    { return explicitBeanInfoClassSourceForBeanClass( beanClass, excludedPropertyNames, excludedPropertyTypes, false, false ); }

    /**
     * Generates Java source for an explicit <code>BeanInfo</code> class describing <code>beanClass</code>.
     *
     * <h4>Descriptor caching ({@code suppressDescriptorCaching})</h4>
     *
     * <p>When {@code suppressDescriptorCaching} is {@code false} (the default), the generated
     * <code>BeanInfo</code> computes its <code>BeanDescriptor</code> and its property, event-set, and
     * method descriptor arrays <em>once</em>, into instance fields, and each accessor returns that cached
     * state (handing back a defensive <code>clone()</code> of the arrays). This matches how the class is
     * actually consumed: {@link java.beans.Introspector} maintains its own per-class cache of
     * <code>BeanInfo</code> instances and reuses a single instance for every introspection of the bean, so
     * regenerating and re-validating every descriptor on each accessor call would be almost pure waste.</p>
     *
     * <p>Caching has a cost, though, and it is not primarily about CPU. The cached descriptor <em>objects</em>
     * are shared across all callers, and {@code java.beans} descriptors are mutable (<code>setShortDescription</code>,
     * <code>setValue</code>, <code>setBound</code>, <code>setExpert</code>, etc.). Because the
     * <code>Introspector</code> caches the <code>BeanInfo</code> instance, a single caller that mutates a
     * returned descriptor poisons that shared object for <em>every</em> future caller, process-wide and
     * indefinitely. The defensive array <code>clone()</code> protects the <em>membership</em> of each
     * descriptor set (no caller can add or remove descriptors), but it does not protect the mutable state
     * <em>within</em> a descriptor.</p>
     *
     * <p>Set {@code suppressDescriptorCaching} to {@code true} to have every accessor rebuild its descriptors
     * fresh on each call. This is slower and produces more garbage, but it is the only configuration that
     * gives true per-caller isolation: a mutation by one caller cannot propagate to other callers or persist
     * into the future. (Java's bean APIs offer no read-only descriptor variant, and copy-on-read in the
     * cached case would defeat caching entirely, so this binary flag is the practical seam.) Prefer it for
     * security-sensitive deployments in which untrusted code may receive the <code>BeanInfo</code> and could
     * mutate its descriptors.</p>
     *
     * <h4>Resilience logging ({@code includeMLogging})</h4>
     *
     * <p>A generated <code>BeanInfo</code> may run on a JVM or library version where some method, property,
     * or event that existed when it was generated is no longer present; such descriptors are skipped rather
     * than allowed to abort the whole <code>BeanInfo</code>. When {@code includeMLogging} is {@code true},
     * each skip is logged at {@code WARNING} via {@code com.mchange.v2.log}; when {@code false}, skips are
     * silent.</p>
     *
     * @param suppressDescriptorCaching if {@code true}, rebuild descriptors fresh on every accessor call
     *        (isolated but slow) instead of caching and sharing them (fast but mutably shared)
     * @param includeMLogging if {@code true}, log a {@code WARNING} whenever a descriptor is skipped because
     *        it is not valid in the runtime environment
     */
    public static String explicitBeanInfoClassSourceForBeanClass( Class beanClass, Set excludedPropertyNames, Set excludedPropertyTypes, boolean suppressDescriptorCaching, boolean includeMLogging )
	throws IntrospectionException, IOException
    {
	if ( excludedPropertyNames == null )
	    excludedPropertyNames = Collections.EMPTY_SET;
	if ( excludedPropertyTypes == null )
	    excludedPropertyTypes = Collections.EMPTY_SET;

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
		if ( excludedPropertyNames.contains( pd.getName() ) )
		    continue;
		if ( isExcludedByType( pd, excludedPropertyTypes ) )
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
	writeImports( iw, hasIndexed, includeMLogging );
	iw.println();
	writeClassJavaDocComment( iw, beanClass );
	iw.println("public class " + beanInfoClassName + " extends SimpleBeanInfo");
	iw.println('{');
	iw.upIndent();

	iw.println("private final static Class BEAN_CLASS = " + beanClassLiteral + ".class;");
	iw.println();
        if ( includeMLogging )
            iw.println("private final static MLogger logger = MLog.getLogger( " + beanInfoClassName + ".class );");

        if ( !suppressDescriptorCaching )
        {
            iw.println();
            iw.println("private BeanDescriptor       _beanDescriptor      = _getBeanDescriptor();");
            iw.println("private PropertyDescriptor[] _propertyDescriptors = _getPropertyDescriptors();");
            iw.println("private EventSetDescriptor[] _eventSetDescriptors = _getEventSetDescriptors();");
            iw.println("private MethodDescriptor[]   _methodDescriptors   = _getMethodDescriptors();");
        }

        iw.println();
	iw.println("public BeanDescriptor getBeanDescriptor()");
	iw.print("{ ");
        if (suppressDescriptorCaching)
            iw.print("return _getBeanDescriptor();");
        else
            iw.print("return _beanDescriptor;");
	iw.println(" }");
        iw.println();
	iw.println("public PropertyDescriptor[] getPropertyDescriptors()");
	iw.print("{ ");
        if (suppressDescriptorCaching)
            iw.print("return _getPropertyDescriptors();");
        else
            iw.print("return (PropertyDescriptor[]) _propertyDescriptors.clone();");
	iw.println(" }");
        iw.println();
	iw.println("public EventSetDescriptor[] getEventSetDescriptors()");
	iw.print("{ ");
        if (suppressDescriptorCaching)
            iw.print("return _getEventSetDescriptors();");
        else
            iw.print("return (EventSetDescriptor[]) _eventSetDescriptors.clone();");
	iw.println(" }");
        iw.println();
	iw.println("public MethodDescriptor[] getMethodDescriptors()");
	iw.print("{ ");
        if (suppressDescriptorCaching)
            iw.print("return _getMethodDescriptors();");
        else
            iw.print("return (MethodDescriptor[]) _methodDescriptors.clone();");
	iw.println(" }");
        iw.println();
	write_getBeanDescriptor( iw );
	iw.println();
	write_getPropertyDescriptors( iw, includedPds, includeMLogging );
	iw.println();
	write_getEventSetDescriptors( iw, esds, includeMLogging );
	iw.println();
	write_getMethodDescriptors( iw, mds, includeMLogging );

	iw.downIndent();
	iw.println('}');

	iw.flush();
	return sw.toString();
    }

    private static boolean isExcludedByType( PropertyDescriptor pd, Set excludedPropertyTypes )
    {
	if ( excludedPropertyTypes.isEmpty() )
	    return false;

	if ( pd instanceof IndexedPropertyDescriptor )
	    {
		// An indexed property can be defined by an array-valued accessor (e.g. String[] getTags()),
		// by a single-index accessor (e.g. String getTags(int)), or by both. Depending on the
		// convention used, the descriptor may report only the array type, only the element type, or
		// both. To exclude such properties consistently regardless of how they were declared, we
		// always consider both the element type and the array type, deriving whichever the descriptor
		// does not directly report, and exclude if either is assignable to an excluded type.
		IndexedPropertyDescriptor ipd = (IndexedPropertyDescriptor) pd;
		Class elementType = ipd.getIndexedPropertyType();
		Class arrayType   = ipd.getPropertyType();

		if ( elementType == null && arrayType != null )
		    elementType = arrayType.getComponentType();
		if ( arrayType == null && elementType != null )
		    arrayType = Array.newInstance( elementType, 0 ).getClass();

		return isAssignableToAnyOf( elementType, excludedPropertyTypes )
		    || isAssignableToAnyOf( arrayType, excludedPropertyTypes );
	    }
	else
	    return isAssignableToAnyOf( pd.getPropertyType(), excludedPropertyTypes );
    }

    private static boolean isAssignableToAnyOf( Class type, Set excludedPropertyTypes )
    {
	if ( type == null )
	    return false;
	for ( Iterator ii = excludedPropertyTypes.iterator(); ii.hasNext(); )
	    {
		Class excludedType = (Class) ii.next();
		if ( excludedType.isAssignableFrom( type ) )
		    return true;
	    }
	return false;
    }

    private static void write_getBeanDescriptor( IndentedWriter iw ) throws IOException
    {
	iw.println("private BeanDescriptor _getBeanDescriptor()");
	iw.println("{ return new BeanDescriptor( BEAN_CLASS ); }");
    }

    private static void write_getPropertyDescriptors( IndentedWriter iw, List pds, boolean includeMLogging ) throws IOException
    {
	iw.println("private PropertyDescriptor[] _getPropertyDescriptors()");
	iw.println('{');
	iw.upIndent();

	int n = pds.size();
	if ( n == 0 )
	    iw.println("return new PropertyDescriptor[0];");
	else
	    {
                iw.println("ArrayList<PropertyDescriptor> propertyDescriptors = new ArrayList<PropertyDescriptor>();");
		for ( int i = 0; i < n; ++i )
                {
                    iw.println("try");
                    iw.println('{');
                    iw.upIndent();

		    iw.println("propertyDescriptors.add( " + propertyDescriptorExpression( (PropertyDescriptor) pds.get(i) ) + " );");

                    iw.downIndent();
                    iw.println('}');
                    iw.println("catch ( IntrospectionException e )");
                    if (includeMLogging)
                    {
                        iw.println("{");
                        iw.upIndent();

                        iw.println("if (logger.isLoggable(MLevel.WARNING))");
                        iw.println("{");
                        iw.upIndent();
                        iw.println("logger.log(MLevel.WARNING, \"PropertyDescriptor for property '" + ((PropertyDescriptor) pds.get(i)).getName() + "' is not valid in the runtime VM. Omitting.\"/*, e*/);");
                        iw.downIndent();
                        iw.println("}");

                        iw.downIndent();
                        iw.println("}");
                    }
                    else
                        iw.println("{ /* PropertyDescriptor for property '" + ((PropertyDescriptor) pds.get(i)).getName() + "' is not valid in the runtime VM. Omitting. */ }");
                }
		iw.println("return propertyDescriptors.toArray(new PropertyDescriptor[propertyDescriptors.size()]);");
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

    private static void write_getEventSetDescriptors( IndentedWriter iw, EventSetDescriptor[] esds, boolean includeMLogging ) throws IOException
    {
	iw.println("private EventSetDescriptor[] _getEventSetDescriptors()");
	iw.println('{');
	iw.upIndent();

	if ( esds.length == 0 )
	    iw.println("return new EventSetDescriptor[0];");
	else
	    {
                iw.println("ArrayList<EventSetDescriptor> eventSetDescriptors = new ArrayList<EventSetDescriptor>();");
		for ( int i = 0, len = esds.length; i < len; ++i )
                {
                    iw.println("try");
                    iw.println('{');
                    iw.upIndent();

		    writeAddEventSetDescriptor( iw, esds[i], i );

                    iw.downIndent();
                    iw.println('}');
                    iw.println("catch ( IntrospectionException e )");
                    if (includeMLogging)
                    {
                        iw.println("{");
                        iw.upIndent();

                        iw.println("if (logger.isLoggable(MLevel.WARNING))");
                        iw.println("{");
                        iw.upIndent();
                        iw.println("logger.log(MLevel.WARNING, \"EventSetDescriptor '" + ((EventSetDescriptor) esds[i]) + "' is not valid under the runtime VM. Skipping.\"/*, e*/);");
                        iw.downIndent();
                        iw.println("}");

                        iw.downIndent();
                        iw.println("}");
                    }
                    else
                        iw.println("{ /* EventSetDescriptor '" + esds[i] + "' is not valid under the runtime JVM. Skipping. */ }");
                }
		iw.println("return eventSetDescriptors.toArray(new EventSetDescriptor[eventSetDescriptors.size()]);");
	    }

	iw.downIndent();
	iw.println('}');
    }

    private static void writeAddEventSetDescriptor( IndentedWriter iw, EventSetDescriptor esd, int index ) throws IOException
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
		iw.println("eventSetDescriptors.add( " + var + " );");
	    }
	else
	    iw.println("eventSetDescriptors.add( " + ctor + " );");
    }

    private static void write_getMethodDescriptors( IndentedWriter iw, MethodDescriptor[] mds, boolean includeMLogging ) throws IOException
    {
	iw.println("private MethodDescriptor[] _getMethodDescriptors()");
	iw.println('{');
	iw.upIndent();

	if ( mds.length == 0 )
	    iw.println("return new MethodDescriptor[0];");
	else
	    {
                iw.println("ArrayList<MethodDescriptor> methodDescriptors = new ArrayList<MethodDescriptor>();");
		for ( int i = 0, len = mds.length; i < len; ++i )
		    {
			Method m = mds[i].getMethod();
                        iw.println("try");
                        iw.println("{");
                        iw.upIndent();
			iw.println("methodDescriptors.add( new MethodDescriptor( BEAN_CLASS.getMethod( " + quote( m.getName() ) + ", " + classArrayLiteral( m.getParameterTypes() ) + " ) ) );");
                        iw.downIndent();
                        iw.println("}");
                        iw.println("catch ( NoSuchMethodException e )");
                        if (includeMLogging)
                        {
                            iw.println("{");
                            iw.upIndent();

                            iw.println("if (logger.isLoggable(MLevel.WARNING))");
                            iw.println("{");
                            iw.upIndent();
                            iw.println("logger.log(MLevel.WARNING, \"Method '" + m.getName() + "', which existed at the time this BeanInfo was defined, does not exist in the current runtime environment and has been skipped.\"/*, e*/);");
                            iw.downIndent();
                            iw.println("}");

                            iw.downIndent();
                            iw.println("}");
                        }
                        else
                            iw.println("{ /* Method '" + m.getName() + "', which existed at the time this BeanInfo was defined, does not exist in the current runtime environment and has been skipped. */ }");
                        }
		iw.println("return methodDescriptors.toArray(new MethodDescriptor[methodDescriptors.size()]);");
	    }

	iw.downIndent();
	iw.println('}');
    }

    private static void writeImports( IndentedWriter iw, boolean hasIndexed, boolean includeMLogging ) throws IOException
    {
	iw.println("import java.beans.BeanDescriptor;");
	iw.println("import java.beans.EventSetDescriptor;");
	if ( hasIndexed )
	    iw.println("import java.beans.IndexedPropertyDescriptor;");
	iw.println("import java.beans.IntrospectionException;");
	iw.println("import java.beans.MethodDescriptor;");
	iw.println("import java.beans.PropertyDescriptor;");
	iw.println("import java.beans.SimpleBeanInfo;");
        iw.println("import java.util.ArrayList;");
        if ( includeMLogging )
        {
            iw.println();
            iw.println("import com.mchange.v2.log.*;");
        }
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
		Set excludedPropertyNames = new HashSet();
		for ( int i = 1; i < argv.length; ++i )
		    excludedPropertyNames.add( argv[i] );

		System.out.println( explicitBeanInfoClassSourceForBeanClass( beanClass, excludedPropertyNames, Collections.EMPTY_SET ) );
	    }
	catch ( Exception e )
	    { e.printStackTrace(); }
    }

    private BeanInfoGen()
    {}
}
