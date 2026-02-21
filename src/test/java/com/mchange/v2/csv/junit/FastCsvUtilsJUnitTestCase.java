package com.mchange.v2.csv.junit;

import junit.framework.TestCase;
import com.mchange.v2.csv.FastCsvUtils;

public class FastCsvUtilsJUnitTestCase extends TestCase
{
    final static String[] SIMPLE = {"these", "are", "some", "simple", "strings"};
    final static String[] HARDER = {"th,ese", "a\"re", "some", "\"harder\"", "s,t,r,i,n,g,s"};
    final static String[] MULTILINE = {"th\nese", "a\r\nre", "just", "w\"\rei\nrd\"", "s,t,r,i,n,g,s"};
    final static String[] BLANKS = {"this","array","contains","","","","three","blanks"};

    private void assertAllEqual(String msg, Object[] expected, Object[] found)
    {
	assertEquals(msg + " [array length mismatch]", expected.length, found.length);
	for (int i = 0, len = expected.length; i < len; ++i)
	    assertEquals(msg, expected[i],found[i]);
    }

    private void printArray(String[] chk)
    {
	for (int i = 0, len = chk.length; i < len; ++i)
	    System.err.println(chk[i]);
    }

    public void testSplitLine()
    {
	try
	    {
		String[] cannedParse1 = FastCsvUtils.splitRecord("these,are,some,simple,strings");
		assertAllEqual("SIMPLE (1): Canned parse should match array.", SIMPLE, cannedParse1);

		String[] cannedParse2 = FastCsvUtils.splitRecord("\"these\",\"are\",\"some\",\"simple\",\"strings");
		assertAllEqual("SIMPLE (2): Canned parse with some quoted words should match array.", SIMPLE, cannedParse2);

		String[] cannedParse3 = FastCsvUtils.splitRecord("\"these\"  ,  are\t, \"some\"\t,\"simple\",\"strings");
		assertAllEqual("SIMPLE (3): Unquoted whitespace at beginning or end shouldn't matter.", SIMPLE, cannedParse3);

		String cp4str = "\"th,ese\",\"a\"\"re\",some,\"\"\"harder\"\"\",\"s,t,r,i,n,g,s\"";
		String[] cannedParse4 = FastCsvUtils.splitRecord(cp4str);
		assertAllEqual("HARDER (4): Canned parse with some embedded commas and quotes should match array.", HARDER, cannedParse4);

		String cp5str = "     \t \"th\nese\"\t, \"a\r\nre\",  just\t,\"w\"\"\rei\nrd\"\"\",\"s,t,r,i,n,g,s\"          ";
		String[] cannedParse5 = FastCsvUtils.splitRecord(cp5str);
		assertAllEqual("MULTILINE (5): Canned parse with ignorable whitespace and  some embedded commas, quotes, and newlines should match array.", MULTILINE, cannedParse5);

		String cp6str = "this,array, \"contains\",,   ,\"\", \tthree, \"blanks\"";
		String[] cannedParse6 = FastCsvUtils.splitRecord(cp6str);
		assertAllEqual("BLANKS (6): Canned parse with a variety of empty fields.", BLANKS, cannedParse6);

	    }
	catch (Exception e)
	    {
		e.printStackTrace();
		fail( e.getMessage() );
	    }
    }

    public void testGenerateQuotedCsvItem()
    {
	// simple string, no internal quotes
	assertEquals("Simple quoting", "\"hello\"", FastCsvUtils.generateQuotedCsvItem("hello"));

	// empty string
	assertEquals("Empty string quoting", "\"\"", FastCsvUtils.generateQuotedCsvItem(""));

	// string with one embedded quote
	assertEquals("Single embedded quote", "\"a\"\"re\"", FastCsvUtils.generateQuotedCsvItem("a\"re"));

	// string with quotes wrapping content
	assertEquals("Quotes wrapping content", "\"\"\"harder\"\"\"", FastCsvUtils.generateQuotedCsvItem("\"harder\""));

	// string with commas (just wraps, no special escaping for commas)
	assertEquals("Commas in content", "\"s,t,r,i,n,g,s\"", FastCsvUtils.generateQuotedCsvItem("s,t,r,i,n,g,s"));

	// string with newlines
	assertEquals("Newlines in content", "\"line1\nline2\"", FastCsvUtils.generateQuotedCsvItem("line1\nline2"));

	// string with CRLF
	assertEquals("CRLF in content", "\"line1\r\nline2\"", FastCsvUtils.generateQuotedCsvItem("line1\r\nline2"));

	// string with multiple consecutive quotes
	assertEquals("Multiple consecutive quotes", "\"a\"\"\"\"b\"", FastCsvUtils.generateQuotedCsvItem("a\"\"b"));
    }

