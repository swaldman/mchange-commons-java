/*
 * Distributed as part of mchange-commonslib v.0.2
 *
 * Copyright (C) 2010 Machinery For Change, Inc.
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
