package com.mchange.v1.db.sql.schemarep;

import java.util.Collection;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

public class UniquenessConstraintRepImpl implements UniquenessConstraintRep
{
    Set<String> uniqueColNames;

    public UniquenessConstraintRepImpl(Collection<String> colNames)
    { uniqueColNames = Collections.unmodifiableSet( new HashSet<String>( colNames ) ); }
	
    @Override
    public Set<String> getUniqueColumnNames()
    { return uniqueColNames; }

    @Override
    public boolean equals( Object o )
    {
	return 
	    o != null &&
	    this.getClass() == o.getClass() &&
	    this.uniqueColNames.equals( ((UniquenessConstraintRepImpl) o ).uniqueColNames );
    }

    @Override
    public int hashCode()
    {
	return 
	    this.getClass().hashCode() ^ 
	    uniqueColNames.hashCode();
    }
}
