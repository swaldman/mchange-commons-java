package com.mchange.v2.naming;

import javax.naming.Name;

public class ApparentlyLocalOrFirstComponentIsJavaIdentifierNameGuard implements NameGuard
{
    NameGuard apparentlyLocalNameGuard = new ApparentlyLocalNameGuard();
    NameGuard firstComponentIsJavaIdentifierNameGuard = new FirstComponentIsJavaIdentifierNameGuard();

    @Override
    public boolean nameIsAcceptable( Name name )
    {
        return
            apparentlyLocalNameGuard.nameIsAcceptable( name ) ||
            firstComponentIsJavaIdentifierNameGuard.nameIsAcceptable( name );
    }

    @Override
    public boolean nameIsAcceptable( String name )
    {
        return
            apparentlyLocalNameGuard.nameIsAcceptable( name ) ||
            firstComponentIsJavaIdentifierNameGuard.nameIsAcceptable( name );
    }

    @Override
    public String onlyAcceptableWhen()
    {
        return
            apparentlyLocalNameGuard.onlyAcceptableWhen() +
            " or " +
            firstComponentIsJavaIdentifierNameGuard.onlyAcceptableWhen();
    }
}
