package com.mchange.v2.net;

import java.io.UnsupportedEncodingException;
import java.util.*;
import java.net.URLDecoder;

public final class QueryStringParser
{
    private final static Object NoValue = new Object();

    // significantly modified from the AI output of a Google query...
    public static Map<String, List<String>> parseQueryString(String rawQuery) {
        try
        {
            Map<String, List<String>> queryPairs = new HashMap<>();
            if (rawQuery == null || rawQuery.isEmpty()) {
                return queryPairs;
            }

            // Split by ampersand to separate pairs
            String[] pairs = rawQuery.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf("=");

                // Handle missing '=' or empty keys/values safely
                // we interpret the value for 'key=' and empty string,
                // but for 'key' we retain no value
                String key = idx > 0 ? pair.substring(0, idx) : pair;
                Object value = idx > 0 && pair.length() > idx + 1 ? pair.substring(idx + 1) : NoValue;

                // Decode URL-encoded key characters
                String decodedKey = URLDecoder.decode(key,"UTF8");

                // Group multiple values under the same key
                List<String> values = queryPairs.get(decodedKey);
                if (values == null)
                {
                    values = new ArrayList<String>();
                    queryPairs.put(decodedKey,values);
                }

                // Decode URL-encoded key characters, if there is a value
                if (value != NoValue) values.add(URLDecoder.decode((String)value,"UTF8")); 
            }
            return queryPairs;
        }
        catch (UnsupportedEncodingException e)
        { throw new InternalError( "Huh? Encoding 'UTF8' is not supported?!?", e ); }
    }

    private QueryStringParser() {}
}
