/*
 * Distributed as part of mchange-commons-java v.0.2.4
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

import java.util.Set;

public interface WritableCachedStore extends CachedStore
{
    public void write(Object key, Object value) throws CachedStoreException;

    public void remove(Object key) throws CachedStoreException;

    /**
     * flushes writes to back end, IN NO PARTICULAR ORDER!
     * (If you write a key twice, the second write reliably supercedes
     * the first, however.)
     *
     * If this method fails, you can use getFailedWrites()
     * to see what keys could not be written. Values that failed
     * to write are still "pending": they will attempt to write
     * on the next flush, and will still be read from the cache. Use 
     * clearPendingWrites() to avoid seeing or writing these values
     * again.
     */
    public void flushWrites() throws CacheFlushException;

    /** 
     *	@return null if all attempts to write to backend storage
     *	have succeeded, an unmodifiable Set of keys otherwise.
     *  failedWrites includes keys for whom a call to write or
     *  remove initially succeeded, but an attempt to flush
     *  the write to storage failed. A failed write may be
     *  any of an attempted first write, overwrite, or remove.
     */
    public Set  getFailedWrites() throws CachedStoreException;

    /**
     * Clears any pending (unflushed or failed) writes and removes.
     */
    public void clearPendingWrites() throws CachedStoreException;

    /**
     * Clears (WITHOUT WRITING) any pending (unflushed or failed) 
     * writes  and removes, and any cached reads.
     */
    public void reset() throws CachedStoreException;

    /**
     * flushes writes and then clears cached reads. On
     * successful completion, the cache and the back-end
     * store will be in sync with one another.
     */
    public void sync() throws CachedStoreException;

    public interface Manager extends CachedStore.Manager
    {
	public void writeToStorage(Object key, Object value) throws Exception;
	public void removeFromStorage(Object key) throws Exception;
    }
}

