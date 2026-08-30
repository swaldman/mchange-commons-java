package com.mchange.v2.coalesce;

import com.mchange.v1.identicator.*;

class CoalesceIdenticator implements Identicator
{
    CoalesceChecker cc;

    CoalesceIdenticator( CoalesceChecker cc )
    { this.cc = cc; }

    public boolean identical(Object a, Object b)
    { return cc.checkCoalesce( a , b ); }

    public int hash(Object o)
    { return cc.coalesceHash( o ); }
}
