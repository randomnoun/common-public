package com.randomnoun.common;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a 
 * <a rel="license" href="http://creativecommons.org/licenses/by/3.0/">Creative Commons Attribution 3.0 Unported License</a>.
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.randomnoun.common.XmlUtil.SimpleTableContentHandler;

import junit.framework.TestCase;

/** Test the XmlUtil class.
 * 
 * @author knoxg
 * @blog http://www.randomnoun.com/wp/2013/01/24/exciting-things-with-xml/
 * @version $Id$
 *
 */
public class XmlUtilTest extends TestCase {

	private String getResourceAsString(String name) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		InputStream is = XmlUtilTest.class.getResourceAsStream("./" + name);
		if (is==null) { 
			is = XmlUtilTest.class.getResourceAsStream(name);
			if (is==null) {
				throw new IllegalStateException("Missing resource"); 
			}
		}
		int ch = is.read(); while (ch!=-1) { baos.write(ch); ch=is.read(); }
		is.close();
		return baos.toString();
	}
	
	public void testGetCleanXml() throws IOException, SAXException {
		String input, output;		
		input = "<html>So here's <br>some HTML with<p>some <b>unclosed <i>formatting</p>tags.";
		output = XmlUtil.getCleanXml(input, false); // apply HTML rules
		assertEquals("<html xmlns=\"http://www.w3.org/1999/xhtml\"><body>So here's <br clear=\"none\"></br>some HTML with<p>some <b>unclosed <i>formatting</i></b></p><b><i>tags.</i></b></body></html>", output.trim());
		
		output = XmlUtil.getCleanXml(input, true); // apply XML rules
		assertEquals("<html xmlns=\"http://www.w3.org/1999/xhtml\"><body>So here's <br clear=\"none\">some HTML with<p>some <b>unclosed <i>formatting</i></b></p><b><i>tags.</i></b></body></html>", output.trim());
		
		// clean the 'extreme' HTML from http://ccil.org/~cowan/XML/tagsoup/extreme.html
		input = getResourceAsString("/tagsoupInput.txt");
		output = XmlUtil.getCleanXml(input, true);
		String expected = getResourceAsString("/tagsoupOutput.txt"); expected=expected.replaceAll("\r\n",  "\n");
		assertEquals(expected, output);
		
	}

	public void testGetText() throws SAXException {
		String input, output;
		
		input = "<p>Here is some <b>bold text</b> and <i>some <u>underlined</u> italics</i> text</p>";
		Document d = XmlUtil.toDocument(input);
		Element paraEl = d.getDocumentElement(); // top-level element is a paragraph element
		output = XmlUtil.getText(paraEl);
		assertEquals("Here is some bold text and some underlined italics text", output);
		
	}

	public void testGetTextPreserveElements() throws SAXException {
		String input, output;
		Document d;
		
		input = "<p>Here is some <b>bold text</b> and <i>some <u>underlined</u> italics</i> text</p>";
		d = XmlUtil.toDocument(input);
		Element paraEl = d.getDocumentElement(); // top-level element is a paragraph element
		output = XmlUtil.getTextPreserveElements(paraEl, new String[] { "b", "i", "u" } );
		assertEquals("Here is some <b>bold text</b> and <i>some <u>underlined</u> italics</i> text", output);
		
		output = XmlUtil.getTextPreserveElements(paraEl, new String[] { "b", "i" } );
		assertEquals("Here is some <b>bold text</b> and <i>some underlined italics</i> text", output);
		
		output = XmlUtil.getTextPreserveElements(paraEl, new String[] { "i" } );
		assertEquals("Here is some bold text and <i>some underlined italics</i> text", output);

		// attributes are preserved, but may be reordered
		// self-closing elements are expanded to an opening and closing element
		input = "<p>And this would be a paragraph with two images in it: <img src=\"src1.png\" /> and <img src=\"src2.png\"></img></p>";
		d = XmlUtil.toDocument(input);
		paraEl = d.getDocumentElement(); // top-level element is a paragraph element
		output = XmlUtil.getTextPreserveElements(paraEl, new String[] { "img" } );
		assertEquals("And this would be a paragraph with two images in it: <img src=\"src1.png\"></img> and <img src=\"src2.png\"></img>", output);
		
	}

	public void testGetTextNonRecursive() throws SAXException {
		String input, output;
		
		input = "<p>Here is some <b>bold text</b> and <i>some <u>underlined</u> italics</i> text</p>";
		Document d = XmlUtil.toDocument(input);
		Element paraEl = d.getDocumentElement(); // top-level element is a paragraph element
		output = XmlUtil.getTextNonRecursive(paraEl);
		assertEquals("Here is some  and  text", output);
		
	}

	public void testToDocumentString() {
		// should have been tested above, but maybe do some more samples here

	}

	public void testToDocumentInputStream() throws SAXException, TransformerException {
		String input, output;
		InputStream is;
		Document d;
		
		// DOCTYPEs are discarded
		input = "<!DOCTYPE something><p>XML with a DOCTYPE</p>";
		is = new ByteArrayInputStream(input.getBytes());
		d = XmlUtil.toDocument(is);
		output = XmlUtil.getXmlString(d.getDocumentElement(), false);
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><p>XML with a DOCTYPE</p>", output);

		// XML with namespaces
		input = "<root xmlns:dc=\"http://purl.org/dc/terms/\"" +
          " xmlns:html=\"http://www.w3.org/1999/xhtml\">" +
		  "<html:p><dc:something>dublin core dublin core</dc:something>" +
		  "Well this should be aware of namespaces then</html:p></root>";
		is = new ByteArrayInputStream(input.getBytes());
		d = XmlUtil.toDocument(is); 
		output = XmlUtil.getXmlString(d.getDocumentElement(), false);
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + input, output);
		
	}

	public void testGetXmlString() throws SAXException, TransformerException {
		String input, output;
		Document d;
		Element paraEl;
		
		input = "<p>Here is some <b>bold text</b> and <i>some <u>underlined</u> italics</i> text</p>";
		d = XmlUtil.toDocument(input);
		paraEl = d.getDocumentElement(); 
		output = XmlUtil.getXmlString(paraEl, true);
		assertEquals(input, output); // output should match input
		
		output = XmlUtil.getXmlString(paraEl, false);
		// output should match input with XML declaration
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + input, output);
		
	}
	

	public void testCompact() throws ParserConfigurationException, SAXException, TransformerException {
		String input =
			"<body>\r\n" +
		    "  <el>  This is an element\r\n" +
			"    <el2> This is another one</el2>\r\n" +
		    "    <el3>and another <!-- with some comments --></el3>\r\n" +
			"  </el>\r\n" +
		    "</body>";
		Document d = XmlUtil.toDocument(input);
		String output = XmlUtil.getXmlString(d.getDocumentElement(), true);
		assertEquals(input.replaceAll("\n", System.getProperty("line.separator")), output);
		assertEquals(input, output); 
		XmlUtil.compact(d.getDocumentElement());
		output = XmlUtil.getXmlString(d.getDocumentElement(), true);
		assertEquals("<body><el>This is an element<el2>This is another one</el2>" +
		  "<el3>and another<!-- with some comments --></el3></el></body>", output);
				
	}

	public void testProcessContentHandler_SimpleTable() throws SAXException {
		String input = 
			"<table>" +
			"<tr><td>A1</td><td>B1</td><td>C1</td></tr>" +
			"<tr><td>A2</td><td>B2</td></tr>" +
			"<tr><td>A3</td><td>B3</td><td>C3</td></tr>" +
			"</table>";
		
		SimpleTableContentHandler stch = new SimpleTableContentHandler();
		XmlUtil.processContentHandler(stch, input);
		List<List<String>> table = stch.getTable();
		assertEquals(3, table.size()); // 3 rows
		assertEquals(3, table.get(0).size()); // 3 columns in first row
		assertEquals(2, table.get(1).size()); // 2 columns in second row
		assertEquals("C1", table.get(0).get(2)); // contents of row 1, cell 3
	}

	/** A test class that extends XmlUtil.AbstractStackContentHandler.
	 * The "real" DeviceContentHandler populates DeviceTO and DevicePropertyTO objects here, 
	 * but for the purposes of this unit test, I'm just storing the result in 
	 * structured Lists of Maps.   
	 */
	public static class DeviceContentHandler extends XmlUtil.AbstractStackContentHandler {
		List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
		Map<String, Object> d = null;  // current device
		Map<String, String> prop = null;  // current property
		
		// the 'path' variable is maintained by the AbstractStackContentHandler as a 
		// slash-delimited path of the current element within the XML document. 

		/** paths that match this pattern define attributes of a device */
		Pattern p1 = Pattern.compile("^devices/device/(name|className|type|active|universeNumber)$");
		
		/** stacks that match this pattern define attributes of a deviceProperty */
		Pattern p2 = Pattern.compile("^devices/device/deviceProperties/deviceProperty/(key|value)$");
		
		/** process the start of an XML element */
		public void element(String path) throws SAXException {
			if (path.equals("devices/device")) {
				d = new HashMap<String, Object>();
				d.put("deviceProperties", new ArrayList<Map<String, String>>());;
				result.add(d);
			} else if (path.equals("devices/device/deviceProperties")) {
				// 
			} else if (path.equals("devices/device/deviceProperties/deviceProperty")) {
				prop = new HashMap<String, String>();
				((List) d.get("deviceProperties")).add(prop);
			}
		}
		
		/** process the text of an XML element */
		public void elementText(String path, String content) throws SAXException {
			Matcher m1 = p1.matcher(path);
			if (m1.matches()) {
				d.put(m1.group(1), content);
			} else {
				Matcher m2 = p2.matcher(path);
				if (m2.matches()) {
					prop.put(m2.group(1), content);
				}
			}
		}
	}
	
	public void testProcessContentHandler_AbstractStack() throws SAXException, IOException {
		String input = getResourceAsString("/device.xml");
    	DeviceContentHandler dch = new DeviceContentHandler();
    	XmlUtil.processContentHandler(dch, input);
    	
    	List<Map<String, Object>> devices = dch.result;
    	assertEquals(4, devices.size());
    	
    	Map<String, Object> device = devices.get(1); // second device
    	assertEquals("Art-Net", device.get("name"));
    	assertEquals("com.randomnoun.dmx.dmxDevice.artNet.ArtNet", device.get("className"));
    	
    	List<Map<String, String>> properties = (List) device.get("deviceProperties");
    	assertEquals(6, properties.size()); // seven properties
    	
    	Map<String, String> property = properties.get(0);
    	assertEquals("artNetSubnetId", property.get("key"));
    	assertEquals("0", property.get("value"));
    
	}

	

}
