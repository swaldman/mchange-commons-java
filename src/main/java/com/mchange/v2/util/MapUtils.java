package com.mchange.v2.util;

import java.util.Iterator;
import java.util.Map;

public final class MapUtils
{
    public static String entryAsString(boolean quoteKey, boolean quoteValue, String connector, Map.Entry entry)
    {
        String key   = quoteKey   ? "\"" + entry.getKey()   + "\"" : entry.getKey().toString();
        String value = quoteValue ? "\"" + entry.getValue() + "\"" : entry.getValue().toString();
        return key + connector + value;
    }

    public static String joinEntriesIntoString(boolean quoteKey, boolean quoteValue, String connector, String delimiter, Map map)
    {
        StringBuilder sb = new StringBuilder();
        Iterator ii = map.entrySet().iterator();
        boolean hn = ii.hasNext();
        while (hn)
        {
            sb.append(entryAsString(quoteKey,quoteValue,connector,(Map.Entry)ii.next()));
            hn = ii.hasNext();
            if (hn) sb.append(delimiter);
        }
        return sb.toString();
    }

    private MapUtils() {}
}
