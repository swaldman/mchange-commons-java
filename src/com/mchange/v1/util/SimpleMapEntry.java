package com.mchange.v1.util;

import java.util.Map;

public class SimpleMapEntry<K,V> extends AbstractMapEntry<K,V> implements Map.Entry<K,V>
{
    K key;
    V value;

    public SimpleMapEntry(K key, V value)
    {
	this.key = key;
	this.value = value;
    }

    @Override
    public K getKey()
    { return key; }

    @Override
    public V getValue()
    { return value; }

    @Override
    public V setValue(V value)
    {
	V old = this.value;
	this.value = value;
	return old;
    }
}
