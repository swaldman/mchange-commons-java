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

public final class CommandLineUtils
{
    /**
     * "Parses" a command line by making use several conventions:
     * <UL>
     * <LI> Certain arguments are considered "switches", by virtue
     *      of being prefixed with some string, usually "-", "/", or "--"
     * <LI> Switches may have arguments associated with them. This implementation
     *      permits only a single argument per switch
     * <LI> Switch arguments are determined via two conventions:
     *      <OL>
     *      <LI> If a switch is of the form "--switch=value" (where "--" is
     *           set as the switch prefix), value is the switches argument.
     *      <LI> If a switch is not of this form (simply "--switch"), then the
     *           following item on the command line is considered the switch's
     *           argument if and only if
     *           <OL>
     *           <LI> the argSwitches array contains the switch, and
     *           <LI> the next item on the command line is not itself a switch
     *           </OL>
     *      </OL>
     * </UL>
     *
     * @param argv the entire list of arguments, usually the argument to a main function
     * @param switchPrefix the string which separates "switches" from regular command line args.
     *        Must be non-null
     * @param validSwitches a list of all the switches permissible for this command line.
     *        If non-null, an UnexpectedSwitchException will be thrown if a switch not
     *        in this list is encountered. Use null to accept any switches.
     * @param requiredSwitches a list of all the switches required by this command line.
     *        If non-null, an MissingSwitchException will be thrown if a switch
     *        in this list is not present. Use null if no switches should be considered required.
     * @param argSwitches a list of switches that should have an argument associated with them
     *        If non-null, an MissingSwitchArgumentException will be thrown if a switch
     *        in this list has no argument is not present. Use null if no switches should 
     *        be considered to require arguments. However, this parameter is required if 
     *        distinct items on a command line should be considered arguments to preceding
     *        items. (For example, "f" must be an argSwitch for "-f myfile.txt" to be parsed
     *        as switch and argument, but argSwitches is not required to parse "--file=myfile.txt"
     */
    public static ParsedCommandLine parse(String[] argv, 
				   String switchPrefix, 
				   String[] validSwitches,
				   String[] requiredSwitches,
				   String[] argSwitches)
	throws BadCommandLineException
    {
	return new ParsedCommandLineImpl( argv,
					  switchPrefix,
					  validSwitches,
					  requiredSwitches,
					  argSwitches );
    }

    private CommandLineUtils()
    {}
}




