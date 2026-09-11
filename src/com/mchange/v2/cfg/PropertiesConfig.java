package com.mchange.v2.cfg;

import java.util.Properties;

public interface PropertiesConfig
{
    /**
     *  A prefix should either be "" or a complete token in a . separated name
     */
    public Properties getPropertiesByPrefix(String pfx);
    public String getProperty( String key );
}
