package com.mchange.v1.cachedstore;

import java.util.*;
import java.lang.reflect.*;

public final class SoftSetFactory
{
    public static Set<Object> createSynchronousCleanupSoftSet()
    {
	final ManualCleanupSoftSet inner = new ManualCleanupSoftSet(); 
	InvocationHandler handler = new InvocationHandler()
	    {
		@Override
		public Object invoke(Object proxy, Method m, Object[] args) 
		    throws Throwable
		{
		    inner.vacuum();
		    return m.invoke( inner, args ); 
		}
	    };
	@SuppressWarnings("unchecked") // the proxy implements Set, and the set it fronts holds Objects
	Set<Object> out = (Set<Object>) Proxy.newProxyInstance( SoftSetFactory.class.getClassLoader(),
					     new Class<?>[] { Set.class },
					     handler );
	return out;
    }

    private SoftSetFactory()
    {}
}
