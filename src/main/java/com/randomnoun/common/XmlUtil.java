package com.randomnoun.common;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.ccil.cowan.tagsoup.*;
import org.ccil.cowan.tagsoup.Parser;

import org.w3c.dom.*;
import org.w3c.dom.Element;
import org.xml.sax.*;

import org.apache.log4j.Logger;

/** XML utility functions
 *
 * @author knoxg
 * @blog http://www.randomnoun.com/wp/2013/01/25/exciting-things-with-xml/
 * @version $Id$
 */
public class XmlUtil {
	
    /** A revision marker to be used in exception stack traces. */
    public static final String _revision = "$Id$";


	/** Clean some HTML text through the tagsoup filter. The returned string is guaranteed to be 
	 * well-formed XML (and can therefore be used by other tools that expect valid XML). 
	 * 
	 * @param inputXml input XML document
	 * @param isHtml if true, uses the HTML schema, omits the XML declaration, and uses the html method
	 * 
	 * @throws SAXException if the tagsoup library could not parse the input string
	 * @throws IllegalStateException if an error occurred reading from a string (should never occur)
	 */ 
	public static String getCleanXml(String inputXml, boolean isHtml) throws SAXException {
		return getCleanXml(new ByteArrayInputStream(inputXml.getBytes()), isHtml);
	}
	
	/** Clean a HTML inputStream through the tagsoup filter. The returned string is guaranteed to be 
	 * well-formed XML (and can therefore be used by other tools that expect valid XML). 
	 * 
	 * @param is input XML stream
	 * @param isHtml if true, uses the HTML schema, omits the XML declaration, and uses the html method
	 * 
	 * @throws SAXException if the tagsoup library could not parse the input string
	 * @throws IllegalStateException if an error occurred reading from a string (should never occur)
	 */ 
	public static String getCleanXml(InputStream inputStream, boolean isHtml) throws SAXException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			InputSource is = new InputSource();
			is.setByteStream(inputStream); // could use raw inputstream here later

			XMLReader xmlReader = new Parser();
			Writer w = new OutputStreamWriter(baos);
			XMLWriter tagsoupXMLWriter = new XMLWriter(w);
			tagsoupXMLWriter.setOutputProperty(XMLWriter.OMIT_XML_DECLARATION, "yes");
			if (isHtml) {
				HTMLSchema theSchema = new HTMLSchema();
				xmlReader.setProperty(Parser.schemaProperty, theSchema);
	
				tagsoupXMLWriter.setOutputProperty(XMLWriter.METHOD, "html");
				tagsoupXMLWriter.setPrefix(theSchema.getURI(), "");
			}
			
