package com.mchange.v1.db.sql.schemarep;

import java.util.Collection;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

public class UniquenessConstraintRepImpl implements UniquenessConstraintRep
{
    Set uniqueColNames;

    public UniquenessConstraintRepImpl(Collection colNames)
    { uniqueColNames = Collections.unmodifiableSet( new HashSet( colNames ) ); }
	
    public Set getUniqueColumnNames()
    { return uniqueColNames; }

    public boolean equals( Object o )
    {
	return 
	    o != null &&
	    this.getClass() == o.getClass() &&
	    this.uniqueColNames.equals( ((UniquenessConstraintRepImpl) o ).uniqueColNames );
    }

    public int hashCode()
    {
	return 
	    this.getClass().hashCode() ^ 
	    uniqueColNames.hashCode();
    }
}
