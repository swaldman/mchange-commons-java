package com.mchange.v1.util;

/**
 * An interface intended to be shared by the many sorts
 * of objects that offer some kind of close method to
 * cleanup resources they might be using. (I wish Sun
 * had defined, and used, such an interface in the standard
 * APIs.
 */
public interface ClosableResource
{
    /**
     * forces the release of any resources that might be
     * associated with this object.
     */
    public void close() throws Exception;
}
