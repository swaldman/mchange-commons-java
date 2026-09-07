package com.mchange.v2.coalesce;

import java.util.Iterator;

class SyncedCoalescer implements Coalescer
{
    Coalescer inner;
    
    public SyncedCoalescer( Coalescer inner )
    { this.inner = inner; }
    
    @Override
    public synchronized Object coalesce( Object o )
    { return inner.coalesce( o ); }
    
    @Override
    public synchronized int countCoalesced()
    { return inner.countCoalesced(); }

    @Override
    public synchronized Iterator iterator()
    { return inner.iterator(); }
}
