package com.mchange.util;

import java.io.*;
import java.rmi.*;

public interface PasswordManager
{
  public boolean validate(String username, String password) 
    throws IOException;

  public boolean updatePassword(String username, String oldPassword, String newPassword)
    throws IOException;
}
