/*
 * Distributed as part of mchange-commonslib v.0.2
 *
 * Copyright (C) 2010 Machinery For Change, Inc.
 *
 * Author: Steve Waldman <swaldman@mchange.com>
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License version 2.1, as 
 * published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; see the file LICENSE.  If not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307, USA.
 */


package com.mchange.v2.reflect;

import java.lang.reflect.*;
import java.util.*;

public final class ReflectUtils
{
    public final static Class[] PROXY_CTOR_ARGS = new Class[]{ InvocationHandler.class };

    public static Constructor findProxyConstructor(ClassLoader proxyClassLoader, Class intfc)
	throws NoSuchMethodException
    { return findProxyConstructor( proxyClassLoader, new Class[] { intfc } ); }

    public static Constructor findProxyConstructor(ClassLoader proxyClassLoader, Class[] interfaces)
	throws NoSuchMethodException
    {
	Class proxyCl = Proxy.getProxyClass(proxyClassLoader, interfaces);
	return proxyCl.getConstructor( PROXY_CTOR_ARGS ); 
    }

    public static boolean isPublic( Member m )
    { return ((m.getModifiers() & Modifier.PUBLIC) != 0); }

    public static boolean isPublic( Class cl )
    { return ((cl.getModifiers() & Modifier.PUBLIC) != 0); }

    public static Class findPublicParent( Class cl  )
    {
	do cl = cl.getSuperclass();
	while (cl != null && ! isPublic(cl) );
	return cl;
    }

    public static Iterator traverseInterfaces( Class cl )
    {
	Set set = new HashSet();
	if (cl.isInterface()) set.add( cl );
	addParentInterfaces( set, cl );
	return set.iterator();
    }

    private static void addParentInterfaces(Set set, Class cl)
    {
	Class[] intfcs = cl.getInterfaces();
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
	Class origClass = m.getDeclaringClass();
	if (isPublic( origClass ))
	    return m;

	//climb for public parent class
	Class cl = origClass;
	while ((cl = findPublicParent(cl)) != null)
	    {
		try
		    { return cl.getMethod( m.getName(), m.getParameterTypes() ); }
		catch (NoSuchMethodException e)
		    { /* IGNORE... we didn't find it (this'll be slow) */ }
	    }

	Iterator ii = traverseInterfaces( origClass );
	while ( ii.hasNext() )
	    {
		cl = (Class) ii.next();
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
