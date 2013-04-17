package com.mchange.v2.cfg;

import java.util.Properties;

public interface PropertiesConfig
{
    public Properties getPropertiesByPrefix(String pfx);
    public String getProperty( String key );
}