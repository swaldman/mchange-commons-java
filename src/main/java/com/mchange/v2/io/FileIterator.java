package com.mchange.v2.io;

import java.io.File;
import java.io.IOException;
import java.util.NoSuchElementException;
import com.mchange.v1.util.UIterator;

public interface FileIterator extends UIterator
{
    public File nextFile() throws IOException;

    public boolean hasNext() throws IOException;
    public Object next() throws IOException;
    public void remove() throws IOException;
    public void close() throws IOException;

    public final static FileIterator EMPTY_FILE_ITERATOR = new FileIterator()
    {
	public File nextFile() {throw new NoSuchElementException();}
	public boolean hasNext() {return false;}
	public Object next() {throw new NoSuchElementException();}
	public void remove() {throw new IllegalStateException();}
	public void close() {}
    };
}
