package com.mchange.v2.ser;

public interface Indirector
{
    public IndirectlySerialized indirectForm( Object orig ) throws Exception;
}
