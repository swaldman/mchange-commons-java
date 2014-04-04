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

