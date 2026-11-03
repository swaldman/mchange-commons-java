package com.mchange.v2.cfg.junit.extras;

/** Claims {@code beta:} identifiers. See SchemeExtraPropertiesConfigSources. */
public final class BetaExtraPropertiesConfigSources extends SchemeExtraPropertiesConfigSources
{
    @Override
    protected String scheme() { return "beta:"; }
}
