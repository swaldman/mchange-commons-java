/*
 * Distributed as part of mchange-commons-java v.0.2.3
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

import com.mchange.lang.PotentiallySecondary;
import com.mchange.v1.lang.Synchronizer;

public final class CachedStoreUtils
{
    final static boolean DEBUG = true;

    public static CachedStore synchronizedCachedStore(CachedStore orig)
    { return (CachedStore) Synchronizer.createSynchronizedWrapper( orig );  }

    public static TweakableCachedStore synchronizedTweakableCachedStore(TweakableCachedStore orig)
    { return (TweakableCachedStore) Synchronizer.createSynchronizedWrapper( orig );  }

    public static WritableCachedStore synchronizedWritableCachedStore(WritableCachedStore orig)
    { return (WritableCachedStore) Synchronizer.createSynchronizedWrapper( orig );  }

    public static CachedStore untweakableCachedStore(final TweakableCachedStore orig)
    {
	return new CachedStore()
	    {
		public Object find(Object key) throws CachedStoreException
		{ return orig.find( key ); }

		public void reset() throws CachedStoreException
		{ orig.reset(); }
	    };
    }

    static CachedStoreException toCachedStoreException( Throwable t )
    {
	if (DEBUG) t.printStackTrace();

	if (t instanceof CachedStoreException)
	    return (CachedStoreException) t;
	else if (t instanceof PotentiallySecondary)
	    {
		Throwable t2 = ((PotentiallySecondary) t).getNestedThrowable();
		if (t2 instanceof CachedStoreException)
		    return (CachedStoreException) t2;
	    }
	return new CachedStoreException( t );
	    
    }

    static CacheFlushException toCacheFlushException( Throwable t )
    {
	if (DEBUG) t.printStackTrace();

	if (t instanceof CacheFlushException)
	    return (CacheFlushException) t;
	else 
	    return new CacheFlushException( t );
	    
    }

    private CachedStoreUtils()
    {}
}
