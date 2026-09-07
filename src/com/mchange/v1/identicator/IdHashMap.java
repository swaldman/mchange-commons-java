package com.mchange.v1.identicator;

import java.util.*;

public final class IdHashMap<K,V> extends IdMap<K,V> implements Map<K,V>
{
    public IdHashMap(Identicator id)
    { super ( new HashMap<IdHashKey,V>(), id ); }

    @Override
    protected IdHashKey createIdKey(Object o)
    { return new StrongIdHashKey( o, id ); }
}
