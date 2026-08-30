package com.mchange.v2.encounter;

import java.util.Map;
import com.mchange.v2.util.WeakIdentityHashMapFactory;

public class WeakIdentityEncounterCounter extends AbstractEncounterCounter
{
    public WeakIdentityEncounterCounter()
    { super( WeakIdentityHashMapFactory.create() ); }
}
