package com.mchange.v2.encounter;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * @deprecated -- Use of WeakHashMap can't properly guarantee counts with equality semantics,
 *                as an Object can be encounted garbage collected, then re-encountered with 
 *                no apparent history. Clients should explicit choose the semantics they want
 *                via WeakEqualityEncounterCounter or StrongEqualityEncounterCounter
 */
public class EqualityEncounterCounter extends AbstractEncounterCounter
{
    public EqualityEncounterCounter()
    { super( new WeakHashMap() ); }
}
