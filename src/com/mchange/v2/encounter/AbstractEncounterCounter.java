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
    @Override
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

    @Override
    public long reset(Object o)
    {
	long out = encounter(o);
	m.remove( o );
	return out;
    }

    @Override
    public void resetAll()
    { m.clear(); }
}
