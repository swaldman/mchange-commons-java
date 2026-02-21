package com.mchange.v2.encounter;

import java.util.Map;

class AbstractEncounterCounter implements EncounterCounter
{
    final static Long ONE = Long.valueOf(1);
    Map m;

    AbstractEncounterCounter(Map m)
    { this.m = m; }

    /**
     *  @return how many times have I seen this object before?
     */
    public long encounter(Object o)
    {
	Long oldLong = (Long) m.get(o);
	Long newLong;
	long out;
	if (oldLong == null)
	    {
		out = 0;
		newLong = ONE;
	    }
	else
	    {
		out = oldLong.longValue(); 
		newLong = Long.valueOf(out + 1);
	    }
	m.put( o, newLong );
	return out;
    }

    public long reset(Object o)
    {
	long out = encounter(o);
	m.remove( o );
	return out;
    }

    public void resetAll()
    { m.clear(); }
}
