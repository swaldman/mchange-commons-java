package com.mchange.v1.lang;

import java.lang.reflect.*;
import java.util.Set;
import java.util.HashSet;

//Java 1.3+ ONLY!!!
public final class Synchronizer
{
    /**
     * Creates an object that implements all the same
     * public interfaces as the original object, but that
     * synchronizes all access (using the wrappers' own lock).
     */
    public static Object createSynchronizedWrapper(final Object o)
    {
	InvocationHandler handler = new InvocationHandler()
	    {
		public Object invoke(Object proxy, Method m, Object[] args) 
		    throws Throwable
		{
                    try
                    {
		        synchronized (proxy)
			{ return m.invoke( o, args ); }
                    }
                    catch (InvocationTargetException e)
                    {
                        // more cautious might be
                        //Throwable t = e.getTargetException();
                        //throw (t == null ? e : t);

                        // but t should never be null, so just...
                        throw e.getTargetException();
                    }
		}
	    };
	Class cl = o.getClass();
	return Proxy.newProxyInstance( cl.getClassLoader(), 
				       recurseFindInterfaces(cl),
				       handler );
    }

    // we could happily generate proxies of nonpublic interfaces, but calls to
    // them would fail unless the interfaces happened to be in this class' package.
    // we could try to make those calls succeed by calling m.setAccessible(true) before
    // invoking the methods, but we don't want to try to create circumventions of
    // java's ordinary accessibility rules here.
    private static Class[] recurseFindInterfaces(final Class cl)
    {
        Class scl = cl;
	Set s = new HashSet();
	while( scl != null )
	    {
		Class[] interfaces = scl.getInterfaces();
		for (int i = 0, len = interfaces.length; i < len; ++i)
                {
                    Class intfc = interfaces[i];
                    if ((intfc.getModifiers() & Modifier.PUBLIC) != 0)
                        s.add(intfc);
                }
		scl = scl.getSuperclass();
	    }
        if (s.size() == 0)
            throw new IllegalArgumentException("Cannot create a synchronizing proxy, " + cl.getName() + " implements no public interfaces.");
	Class[] out = new Class[ s.size() ];
	s.toArray( out );
	return out;
    }

    private Synchronizer()
    {}
}
