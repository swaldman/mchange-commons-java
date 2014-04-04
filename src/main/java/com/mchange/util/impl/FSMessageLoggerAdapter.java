/*
 * Distributed as part of mchange-commons-java 0.2.7
 *
 * Copyright (C) 2014 Machinery For Change, Inc.
 *
 * Author: Steve Waldman <swaldman@mchange.com>
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of EITHER:
 *
 *     1) The GNU Lesser General Public License (LGPL), version 2.1, as 
 *        published by the Free Software Foundation
 *
 * OR
 *
 *     2) The Eclipse Public License (EPL), version 1.0
 *
 * You may choose which license to accept if you wish to redistribute
 * or modify this work. You may offer derivatives of this work
 * under the license you have chosen, or you may provide the same
 * choice of license which you have been offered here.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * You should have received copies of both LGPL v2.1 and EPL v1.0
 * along with this software; see the files LICENSE-EPL and LICENSE-LGPL.
 * If not, the text of these licenses are currently available at
 *
 * LGPL v2.1: http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 *  EPL v1.0: http://www.eclipse.org/org/documents/epl-v10.php 
 * 
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
