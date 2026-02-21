package com.mchange.v2.encounter;

public interface EncounterCounter
{
    /**
     *  @return how many times have I seen this object before?
     */
    public long encounter(Object o);

    /**
     *  @return how many times have I seen this object before, then
     *          remove this Object's history, resetting its count and
     *          eliminating any reference from strong counters.
     */
    public long reset(Object o);

    /**
     *          Remove all Object histories, resetting counts and
     *          clearing any references from strong counters.
     */
    public void resetAll();
}
