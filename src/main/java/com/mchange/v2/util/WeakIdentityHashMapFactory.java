package com.mchange.v2.util;

import java.util.Map;
import com.mchange.v1.identicator.*;

public final class WeakIdentityHashMapFactory
{
    public static Map create()
    {
	Identicator id = new StrongIdentityIdenticator();
	return new IdWeakHashMap( id );
    }
}
