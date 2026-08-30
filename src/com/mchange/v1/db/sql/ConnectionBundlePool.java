package com.mchange.v1.db.sql;

import java.sql.*;
import com.mchange.v1.util.*;

public interface ConnectionBundlePool extends ClosableResource
{
    public ConnectionBundle checkoutBundle() throws SQLException, InterruptedException, BrokenObjectException;
    public void checkinBundle(ConnectionBundle bndl) throws SQLException, BrokenObjectException;
    public void close() throws SQLException;
}
