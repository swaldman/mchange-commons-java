package com.mchange.v2.util;

import java.util.Map;
import com.mchange.v1.identicator.*;

public final class WeakIdentityHashMapFactory
{
    public static <K,V> Map<K,V> create()
    {
	Identicator id = new StrongIdentityIdenticator();
	return new IdWeakHashMap<K,V>( id );
    }
}
