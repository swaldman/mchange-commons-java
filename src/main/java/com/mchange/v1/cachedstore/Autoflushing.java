package com.mchange.v1.cachedstore;

/**
 * A labelling interface used to mark
 * WritableCachedStores whose writes may
 * automatically flush even when flushWrites()
 * has not been called directly.
 */
public interface Autoflushing
{}
