package com.mchange.v1.db.sql;

import java.sql.ResultSet;
import java.sql.SQLException;
import com.mchange.v1.util.UIterator;

public abstract class SimpleCursor implements UIterator<Object>
{
    ResultSet rs;
    int       available = -1; //1 true, 0 false, -1 unchecked
    
    public SimpleCursor(ResultSet rs)
    {this.rs = rs;}

    @Override
    public boolean hasNext() throws SQLException
    {
	ratchet();
	return (available == 1);
    }
    
    @Override
    public Object next() throws SQLException
    {
	ratchet();
	Object out = objectFromResultSet(rs);
	clear();
	return out;
    }
    
    @Override
    public void remove()
    {throw new UnsupportedOperationException();}

    @Override
    public void close() throws Exception
    {
	rs.close();
	rs = null;
    }

    @Override
    @SuppressWarnings("deprecation") // a close() safety net; finalize() is deprecated but still called
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

