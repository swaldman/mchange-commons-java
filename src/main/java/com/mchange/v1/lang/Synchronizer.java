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

package com.mchange.v1.lang;

import java.lang.reflect.*;
import java.util.Set;
import java.util.HashSet;

//Java 1.3 ONLY!!!
public final class Synchronizer
{
    /**
     * Creates an object that implements all the same
     * interfaces as the original object, but that
     * synchronizes all access (using the wrappers' own lock).
     */
    public static Object createSynchronizedWrapper(final Object o)
    {
	InvocationHandler handler = new InvocationHandler()
	    {
		public Object invoke(Object proxy, Method m, Object[] args) 
		    throws Throwable
		{
		    synchronized (proxy)
			{ return m.invoke( o, args ); }
		}
	    };
	Class cl = o.getClass();
	return Proxy.newProxyInstance( cl.getClassLoader(), 
				       recurseFindInterfaces(cl),
				       handler );
    }

    private static Class[] recurseFindInterfaces(Class cl)
    {
	Set s = new HashSet();
	while( cl != null )
	    {
		Class[] interfaces = cl.getInterfaces();
		for (int i = 0, len = interfaces.length; i < len; ++i)
		    s.add(interfaces[i]);
		cl = cl.getSuperclass();
	    }
	Class[] out = new Class[ s.size() ];
	s.toArray( out );
	return out;
    }

    private Synchronizer()
    {}
}
