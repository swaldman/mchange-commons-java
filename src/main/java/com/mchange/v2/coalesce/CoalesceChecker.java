package com.mchange.v2.coalesce;

public interface CoalesceChecker
{
    /**
     *  @return true iff a and b should be considered equivalent,
     *          so that a Coalescer should simply return whichever
     *          Object it considers canonical.
     */
    public boolean checkCoalesce( Object a, Object b );

    /**
     *  Any two objects for which checkCoalese() would return true <b>must</b>
     *  coalesce hash to the same value!!!
     */
    public int coalesceHash( Object a );
}
