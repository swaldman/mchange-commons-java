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

package com.mchange.v2.cmdline;

import java.util.*;

class ParsedCommandLineImpl implements ParsedCommandLine
{
    String[] argv; 
    String   switchPrefix;

    String[] unswitchedArgs;

    //we are relying upon the fact that
    //HashMaps are null-accepting collections
    HashMap  foundSwitches = new HashMap(); 
    
    ParsedCommandLineImpl(String[] argv, 
			  String switchPrefix, 
			  String[] validSwitches,
			  String[] requiredSwitches,
			  String[] argSwitches)
	throws BadCommandLineException
    {
	this.argv = argv;
	this.switchPrefix = switchPrefix;

	List unswitchedArgsList = new LinkedList();
	int sp_len = switchPrefix.length();

	for (int i = 0; i < argv.length; ++i)
	    {
		if (argv[i].startsWith(switchPrefix)) //okay, this is a switch
		{
		    String sw = argv[i].substring( sp_len );
		    String arg = null;

		    int eq = sw.indexOf('=');
		    if ( eq >= 0 ) //equals convention
			{
			    arg = sw.substring( eq + 1 );
			    sw = sw.substring( 0, eq );
			}
		    else if ( contains( sw, argSwitches ) ) //we expect an argument, next arg convention
			{
			    if (i < argv.length - 1 && !argv[i + 1].startsWith( switchPrefix) )
				arg = argv[++i];
			}

		    if (validSwitches != null && ! contains( sw, validSwitches ) )
			throw new UnexpectedSwitchException("Unexpected Switch: " + sw, sw);
		    if (argSwitches != null && arg != null && ! contains( sw, argSwitches ))
			throw new UnexpectedSwitchArgumentException("Switch \"" + sw +
								    "\" should not have an " +
								    "argument. Argument \"" +
								    arg + "\" found.", sw, arg);
		    foundSwitches.put( sw, arg );
		}
		else
		    unswitchedArgsList.add( argv[i] );
	    }

	if (requiredSwitches != null)
	    {
		for (int i = 0; i < requiredSwitches.length; ++i)
		    if (! foundSwitches.containsKey( requiredSwitches[i] ))
			throw new MissingSwitchException("Required switch \"" + requiredSwitches[i] +
							 "\" not found.", requiredSwitches[i]);
	    }

	unswitchedArgs = new String[ unswitchedArgsList.size() ];
	unswitchedArgsList.toArray( unswitchedArgs );
    }

    public String getSwitchPrefix()
    { return switchPrefix; }

    public String[] getRawArgs()
    { return (String[]) argv.clone(); }
    
    public boolean includesSwitch(String sw)
    { return foundSwitches.containsKey( sw ); }

    public String getSwitchArg(String sw)
    { return (String) foundSwitches.get(sw); }

    public String[] getUnswitchedArgs()
    { return (String[]) unswitchedArgs.clone(); }

    private static boolean contains(String string, String[] list)
    {
	for (int i = list.length; --i >= 0;)
	    if (list[i].equals(string)) return true;
	return false;
    }
    
}






