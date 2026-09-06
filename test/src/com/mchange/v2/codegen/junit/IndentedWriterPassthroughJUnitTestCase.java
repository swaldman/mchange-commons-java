package com.mchange.v2.codegen.junit;

import java.io.StringWriter;
import java.sql.ResultSet;

import junit.framework.TestCase;

import com.mchange.v2.codegen.CodegenUtils;
import com.mchange.v2.codegen.intfc.DelegatorGenerator;
import com.mchange.v2.io.IndentedWriter;

/**
 *  CodegenUtils.toIndentedWriter(...) and DelegatorGenerator used to traffic in the
 *  deprecated com.mchange.v2.codegen.IndentedWriter, an empty subclass of
 *  com.mchange.v2.io.IndentedWriter. They now use the io class directly.
 *
 *  The instanceof short circuit in toIndentedWriter exists to avoid re-wrapping a
 *  writer that is already indented. While it named the codegen subclass, an
 *  io.IndentedWriter failed the test and got wrapped -- and because
 *  IndentedWriter.write(...) does not indent (only print/println do), wrapping
 *  discarded the caller's indent level rather than compounding it.
 */
public class IndentedWriterPassthroughJUnitTestCase extends TestCase
{
    /**
     *  The deprecated codegen class must remain assignable to the io class, or the
     *  migration silently stopped being a widening.
     */
    @SuppressWarnings("deprecation")
    public void testDeprecatedCodegenWriterIsStillAnIoWriter()
    {
	StringWriter sw = new StringWriter();
	com.mchange.v2.codegen.IndentedWriter legacy = new com.mchange.v2.codegen.IndentedWriter(sw);
	assertTrue("codegen.IndentedWriter should still be an io.IndentedWriter",
		   legacy instanceof IndentedWriter);
	assertSame("an already indented legacy writer should pass through",
		   legacy, CodegenUtils.toIndentedWriter(legacy));
    }

    public void testAlreadyIndentedWriterPassesThrough()
    {
	StringWriter sw = new StringWriter();
	IndentedWriter iw = new IndentedWriter(sw);
	assertSame("an io.IndentedWriter should be returned as is, not re-wrapped",
		   iw, CodegenUtils.toIndentedWriter(iw));
    }

    public void testPlainWriterIsWrapped()
    {
	StringWriter sw = new StringWriter();
	Object wrapped = CodegenUtils.toIndentedWriter(sw);
	assertNotNull("a plain Writer should be wrapped", wrapped);
	assertNotSame("a plain Writer should be wrapped, not returned", sw, wrapped);
	assertTrue("the wrapper should be an io.IndentedWriter", wrapped instanceof IndentedWriter);
    }

    /**
     *  The caller's indent level must survive. Re-wrapping loses it, because the
     *  wrapper reaches the underlying writer through write(...), which does not indent.
     */
    public void testCallerIndentLevelIsHonored() throws Exception
    {
	StringWriter sw = new StringWriter();
	IndentedWriter outer = new IndentedWriter(sw);
	outer.upIndent();

	IndentedWriter used = (IndentedWriter) CodegenUtils.toIndentedWriter(outer);
	used.upIndent();
	used.println("x");
	used.flush();

	assertEquals("one level from the caller plus one from the generator",
		     2, leadingTabs(sw.toString()));
    }

    /**
     *  Generating through a plain Writer and through an io.IndentedWriter at indent
     *  zero must produce the same text. Pre-migration the second path was wrapped,
     *  so this is the equivalence the migration is presumed to have established.
     */
    public void testGeneratedOutputIsIndependentOfWriterFlavor() throws Exception
    {
	StringWriter plain = new StringWriter();
	new DelegatorGenerator().writeDelegator(ResultSet.class, "test.gen.PlainDelegator", plain);

	StringWriter backing = new StringWriter();
	new DelegatorGenerator().writeDelegator(ResultSet.class, "test.gen.PlainDelegator",
						new IndentedWriter(backing));

	assertEquals("generated text should not depend on the writer flavor",
		     stripBanner(plain.toString()), stripBanner(backing.toString()));
    }

    /**
     *  Keeps the comparison above honest: the generated text must be substantial and
     *  actually indented, so an empty-vs-empty comparison cannot pass for equivalence.
     */
    public void testGeneratedOutputIsSubstantial() throws Exception
    {
	StringWriter sw = new StringWriter();
	new DelegatorGenerator().writeDelegator(ResultSet.class, "test.gen.PlainDelegator", sw);
	String generated = stripBanner(sw.toString());

	assertTrue("generated source should be substantial", generated.length() > 1000);
	assertTrue("generated source should declare the class",
		   generated.indexOf("class PlainDelegator") >= 0);
	assertTrue("generated source should delegate a known ResultSet method",
		   generated.indexOf("getMetaData") >= 0);

	// IndentedWriter indents with tabs by default, and nests them for method bodies.
	assertTrue("generated source should be tab indented",
		   generated.indexOf("\n\tpublic ") >= 0);
	assertTrue("method bodies should be indented a further level",
		   generated.indexOf("\n\t\treturn inner.") >= 0);
    }

    /** Drops the generated banner, whose timestamp differs run to run. */
    private static String stripBanner(String s)
    {
	StringBuffer out = new StringBuffer();
	String[] lines = s.split("\n", -1);
	for (int i = 0; i < lines.length; ++i)
	    if (! lines[i].trim().startsWith("*"))
		out.append(lines[i]).append('\n');
	return out.toString();
    }

    private static int leadingTabs(String s)
    {
	int n = 0;
	while (n < s.length() && s.charAt(n) == '\t') ++n;
	return n;
    }
}
