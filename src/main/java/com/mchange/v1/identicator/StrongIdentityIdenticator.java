package com.mchange.v1.identicator;

public class StrongIdentityIdenticator implements Identicator
{
    public boolean identical(Object a, Object b)
    { return a == b; }

    public int hash(Object o)
    { return System.identityHashCode( o ); }
}
