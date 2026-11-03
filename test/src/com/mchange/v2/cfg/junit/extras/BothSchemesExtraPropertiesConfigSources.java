package com.mchange.v2.cfg.junit.extras;

/**
 *  Claims BOTH {@code alpha:} and {@code beta:}, so it can contend with the single-scheme
 *  resolvers and let a test see which one won.
 */
public final class BothSchemesExtraPropertiesConfigSources extends SchemeExtraPropertiesConfigSources
{
    @Override
    protected String scheme() { return ""; }   // "".startsWith is always true: claims everything
}
