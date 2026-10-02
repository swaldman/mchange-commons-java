package com.mchange.v2.naming;

import java.util.Hashtable;
import com.mchange.v2.cfg.PropertiesConfig;

/**
 *  Implementations should be sharable, stateless immutable objects, creatable by no-arg constructor,
 *  all identical and equal within a single class.
 */
public interface UnsafeInitialContextEnvFilter
{
    /**
     *  Note: the returned environment must replace the original everywhere downstream, not only in the InitialContext constructor.
     *        applications that inadvertantly use a leaked reference to the unsanitized environment are subject to compromise by
     *        the unsanitized environment.
     *
     *  @param env the untrusted environment, as it arrived on the materializing object.
     *  @param materializingClass the class that materializes an unsafe InitialContext environment
     *         and so must sanitize it before any lookup, or passing references to outside contexts.
     *  @param pcfg can be null, a configuration source.
     *
     *  @return the environment to use: env itself to accept it as-is, a filtered copy to accept
     *          part of it, or <i>null</i> to discard it entirely and use the JVM's default
     *          environment, as <code>new InitialContext()</code> would.
     *
     *  @throws ForbiddenInitialContextException to refuse the lookup altogether. Note the
     *          difference from returning null: null proceeds against the default environment,
     *          this does not proceed at all.
     */
    public Hashtable<?,?> safeEnv( Hashtable<?,?> env, Class<?> materializingClass, PropertiesConfig pcfg ) throws ForbiddenInitialContextException;
}
