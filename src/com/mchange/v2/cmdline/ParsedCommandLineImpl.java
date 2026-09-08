package com.mchange.v2.cmdline;

import java.util.*;

class ParsedCommandLineImpl implements ParsedCommandLine
{
    String[] argv; 
    String   switchPrefix;

    String[] unswitchedArgs;

    //we are relying upon the fact that
    //HashMaps are null-accepting collections
    HashMap<String,String>  foundSwitches = new HashMap<String,String>(); 
    
    ParsedCommandLineImpl(String[] argv, 
			  String switchPrefix, 
			  String[] validSwitches,
			  String[] requiredSwitches,
			  String[] argSwitches)
	throws BadCommandLineException
    {
	this.argv = argv;
	this.switchPrefix = switchPrefix;

	List<String> unswitchedArgsList = new LinkedList<String>();
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

    @Override
    public String getSwitchPrefix()
    { return switchPrefix; }

    @Override
    public String[] getRawArgs()
    { return (String[]) argv.clone(); }
    
    @Override
    public boolean includesSwitch(String sw)
    { return foundSwitches.containsKey( sw ); }

    @Override
    public String getSwitchArg(String sw)
    { return (String) foundSwitches.get(sw); }

    @Override
    public String[] getUnswitchedArgs()
    { return (String[]) unswitchedArgs.clone(); }

    private static boolean contains(String string, String[] list)
    {
	for (int i = list.length; --i >= 0;)
	    if (list[i].equals(string)) return true;
	return false;
    }
    
}






