/*
 * Distributed as part of mchange-commons-java 0.2.7
 *
 * Copyright (C) 2014 Machinery For Change, Inc.
 *
 * Author: Steve Waldman <swaldman@mchange.com>
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of EITHER:
 *
 *     1) The GNU Lesser General Public License (LGPL), version 2.1, as 
 *        published by the Free Software Foundation
 *
 * OR
 *
 *     2) The Eclipse Public License (EPL), version 1.0
 *
 * You may choose which license to accept if you wish to redistribute
 * or modify this work. You may offer derivatives of this work
 * under the license you have chosen, or you may provide the same
 * choice of license which you have been offered here.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * You should have received copies of both LGPL v2.1 and EPL v1.0
 * along with this software; see the files LICENSE-EPL and LICENSE-LGPL.
 * If not, the text of these licenses are currently available at
 *
 * LGPL v2.1: http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 *  EPL v1.0: http://www.eclipse.org/org/documents/epl-v10.php 
 * 
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



