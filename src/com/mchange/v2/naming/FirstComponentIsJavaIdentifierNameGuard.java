package com.mchange.v2.naming;

import javax.naming.Name;
import javax.lang.model.SourceVersion;

public class FirstComponentIsJavaIdentifierNameGuard implements NameGuard
{
    public boolean nameIsAcceptable( Name name )   { return !name.isEmpty() && SourceVersion.isName(name.get(0)); }

    // note that we don't bother checking for escaped slashes, we just
    // leave the backslash in the String, which will cause it to fail
    // the Java identifier tes, which is what we want.
    public boolean nameIsAcceptable( String name )
    {
        String firstComponent;
        int slashIndex = name.indexOf("/");
        if ( slashIndex >= 0)
            firstComponent = name.substring(0,slashIndex);
        else
            firstComponent = name;
        return SourceVersion.isName(firstComponent);
    }

    public String onlyAcceptableWhen()
    { return "their first component would be a valid Java identifier (e.g. jdbc or jms)"; }
}
