package com.mchange.v2.cfg;

import java.util.AbstractMap;

/**
 *  A configuration entry -- the key that was consulted and the value found under it -- together
 *  with the means to turn that value into the object it names.
 *
 *  <p>Separating the two lets a caller report <i>what was configured</i> even when resolution or
 *  the resolved object's own behaviour is what went wrong. A message that names the key and the
 *  value tells a deployer where to go; one that names only the interface does not.</p>
 */
public abstract class ResolvingEntry<T> extends AbstractMap.SimpleImmutableEntry<String,String>
{
    public ResolvingEntry(String key, String value)
    { super( key, value ); }

    public abstract T resolve() throws Exception;
}
