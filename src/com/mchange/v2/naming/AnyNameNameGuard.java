package com.mchange.v2.naming;

import javax.naming.Name;

public class AnyNameNameGuard implements NameGuard
{
    @Override
    public boolean nameIsAcceptable( Name name )   { return true; }
    @Override
    public boolean nameIsAcceptable( String name ) { return true; }

    @Override
    public String onlyAcceptableWhen() { return "they are, well, any name, all names are acceptable"; }
}
