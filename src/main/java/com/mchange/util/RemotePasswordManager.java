package com.mchange.util;

import java.io.*;
import java.rmi.*;

public interface RemotePasswordManager extends PasswordManager, Remote
{
  public boolean validate(String username, String password) 
    throws RemoteException, IOException;

  public boolean updatePassword(String username, String oldPassword, String newPassword)
    throws RemoteException, IOException;
}
