package com.mchange.v2.cfg.junit.extras;

import java.util.ArrayList;
import java.util.List;

import com.mchange.v2.cfg.ConfigVetoedException;
import com.mchange.v2.cfg.DelayedLogItem;
import com.mchange.v2.cfg.ExtraPropertiesConfigSources;
import com.mchange.v2.cfg.PropertiesConfigSource;
import com.mchange.v2.cfg.VetoableConfig;

/**
 *  A resolver whose source implements {@link VetoableConfig} and always refuses.
 *
 *  <p>Here because vetoability is not a property of the identifier but of the source a resolver
 *  returns, so the library can only discover it by resolving. An extension can therefore make
 *  an identifier refusable, and plain {@code MConfig.AsProvided} has to decline such an
 *  identifier rather than read it -- which is the one thing an extension point could
 *  accidentally route around.</p>
 */
public final class VetoableExtraPropertiesConfigSources extends ExtraPropertiesConfigSources
{
    public final static String SCHEME = "vetoable:";

    @Override
    public PropertiesConfigSource propertiesConfigSourceForIdentifier( String identifier, List<DelayedLogItem> delayedLogItemsOut )
    {
        if ( identifier == null || ! identifier.startsWith( SCHEME ) ) return null;
        return new VetoingSource();
    }

    private final static class VetoingSource implements PropertiesConfigSource, VetoableConfig
    {
        @Override
        public Parse propertiesFromSource( String identifier ) throws ConfigVetoedException
        {
            throw new ConfigVetoedException( this, identifier,
                                             "This test source refuses every identifier it claims.",
                                             null, new ArrayList<DelayedLogItem>() );
        }
    }
}
