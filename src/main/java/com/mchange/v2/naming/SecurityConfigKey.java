package com.mchange.v2.naming;

public class SecurityConfigKey {

    public final static String SUPPORT_REFERENCE_REMOTE_FACTORY_CLASS_LOCATION = "com.mchange.v2.naming.supportReferenceRemoteFactoryClassLocation";

    // not used directly here, but intended for use by applications (e.g. c3p0)
    public final static String PERMIT_NONLOCAL_JNDI_NAMES                       = "com.mchange.v2.naming.permitNonlocalJndiNames";

    private SecurityConfigKey() {}
}
