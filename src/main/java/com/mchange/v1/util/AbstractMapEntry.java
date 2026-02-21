package com.mchange.v1.util;

import java.util.Map;
import com.mchange.v2.lang.ObjectUtils;

public abstract class AbstractMapEntry implements Map.Entry
{
    public abstract Object getKey();

    public abstract Object getValue();

    public abstract Object setValue(Object value);

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

    public int hashCode()
    {
	return 
	    (this.getKey()   == null ? 0 : this.getKey().hashCode()) ^
	    (this.getValue() == null ? 0 : this.getValue().hashCode());
    }
}
