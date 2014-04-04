/*
 * Distributed as part of mchange-commons-java 0.2.7
 *
 * Copyright (C) 2014 Machinery For Change, Inc.
 *
 * Author: Steve Waldman <swaldman@mchange.com>
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of EITHER:
 *
 *     1) The GNU Lesser General Public License (LGPL), version 2.1, as 
 *        published by the Free Software Foundation
 *
 * OR
 *
 *     2) The Eclipse Public License (EPL), version 1.0
 *
 * You may choose which license to accept if you wish to redistribute
 * or modify this work. You may offer derivatives of this work
 * under the license you have chosen, or you may provide the same
 * choice of license which you have been offered here.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * You should have received copies of both LGPL v2.1 and EPL v1.0
 * along with this software; see the files LICENSE-EPL and LICENSE-LGPL.
 * If not, the text of these licenses are currently available at
 *
 * LGPL v2.1: http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 *  EPL v1.0: http://www.eclipse.org/org/documents/epl-v10.php 
 * 
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
