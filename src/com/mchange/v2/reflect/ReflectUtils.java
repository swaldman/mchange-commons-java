package com.mchange.v2.reflect;

import java.lang.reflect.*;
import java.util.*;

public final class ReflectUtils
{
    public final static Class<?>[] PROXY_CTOR_ARGS = new Class<?>[]{ InvocationHandler.class };

    public static Constructor<?> findProxyConstructor(ClassLoader proxyClassLoader, Class<?> intfc)
	throws NoSuchMethodException
    { return findProxyConstructor( proxyClassLoader, new Class<?>[] { intfc } ); }

    //
    // Proxy.getProxyClass is deprecated as of jdk 9, which directs callers to
    // Proxy.newProxyInstance instead. That is not a replacement here.
    //
    // newProxyInstance returns one proxy INSTANCE. This returns the Constructor, so
    // a caller can resolve the proxy class once and then instantiate it repeatedly,
    // each time with a different InvocationHandler. That is the entire purpose of
    // the method, and it is public API with no callers inside this library, so it
    // exists for downstream code that depends on exactly this signature.
    //
    // The deprecation is aimed at named modules, where a generated proxy class is
    // encapsulated and Constructor.newInstance on it throws IllegalAccessException.
    // Proxies generated for a ClassLoader land in an unnamed module, where the
    // constructor remains accessible, which is how this library is used.
    //
    @SuppressWarnings("deprecation")
    public static Constructor<?> findProxyConstructor(ClassLoader proxyClassLoader, Class<?>[] interfaces)
	throws NoSuchMethodException
    {
	Class<?> proxyCl = Proxy.getProxyClass(proxyClassLoader, interfaces);
	return proxyCl.getConstructor( PROXY_CTOR_ARGS ); 
    }

    public static boolean isPublic( Member m )
    { return ((m.getModifiers() & Modifier.PUBLIC) != 0); }

    public static boolean isPublic( Class<?> cl )
    { return ((cl.getModifiers() & Modifier.PUBLIC) != 0); }

    public static Class<?> findPublicParent( Class<?> cl  )
    {
	do cl = cl.getSuperclass();
	while (cl != null && ! isPublic(cl) );
	return cl;
    }

    public static Iterator<Class<?>> traverseInterfaces( Class<?> cl )
    {
	Set<Class<?>> set = new HashSet<Class<?>>();
	if (cl.isInterface()) set.add( cl );
	addParentInterfaces( set, cl );
	return set.iterator();
    }

    private static void addParentInterfaces(Set<Class<?>> set, Class<?> cl)
    {
	Class<?>[] intfcs = cl.getInterfaces();
	for (int i = 0, len = intfcs.length; i < len; ++i)
	    {
		set.add( intfcs[i] );
		addParentInterfaces( set, intfcs[i] );
	    }
    }

    /**
     * Finds a version of the Method m in a public class
     * or interface. Classes versions will be found before
     * interface versions, but no guarantees about which
     * interface if the method is declared in both.
     */
    public static Method findInPublicScope( Method m )
    {
	if (! isPublic(m))
	    return null;
	Class<?> origClass = m.getDeclaringClass();
	if (isPublic( origClass ))
	    return m;

	//climb for public parent class
	Class<?> cl = origClass;
	while ((cl = findPublicParent(cl)) != null)
	    {
		try
		    { return cl.getMethod( m.getName(), m.getParameterTypes() ); }
		catch (NoSuchMethodException e)
		    { /* IGNORE... we didn't find it (this'll be slow) */ }
	    }

	Iterator<Class<?>> ii = traverseInterfaces( origClass );
	while ( ii.hasNext() )
	    {
		cl = ii.next();
		if ( isPublic( cl ) )
		    {
			try
			    { return cl.getMethod( m.getName(), m.getParameterTypes() ); }
			catch (NoSuchMethodException e)
			    { /* IGNORE... we didn't find it (this'll be slow) */ }
		    }
	    }

	return null;
    }

    private ReflectUtils()
    {}
}
