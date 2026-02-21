package com.mchange.v1.db.sql.schemarep;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import com.mchange.v1.util.ListUtils;

public class ForeignKeyRepImpl implements ForeignKeyRep
{
    List locColNames;
    String refTableName;
    List refColNames;

    public ForeignKeyRepImpl(List locColNames, String refTableName, List refColNames)
    {
	this.locColNames = Collections.unmodifiableList( new ArrayList( locColNames ) );
	this.refTableName = refTableName;
	this.refColNames = Collections.unmodifiableList( new ArrayList( refColNames ) );
    }

    public List getLocalColumnNames()
    { return locColNames; }

    public String getReferencedTableName()
    { return refTableName; }

    public List getReferencedColumnNames()
    { return refColNames; }

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

    public int hashCode()
    {
	return 
	    ListUtils.hashContents ( this.locColNames ) ^
	    this.refTableName.hashCode() ^
	    ListUtils.hashContents( this.refColNames );
    }
}
