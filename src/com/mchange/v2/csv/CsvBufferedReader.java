package com.mchange.v2.csv;

import java.io.*;

public class CsvBufferedReader extends BufferedReader 
{
    private BufferedReader inner;

    public CsvBufferedReader( BufferedReader inner ) { 
	super( inner );
    	this.inner = inner; 
    }

    @Override
    public String readLine() throws IOException
    { 
	try { return FastCsvUtils.csvReadLine( inner ); }
	catch ( MalformedCsvException e )
	    { throw new IOException("Badly formatted CSV file.", e); }
    } 
    
    public String[] readSplitLine() throws IOException, MalformedCsvException
    {
	String line = this.readLine();
	return line == null ? null : FastCsvUtils.csvSplitLine( line );
    }

    // simple delegations
    @Override
    public int read() throws IOException                                 { return inner.read(); }
    @Override
    public int read(char[] cbuf, int off, int len) throws IOException    { return inner.read( cbuf, off, len ); }
    @Override
    public long skip(long n) throws IOException                          { return inner.skip(n); }
    @Override
    public boolean ready() throws IOException                            { return inner.ready(); }
    @Override
    public boolean markSupported()                                       { return inner.markSupported(); }
    @Override
    public void mark(int readAheadLimit) throws IOException              { inner.mark( readAheadLimit ); }
    @Override
    public void reset() throws IOException                               { inner.reset(); }
    @Override
    public void close() throws IOException                               { inner.close(); }
    @Override
    public java.util.stream.Stream<String> lines()                       { throw new UnsupportedOperationException("lines() not yet implemented for CsvBufferedReader!"); }
}
