package com.mchange.util;

import java.util.*;

public interface Queue extends Cloneable //clones are shallow copies
{
  public void enqueue(Object o);
  public Object dequeue() throws NoSuchElementException;
  public Object peek() throws NoSuchElementException;
  public boolean hasMoreElements();
  public int size();
  public Object clone();
}
