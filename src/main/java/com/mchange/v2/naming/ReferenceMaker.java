package com.mchange.v2.naming;

import javax.naming.Reference;
import javax.naming.NamingException;

public interface ReferenceMaker
{
    public Reference createReference( Object o )
	throws NamingException;
}
