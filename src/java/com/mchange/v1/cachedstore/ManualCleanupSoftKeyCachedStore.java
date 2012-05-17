/*
 * Distributed as part of mchange-commons-java v.0.2.1
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


package com.mchange.v1.cachedstore;

import java.lang.ref.ReferenceQueue;

class ManualCleanupSoftKeyCachedStore extends KeyTransformingCachedStore implements Vacuumable
{
    ReferenceQueue queue = new ReferenceQueue();

    public ManualCleanupSoftKeyCachedStore(CachedStore.Manager manager)
    { super( manager ); }

    protected Object toUserKey( Object cachePutKey )
    { return ((SoftKey) cachePutKey).get(); }

    protected Object toCacheFetchKey( Object userKey )
    { return new SoftKey( userKey, null ); }

    protected Object toCachePutKey( Object userKey )
    { return new SoftKey( userKey, queue ); }

    public void vacuum() throws CachedStoreException
    { 
	SoftKey key;
	while ((key = (SoftKey) queue.poll()) != null)
	    {
		//System.err.println("Vacuuming Key ---> " + key);
		this.removeByTransformedKey( key );
	    }
    }
}


