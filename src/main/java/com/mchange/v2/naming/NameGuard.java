package com.mchange.v2.naming;

import javax.naming.Name;

public interface NameGuard
{
    public boolean nameIsAcceptable( Name name );
    public boolean nameIsAcceptable( String name );

    /**
     * This should be a descriptive message that might follow "names are only acceptable when "
     *
     * A period will be appended to the message. (No terminal period should be included in the
     * String.)
     */
    public String onlyAcceptableWhen();
}
