/*
 * Distributed as part of mchange-commons-java v.0.2.4
 *
 * Copyright (C) 2013 Machinery For Change, Inc.
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

package com.mchange.v2.log;

import java.util.*;

public final class MLevel
{
    public final static MLevel ALL;
    public final static MLevel CONFIG;
    public final static MLevel FINE;
    public final static MLevel FINER;
    public final static MLevel FINEST;
    public final static MLevel INFO;
    public final static MLevel OFF;
    public final static MLevel SEVERE;
    public final static MLevel WARNING;

    private final static Map integersToMLevels;
    private final static Map namesToMLevels;

    public static MLevel fromIntValue(int intval)
    { return (MLevel) integersToMLevels.get( new Integer( intval ) ); }

    public static MLevel fromSeverity(String name)
    { return (MLevel) namesToMLevels.get( name ); }

    static
    {
	Class lvlClass;
	boolean jdk14api;  //not just jdk14 -- it is possible for the api to be present with older vms
	try
	    { 
		lvlClass = Class.forName( "java.util.logging.Level" ); 
		jdk14api = true;
	    }
	catch (ClassNotFoundException e )
	    { 
		lvlClass = null;
		jdk14api = false; 
	    }

	MLevel all;
	MLevel config;
	MLevel fine;
	MLevel finer;
	MLevel finest;
	MLevel info;
	MLevel off;
	MLevel severe;
	MLevel warning;

	try
	    { 
		// numeric values match the intvalues from java.util.logging.Level
		all = new MLevel( (jdk14api ? lvlClass.getField("ALL").get(null) : null), Integer.MIN_VALUE, "ALL" );
		config = new MLevel( (jdk14api ? lvlClass.getField("CONFIG").get(null) : null), 700, "CONFIG" );
		fine = new MLevel( (jdk14api ? lvlClass.getField("FINE").get(null) : null), 500, "FINE" );
		finer = new MLevel( (jdk14api ? lvlClass.getField("FINER").get(null) : null), 400, "FINER" );
		finest = new MLevel( (jdk14api ? lvlClass.getField("FINEST").get(null) : null), 300, "FINEST" );
		info = new MLevel( (jdk14api ? lvlClass.getField("INFO").get(null) : null), 800, "INFO" );
		off = new MLevel( (jdk14api ? lvlClass.getField("OFF").get(null) : null), Integer.MAX_VALUE, "OFF" );
		severe = new MLevel( (jdk14api ? lvlClass.getField("SEVERE").get(null) : null), 900, "SEVERE" );
		warning = new MLevel( (jdk14api ? lvlClass.getField("WARNING").get(null) : null), 1000, "WARNING" );
	    }
	catch ( Exception e )
	    { 
		e.printStackTrace();
		throw new InternalError("Huh? java.util.logging.Level is here, but not its expected public fields?");
	    }

	ALL = all;
	CONFIG = config;
	FINE = fine;
	FINER = finer;
	FINEST = finest;
	INFO = info;
	OFF = off;
	SEVERE = severe;
	WARNING = warning;

	Map tmp = new HashMap();
	tmp.put( new Integer(all.intValue()), all);
	tmp.put( new Integer(config.intValue()), config);
	tmp.put( new Integer(fine.intValue()), fine);
	tmp.put( new Integer(finer.intValue()), finer);
	tmp.put( new Integer(finest.intValue()), finest);
	tmp.put( new Integer(info.intValue()), info);
	tmp.put( new Integer(off.intValue()), off);
	tmp.put( new Integer(severe.intValue()), severe);
	tmp.put( new Integer(warning.intValue()), warning);

	integersToMLevels = Collections.unmodifiableMap( tmp );

	tmp = new HashMap();
	tmp.put( all.getSeverity(), all);
	tmp.put( config.getSeverity(), config);
	tmp.put( fine.getSeverity(), fine);
	tmp.put( finer.getSeverity(), finer);
	tmp.put( finest.getSeverity(), finest);
	tmp.put( info.getSeverity(), info);
	tmp.put( off.getSeverity(), off);
	tmp.put( severe.getSeverity(), severe);
	tmp.put( warning.getSeverity(), warning);

	namesToMLevels = Collections.unmodifiableMap( tmp );
    }

    Object level;
    int    intval;
    String lvlstring;

    public int intValue()
    { return intval; }

    public Object asJdk14Level()
    { return level; }

    public String getSeverity()
    { return lvlstring; }

    public String toString()
    { return this.getClass().getName() + this.getLineHeader(); }

    public String getLineHeader()
    { return "[" + lvlstring + ']';}

    public boolean isLoggable( MLevel filterLevel )
    { return this.intval >= filterLevel.intval; }

    private MLevel(Object level, int intval, String lvlstring)
    {
	this.level = level;
	this.intval = intval;
	this.lvlstring = lvlstring;
    }
}
