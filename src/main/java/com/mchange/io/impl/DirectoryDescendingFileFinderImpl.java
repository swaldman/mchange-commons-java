package com.mchange.io.impl;

import java.io.*;
import java.util.*;
import com.mchange.io.*;

/**
 * @deprecated use com.mchange.v2.io.DirectoryDescentUtils
 */
public class DirectoryDescendingFileFinderImpl implements IOEnumeration, FileEnumeration
{
    private final static Object dummy = new Object();

    Hashtable markedDirex  = new Hashtable();

    Stack direx   = new Stack();
    Stack files   = new Stack();

    FilenameFilter filter;

    boolean canonical;

    public DirectoryDescendingFileFinderImpl(File root, FilenameFilter filter, boolean canonical) 
	throws IOException
    {
	if (!root.isDirectory())
	    throw new IllegalArgumentException(root.getName() + " is not a directory.");
	this.filter    = filter;
	this.canonical = canonical;
	blossomDirectory(root);
	while (files.empty() && !direx.empty())
	    blossomDirectory((File) direx.pop());
    }

    public DirectoryDescendingFileFinderImpl(File root) throws IOException
    {this(root, null, false);}

    public boolean hasMoreFiles()
    {return !files.empty();}

    public File nextFile() throws IOException
    {
	if (files.empty()) throw new NoSuchElementException();
	File out = (File) files.pop();
	while (files.empty() && !direx.empty())
	    blossomDirectory((File) direx.pop());
	return out;
    }

    public boolean hasMoreElements()
    {return hasMoreFiles();}

    public Object nextElement() throws IOException
    {return nextFile();}

    private void blossomDirectory(File dir) throws IOException
    {
	//System.out.println(">> blossomDirectory() on " + dir.getPath());
	String canonicalPath = dir.getCanonicalPath();
	String[] listing = (filter == null ? dir.list() : dir.list(filter));
	for (int i = listing.length; --i >= 0; )
	    {
		//System.out.println(">> listing: " + listing[i]);
		if (filter == null || filter.accept(dir, listing[i]))
		    {
			String name = (canonical ? canonicalPath : dir.getPath()) + File.separator + listing[i];
			File file = new File(name);
			//System.out.println(">> parent: " + dir.getName());
			//System.out.println(">> created file: " + file.getPath());
			if (file.isFile()) files.push(file);
			else //dir
			    {
				if (!markedDirex.containsKey(file.getCanonicalPath()))
				    direx.push(file);
			    }
		    }
	    }
	markedDirex.put(canonicalPath, dummy);
    }

    //the only difference between this and blossomDirectory()
    //above is that the root directory path is not prepended to
    //files and directories... we want the files outputted to
    //be specified relative to the root in the constructor.
    //   private void blossomRoot(File root) throws IOException
    //     {
    //       String canonicalPath = root.getCanonicalPath();
    //       String[] listing = (filter == null ? root.list() : root.list(filter));
    //       for (int i = listing.length; --i >= 0; )
    // 	{
    // 	  if (filter == null || filter.accept(root, listing[i]))
    // 	    {
    // 	      File file = new File(listing[i]); //here is the difference
    // 	      if (file.isFile()) files.push(file);
    // 	      else //dir
    // 		{
    // 		  if (!markedDirex.containsKey(file.getCanonicalPath()))
    // 		    direx.push(file);
    // 		}
    // 	    }
    // 	}
    //       markedDirex.put(canonicalPath, dummy);
    //     }

    public static void main(String[] argv)
    {
	try
	    {
		File root = new File(argv[0]);
		FileEnumeration files = new DirectoryDescendingFileFinderImpl(root);
		while (files.hasMoreFiles())
		    System.out.println(files.nextFile().getAbsolutePath());
	    }
	catch (Exception e)
	    {e.printStackTrace();}
    }
}

