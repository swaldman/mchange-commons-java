/*
 * Distributed as part of mchange-commons-java 0.2.7
 *
 * Copyright (C) 2014 Machinery For Change, Inc.
 *
 * Author: Steve Waldman <swaldman@mchange.com>
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of EITHER:
 *
 *     1) The GNU Lesser General Public License (LGPL), version 2.1, as 
 *        published by the Free Software Foundation
 *
 * OR
 *
 *     2) The Eclipse Public License (EPL), version 1.0
 *
 * You may choose which license to accept if you wish to redistribute
 * or modify this work. You may offer derivatives of this work
 * under the license you have chosen, or you may provide the same
 * choice of license which you have been offered here.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * You should have received copies of both LGPL v2.1 and EPL v1.0
 * along with this software; see the files LICENSE-EPL and LICENSE-LGPL.
 * If not, the text of these licenses are currently available at
 *
 * LGPL v2.1: http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 *  EPL v1.0: http://www.eclipse.org/org/documents/epl-v10.php 
 * 
 */

package com.mchange.v1.xml;

import java.util.*;
import org.xml.sax.*;
import org.w3c.dom.*;
import com.mchange.v1.util.DebugUtils;

public final class DomParseUtils
{
    final static boolean DEBUG = true;

    /**
     * @return null if child doesn't exist.
     */
    public static String allTextFromUniqueChild(Element elem, String childTagName)
	throws DOMException
    { return allTextFromUniqueChild( elem, childTagName, false ); }

    /**
     * @return null if child doesn't exist.
     */
	       public static String allTextFromUniqueChild(Element elem, String childTagName, boolean trim)
	throws DOMException
    {
	Element uniqueChild = uniqueChildByTagName( elem, childTagName );
	if (uniqueChild == null)
	    return null;
	else
	    return DomParseUtils.allTextFromElement( uniqueChild, trim );
    }

    public static Element uniqueChild(Element elem, String childTagName) throws DOMException
    { return uniqueChildByTagName( elem, childTagName); }

    /**
     * @deprecated use uniqueChild(Element elem, String childTagName)
     */
    public static Element uniqueChildByTagName(Element elem, String childTagName) throws DOMException
    {
	NodeList nl = elem.getElementsByTagName(childTagName);
	int len = nl.getLength();
	if (DEBUG)
	    DebugUtils.myAssert( len <= 1 ,
				 "There is more than one (" + len + ") child with tag name: " + 
				 childTagName + "!!!" );
	return (len == 1 ? (Element) nl.item( 0 ) : null);
    }

    public static String allText(Element elem) throws DOMException
    { return allTextFromElement( elem ); }

    public static String allText(Element elem, boolean trim) throws DOMException
    { return allTextFromElement( elem, trim ); }

    /** @deprecated use allText(Element elem) */
    public static String allTextFromElement(Element elem) throws DOMException
    { return allTextFromElement( elem, false); }

    /** @deprecated use allText(Element elem, boolean trim) */
    public static String allTextFromElement(Element elem, boolean trim) throws DOMException
    {
	StringBuffer textBuf = new StringBuffer();
	NodeList nl = elem.getChildNodes();
	for (int j = 0, len = nl.getLength(); j < len; ++j)
	    {
		Node node = nl.item(j);
		if (node instanceof Text) //includes Text and CDATA!
		    textBuf.append(node.getNodeValue());
	    }
	String out = textBuf.toString();
	return ( trim ? out.trim() : out );
    }

    public static String[] allTextFromImmediateChildElements( Element parent, String tagName ) 
	throws DOMException
    { return allTextFromImmediateChildElements( parent, tagName, false ); }

    public static String[] allTextFromImmediateChildElements( Element parent, String tagName, boolean trim ) 
	throws DOMException
    {
	NodeList nl = immediateChildElementsByTagName( parent, tagName );
	int len = nl.getLength();
	String[] out = new String[ len ];
	for (int i = 0; i < len; ++i)
	    out[i] = allText( (Element) nl.item(i), trim );
	return out;
    }


    public static NodeList immediateChildElementsByTagName( Element parent, String tagName )
	throws DOMException
    { return getImmediateChildElementsByTagName( parent, tagName ); }

    /**
     * @deprecated use immediateChildrenByTagName( Element parent, String tagName )
     */
    public static NodeList getImmediateChildElementsByTagName( Element parent, String tagName )
	throws DOMException
    {
	final List nodes = new ArrayList();
	for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling())
	    if (child instanceof Element && ((Element) child).getTagName().equals(tagName))
		nodes.add(child);
	return new NodeList()
	    {
		public int getLength()
		{ return nodes.size(); }

		public Node item( int i )
		{ return (Node) nodes.get( i ); }
	    };
    }

    public static String allTextFromUniqueImmediateChild(Element elem, String childTagName)
	throws DOMException
    {
	Element uniqueChild = uniqueImmediateChildByTagName( elem, childTagName );
	if (uniqueChild == null)
	    return null;
	return DomParseUtils.allTextFromElement( uniqueChild );
    }

    public static Element uniqueImmediateChild(Element elem, String childTagName) 
	throws DOMException
    { return uniqueImmediateChildByTagName( elem, childTagName); }

    /**
     * @deprecated use uniqueImmediateChild(Element elem, String childTagName) 
     */
    public static Element uniqueImmediateChildByTagName(Element elem, String childTagName) 
	throws DOMException
    {
	NodeList nl = getImmediateChildElementsByTagName(elem, childTagName);
	int len = nl.getLength();
	if (DEBUG)
	    DebugUtils.myAssert( len <= 1 ,
				 "There is more than one (" + len + ") child with tag name: " + 
				 childTagName + "!!!" );
	return (len == 1 ? (Element) nl.item( 0 ) : null);
    }

    /**
     * @deprecated use Element.getAttribute(String val)
     */
    public static String attrValFromElement(Element element, String attrName)
	throws DOMException
    {
	Attr attr = element.getAttributeNode( attrName );
	return (attr == null ? null : attr.getValue());
    }

    private DomParseUtils()
    {}
}


