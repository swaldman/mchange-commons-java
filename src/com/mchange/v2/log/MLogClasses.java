package com.mchange.v2.log;

import java.util.*;

public final class MLogClasses
{
    static final String LOG4J_CNAME = "com.mchange.v2.log.log4j.Log4jMLog";
	static final String LOG4J2_CNAME = "com.mchange.v2.log.log4j2.Log4j2MLog";
    static final String SLF4J_CNAME = "com.mchange.v2.log.slf4j.Slf4jMLog";
    static final String JDK14_CNAME = "com.mchange.v2.log.jdk14logging.Jdk14MLog";

    final static String[] SEARCH_CLASSNAMES = { SLF4J_CNAME, LOG4J_CNAME, LOG4J2_CNAME, JDK14_CNAME };

    final static Map<String,String> ALIASES;

    static 
    {
	HashMap<String,String> map = new HashMap<String,String>();
	map.put("log4j", LOG4J_CNAME);
	map.put("log4j2", LOG4J2_CNAME);
	map.put("slf4j", SLF4J_CNAME);
	map.put("jdk14", JDK14_CNAME);
	map.put("jul", JDK14_CNAME);
	map.put("java.util.logging", JDK14_CNAME);
	map.put("fallback", "com.mchange.v2.log.FallbackMLog");
	ALIASES = Collections.unmodifiableMap( map );
    }

    static String resolveIfAlias( String name )
    {
	String out = ALIASES.get( name.toLowerCase() );
	if ( out == null ) out = name;
	return out;
    }

    private MLogClasses()
    {}
}
