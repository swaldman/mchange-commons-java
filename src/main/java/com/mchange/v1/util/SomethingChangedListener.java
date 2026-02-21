package com.mchange.v1.util;

import java.util.EventListener;

public interface SomethingChangedListener extends EventListener
{
    public void somethingChanged(SomethingChangedEvent evt);
}
