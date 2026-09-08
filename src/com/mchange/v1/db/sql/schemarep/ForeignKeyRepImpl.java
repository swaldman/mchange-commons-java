package com.mchange.v1.db.sql.schemarep;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import com.mchange.v1.util.ListUtils;

public class ForeignKeyRepImpl implements ForeignKeyRep
{
    List<String> locColNames;
    String refTableName;
    List<String> refColNames;

    public ForeignKeyRepImpl(List<String> locColNames, String refTableName, List<String> refColNames)
    {
	this.locColNames = Collections.unmodifiableList( new ArrayList<String>( locColNames ) );
	this.refTableName = refTableName;
	this.refColNames = Collections.unmodifiableList( new ArrayList<String>( refColNames ) );
    }

    @Override
    public List<String> getLocalColumnNames()
    { return locColNames; }

    @Override
    public String getReferencedTableName()
    { return refTableName; }

    @Override
    public List<String> getReferencedColumnNames()
    { return refColNames; }

    @Override
    public boolean equals( Object o )
    {
	if (o == null || this.getClass() != o.getClass())
	    return false;

	ForeignKeyRepImpl other = (ForeignKeyRepImpl) o;
	return
	    ListUtils.equivalent( this.locColNames, other.locColNames ) &&
	    this.refTableName.equals( other.refTableName ) &&
	    ListUtils.equivalent( this.refColNames, other.refColNames );
    }

    @Override
    public int hashCode()
    {
	return 
	    ListUtils.hashContents ( this.locColNames ) ^
	    this.refTableName.hashCode() ^
	    ListUtils.hashContents( this.refColNames );
    }
}
