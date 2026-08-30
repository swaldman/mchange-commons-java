package com.mchange.v1.util;

/**
 * This is often bound to a scarce resource! Don't
 * forget to close it when you are done!!!
 */
public interface UnreliableIterator extends UIterator
{
    public boolean hasNext() throws UnreliableIteratorException;
    public Object  next()    throws UnreliableIteratorException;
    public void    remove()  throws UnreliableIteratorException;
    public void    close()   throws UnreliableIteratorException;
}
