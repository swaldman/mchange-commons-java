package com.mchange.v2.csv;

import java.io.*;

class CsvBufferedReader extends BufferedReader 
{
    private BufferedReader inner;

    public CsvBufferedReader( BufferedReader inner ) { 
	super( inner );
    	this.inner = inner; 
    }

    @Override
    public String readLine() throws IOException
    { return FastCsvUtils.csvReadLine( inner ); }
    
    public String[] readSplitLine() throws IOException, MalformedCsvException
    {
	String line = this.readLine();
	return FastCsvUtils.splitRecord( line );
    }

    // simple delegations
    public int read() throws IOException                                 { return inner.read(); }
    public int read(char[] cbuf, int off, int len) throws IOException    { return inner.read( cbuf, off, len ); }
    public long skip(long n) throws IOException                          { return inner.skip(n); }
    public boolean ready() throws IOException                            { return inner.ready(); }
    public boolean markSupported()                                       { return inner.markSupported(); }
    public void mark(int readAheadLimit) throws IOException              { inner.mark( readAheadLimit ); }
    public void reset() throws IOException                               { inner.reset(); }
    public void close() throws IOException                               { inner.close(); }
    public java.util.stream.Stream<String> lines()                       { throw new UnsupportedOperationException("lines() not yet implemented for CsvBufferedReader!"); }
}
