package com.mchange.v1.util;

/**
 * This is often bound to a scarce resource! Don't
 * forget to close it when you are done!!!
 */
public interface UnreliableIterator<T> extends UIterator<T>
{
    @Override
    public boolean hasNext() throws UnreliableIteratorException;
    @Override
    public T       next()    throws UnreliableIteratorException;
    @Override
    public void    remove()  throws UnreliableIteratorException;
    @Override
    public void    close()   throws UnreliableIteratorException;
}
