package com.mchange.v2.io;

import java.io.*;

public final class FileUtils
{
    public static File findRelativeToParent(File parentDir, File file) throws IOException
    {
	String parentPath = parentDir.getPath();
	String filePath = file.getPath();
	if (! filePath.startsWith( parentPath ) )
	    throw new IllegalArgumentException( filePath + " is not a child of " + parentPath + " [no transformations or canonicalizations tried]" );
	String maybeRelative = filePath.substring( parentPath.length() );
	File out = new File( maybeRelative );
	if ( out.isAbsolute() )
	    out = new File( out.getPath().substring(1) );
	return out;
    }

    public static long diskSpaceUsed( File maybeDir ) throws IOException
    {
	long sum = 0;
	for (FileIterator ff = DirectoryDescentUtils.depthFirstEagerDescent( maybeDir ); ff.hasNext();)
	    {
		File addMe = ff.nextFile();
		//System.err.println("diskSpacedUsed() -- checking: " + addMe);
		if (! addMe.isFile())
		    continue;

		sum += addMe.length();
	    }
	return sum;
    }

    public static void touchExisting( File file ) throws IOException
    {
	if ( file.exists() )
	    unguardedTouch( file );
    }

    public static void touch( File file ) throws IOException
    {
	if (! file.exists() )
	    createEmpty( file );
	unguardedTouch( file );
    }

    public static void createEmpty( File file ) throws IOException
    {
	RandomAccessFile raf = null;
	try
	    {
		raf = new RandomAccessFile( file, "rws" );
		raf.setLength( 0 );
	    }
	finally
	    {
		try { if (raf != null) raf.close(); }
		catch ( IOException e )
		    { e.printStackTrace(); }
	    }
    }

    private static void unguardedTouch( File file ) throws IOException
    { file.setLastModified( System.currentTimeMillis() ); }	

    private FileUtils()
    {}
}
