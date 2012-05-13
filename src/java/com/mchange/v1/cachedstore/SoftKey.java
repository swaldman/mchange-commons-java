/*
 * Distributed as part of mchange-commonslib v.0.2
 *
 * Copyright (C) 2010 Machinery For Change, Inc.
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


package com.mchange.v1.cachedstore;

import java.lang.ref.*;

final class SoftKey extends SoftReference
{
    int hash_code;
    
    SoftKey(Object o, ReferenceQueue queue)
    {
	super( o , queue );
	this.hash_code = o.hashCode();
    }
    
    public int hashCode()
    { return hash_code; }
    
    /**
     *  we equals ourself, and any soft key whose ref equals ours.
     *  If we are cleared, we only equals ourself 
     */
    public boolean equals( Object o ) 
    {
	if (this == o) return true;
	else
	    {
		Object r1 = this.get();
		if (r1 == null)
		    return false;
		if (this.getClass() == o.getClass())
		    {
			SoftKey other = (SoftKey) o;
			return r1.equals( other.get() );
		    }
		else
		    return false;
	    }
    }
}
