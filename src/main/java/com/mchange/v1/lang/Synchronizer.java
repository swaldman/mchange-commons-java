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
