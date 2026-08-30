package com.mchange.v1.lang;

import java.lang.reflect.*;
import java.util.Set;
import java.util.HashSet;

//Java 1.5+ ONLY!!!
public final class Synchronizer
{
    private final static Class[] EMPTY_CLASS_ARRAY = new Class[0];
    private final static Method OBJECT_EQUALS;
    private final static Method OBJECT_HASHCODE;
    private final static Method OBJECT_TO_STRING;

    static
    {
        try
        {
            OBJECT_EQUALS = Object.class.getMethod("equals", new Class[]{ Object.class });
            OBJECT_HASHCODE = Object.class.getMethod("hashCode", EMPTY_CLASS_ARRAY);
            OBJECT_TO_STRING = Object.class.getMethod("toString", EMPTY_CLASS_ARRAY);
        }
        catch (NoSuchMethodException e)
        { throw new Error("Failed to find basic Object methods?!?", e); }
    }

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
                        if (m.getDeclaringClass() != Object.class)
                        {
                            synchronized (proxy)
                            { return m.invoke( o, args ); }
                        }
                        else if (OBJECT_EQUALS.equals(m))
                            return Boolean.valueOf(proxy == args[0]);
                        else if (OBJECT_HASHCODE.equals(m))
                            return Integer.valueOf(System.identityHashCode(proxy));
                        else if (OBJECT_TO_STRING.equals(m))
                        {
                            synchronized (proxy)
                            { return o.toString(); }
                        }
                        else
                            throw new NoSuchMethodError("If you see this, it's a bug in " + Synchronizer.class.getName() + "; Unhandled, object method on proxy unexpectedly delivered to InvocationHandler: " + m);
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
