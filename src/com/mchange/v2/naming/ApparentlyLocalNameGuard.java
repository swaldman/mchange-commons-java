package com.mchange.v2.naming;

import javax.naming.Name;

public class ApparentlyLocalNameGuard implements NameGuard
{
    @Override
    public boolean nameIsAcceptable( Name name )   { return !name.isEmpty() && name.get(0).startsWith("java:"); }
    @Override
    public boolean nameIsAcceptable( String name ) { return name.startsWith("java:"); }

    @Override
    public String onlyAcceptableWhen()
    {
        return
            "they appear to be local to the JVM, that is if they are Strings, they begin with 'java:' " +
            "or they are Name objects their first component begins with 'java:'";
    }
}