			xmlReader.setContentHandler(tagsoupXMLWriter);
			xmlReader.parse(is);
			return baos.toString();
		} catch (IOException ioe) {
			throw (IllegalStateException) new IllegalStateException("IO Exception reading from string").initCause(ioe);		
		}
	}


	/**
	 * Iterates through the child nodes of the specified element, and returns the contents
	 * of all Text and CDATA elements among those nodes, concatenated into a string.
	 *
	 * <p>Elements are recursed into.
	 *
	 * @param element the element that contains, as child nodes, the text to be returned.
	 * @return the contents of all the CDATA children of the specified element.
	 */
	public static String getText(Element element)
	{
		if (element == null) { throw new NullPointerException("null element"); }
		StringBuffer buf = new StringBuffer();
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); ++i) {
			org.w3c.dom.Node child = children.item(i);
			short nodeType = child.getNodeType();
			if (nodeType == org.w3c.dom.Node.CDATA_SECTION_NODE) {
				buf.append(((org.w3c.dom.Text) child).getData());			
			} else if (nodeType == org.w3c.dom.Node.TEXT_NODE) {
				buf.append(((org.w3c.dom.Text) child).getData());
			} else if (nodeType == org.w3c.dom.Node.ELEMENT_NODE) {
				buf.append(getText((Element) child));
			}
		}
		return buf.toString();
	}

	/**
	 * Iterates through the child nodes of the specified element, and returns the contents
	 * of all Text and CDATA elements among those nodes, concatenated into a string. 
	 * Any elements with tagNames that are included in the tagNames parameter of this
	 * method are also included. 
	 * 
	 * <p>Attributes of these tags are also included in the result, but may be reordered.
	 * 
	 * <p>Self-closing elements (e.g. <code>&lt;br/&gt;</code>)
	 * are expanded into opening and closing elements (e.g. <code>&lt;br&gt;&lt;/br&gt;</code>)
	 *
	 * <p>Elements are recursed into.
	 *
	 * @param element the element that contains, as child nodes, the text to be returned.
	 * @return the contents of all the CDATA children of the specified element.
	 */
	public static String getTextPreserveElements(Element element, String[] tagNames) {
		if (element == null) { throw new NullPointerException("null element"); }
		Set<String> tagNamesSet = new HashSet<String>(Arrays.asList(tagNames));
		StringBuffer buf = new StringBuffer();
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); ++i) {
			org.w3c.dom.Node child = children.item(i);
			short nodeType = child.getNodeType();
			if (nodeType == org.w3c.dom.Node.CDATA_SECTION_NODE) {
				buf.append(((org.w3c.dom.Text) child).getData());			
			} else if (nodeType == org.w3c.dom.Node.TEXT_NODE) {
				buf.append(((org.w3c.dom.Text) child).getData());
			} else if (nodeType == org.w3c.dom.Node.ELEMENT_NODE) {
				String tagName = ((Element) child).getTagName();
				boolean includeEl = tagNamesSet.contains(tagName);
				if (includeEl) {
					buf.append('<');
					buf.append(tagName);
					NamedNodeMap nnm = ((Element) child).getAttributes();
					for (int j = 0; j < nnm.getLength(); j++) {
						Attr attr = (Attr) nnm.item(j);
						buf.append(" " + attr.getName());
						if (attr.getValue()!=null) {
							buf.append("=\"" + attr.getValue() + "\"");
						}
					}
					buf.append('>');
				}
				buf.append(getTextPreserveElements((Element) child, tagNames));
				if (includeEl) {
					buf.append("</" + tagName + ">");
				}
			}
		}
		return buf.toString();
	}	


	
	/**
	 * Iterates through the child nodes of the specified element, and returns the contents
	 * of all Text and CDATA elements among those nodes, concatenated into a string.
	 * 
	 * <p>Elements are not recursed into.
	 *
	 * @param element the element that contains, as child nodes, the text to be returned.
	 * @return the contents of all the CDATA children of the specified element.
	 */
	public static String getTextNonRecursive(Element element)
	{
		if (element == null) { throw new NullPointerException("null element"); }
		StringBuffer buf = new StringBuffer();
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); ++i) {
			org.w3c.dom.Node child = children.item(i);
			short nodeType = child.getNodeType();
			if (nodeType == org.w3c.dom.Node.CDATA_SECTION_NODE) {
				buf.append(((org.w3c.dom.Text) child).getData());			
			} else if (nodeType == org.w3c.dom.Node.TEXT_NODE) {
				buf.append(((org.w3c.dom.Text) child).getData());
			} else if (nodeType == org.w3c.dom.Node.ELEMENT_NODE) {
				// ignore child elements
			}
		}
		return buf.toString();
	}
	
	/** Return a DOM document object from an XML string
	 * 
	 * @param text the string representation of the XML to parse 
	 */
	public static Document toDocument(String text) throws SAXException {
		return toDocument(new ByteArrayInputStream(text.getBytes()));
	}
	
	/** Return a DOM document object from an InputStream
	 * 
	 * @param is the InputStream containing the XML to parse 
	 */
	public static Document toDocument(InputStream is) throws SAXException {
		try {
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			Document doc = docBuilder.parse(is);
			doc.getDocumentElement().normalize(); // Collapses adjacent text nodes into one node.
			return doc;
		} catch (ParserConfigurationException pce) {
			// this can never happen 
			throw (IllegalStateException) new IllegalStateException("Error creating DOM parser").initCause(pce);
		} catch (IOException ioe) {
			// this can also never happen
			throw (IllegalStateException) new IllegalStateException("Error retrieving information").initCause(ioe);
		} 
	}
	
	/** Converts a document node subtree back into an XML string 
	 * 
	 * @param node a DOM node 
	 * @param omitXmlDeclaration if true, omits the XML declaration from the returned result
	 * 
	 * @return the XML for this node
	 * 
	 * @throws TransformerException if the transformation to XML failed
	 * @throws IllegalStateException if the transformer could not be initialised 
	 */
	public static String getXmlString(Node node, boolean omitXmlDeclaration) 
		throws TransformerException 
	{
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(node);
			StreamResult result = new StreamResult(baos);
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, omitXmlDeclaration ? "yes": "no");
			transformer.transform(source, result);
			return baos.toString();
		} catch (TransformerConfigurationException tce) {
			throw (IllegalStateException) new IllegalStateException("Could not initialise transfoermer").initCause(tce);
		}
	}
	

	/** Remove leading/trailing whitespace from all text nodes in this nodeList.
	 * Will iterate through subnodes recursively.
	 * 
	 * @param nodeList
	 */
	public static void compact(Node node) {
		if (node.getNodeType()==Node.TEXT_NODE) {
			org.w3c.dom.Text el = (org.w3c.dom.Text) node;
			if (el.getNodeValue()!=null) {
				el.setNodeValue(el.getNodeValue().trim());
			}
		} else if (node.getNodeType()==Node.ELEMENT_NODE) {
			NodeList childNodes = node.getChildNodes();
			if (childNodes != null && childNodes.getLength() > 0) {
				int len = childNodes.getLength();
				for (int i=0; i<len; i++) {
					Node childNode = childNodes.item(i);
				    compact(childNode);
				}
			}
		}
	}
	
	
	/** Parse a string of XML text using a SAX contentHandler. Nothing is returned by this method - it 
	 * is assumed that the contentHandler supplied maintains it's own state as it parses the XML supplied,
	 * and that this state can be extracted from this object afterwards.
	 * 
	 * @param contentHandler a SAX content handler 
	 * @param xmlText an XML document (or part thereof)
	 * 
	 * @throws SAXException if the document could not be parsed
	 * @throws IllegalException if the parser could not be initialised, or an I/O error occurred 
	 *   (should not happen since we're just dealing with strings)
	 */
	public static void processContentHandler(ContentHandler contentHandler, String xmlText) throws SAXException {
		 SAXParserFactory factory = SAXParserFactory.newInstance();
		 try {
			 // Parse the input
			 SAXParser saxParser = factory.newSAXParser();
			 XMLReader xmlReader = saxParser.getXMLReader();
			 xmlReader.setContentHandler(contentHandler);
			 xmlReader.parse(new InputSource(new ByteArrayInputStream(xmlText.getBytes())));
		 } catch (IOException ioe) {
		 	throw (IllegalStateException) new IllegalStateException("IO Exception reading from string").initCause(ioe);
		 } catch (ParserConfigurationException pce) {
			throw (IllegalStateException) new IllegalStateException("Could not initialise parser").initCause(pce);		 		
		 }
	}
	
	/** Convert a table into a List of Lists (each top-level list represents a table row,
	 * each second-level list represents a table cell). Only contents are returned; attributes
	 * and formatting are ignored.
	 * 
	 * <p>This class will probably not work when tables are embedded within other tables
	 */
	public static class SimpleTableContentHandler
		implements ContentHandler 
	{
		/** Logger instance for this class */
		public static final Logger logger = Logger.getLogger(SimpleTableContentHandler.class);

		/** Current table */
		List<List<String>> thisTable = null;
		/** Current row in table */
		List<String> thisRow = null;
		/** Current cell in row */
		String thisCell = "";

		/** The state of this parser */
		private enum State {
			/** start of doc, expecting 'table' */
			START,
			/** in table element, expecting 'tr' */
			IN_TABLE,
			/** in tr element, expecting 'td' (or other ignored elements) */
			IN_TR,
			/** in td element, capturing to closing tag */
			IN_TD
		}

		State state = State.START;
		
		// unused interface methods
		public void setDocumentLocator(Locator locator) { }
		public void startDocument() throws SAXException { }
		public void endDocument() throws SAXException { }
		public void startPrefixMapping(String prefix, String uri) throws SAXException { }
		public void endPrefixMapping(String prefix) throws SAXException { }
		public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException { }
		public void processingInstruction(String target, String data) throws SAXException { }
		public void skippedEntity(String name) throws SAXException { }


		public void startElement(String uri, String localName, String qName, Attributes atts)
			throws SAXException 
		{
			switch (state) {
				case START: 
					if (qName.equals("table")) {
						thisTable = new ArrayList<List<String>>(); 
						state = State.IN_TABLE; 
					} else {
						logger.warn("Warning: top-level element '" + qName + "' found (expected 'table')");
					}
					break;
				
				case IN_TABLE:
					if (qName.equals("tr")) {
						thisRow = new ArrayList<String>();
						thisTable.add(thisRow);
						state = State.IN_TR;
					}
					break;
					
				case IN_TR: 
					if (qName.equals("td")) {
						thisCell = "";
						state = State.IN_TD;
					}
					break;
					
				case IN_TD:
					break;
					
				default:
					throw new IllegalStateException("Illegal state " + state + " in SimpleTableContentHandler");
				
			}
		}

		public void characters(char[] ch, int start, int length)
			throws SAXException {
			if (state==State.IN_TD) {
				thisCell += new String(ch, start, length);
			}
		}

		public void endElement(String uri, String localName, String qName)
			throws SAXException 
		{
			if (state == State.IN_TD && qName.equals("td")) {
				thisRow.add(thisCell);
				state = State.IN_TR;
			} else if (state == State.IN_TR && qName.equals("tr")) {
				state = State.IN_TABLE;
			}
		}
	
		public List<List<String>> getTable() {
			return thisTable;
		}
	}
	
	/** An abstract stack-based XML parser. Similar to the apache digester, but without
	 * the dozen or so dependent JARs.
	 * 
	 * <p>Only element text is captured 
	 * <p>Element attributes are not parsed by this class.
	 * <p>Mixed text/element nodes are not parsed by this class.
	 * 
	 */
	public abstract static class AbstractStackContentHandler implements ContentHandler 
	{
		/** Logger instance for this class */
		public static final Logger logger = Logger.getLogger(AbstractStackContentHandler.class);

		/** Location in stack */
		protected String stack = "";
		protected String text = null;     // text captured so far
		
		// unused interface methods
		public void setDocumentLocator(Locator locator) { }
		public void startDocument() throws SAXException { }
		public void endDocument() throws SAXException { }
		public void startPrefixMapping(String prefix, String uri) throws SAXException { }
		public void endPrefixMapping(String prefix) throws SAXException { }
		public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException { }
		public void processingInstruction(String target, String data) throws SAXException { }
		public void skippedEntity(String name) throws SAXException { }

		public void startElement(String uri, String localName, String qName, Attributes atts)
			throws SAXException 
		{
			stack = stack.equals("") ? qName : stack + "/" + qName;
			text = "";
			element(stack);
		}
		public void characters(char[] ch, int start, int length) throws SAXException {
			text += new String(ch, start, length);
		}
		public void endElement(String uri, String localName, String qName)
			throws SAXException 
		{
			elementText(stack, text);
			text = ""; // probably not necessary
			stack = stack.contains("/") ? stack.substring(0, stack.lastIndexOf("/")) : "";
		}

		// abstract methods to be implemented by subclasses
		public abstract void element(String path) throws SAXException;
		public abstract void elementText(String path, String content) throws SAXException;
	}
	

	/** Convert a NodeList into something that Java1.5 can treat as Iterable,
	 * so that it can be used in <tt>for (Node node : nodeList) { ... }</tt> style
	 * constructs.
	 * 
	 * <p>(org.w3c.dom.traversal.NodeListIterator doesn't currently implement Iterable)
	 * 
	 */
	public static class NodeListIterator implements Iterable<org.w3c.dom.Node> {
		private final NodeList nodeList;
		public NodeListIterator(NodeList nodeList) {
			this.nodeList = nodeList;
		}
		public Iterator<org.w3c.dom.Node> iterator() {
			return new Iterator<org.w3c.dom.Node>() {
				private int index = 0;
				public boolean hasNext() {
					return index < nodeList.getLength();
				}
				public org.w3c.dom.Node next() {
					return nodeList.item(index++);
				}
				public void remove() {
					throw new UnsupportedOperationException("remove() not allowed in NodeList");
				}
			};
		}
	}

	
	
}
