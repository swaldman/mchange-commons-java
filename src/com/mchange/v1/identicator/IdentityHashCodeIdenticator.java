package com.mchange.v1.identicator;

/**
 * Two entities are considered equivalent if they share the same IdentityHashCode
 */
public class IdentityHashCodeIdenticator implements Identicator
{
    public static IdentityHashCodeIdenticator INSTANCE = new IdentityHashCodeIdenticator();

    public boolean identical(Object a, Object b)
    { return System.identityHashCode(a) == System.identityHashCode(b); }

    public int hash(Object o)
    { return System.identityHashCode( o ); }
}
