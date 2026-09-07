package com.mchange.v1.db.sql.schemarep;

import java.util.*;
import com.mchange.v1.util.ListUtils;

public class TableRepImpl implements TableRep
{
    String tableName;
    List   colNameList;
    Map    namesToColReps;
    Set    primaryKeyColNames;
    Set    foreignKeyReps;
    Set    uniqConstrReps;
    
    public TableRepImpl(String tableName, List colReps, 
			Collection primaryKeyColNames, 
			Collection foreignKeyReps,
			Collection uniqConstrReps)
    {
	this.tableName = tableName;
	List tempColNameList = new ArrayList();
	Map tempNamesToColReps = new HashMap();
	for (int i = 0, len = colReps.size(); i < len; ++i)
	    {
		ColumnRep colRep = (ColumnRep) colReps.get(i);
		String colName = colRep.getColumnName();
		tempColNameList.add( colName );
		tempNamesToColReps.put( colName, colRep );
	    }
	this.colNameList = Collections.unmodifiableList( tempColNameList );
	this.namesToColReps = Collections.unmodifiableMap ( tempNamesToColReps );
	this.primaryKeyColNames = (primaryKeyColNames == null ?
				   Collections.EMPTY_SET :
				   Collections.unmodifiableSet( new HashSet( primaryKeyColNames ) ) );
	this.foreignKeyReps = (foreignKeyReps == null ?
			       Collections.EMPTY_SET :
			       Collections.unmodifiableSet( new HashSet( foreignKeyReps ) ) );
	this.uniqConstrReps = (uniqConstrReps == null ?
			       Collections.EMPTY_SET :
			       Collections.unmodifiableSet( new HashSet( uniqConstrReps ) ) );
    }

    @Override
    public String getTableName()
    { return tableName; }

    @Override
    public Iterator getColumnNames()
    { return colNameList.iterator(); }

    @Override
    public ColumnRep columnRepForName(String name)
    { return (ColumnRep) namesToColReps.get( name ); }

    @Override
    public Set getPrimaryKeyColumnNames()
    { return primaryKeyColNames; }

    @Override
    public Set getForeignKeyReps()
    { return foreignKeyReps; }

    @Override
    public Set getUniquenessConstraintReps()
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



