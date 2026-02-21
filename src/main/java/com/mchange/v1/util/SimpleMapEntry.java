package com.mchange.v1.util;

import java.util.Map;

public class SimpleMapEntry extends AbstractMapEntry implements Map.Entry
{
    Object key;
    Object value;

    public SimpleMapEntry(Object key, Object value)
    {
	this.key = key;
	this.value = value;
    }

    public Object getKey()
    { return key; }

    public Object getValue()
    { return value; }

    public Object setValue(Object value)
    {
	Object old = value;
	this.value = value;
	return old;
    }
}
