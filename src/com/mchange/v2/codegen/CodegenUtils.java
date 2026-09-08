package com.mchange.v2.codegen;

import java.lang.reflect.*;
import java.io.File;
import java.io.Writer;
import java.util.Comparator;
import java.util.Set;
import com.mchange.v1.lang.ClassUtils;
import com.mchange.v2.io.IndentedWriter;

public final class CodegenUtils
{
    public static String getModifierString( int modifiers )
    {
	StringBuffer sb = new StringBuffer(32);
	if ( Modifier.isPublic( modifiers ) )
	    sb.append("public ");
	if ( Modifier.isProtected( modifiers ) )
	    sb.append("protected ");
	if ( Modifier.isPrivate( modifiers ) )
	    sb.append("private ");
	if ( Modifier.isAbstract( modifiers ) )
	    sb.append("abstract ");
	if ( Modifier.isStatic( modifiers ) )
	    sb.append("static ");
	if ( Modifier.isFinal( modifiers ) )
	    sb.append("final ");
	if ( Modifier.isSynchronized( modifiers ) )
	    sb.append("synchronized ");
	if ( Modifier.isTransient( modifiers ) )
	    sb.append("transient ");
	if ( Modifier.isVolatile( modifiers ) )
	    sb.append("volatile ");
	if ( Modifier.isStrict( modifiers ) )
	    sb.append("strictfp ");
	if ( Modifier.isNative( modifiers ) )
	    sb.append("native ");
	if ( Modifier.isInterface( modifiers ) ) //????
	    sb.append("interface ");
	return sb.toString().trim();
    }

    public static Class<?> unarrayClass( Class<?> cl )
    {
	Class<?> out = cl;
	while ( out.isArray() )
	    out = out.getComponentType();
	return out;
    }

    public static boolean inSamePackage(String cn1, String cn2)
    {
       int pkgdot = cn1.lastIndexOf('.');
       int pkgdot2 = cn2.lastIndexOf('.');

       //always return true of one class is a primitive or unpackages
       if (pkgdot < 0 || pkgdot2 < 0)
           return true;
       if ( cn1.substring(0, pkgdot).equals(cn1.substring(0, pkgdot)) )
       {
          if (cn2.indexOf('.') >= 0)
            return false;
          else
            return true;
       }
       else
         return false;
    }

    /**
     * @return fully qualified class name last element
     */
    public static String fqcnLastElement(String fqcn)
    { return ClassUtils.fqcnLastElement( fqcn ); }

    public static String methodSignature( Method m )
    { return methodSignature( m, null ); }

    public static String methodSignature( Method m, String[] argNames )
    { return methodSignature( Modifier.PUBLIC, m, argNames ); }

    public static String methodSignature( int modifiers, Method m, String[] argNames )
    {
	StringBuffer sb = new StringBuffer(256);
        sb.append(getModifierString(modifiers));
	sb.append(' ');
	sb.append( typeParameterDeclaration( m ) );
	sb.append( typeString( m.getGenericReturnType() ) );
	sb.append(' ');
	sb.append( m.getName() );
	sb.append('(');
        Type[] cls = m.getGenericParameterTypes();
        for(int i = 0, len = cls.length; i < len; ++i)
        {
           if (i != 0)
             sb.append(", ");
           sb.append( typeString( cls[i] ) );
	   sb.append(' ');
           sb.append( argNames == null ? String.valueOf((char) ('a' + i)) : argNames[i] );
        }
        sb.append(')');
	Type[] excClasses = m.getGenericExceptionTypes();
	if (excClasses.length > 0)
        {
           sb.append(" throws ");
           for (int i = 0, len = excClasses.length; i < len; ++i)
           {
             if (i != 0)
               sb.append(", ");
             sb.append( typeString( excClasses[i] ) );
           }   
        }
        return sb.toString();
    }

    /**
     *  Renders a reflected {@link Type} as Java source, using simple (unqualified) class
     *  names, so that generated code carries the generic signatures of what it implements
     *  rather than their erasures.
     *
     *  Type variables declared by the <i>method</i> are rendered by name, because a
     *  generated method can redeclare them. Type variables declared by a <i>class</i> or
     *  interface are rendered as their erasure, because generators here emit raw
     *  implementing classes, so a class-level variable would not be in scope in the
     *  generated source. That substitution reproduces exactly what this class emitted
     *  before it understood generics.
     */
    public static String typeString( Type t )
    {
	StringBuffer sb = new StringBuffer(64);
	appendTypeString( sb, t );
	return sb.toString();
    }

