package com.mchange.v2.coalesce;

import java.util.*;
import java.lang.ref.WeakReference;

final class StrongEqualsCoalescer extends AbstractStrongCoalescer 
    implements Coalescer
{
    StrongEqualsCoalescer()
    { super( new HashMap() ); }
}



