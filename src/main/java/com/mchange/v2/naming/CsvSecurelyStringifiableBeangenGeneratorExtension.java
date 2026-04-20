package com.mchange.v2.naming;

import java.util.*;
import java.io.IOException;
import com.mchange.v2.lang.Coerce;
import com.mchange.v2.io.IndentedWriter;
import com.mchange.v2.codegen.bean.*;

public class CsvSecurelyStringifiableBeangenGeneratorExtension implements GeneratorExtension
{
    boolean baseClass = false;
    Map propNameToEncodeOverrideFunction = new HashMap();
    Map propNameToDecodeOverrideFunction = new HashMap();

    public void setBaseClass(boolean baseClass)
    { this.baseClass = baseClass; }

    public boolean isBaseClass()
    { return this.baseClass; }

    public void setEncodeOverrideFunction(String propName, String function)
    { propNameToEncodeOverrideFunction.put(propName, function); }

    public void setDecodeOverrideFunction(String propName, String function)
    { propNameToDecodeOverrideFunction.put(propName, function); }

    public Collection extraGeneralImports()
    { 
	Set set = new HashSet();
        set.add("java.io");
        set.add("java.util");
	return set;
    }

    public Collection extraSpecificImports()
    {
	Set set = new HashSet();
        set.add( "com.mchange.v2.lang.Coerce" );
        set.add( "com.mchange.v2.csv.FastCsvUtils" );
        set.add( "com.mchange.v2.csv.CsvBufferedReader" );
        set.add( "com.mchange.v2.naming.SecurelyStringifiable" );
        set.add( "com.mchange.v2.naming.SecurelyStringifiableException" );
	return set;
    }

    public Collection extraInterfaceNames()
    {
	Set set = new HashSet();
	return set;
    }

