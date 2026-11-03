package com.mchange.v2.cfg.junit.extras;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.mchange.v2.cfg.DelayedLogItem;
import com.mchange.v2.cfg.ExtraPropertiesConfigSources;
import com.mchange.v2.cfg.PropertiesConfigSource;

/**
 *  Base for the test resolvers: claims identifiers beginning with one scheme and answers with a
 *  single property naming whichever resolver produced it.
 *
 *  <p>That the value names the resolver is the point. Ordering questions -- which of two
 *  resolvers claiming the same scheme actually ran -- are otherwise unobservable, because both
 *  would supply an indistinguishable answer.</p>
 *
 *  <p>Loaded by the scenario's ClassLoader rather than this test's, so the
 *  {@code ExtraPropertiesConfigSources} it extends is the scenario's copy. That happens because
 *  the test classes' own code source is put on the scenario classpath; see the test case.</p>
 */
public abstract class SchemeExtraPropertiesConfigSources extends ExtraPropertiesConfigSources
{
    /** Every resolver here answers under this key, with its own name as the value. */
    public final static String WHO_KEY = "com.mchange.v2.cfg.junit.extras.whoAnswered";

    protected abstract String scheme();

    @Override
    public PropertiesConfigSource propertiesConfigSourceForIdentifier( String identifier, List<DelayedLogItem> delayedLogItemsOut )
    {
        if ( identifier == null || ! identifier.startsWith( scheme() ) ) return null;

        final String who = this.getClass().getName();
        return new PropertiesConfigSource()
        {
            @Override
            public Parse propertiesFromSource( String id )
            {
                Properties p = new Properties();
                p.setProperty( WHO_KEY, who );
                p.setProperty( "identifier.as.given", id );
                return new Parse( p, new ArrayList<DelayedLogItem>() );
            }
        };
    }
}
