package com.mchange.v1.identicator;

abstract class IdHashKey
{
    Identicator id;
    
    public IdHashKey( Identicator id )
    { this.id = id; }

    public abstract Object getKeyObj();

    public Identicator getIdenticator()
    { return id; }

    public abstract boolean equals(Object o);

    public abstract int hashCode();
}
