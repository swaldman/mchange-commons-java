package com.mchange.v2.coalesce;

import java.util.*;

class CoalescerIterator implements Iterator
{
    Iterator inner;

    CoalescerIterator(Iterator inner)
    { this.inner = inner; }

    public boolean hasNext()
    { return inner.hasNext(); }
    
    public Object next()
    { return inner.next(); }
    
    public void remove()
    { throw new UnsupportedOperationException("Objects cannot be removed from a coalescer!"); }
}
