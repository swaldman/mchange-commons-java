package com.mchange.v1.cachedstore;

import java.util.*;
import java.lang.reflect.*;

public final class SoftSetFactory
{
    public static Set createSynchronousCleanupSoftSet()
    {
	final ManualCleanupSoftSet inner = new ManualCleanupSoftSet(); 
	InvocationHandler handler = new InvocationHandler()
	    {
		public Object invoke(Object proxy, Method m, Object[] args) 
		    throws Throwable
		{
		    inner.vacuum();
		    return m.invoke( inner, args ); 
		}
	    };
	return (Set) Proxy.newProxyInstance( SoftSetFactory.class.getClassLoader(),
					     new Class[] { Set.class },
					     handler );
    }

    private SoftSetFactory()
    {}
}
