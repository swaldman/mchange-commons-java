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


package com.mchange.util.impl;

import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.io.IOException;
import com.mchange.util.MessageLogger;
import com.mchange.util.FailSuppressedMessageLogger;

public class FSMessageLoggerAdapter implements FailSuppressedMessageLogger
{
  MessageLogger inner;
  List          failures = null;

  public FSMessageLoggerAdapter(MessageLogger wrapMe)
    {this.inner = wrapMe;}

  /* we presume accesses to inner are already thread-safe */
  /* and do not synchronize.                              */
  public void log(String message)
    {
      try {inner.log(message);}
      catch (IOException e)
	{addFailure(e);}
    }

  public void log(Throwable t, String message)
    {
      try {inner.log(t, message);}
      catch (IOException e)
	{addFailure(e);}
    }

  public synchronized Iterator getFailures()
    {
      if (inner instanceof FailSuppressedMessageLogger)
	return ((FailSuppressedMessageLogger) inner).getFailures();
      else return (failures != null ? failures.iterator() : null);
    }

  public synchronized void clearFailures()
    {
      if (inner instanceof FailSuppressedMessageLogger)
	((FailSuppressedMessageLogger) inner).clearFailures();
      else failures = null;
    }
  
  private synchronized void addFailure(IOException e)
    {
      if (failures == null)
	failures = new LinkedList();
      failures.add(e);
    }
}
