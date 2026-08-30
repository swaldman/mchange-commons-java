package com.mchange.v2.coalesce;

import java.util.*;
import java.lang.ref.WeakReference;

class WeakEqualsCoalescer extends AbstractWeakCoalescer
{
    WeakEqualsCoalescer()
    { super( new WeakHashMap() ); }
}



