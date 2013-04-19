package com.mchange.v3.hocon;

import java.util.*;
import com.mchange.v2.cfg.*;
import com.typesafe.config.*;

public final class HoconUtils
{
    public static class PropertiesConversion
    {
	Properties  properties;
	Set<String> unrenderable;
    }

    public static PropertiesConversion configToProperties( Config config )
    {
	Set<Map.Entry<String,ConfigValue>> entries = config.entrySet();

	Properties  properties = new Properties();
	Set<String> unrenderable = new HashSet<String>();

	for( Map.Entry<String,ConfigValue> entry : entries )
	{
	    String path = entry.getKey();
	    String value = null;
	    try
	    { value = config.getString( path ); }
	    catch( ConfigException.Missing e )
	    { unrenderable.add( path ); }

	    if ( value != null )
		properties.setProperty( path, value );
	}

	PropertiesConversion out = new PropertiesConversion();
	out.properties = properties;
	out.unrenderable = unrenderable;
	return out;
    }
    
    private HoconUtils()
    {}
}