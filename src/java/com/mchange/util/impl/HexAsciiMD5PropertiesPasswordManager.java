/*
 * Distributed as part of mchange-commonslib v.0.2
 *
 * Copyright (C) 2010 Machinery For Change, Inc.
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
import java.security.*;
import java.util.*;
import com.mchange.lang.*;
import com.mchange.util.*;

public class HexAsciiMD5PropertiesPasswordManager implements PasswordManager
{
  private final static String DIGEST_ALGORITHM  = "MD5";
  private final static String PASSWORD_ENCODING = "8859_1";

  private final static String DEF_PASSWORD_PROP_PFX = "password";
  private final static String DEF_HEADER = "com.mchange.util.impl.HexAsciiMD5PropertiesPasswordManager data";

  private final static boolean DEBUG = true;

  SyncedProperties props;
  String           pfx;
  MessageDigest    md;

  public HexAsciiMD5PropertiesPasswordManager(File propsFile, String pfx, String[] header) throws IOException
    {this(new SyncedProperties(propsFile, header), pfx);}

  public HexAsciiMD5PropertiesPasswordManager(File propsFile, String pfx, String header) throws IOException
    {this(new SyncedProperties(propsFile, header), pfx);}

  public HexAsciiMD5PropertiesPasswordManager(File propsFile) throws IOException
    {this(propsFile, DEF_PASSWORD_PROP_PFX, DEF_HEADER);}

  private HexAsciiMD5PropertiesPasswordManager(SyncedProperties sp, String pfx) throws IOException
    {
      try
	{
	  this.props = sp;
	  this.pfx   = pfx;
	  this.md    = MessageDigest.getInstance(DIGEST_ALGORITHM);
	}
      catch (NoSuchAlgorithmException e)
	{throw new InternalError(DIGEST_ALGORITHM + " is not supported???");}
    }

  public synchronized boolean validate(String username, String password) throws IOException
    {
      try
	{
	  String hStr = props.getProperty(pfx != null ? pfx + '.' + username : username);
	  byte[] fileAuth     = ByteUtils.fromHexAscii(hStr);
	  byte[] incomingAuth = md.digest(password.getBytes(PASSWORD_ENCODING));
	  return Arrays.equals(fileAuth, incomingAuth);
	}
      catch (NumberFormatException e)
	{throw new IOException("Password file corrupted! [contains invalid hex ascii string]");}
      catch (UnsupportedEncodingException e)
	{
	  if (DEBUG) e.printStackTrace();
	  throw new InternalError(PASSWORD_ENCODING + "is an unsupported encoding???");
	}
    }

  public synchronized boolean updatePassword(String username, String oldPassword, String newPassword) throws IOException
    {
      if (!validate(username, oldPassword)) return false;
      props.put(pfx + '.' + username, ByteUtils.toHexAscii(md.digest(newPassword.getBytes(PASSWORD_ENCODING))));
      return true;
    }
}