    public void testGenerateCsvItemsAlwaysQuoted()
    {
	String[] quotedSimple = FastCsvUtils.generateCsvItemsAlwaysQuoted(SIMPLE);
	assertEquals("Length preserved for SIMPLE", SIMPLE.length, quotedSimple.length);
	assertEquals("\"these\"", quotedSimple[0]);
	assertEquals("\"are\"", quotedSimple[1]);
	assertEquals("\"some\"", quotedSimple[2]);
	assertEquals("\"simple\"", quotedSimple[3]);
	assertEquals("\"strings\"", quotedSimple[4]);

	String[] quotedHarder = FastCsvUtils.generateCsvItemsAlwaysQuoted(HARDER);
	assertEquals("Length preserved for HARDER", HARDER.length, quotedHarder.length);
	assertEquals("\"th,ese\"", quotedHarder[0]);
	assertEquals("\"a\"\"re\"", quotedHarder[1]);
	assertEquals("\"some\"", quotedHarder[2]);
	assertEquals("\"\"\"harder\"\"\"", quotedHarder[3]);
	assertEquals("\"s,t,r,i,n,g,s\"", quotedHarder[4]);

	// empty array
	String[] quotedEmpty = FastCsvUtils.generateCsvItemsAlwaysQuoted(new String[]{});
	assertEquals("Empty array", 0, quotedEmpty.length);
    }

    public void testGenerateCsvLineQuotedUnterminated()
    {
	String simpleLine = FastCsvUtils.generateCsvLineQuotedUnterminated(SIMPLE);
	assertEquals("SIMPLE line", "\"these\",\"are\",\"some\",\"simple\",\"strings\"", simpleLine);

	// single element array
	String singleLine = FastCsvUtils.generateCsvLineQuotedUnterminated(new String[]{"hello"});
	assertEquals("Single element", "\"hello\"", singleLine);

	// empty array
	String emptyLine = FastCsvUtils.generateCsvLineQuotedUnterminated(new String[]{});
	assertEquals("Empty array", "", emptyLine);

	// line with items that need quote escaping
	String harderLine = FastCsvUtils.generateCsvLineQuotedUnterminated(HARDER);
	assertEquals("HARDER line", "\"th,ese\",\"a\"\"re\",\"some\",\"\"\"harder\"\"\",\"s,t,r,i,n,g,s\"", harderLine);
    }

    public void testGenerateThenSplitRoundTrip()
    {
	try
	    {
		// SIMPLE round trip
		String simpleLine = FastCsvUtils.generateCsvLineQuotedUnterminated(SIMPLE);
		String[] simpleParsed = FastCsvUtils.splitRecord(simpleLine);
		assertAllEqual("SIMPLE round trip", SIMPLE, simpleParsed);

		// HARDER round trip (embedded commas and quotes)
		String harderLine = FastCsvUtils.generateCsvLineQuotedUnterminated(HARDER);
		String[] harderParsed = FastCsvUtils.splitRecord(harderLine);
		assertAllEqual("HARDER round trip", HARDER, harderParsed);

		// MULTILINE round trip (embedded newlines, CR, CRLF)
		String multilineLine = FastCsvUtils.generateCsvLineQuotedUnterminated(MULTILINE);
		String[] multilineParsed = FastCsvUtils.splitRecord(multilineLine);
		assertAllEqual("MULTILINE round trip", MULTILINE, multilineParsed);

		// BLANKS round trip (empty fields)
		String blanksLine = FastCsvUtils.generateCsvLineQuotedUnterminated(BLANKS);
		String[] blanksParsed = FastCsvUtils.splitRecord(blanksLine);
		assertAllEqual("BLANKS round trip", BLANKS, blanksParsed);
	    }
	catch (Exception e)
	    {
		e.printStackTrace();
		fail( e.getMessage() );
	    }
    }

}
