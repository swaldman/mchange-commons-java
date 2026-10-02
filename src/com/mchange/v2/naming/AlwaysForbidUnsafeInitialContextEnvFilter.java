package com.mchange.v2.naming;

import java.util.Hashtable;
import com.mchange.v2.cfg.PropertiesConfig;

public class AlwaysForbidUnsafeInitialContextEnvFilter implements UnsafeInitialContextEnvFilter
{
    @Override
    public Hashtable<?,?> safeEnv( Hashtable<?,?> env, Class<?> materializingClass, PropertiesConfig pcfg ) throws ForbiddenInitialContextException
    { throw new ForbiddenInitialContextException("Cannot resolve InitialContext environment. Current policy forbids, without any attempt to remedy, any InitialContext environment whose provenance is untrusted."); }
}
