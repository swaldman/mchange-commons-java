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


package com.mchange.util.impl;

import java.io.*;
import java.rmi.*;
import com.mchange.util.*;

public class PlaintextPropertiesPasswordManager implements PasswordManager
{
  private final static String PASSWORD_PROP_PFX = "password.";
  private final static String HEADER = "com.mchange.util.impl.PlaintextPropertiesPasswordManager data";

  SyncedProperties props;

  public PlaintextPropertiesPasswordManager(File propsFile) throws IOException
    {this.props = new SyncedProperties(propsFile, HEADER);}

  public boolean validate(String username, String password) throws IOException
    {return (password.equals(props.getProperty(PASSWORD_PROP_PFX + username)));}

  public boolean updatePassword(String username, String oldPassword, String newPassword) throws IOException
    {
      if (!validate(username, oldPassword)) return false;
      props.put(PASSWORD_PROP_PFX + username, newPassword);
      return true;
    }
}





