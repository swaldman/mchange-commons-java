package com.mchange.v2.ser;

import java.io.IOException;
import java.io.Serializable;

import com.mchange.v2.cfg.PropertiesConfig;

/*
 * We add a version that accept config, because we've had to lock down dereferencing of objects,
 * as previous versions too easily e.g. loaded remote code on resolving references.
 *
 * Now, the no-arg getObject() should default to restrictive (although they may use config provided 
 * as System properties to loosen restrictions. Implementations of getObject(pcfg) can consult the cfg
 * to decide whether to loosen restrictions, as well as System properties.
 */
public interface IndirectlySerialized extends Serializable
{
    public Object getObject() throws ClassNotFoundException, IOException;
    public Object getObject( PropertiesConfig pcfg ) throws ClassNotFoundException, IOException;
}

