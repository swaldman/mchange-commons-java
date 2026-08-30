package com.mchange.v1.util;

import java.util.EventObject;

public class SomethingChangedEvent extends EventObject
{
    public SomethingChangedEvent(Object source)
    { super( source ); }
}
