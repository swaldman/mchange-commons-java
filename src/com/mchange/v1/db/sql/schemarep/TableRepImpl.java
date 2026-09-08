package com.mchange.v1.db.sql.schemarep;

import java.util.*;
import com.mchange.v1.util.ListUtils;

public class TableRepImpl implements TableRep
{
    String tableName;
    List<String>   colNameList;
    Map<String,ColumnRep>    namesToColReps;
    Set<String>    primaryKeyColNames;
    Set<ForeignKeyRep>    foreignKeyReps;
    Set<UniquenessConstraintRep>    uniqConstrReps;
    
    public TableRepImpl(String tableName, List<ColumnRep> colReps, 
			Collection<String> primaryKeyColNames, 
			Collection<ForeignKeyRep> foreignKeyReps,
			Collection<UniquenessConstraintRep> uniqConstrReps)
    {
	this.tableName = tableName;
	List<String> tempColNameList = new ArrayList<String>();
	Map<String,ColumnRep> tempNamesToColReps = new HashMap<String,ColumnRep>();
	for (int i = 0, len = colReps.size(); i < len; ++i)
	    {
		ColumnRep colRep = colReps.get(i);
		String colName = colRep.getColumnName();
		tempColNameList.add( colName );
		tempNamesToColReps.put( colName, colRep );
	    }
	this.colNameList = Collections.unmodifiableList( tempColNameList );
	this.namesToColReps = Collections.unmodifiableMap ( tempNamesToColReps );
	this.primaryKeyColNames = (primaryKeyColNames == null ?
				   Collections.<String>emptySet() :
				   Collections.unmodifiableSet( new HashSet<String>( primaryKeyColNames ) ) );
	this.foreignKeyReps = (foreignKeyReps == null ?
			       Collections.<ForeignKeyRep>emptySet() :
			       Collections.unmodifiableSet( new HashSet<ForeignKeyRep>( foreignKeyReps ) ) );
	this.uniqConstrReps = (uniqConstrReps == null ?
			       Collections.<UniquenessConstraintRep>emptySet() :
			       Collections.unmodifiableSet( new HashSet<UniquenessConstraintRep>( uniqConstrReps ) ) );
    }

    @Override
    public String getTableName()
    { return tableName; }

    @Override
    public Iterator<String> getColumnNames()
    { return colNameList.iterator(); }

    @Override
    public ColumnRep columnRepForName(String name)
    { return (ColumnRep) namesToColReps.get( name ); }

    @Override
    public Set<String> getPrimaryKeyColumnNames()
    { return primaryKeyColNames; }

    @Override
    public Set<ForeignKeyRep> getForeignKeyReps()
    { return foreignKeyReps; }

    @Override
    public Set<UniquenessConstraintRep> getUniquenessConstraintReps()
    { return uniqConstrReps; }

    @Override
    public boolean equals( Object o )
    {
	if (o == null || this.getClass() != o.getClass())
	    return false;

	TableRepImpl other = (TableRepImpl) o;
	return
	    this.tableName.equals( other.tableName ) &&
	    ListUtils.equivalent( this.colNameList, other.colNameList ) &&
	    this.namesToColReps.equals( other.namesToColReps ) &&
	    this.primaryKeyColNames.equals( other.primaryKeyColNames ) &&
	    this.foreignKeyReps.equals( other.foreignKeyReps ) &&
	    this.uniqConstrReps.equals( other.uniqConstrReps );
    }

    @Override
    public int hashCode()
    {
	return 
	    tableName.hashCode() ^
	    ListUtils.hashContents( colNameList ) ^
	    namesToColReps.hashCode() ^
	    primaryKeyColNames.hashCode() ^
	    foreignKeyReps.hashCode() ^
	    uniqConstrReps.hashCode();
    }
}