    public void generate(ClassInfo info, Class superclassType, Property[] props, Class[] propTypes, IndentedWriter iw)
	throws IOException
    {
        iw.println("public static String securelyStringify( " + info.getClassName() + " bean ) throws Exception");
        iw.println("{");
        iw.upIndent();
        iw.println("StringBuilder sb = new StringBuilder();");
        for( int i = 0, len = props.length; i < len; ++i)
        {
            Property p = props[i];
            String propName = p.getName();
            Class propType = propTypes[i];
            boolean refType = !propType.isPrimitive();
            if (refType)
            {
                iw.println("if (bean." + propName + " == null)");
                iw.upIndent();
                iw.println("sb.append( FastCsvUtils.generateCsvLineQuotedUnterminated( new String[] {\"" + propName + "\" } ) );" );
                iw.downIndent();
                iw.println("else");
                iw.println("{");
                iw.upIndent();
            }
            String overrideFunction = (String) propNameToEncodeOverrideFunction.get(propName);
            if (overrideFunction != null)
                iw.println( "sb.append( FastCsvUtils.generateCsvLineQuotedUnterminated( new String[] {\"" + propName + "\", " + overrideFunction + "( (" + propType.getName() + ") bean." + propName + ") } ) );" );
            else if (propType == String.class)
                iw.println( "sb.append( FastCsvUtils.generateCsvLineQuotedUnterminated( new String[] {\"" + propName + "\", bean." + propName + "} ) );" );
	    else if ( Coerce.canCoerce( propType ) )
                iw.println( "sb.append( FastCsvUtils.generateCsvLineQuotedUnterminated( new String[] {\"" + propName + "\", String.valueOf( bean." + propName + ") } ) );" );
            else
                iw.println( "sb.append( FastCsvUtils.generateCsvLineQuotedUnterminated( new String[] {\"" + propName + "\", SecurelyStringifiable.securelyStringify( bean." + propName + " ) } ) );" );
            iw.println( "sb.append(\"\\r\\n\");" );
            if (refType)
            {
                iw.downIndent();
                iw.println("}");
            }
        }
        iw.println("return sb.toString();");
        iw.downIndent();
        iw.println("}");
        iw.println();
        if (baseClass)
            iw.println("public static " + info.getClassName() + " constructSecurelyStringified( String s, " + info.getClassName() + " nascent ) throws Exception");
        else
            iw.println("public static " + info.getClassName() + " constructSecurelyStringified( String s ) throws Exception");
        iw.println("{");
        iw.upIndent();
        iw.println("Set nullSet = new HashSet();");
        iw.println("Map valMap = new HashMap();");
        if (baseClass)
            iw.println(info.getClassName() + " out = nascent;");
        else
            iw.println(info.getClassName() + " out = new " + info.getClassName() + "();"); 
        iw.println("try (CsvBufferedReader csvr = new CsvBufferedReader(new BufferedReader(new StringReader(s)));)");
        iw.println("{");
        iw.upIndent();
        iw.println("String[] csvLine;");
        iw.println("while ((csvLine = csvr.readSplitLine()) != null)");
        iw.println("{");
        iw.upIndent();
        iw.println("switch (csvLine.length)");
        iw.println("{");
        iw.println("case 1:");
        iw.upIndent();
        iw.println("nullSet.add( csvLine[0] );");
        iw.println("break;");
        iw.downIndent();
        iw.println("case 2:");
        iw.upIndent();
        iw.println("valMap.put( csvLine[0], csvLine[1] );");
        iw.println("break;");
        iw.downIndent();
        iw.println("default:");
        iw.upIndent();
        iw.println("throw new SecurelyStringifiableException(\"Expected CSV lines of one or two values. Found \" + csvLine.length + \" in...\\n\" + s);");
        iw.downIndent();
        iw.println("}"); // switch
        iw.downIndent();
        iw.println("}"); // while
        iw.downIndent();
        iw.println("}"); // try
        for( int i = 0, len = props.length; i < len; ++i)
        {
            Property p = props[i];
            String propName = p.getName();
            Class propType = propTypes[i];
            boolean refType = !propType.isPrimitive();

            // decode expected properties here
            if (refType)
            {
                iw.println("if (nullSet.contains(\"" + propName + "\")) out." + propName + " = null;");
                iw.println("else");
                iw.println("{");
                iw.upIndent();
            }
            iw.println( "if (valMap.containsKey(\"" + propName + "\"))" );
            iw.println("{");
            iw.upIndent();
            String overrideFunction = (String) propNameToDecodeOverrideFunction.get(propName);
            if (overrideFunction != null)
                iw.println( "out." + propName + " = (" + propType.getName() + ") " + overrideFunction + "( (String) valMap.get( \"" + propName + "\") );" );
            else if (propType == String.class)
                iw.println( "out." + propName + " = (String) valMap.get( \"" + propName + "\");" );
            else if ( Coerce.canCoerce( propType ) )
            {
                if (propType == byte.class)
                    iw.println( "out." + propName + " = Coerce.toByte( (String) valMap.get( \"" + propName + "\") );" );
                if (propType == char.class)
                    iw.println( "out." + propName + " = Coerce.toChar( (String) valMap.get( \"" + propName + "\") );" );
                if (propType == short.class)
                    iw.println( "out." + propName + " = Coerce.toShort( (String) valMap.get( \"" + propName + "\") );" );
                if (propType == int.class)
                    iw.println( "out." + propName + " = Coerce.toInt( (String) valMap.get( \"" + propName + "\") );" );
                if (propType == long.class)
                    iw.println( "out." + propName + " = Coerce.toLong( (String) valMap.get( \"" + propName + "\") );" );
                if (propType == float.class)
                    iw.println( "out." + propName + " = Coerce.toFloat( (String) valMap.get( \"" + propName + "\") );" );
                if (propType == double.class)
                    iw.println( "out." + propName + " = Coerce.toDouble( (String) valMap.get( \"" + propName + "\") );" );
                else
                    iw.println( "out." + propName + " = (" + propType.getName() + ") Coerce.toObject( (String) valMap.get( \"" + propName + "\"), " + propType.getName() + ".class );" );
            }
            else
                iw.println( "out." + propName + " = (" + propType.getName() + ") SecurelyStringifiable.constructSecurelyStringified( (String) valMap.get( \"" + propName + "\") );" );
            iw.downIndent();
            iw.println("}");
            if (refType)
            {
                iw.downIndent();
                iw.println("}");
            }
        }
        iw.println("return out;");
        iw.downIndent();
        iw.println("}");
    }
}
