/*
 * Distributed as part of mchange-commons-java v.0.2.3
 *
 * Copyright (C) 2012 Machinery For Change, Inc.
 *
 * Author: Steve Waldman <swaldman@mchange.com>
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License version 2.1, as 
 * published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; see the file LICENSE.  If not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307, USA.
 */


package com.mchange.v1.db.sql.schemarep;

import java.util.*;
import com.mchange.v1.util.ListUtils;
import com.mchange.v1.util.SetUtils;
import com.mchange.v1.util.MapUtils;

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

    public String getTableName()
    { return tableName; }

    public Iterator getColumnNames()
    { return colNameList.iterator(); }

    public ColumnRep columnRepForName(String name)
    { return (ColumnRep) namesToColReps.get( name ); }

    public Set getPrimaryKeyColumnNames()
    { return primaryKeyColNames; }

    public Set getForeignKeyReps()
    { return foreignKeyReps; }

    public Set getUniquenessConstraintReps()
    { return uniqConstrReps; }

    public boolean equals( Object o )
    {
	if (o == null || this.getClass() != o.getClass())
	    return false;

	TableRepImpl other = (TableRepImpl) o;
	return
	    this.tableName.equals( other.tableName ) &&
	    ListUtils.equivalent( this.colNameList, other.colNameList ) &&
	    MapUtils.equivalentDisregardingSort( this.namesToColReps, other.namesToColReps ) &&
	    SetUtils.equivalentDisregardingSort( this.primaryKeyColNames, other.primaryKeyColNames ) &&
	    SetUtils.equivalentDisregardingSort( this.foreignKeyReps, other.foreignKeyReps ) &&
	    SetUtils.equivalentDisregardingSort( this.uniqConstrReps, other.uniqConstrReps );
    }

    public int hashCode()
    {
	return 
	    tableName.hashCode() ^
	    ListUtils.hashContents( colNameList ) ^
	    MapUtils.hashContentsDisregardingSort( namesToColReps ) ^
	    SetUtils.hashContentsDisregardingSort( primaryKeyColNames ) ^
	    SetUtils.hashContentsDisregardingSort( foreignKeyReps ) ^
	    SetUtils.hashContentsDisregardingSort( uniqConstrReps );
    }
}



