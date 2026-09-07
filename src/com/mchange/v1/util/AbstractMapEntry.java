package com.mchange.v1.util;

import java.util.Map;
import com.mchange.v2.lang.ObjectUtils;

public abstract class AbstractMapEntry<K,V> implements Map.Entry<K,V>
{
    @Override
    public abstract K getKey();

    @Override
    public abstract V getValue();

    @Override
    public abstract V setValue(V value);

    @Override
    public boolean equals(Object o)
    {
	if (o instanceof Map.Entry)
	    {
		Map.Entry<?,?> other = (Map.Entry<?,?>) o;
		return
		    ObjectUtils.eqOrBothNull( this.getKey(), other.getKey() ) &&
		    ObjectUtils.eqOrBothNull( this.getValue(), other.getValue() );
	    }
	else 
	    return false;
    }

    @Override
    public int hashCode()
    {
	return 
	    (this.getKey()   == null ? 0 : this.getKey().hashCode()) ^
	    (this.getValue() == null ? 0 : this.getValue().hashCode());
    }
}
