package com.mchange.v2.encounter;

import java.util.Map;
import java.util.WeakHashMap;

/**
 *  NOTE: Use of WeakEqualityEncounterCounter can't fully guarantee counts with equality semantics,
 *  as an Object can be encounted garbage collected, then re-encountered with 
 *  no apparent history. 
 */
public class WeakEqualityEncounterCounter extends AbstractEncounterCounter
{
    public WeakEqualityEncounterCounter()
    { super( new WeakHashMap() ); }
}
