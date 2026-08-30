package com.mchange.v2.coalesce;

import java.util.*;
import java.lang.ref.WeakReference;
import com.mchange.v1.identicator.IdWeakHashMap;

final class WeakCcCoalescer extends AbstractWeakCoalescer implements Coalescer
{
    WeakCcCoalescer(CoalesceChecker cc)
    { super( new IdWeakHashMap( new CoalesceIdenticator( cc ) ) ); }
}



