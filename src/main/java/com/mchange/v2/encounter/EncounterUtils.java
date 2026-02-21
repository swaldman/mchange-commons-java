package com.mchange.v2.encounter;

import com.mchange.v1.identicator.*;

public final class EncounterUtils
{
    public static EncounterCounter createStrong( Identicator id )
    { return new GenericEncounterCounter( new IdHashMap( id ) ); }

    public static EncounterCounter createWeak( Identicator id )
    { return new GenericEncounterCounter( new IdWeakHashMap( id ) ); }

    /**
     * returns the inner EncounterCounter wrapped so that all
     * method calls are effectively synchronized.
     */
    public static EncounterCounter syncWrap( final EncounterCounter inner )
    {
	return new EncounterCounter()
	{
	    public synchronized long encounter(Object o) { return inner.encounter(o); }
	    public synchronized long reset(Object o) { return inner.reset(o); }
	    public synchronized void resetAll() { inner.resetAll(); }
	};
    }

    private EncounterUtils()
    {}
}
