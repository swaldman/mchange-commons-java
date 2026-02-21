package com.mchange.v2.coalesce;

import java.util.Iterator;

class SyncedCoalescer implements Coalescer
{
    Coalescer inner;
    
    public SyncedCoalescer( Coalescer inner )
    { this.inner = inner; }
    
    public synchronized Object coalesce( Object o )
    { return inner.coalesce( o ); }
    
    public synchronized int countCoalesced()
    { return inner.countCoalesced(); }

    public synchronized Iterator iterator()
    { return inner.iterator(); }
}
