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

