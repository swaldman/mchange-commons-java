package com.mchange.v2.coalesce;

import java.util.*;
import com.mchange.v1.identicator.IdHashMap;

final class StrongCcCoalescer extends AbstractStrongCoalescer
    implements Coalescer
{
    StrongCcCoalescer( CoalesceChecker cc )
    { super( new IdHashMap( new CoalesceIdenticator( cc ) ) ); }
}



