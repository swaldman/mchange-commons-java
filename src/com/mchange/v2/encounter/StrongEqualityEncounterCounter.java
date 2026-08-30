package com.mchange.v2.encounter;

import java.util.HashMap;

/**
 *  NOTE: Use of StrongEqualityEncounterCounter will maintain a reference to any
 *  Object it has encountered, leading potentially to memory leaks if it is
 *  resources are not reset.
 */
public class StrongEqualityEncounterCounter extends AbstractEncounterCounter
{
    public StrongEqualityEncounterCounter()
    { super( new HashMap() ); }
}
