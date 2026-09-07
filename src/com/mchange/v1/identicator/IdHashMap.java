package com.mchange.v1.identicator;

import java.util.*;

public final class IdHashMap extends IdMap implements Map
{
    public IdHashMap(Identicator id)
    { super ( new HashMap(), id ); }

    @Override
    protected IdHashKey createIdKey(Object o)
    { return new StrongIdHashKey( o, id ); }
}
