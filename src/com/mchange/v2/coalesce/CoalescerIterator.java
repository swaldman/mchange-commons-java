package com.mchange.v2.coalesce;

import java.util.*;

class CoalescerIterator implements Iterator
{
    Iterator inner;

    CoalescerIterator(Iterator inner)
    { this.inner = inner; }

    @Override
    public boolean hasNext()
    { return inner.hasNext(); }
    
    @Override
    public Object next()
    { return inner.next(); }
    
    @Override
    public void remove()
    { throw new UnsupportedOperationException("Objects cannot be removed from a coalescer!"); }
}
