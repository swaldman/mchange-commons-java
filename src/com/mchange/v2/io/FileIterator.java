package com.mchange.v2.io;

import java.io.File;
import java.io.IOException;
import java.util.NoSuchElementException;
import com.mchange.v1.util.UIterator;

public interface FileIterator extends UIterator
{
    public File nextFile() throws IOException;

    @Override
    public boolean hasNext() throws IOException;
    @Override
    public Object next() throws IOException;
    @Override
    public void remove() throws IOException;
    @Override
    public void close() throws IOException;

    public final static FileIterator EMPTY_FILE_ITERATOR = new FileIterator()
    {
	@Override
	public File nextFile() {throw new NoSuchElementException();}
	@Override
	public boolean hasNext() {return false;}
	@Override
	public Object next() {throw new NoSuchElementException();}
	@Override
	public void remove() {throw new IllegalStateException();}
	@Override
	public void close() {}
    };
}