    private static void appendTypeString( StringBuffer sb, Type t )
    {
	if ( t instanceof Class )
	    sb.append( ClassUtils.simpleClassName( (Class<?>) t ) );
	else if ( t instanceof ParameterizedType )
	    {
		ParameterizedType pt = (ParameterizedType) t;
		appendTypeString( sb, pt.getRawType() );
		Type[] args = pt.getActualTypeArguments();
		if (args.length > 0)
		    {
			sb.append('<');
			for (int i = 0; i < args.length; ++i)
			    {
				if (i != 0) sb.append(", ");
				appendTypeString( sb, args[i] );
			    }
			sb.append('>');
		    }
	    }
	else if ( t instanceof GenericArrayType )
	    {
		appendTypeString( sb, ((GenericArrayType) t).getGenericComponentType() );
		sb.append("[]");
	    }
	else if ( t instanceof WildcardType )
	    {
		WildcardType wt = (WildcardType) t;
		Type[] lower = wt.getLowerBounds();
		Type[] upper = wt.getUpperBounds();
		if (lower.length > 0)
		    {
			sb.append("? super ");
			appendTypeString( sb, lower[0] );
		    }
		else if (upper.length > 0 && upper[0] != Object.class)
		    {
			sb.append("? extends ");
			appendTypeString( sb, upper[0] );
		    }
		else
		    sb.append('?');
	    }
	else if ( t instanceof TypeVariable )
	    {
		TypeVariable<?> tv = (TypeVariable<?>) t;
		if ( tv.getGenericDeclaration() instanceof Class )
		    sb.append( ClassUtils.simpleClassName( erasure( tv ) ) ); // not in scope in a raw generated class
		else
		    sb.append( tv.getName() );
	    }
	else // no other Type kinds exist, but do not silently emit nothing
	    throw new IllegalArgumentException("Cannot render an unexpected java.lang.reflect.Type as source: " + t);
    }

    /**
     *  @return the method's type parameters as a source declaration with a trailing space
     *          (for example <code>"&lt;T&gt; "</code>), or the empty String if it declares none.
     */
    public static String typeParameterDeclaration( Method m )
    {
	TypeVariable<Method>[] tvs = m.getTypeParameters();
	if (tvs.length == 0)
	    return "";

	StringBuffer sb = new StringBuffer(32);
	sb.append('<');
	for (int i = 0; i < tvs.length; ++i)
	    {
		if (i != 0) sb.append(", ");
		sb.append( tvs[i].getName() );
		Type[] bounds = tvs[i].getBounds();
		if (bounds.length > 0 && bounds[0] != Object.class)
		    {
			sb.append(" extends ");
			for (int j = 0; j < bounds.length; ++j)
			    {
				if (j != 0) sb.append(" & ");
				appendTypeString( sb, bounds[j] );
			    }
		    }
	    }
	sb.append("> ");
	return sb.toString();
    }

    /**
     *  @return whether rendering this Type as source would mention a method-level type
     *          variable, so that a cast to it would be an unchecked cast.
     */
    public static boolean mentionsMethodTypeVariable( Type t )
    {
	if ( t instanceof TypeVariable )
	    return ! (((TypeVariable<?>) t).getGenericDeclaration() instanceof Class);
	else if ( t instanceof ParameterizedType )
	    {
		for ( Type arg : ((ParameterizedType) t).getActualTypeArguments() )
		    if ( mentionsMethodTypeVariable( arg ) ) return true;
		return false;
	    }
	else if ( t instanceof GenericArrayType )
	    return mentionsMethodTypeVariable( ((GenericArrayType) t).getGenericComponentType() );
	else if ( t instanceof WildcardType )
	    {
		WildcardType wt = (WildcardType) t;
		for ( Type b : wt.getLowerBounds() )
		    if ( mentionsMethodTypeVariable( b ) ) return true;
		for ( Type b : wt.getUpperBounds() )
		    if ( mentionsMethodTypeVariable( b ) ) return true;
		return false;
	    }
	else
	    return false;
    }

