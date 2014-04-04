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

package com.mchange.v1.identicator;

import java.lang.ref.*;

// revisit equals() if ever made non-final

final class WeakIdHashKey extends IdHashKey
{
    Ref keyRef;
    int hash;

    public WeakIdHashKey(Object keyObj, Identicator id, ReferenceQueue rq)
    {
	super( id );

	if (keyObj == null)
	    throw new UnsupportedOperationException("Collection does not accept nulls!");

	this.keyRef = new Ref( keyObj, rq );
	this.hash = id.hash( keyObj );
    }

    public Ref getInternalRef()
    { return this.keyRef; }

    public Object getKeyObj()
    { return keyRef.get(); }

    public boolean equals(Object o)
    {
	// fast type-exact match for final class
	if (o instanceof WeakIdHashKey)
	    {
		WeakIdHashKey other = (WeakIdHashKey) o;
		if (this.keyRef == other.keyRef)
		    return true;
		else
		    {
			Object myKeyObj = this.keyRef.get();
			Object oKeyObj  = other.keyRef.get();
			if (myKeyObj == null || oKeyObj == null)
			    return false;
			else
			    return id.identical( myKeyObj, oKeyObj );
		    }
	    }
	else
	    return false;
    }

    public int hashCode()
    { return hash; }

    class Ref extends WeakReference
    {
	public Ref( Object referant, ReferenceQueue rq )
	{ super( referant, rq ); }
	
	WeakIdHashKey getKey()
	{ return WeakIdHashKey.this; }
    }
}
