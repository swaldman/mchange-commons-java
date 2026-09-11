package com.mchange.v2.reflect.junit;

/**
 *  A trivial, instantiable class for the by-name instantiation tests. Top-level rather
 *  than nested, so a test ClassLoader can define it from its own bytes without dragging
 *  the enclosing test class along.
 */
public class ByNameMarker
{
    public ByNameMarker()
    {}
}
