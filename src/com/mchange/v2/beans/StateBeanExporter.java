package com.mchange.v2.beans;

/**
 * offers a bean suitable for bean getter/setter-based serialization/deserialization
 * a la XMLSerializer. Should have a constructor that accepts the exported Object and
 * constructs a new bean with the same state.
 */
public interface StateBeanExporter
{
    public StateBean exportStateBean();
}
