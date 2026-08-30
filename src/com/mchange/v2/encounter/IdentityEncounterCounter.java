package com.mchange.v2.encounter;

import java.util.Map;
import com.mchange.v2.util.WeakIdentityHashMapFactory;

/**
 * @deprecated use WeakIdentityEncounterCounter (name changed to emphasize for library users
 *             that they need to understand whether implementations are weak or strong
 *             to avoid accidental reference retention in strong counters)
 */
public class IdentityEncounterCounter extends AbstractEncounterCounter
{
    public IdentityEncounterCounter()
    { super( WeakIdentityHashMapFactory.create() ); }
}
