package com.mchange.v2.util;

import java.util.Iterator;

public final class IterableUtils
{
    public static String joinAsString(String delimiter, Iterable i)
    {
        StringBuilder sb = new StringBuilder();
        Iterator ii = i.iterator();
        boolean hn = ii.hasNext();
        while (hn)
        {
            sb.append(ii.next());
            hn = ii.hasNext();
            if (hn) sb.append(delimiter);
        }
        return sb.toString();
    }

    private IterableUtils() {}
}
