package com.mchange.v1.util;

import java.util.Map;
import com.mchange.v2.lang.ObjectUtils;

public abstract class AbstractMapEntry implements Map.Entry
{
    @Override
    public abstract Object getKey();

    @Override
    public abstract Object getValue();

    @Override
    public abstract Object setValue(Object value);

    @Override
    public boolean equals(Object o)
    {
	if (o instanceof Map.Entry)
	    {
		Map.Entry other = (Map.Entry) o;
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
