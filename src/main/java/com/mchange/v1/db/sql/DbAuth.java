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
								




