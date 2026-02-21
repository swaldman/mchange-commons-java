package com.mchange.v1.util;

import java.util.*;

public abstract class NumberedObjectCache
{
    ArrayList al = new ArrayList();
    
    public Object getObject(int num) throws Exception
    {
	Object out = null;
	int req_cap = num + 1;
	if (req_cap > al.size())
	    {
		al.ensureCapacity(req_cap * 2);
		for (int i = al.size(), end = req_cap * 2; i < end; ++i)
		    al.add(null);
		out = addToCache(num);
	    }
	else
	    {
		out = al.get(num);
		if (out == null)
		    out = addToCache(num);
	    }
	return out;
    }

    private Object addToCache(int num) throws Exception
    {
	Object out = findObject(num);
	al.set(num, out);
	return out;
    }

    protected abstract Object findObject(int num) throws Exception;
}
