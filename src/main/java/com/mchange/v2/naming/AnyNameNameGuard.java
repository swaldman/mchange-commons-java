package com.mchange.v2.naming;

import javax.naming.Name;

public class AnyNameNameGuard implements NameGuard
{
    public boolean nameIsAcceptable( Name name )   { return true; }
    public boolean nameIsAcceptable( String name ) { return true; }

    public String onlyAcceptableWhen() { return "they are, well, any name, all names are acceptable"; }
}