    /**
     *  Adds to <code>accum</code> every Class a source rendering of <code>t</code> would
     *  name, so that callers can build an import set covering generic signatures. Array
     *  types contribute their component type; primitives, <code>void</code>, and type
     *  variables contribute nothing nameable.
     */
    public static void collectNamedClasses( Type t, Set<Class<?>> accum )
    {
	if ( t instanceof Class )
	    {
		Class<?> cl = unarrayClass( (Class<?>) t );
		if (! cl.isPrimitive() )
		    accum.add( cl );
	    }
	else if ( t instanceof ParameterizedType )
	    {
		ParameterizedType pt = (ParameterizedType) t;
		collectNamedClasses( pt.getRawType(), accum );
		for ( Type arg : pt.getActualTypeArguments() )
		    collectNamedClasses( arg, accum );
	    }
	else if ( t instanceof GenericArrayType )
	    collectNamedClasses( ((GenericArrayType) t).getGenericComponentType(), accum );
	else if ( t instanceof WildcardType )
	    {
		WildcardType wt = (WildcardType) t;
		for ( Type b : wt.getLowerBounds() )
		    collectNamedClasses( b, accum );
		for ( Type b : wt.getUpperBounds() )
		    if ( b != Object.class )
			collectNamedClasses( b, accum );
	    }
	else if ( t instanceof TypeVariable )
	    {
		// A class-level variable is rendered as its erasure, so that must be importable.
		// A method-level variable is rendered by name, and names nothing.
		TypeVariable<?> tv = (TypeVariable<?>) t;
		if ( tv.getGenericDeclaration() instanceof Class )
		    accum.add( erasure( tv ) );
	    }
    }

    private static Class<?> erasure( TypeVariable<?> tv )
    {
	Type[] bounds = tv.getBounds();
	Type   bound  = (bounds.length > 0 ? bounds[0] : Object.class);
	while ( bound instanceof ParameterizedType )
	    bound = ((ParameterizedType) bound).getRawType();
	if ( bound instanceof Class )
	    return unarrayClass( (Class<?>) bound );
	else if ( bound instanceof TypeVariable )
	    return erasure( (TypeVariable<?>) bound );
	else
	    return Object.class;
    }

    public static String methodCall( Method m )
    { return methodCall( m, null ); }

    public static String methodCall( Method m, String[] argNames )
    {
       StringBuffer sb = new StringBuffer(256);
       sb.append( m.getName() );
       sb.append('(');
        Class<?>[] cls = m.getParameterTypes();
        for(int i = 0, len = cls.length; i < len; ++i)
        {
           if (i != 0)
             sb.append(", ");
           sb.append( argNames == null ? generatedArgumentName( i ) : argNames[i] );
        }
        sb.append(')');
	return sb.toString();
    } 

    public static String reflectiveMethodObjectArray( Method m )
    { return reflectiveMethodObjectArray( m, null ); }

    public static String reflectiveMethodObjectArray( Method m, String[] argNames )
    {
       StringBuffer sb = new StringBuffer(256);
       sb.append( "new Object[] " );
       sb.append('{');
        Class<?>[] cls = m.getParameterTypes();
        for(int i = 0, len = cls.length; i < len; ++i)
        {
           if (i != 0)
             sb.append(", ");
           sb.append( argNames == null ? generatedArgumentName( i ) : argNames[i] );
        }
        sb.append('}');
	return sb.toString();
    }

    public static String reflectiveMethodParameterTypeArray( Method m )
    {
       StringBuffer sb = new StringBuffer(256);
       sb.append( "new Class<?>[] " );
       sb.append('{');
        Class<?>[] cls = m.getParameterTypes();
        for(int i = 0, len = cls.length; i < len; ++i)
        {
           if (i != 0)
             sb.append(", ");
           sb.append( ClassUtils.simpleClassName( cls[i] ) );
	   sb.append(".class");
        }
        sb.append('}');
	return sb.toString();
    }


    public static String generatedArgumentName( int index )
    { return String.valueOf((char) ('a' + index)); }

    public static String simpleClassName( Class<?> cl )
    { return ClassUtils.simpleClassName( cl ); }

    public static IndentedWriter toIndentedWriter( Writer w )
    { return (w instanceof IndentedWriter ? (IndentedWriter) w : new IndentedWriter(w)); }

    public static String packageNameToFileSystemDirPath(String packageName)
    {
	StringBuffer sb = new StringBuffer( packageName );
	for (int i = 0, len = sb.length(); i < len; ++i)
	    if ( sb.charAt(i) == '.' )
		sb.setCharAt(i, File.separatorChar);
	sb.append( File.separatorChar );
	return sb.toString();
    }

    public static String methodToTotalSortingKey(Method m)
    {
        StringBuilder sb = new StringBuilder( m.getName() );
        sb.append('(');
        Class<?>[] params = m.getParameterTypes();
        for (int i = 0; i < params.length; ++i)
        {
            if (i !=0 )
                sb.append(',');
            sb.append( params[i].getName() );
        }
        sb.append(')');
        sb.append( m.getReturnType().getName() ); // keep ordering total under covariant returns
        //System.out.println(sb.toString());
        return sb.toString();
    }

    public final static Comparator<Method> METHOD_COMPARATOR = new Comparator<Method>()
    {
        @Override
        public int compare(Method a, Method b)
        { return key(a).compareTo(key(b)); }

        private String key(Method m)
        { return methodToTotalSortingKey(m); }
    };

    private CodegenUtils()
    {}
}
