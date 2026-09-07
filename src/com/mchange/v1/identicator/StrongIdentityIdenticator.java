package com.mchange.v1.identicator;

public class StrongIdentityIdenticator implements Identicator
{
    @Override
    public boolean identical(Object a, Object b)
    { return a == b; }

    @Override
    public int hash(Object o)
    { return System.identityHashCode( o ); }
}
