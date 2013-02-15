/*
 * Distributed as part of mchange-commons-java v.0.2.4
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

/**
 * @deprecated use com.mchange.v2.mcp.DbAuth
 */
class DbAuth
{
    String username;
    String password;

    public DbAuth(String username, String password)
    {
	this.username = username;
	this.password = password;
    }

    public String getUsername()
    { return username; }

    public String getPassword()
    { return password; }

    public boolean equals(Object o)
    {
	if (o != null && this.getClass() == o.getClass())
	    {
		DbAuth other = (DbAuth) o;
		return (this.username.equals(other.username) && 
			this.password.equals(other.password));
	    }
	else
	    return false;
    }

    public int hashCode()
    { return username.hashCode() ^ password.hashCode(); }
}
								




