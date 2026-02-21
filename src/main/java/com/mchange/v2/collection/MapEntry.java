package com.mchange.v2.collection;

import java.util.*;
import com.mchange.v2.lang.ObjectUtils;

public class MapEntry implements Map.Entry
{
    Object key;
    Object value;

    public MapEntry( Object key, Object value )
    { 
	this.key = key;
	this.value = value;
    }

    public Object getKey()
    { return key; }

    public Object getValue()
    { return value;  }

    public Object setValue(Object o)
    { throw new UnsupportedOperationException(); }

    public boolean equals(Object o)
    {
	if (o instanceof Map.Entry)
	    {
		Map.Entry other = (Map.Entry) o;
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
    public int hashCode()
    {
	return 
	    ObjectUtils.hashOrZero( this.key ) ^
	    ObjectUtils.hashOrZero( this.value );
    }
}
