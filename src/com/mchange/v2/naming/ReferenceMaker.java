package com.mchange.v2.naming;

import javax.naming.Reference;
import javax.naming.NamingException;
import com.mchange.v2.cfg.PropertiesConfig;

public interface ReferenceMaker
{
    public Reference createReference( Object o, PropertiesConfig pcfg )
	throws NamingException;
    public Reference createReference( Object o )
	throws NamingException;
}
