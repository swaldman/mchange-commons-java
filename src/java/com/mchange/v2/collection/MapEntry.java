/*
 * Distributed as part of mchange-commons-java v.0.2.3.2
 *
 * Copyright (C) 2012 Machinery For Change, Inc.
 *
 * Author: Steve Waldman <swaldman@mchange.com>
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License version 2.1, as 
 * published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; see the file LICENSE.  If not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307, USA.
 */

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
