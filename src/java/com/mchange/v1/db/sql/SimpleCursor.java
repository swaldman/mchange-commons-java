/*
 * Distributed as part of mchange-commons-java v.0.2.3.2
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


package com.mchange.v1.db.sql;

import java.sql.ResultSet;
import java.sql.SQLException;
import com.mchange.v1.util.UIterator;

public abstract class SimpleCursor implements UIterator
{
    ResultSet rs;
    int       available = -1; //1 true, 0 false, -1 unchecked
    
    public SimpleCursor(ResultSet rs)
    {this.rs = rs;}

    public boolean hasNext() throws SQLException
    {
	ratchet();
	return (available == 1);
    }
    
    public Object next() throws SQLException
    {
	ratchet();
	Object out = objectFromResultSet(rs);
	clear();
	return out;
    }
    
    public void remove()
    {throw new UnsupportedOperationException();}

    public void close() throws Exception
    {
	rs.close();
	rs = null;
    }

    public void finalize() throws Exception
    {if (rs != null) this.close();}
    
    protected abstract Object objectFromResultSet(ResultSet rs) throws SQLException;
    
    private void ratchet() throws SQLException
    {
	if (available == -1)
	    available = (rs.next() ? 1 : 0);
    }
    
    private void clear()
    {available = -1;}
}

