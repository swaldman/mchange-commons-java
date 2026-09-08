package com.mchange.v2.collection;

import java.util.*;
import com.mchange.v2.lang.ObjectUtils;

public class MapEntry<K,V> implements Map.Entry<K,V>
{
    K key;
    V value;

    public MapEntry( K key, V value )
    { 
	this.key = key;
	this.value = value;
    }

    @Override
    public K getKey()
    { return key; }

    @Override
    public V getValue()
    { return value;  }

    @Override
    public V setValue(V o)
    { throw new UnsupportedOperationException(); }

    @Override
    public boolean equals(Object o)
    {
	if (o instanceof Map.Entry)
	    {
		Map.Entry<?,?> other = (Map.Entry<?,?>) o;
		return 
		    ObjectUtils.eqOrBothNull( this.key   , other.getKey() ) &&
		    ObjectUtils.eqOrBothNull( this.value , other.getValue() );
		
	    }
	else
	    return false;
    }

    /*
     * Conforms to required contract for Map.Entry hashCode()
     */
    @Override
    public int hashCode()
    {
	return 
	    ObjectUtils.hashOrZero( this.key ) ^
	    ObjectUtils.hashOrZero( this.value );
    }
}
