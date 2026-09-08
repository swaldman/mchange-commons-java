package com.mchange.v2.encounter;

import java.util.Map;

final class GenericEncounterCounter extends AbstractEncounterCounter 
{
    GenericEncounterCounter(Map<Object,Long> m)
    { super( m ); }
}
