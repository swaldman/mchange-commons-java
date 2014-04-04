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

package com.mchange.v2.holders;

import java.io.*;
import com.mchange.v2.ser.UnsupportedVersionException;

public final class ChangeNotifyingSynchronizedIntHolder implements ThreadSafeIntHolder, Serializable
{
    transient int value;
    transient boolean notify_all;

    public ChangeNotifyingSynchronizedIntHolder( int value, boolean notify_all )
    { 
	this.value = value; 
	this.notify_all = notify_all;
    }

    public ChangeNotifyingSynchronizedIntHolder()
    { this(0, true); }

    public synchronized int getValue()
    { return value; }

    public synchronized void setValue(int value)
    {
	if (value != this.value)
	    {
		this.value = value; 
		doNotify();
	    }
    }

    public synchronized void increment()
    { 
	++value; 
	doNotify();
    }

    public synchronized void decrement()
    { 
	--value; 
	doNotify();
    }

    //must be called from a sync'ed block...
    private void doNotify()
    {
	if (notify_all) this.notifyAll();
	else this.notify();
    }

    //Serialization
    static final long serialVersionUID = 1; //override to take control of versioning
    private final static short VERSION = 0x0001;
    
    private void writeObject(ObjectOutputStream out) throws IOException
    {
	out.writeShort(VERSION);
	out.writeInt(value);
	out.writeBoolean(notify_all);
    }
    
    private void readObject(ObjectInputStream in) throws IOException
    {
	short version = in.readShort();
	switch (version)
	    {
	    case 0x0001:
		this.value = in.readInt();
		this.notify_all = in.readBoolean();
		break;
	    default:
		throw new UnsupportedVersionException(this, version);
	    }
    }
}
