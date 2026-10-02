package com.mchange.v2.naming;

import java.util.Hashtable;
import com.mchange.v2.cfg.PropertiesConfig;

import com.mchange.v2.log.*;

public class AlwaysReplaceWithDefaultUnsafeInitialContextEnvFilter implements UnsafeInitialContextEnvFilter
{
    final static MLogger logger = MLog.getLogger( AlwaysReplaceWithDefaultUnsafeInitialContextEnvFilter.class );

    //MT: guarded by class' monitor
    static boolean warned = false;

    private static synchronized void warnOnce()
    {
        if (!warned)
        {
            if ( logger.isLoggable(MLevel.WARNING) )
                logger.log(MLevel.WARNING, AlwaysReplaceWithDefaultUnsafeInitialContextEnvFilter.class.getName() +
                           " will replace all potentially dangerous (derived from deserialization or dereferencing) " +
                           "InitialContext environments that it encounters with a JVM default " +
                           "InitialContext environment, ignoring any settings, which may be untrustworthy.");
            warned = true;
        }
    }

    @Override
    public Hashtable<?,?> safeEnv( Hashtable<?,?> env, Class<?> materializingClass, PropertiesConfig pcfg ) throws ForbiddenInitialContextException
    {
        warnOnce();
        return null;
    }
}
