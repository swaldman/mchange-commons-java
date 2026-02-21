package com.mchange.v2.coalesce;

import java.util.Iterator;

public interface Coalescer
{
    public Object coalesce( Object o );
    public int countCoalesced();
    public Iterator iterator();
}
