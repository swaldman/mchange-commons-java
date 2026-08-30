package com.mchange.v2.ser;

public final class IndirectPolicy
{
    public final static IndirectPolicy DEFINITELY_INDIRECT   = new IndirectPolicy("DEFINITELY_INDIRECT");
    public final static IndirectPolicy INDIRECT_ON_EXCEPTION = new IndirectPolicy("INDIRECT_ON_EXCEPTION");
    public final static IndirectPolicy DEFINITELY_DIRECT     = new IndirectPolicy("DEFINITELY_DIRECT");

    String name;

    private IndirectPolicy(String name)
    { this.name = name; }
    
    public String toString()
    { return "[IndirectPolicy: " + name + ']'; }
}
