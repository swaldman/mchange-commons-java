package com.mchange.v1.util;

/**
 * Incomplete parent of "Unreliable Iterator"
 * This is often bound to a scarce resource! Don't
 * forget to close it when you are done!!!
 *
 * This interface is not intended to be implemented
 * directly, but to be extended by subinterfaces
 * that narrow the exceptions reasonably.
 */
public interface UIterator extends ClosableResource
{
    public boolean hasNext() throws Exception;
    public Object  next()    throws Exception;
    public void    remove()  throws Exception;
    public void    close() throws Exception;
}

