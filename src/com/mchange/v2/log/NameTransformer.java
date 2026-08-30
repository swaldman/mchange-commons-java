package com.mchange.v2.log;

/**
 * <p>When the methods return a name, the log requested from MLog.getLogger( XXX )
 * the logger actually acquired will be based on the String returned.</p>
 *
 * <p>When the methods return null, no transformation will occur, and the logger
 * that would have been returned without a transformer will be returned.</p>
 *
 * <p>Implementing classes must have public, no-arg constructors, through which
 * they will be instantiated.</p>
 */
public interface NameTransformer
{
    public String transformName( String name );
    public String transformName( Class cl );
    public String transformName();
}
