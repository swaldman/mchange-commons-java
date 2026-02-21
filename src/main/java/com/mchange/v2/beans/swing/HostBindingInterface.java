package com.mchange.v2.beans.swing;

import java.beans.*;

interface HostBindingInterface
{
    public void syncToValue( PropertyEditor editor, Object value );
    public void addUserModificationListeners();
    public Object fetchUserModification( PropertyEditor editor, Object oldValue );
    public void alertErroneousInput();
}
