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





