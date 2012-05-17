/*
 * Distributed as part of mchange-commons-java v.0.2.1
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.HashMap;

public class ConnectionBundleImpl implements ConnectionBundle
{
    Connection con;
    Map map = new HashMap();

    public ConnectionBundleImpl(Connection con)
    {this.con = con;}

    public Connection getConnection()
    {return con;}

    public PreparedStatement getStatement(String stmt_name)
    {return (PreparedStatement) map.get(stmt_name);}

    public void putStatement(String stmt_name, PreparedStatement stmt)
    {map.put(stmt_name, stmt);}

    public void close() throws SQLException
    {this.con.close();}

    public void finalize() throws Exception
    {if (!con.isClosed()) this.close();}
}
