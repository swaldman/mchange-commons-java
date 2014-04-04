/*
 * Distributed as part of mchange-commons-java 0.2.7
 *
 * Copyright (C) 2014 Machinery For Change, Inc.
 *
 * Author: Steve Waldman <swaldman@mchange.com>
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of EITHER:
 *
 *     1) The GNU Lesser General Public License (LGPL), version 2.1, as 
 *        published by the Free Software Foundation
 *
 * OR
 *
 *     2) The Eclipse Public License (EPL), version 1.0
 *
 * You may choose which license to accept if you wish to redistribute
 * or modify this work. You may offer derivatives of this work
 * under the license you have chosen, or you may provide the same
 * choice of license which you have been offered here.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * You should have received copies of both LGPL v2.1 and EPL v1.0
 * along with this software; see the files LICENSE-EPL and LICENSE-LGPL.
 * If not, the text of these licenses are currently available at
 *
 * LGPL v2.1: http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 *  EPL v1.0: http://www.eclipse.org/org/documents/epl-v10.php 
 * 
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
