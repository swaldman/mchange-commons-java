package com.mchange.v2.naming;

import javax.naming.Name;

public class ApparentlyLocalOrFirstComponentIsJavaIdentifierNameGuard implements NameGuard
{
    NameGuard apparentlyLocalNameGuard = new ApparentlyLocalNameGuard();
    NameGuard firstComponentIsJavaIdentifierNameGuard = new FirstComponentIsJavaIdentifierNameGuard();

    public boolean nameIsAcceptable( Name name )
    {
        return
            apparentlyLocalNameGuard.nameIsAcceptable( name ) ||
            firstComponentIsJavaIdentifierNameGuard.nameIsAcceptable( name );
    }

    public boolean nameIsAcceptable( String name )
    {
        return
            apparentlyLocalNameGuard.nameIsAcceptable( name ) ||
            firstComponentIsJavaIdentifierNameGuard.nameIsAcceptable( name );
    }

    public String onlyAcceptableWhen()
    {
        return
            apparentlyLocalNameGuard.onlyAcceptableWhen() +
            " or " +
            firstComponentIsJavaIdentifierNameGuard.onlyAcceptableWhen();
    }
}
