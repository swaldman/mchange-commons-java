package com.mchange.util.impl;

import com.mchange.util.*;

public class CircularListQueue implements Queue, Cloneable
{
  CircularList list;

  public int     size()               {return list.size();}
  public boolean hasMoreElements()    {return list.size() > 0;}
  public void    enqueue(Object obj)  {list.appendElement(obj);}
  public Object  peek()               {return list.getFirstElement();}
  public Object  dequeue()  
    {
      Object out = list.getFirstElement();
      list.removeFirstElement();
      return out;
    }

  /**
   * Returns a <B><I>shallow</I></B> copy. The queue is
   * cloned, but not the elements within it.
   */
  public Object clone()
    {return new CircularListQueue((CircularList) list.clone());}

  public CircularListQueue () 
    {this.list = new CircularList();}

  private CircularListQueue(CircularList list)
    {this.list = list;}
}
