package com.randomnoun.common.webapp.taglib;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import java.util.*;
import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

/**
 * This is an abstract taglib, which can be used to propagate standard HTML attributes
 * through to a HTML page. It provides handlers for all the 'general' attributes
 * that are present for most HTML tags (e.g. onclick, style, ...).
 *
 * <p>Other taglibs that are used to generate HTML elements (e.g. SelectTag), can
 * extend this class to take advantage of this functionality.
 *
 * <p>Superclasses may override any method defined in this tag to provide their own
 * special handling.
 *
 * <b>The attributes handled by this tag</b> are
      accesskey, styleClass, contenteditable, dir, disabled, lang, language, maxlength,
      size, style, title, width, onactivate, onafterupdate, onbeforeactvate,
      onbeforecut, onbeforedeactivate, onbeforeeditfocus, onbeforepaste,
      onbeforeupdate, onblur, onchange, onclick, oncontextmenu, oncontrolselect,
      oncut, ondblclick, ondeactivate, ondrag, ondragend, ondragenter, ondragleave,
      ondragover, ondragstart, ondrop, onerrorupdate, onfilterchange, onfocus,
      onfocusin, onfocusout, onhelp, onkeydown, onkeypress, onkeyup, onlosecapture,
      onmousedown, onmouseenter, onmouseleave, onmousemove, onmouseout, onmouseover,
      onmouseup, onmousewheel, onmove, onmoveend, onmovestart, onpaste,
      onpropertychange, onreadystatechange, onresize, onresizeend, onresizestart,
      onselect, onselectstart, ontimeerror, hidefocus, readonly, tabindex and
      unselectable.
 *
 * <p>Note that the HTML attribute 'class' is now called 'styleClass', since the
 * Object.getClass() method interferes with standard JavaBean reflection.
 *
 * <p>All attributes are lower-case, except extraAttributes (see below).
 *
 * <p>Note that the attributes 'name', 'type' and 'value' are intentionally not on this list,
 * as they would normally be generated by the specific tag libary itself.
 * 
 * <p>The 'disabled' attributes has some special handling -- if it's set to the value 
 * "false", then the attribute is removed. This is due to IE treating 'disabled="false"' as
 * actually disabling the control. Which is odd.
 *
 * <p>The IE-only attributes hidefocus, readonly, tabindex and unselectable are included
 * on this list, because they seem reasonably useful. The IE-only attributes
 * begin, end, datafld, datasrc, autocomplete, syncmasterm, timecontainer and vcard_name
 * are not on this list, because they don't seem as useful. You can always add them if
 * you think they're needed.
 *
 * <p>Values are EL-evaluated (so you can have JSTL-style <tt>${xxxx}</tt> values).
 *
 * <p>Additional attributes can be expressed using the extraAttributes attribute, e.g.
 * to put a non-standard attribute 'onmousewheelclick' into the element, you could
 * use, for example:
 * <pre style="code">
 * &lt;mm:something extraAttribute="onmousewheelclick='doSomethingInJavascript()'" />
 * </pre>
 *
 * <p>Any tag that extends this class should have the following text in it's .tld descriptor:
 * <pre style="code">
    &lt;!-- Attributes understood by StandardHtmlTag -->
    &lt;attribute>&lt;name>accesskey&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>styleClass&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>contenteditable&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>dir&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>disabled&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>lang&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>language&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>maxlength&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>size&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>style&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>title&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>width&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>onactivate&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>onafterupdate&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>onbeforeactvate&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>onbeforecut&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>onbeforedeactivate&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>onbeforeeditfocus&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>onbeforepaste&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>onbeforeupdate&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>onblur&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>onchange&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>onclick&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>oncontextmenu&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>oncontrolselect&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>oncut&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>ondblclick&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>ondeactivate&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>ondrag&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>ondragend&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>ondragenter&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>ondragleave&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>ondragover&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>ondragstart&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>ondrop&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>onerrorupdate&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>onfilterchange&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>onfocus&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>onfocusin&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>onfocusout&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>onhelp&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>onkeydown&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>onkeypress&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>onkeyup&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>onlosecapture&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>onmousedown&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>onmouseenter&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>onmouseleave&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>onmousemove&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>onmouseout&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>onmouseover&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>onmouseup&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>onmousewheel&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>onmove&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>onmoveend&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>onmovestart&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>onpaste&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>onpropertychange&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>onreadystatechange&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>onresize&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>onresizeend&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>onresizestart&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>onselect&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>onselectstart&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>ontimeerror&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>hidefocus&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>readonly&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>tabindex&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>unselectable&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
    &lt;attribute>&lt;name>extraAttributes&lt;/name>&lt;required>false&lt;/required>&lt;rtexprvalue>true&lt;/rtexprvalue>&lt;/attribute>
 * </pre>
 *
 * NB: Implementing classes MUST call getAttributes().clear(); in their doEnd() method,
 * in case the JSP container decides to reuse taglib instances.
 *
 * @author  knoxg
 * 
 *
 */

/* that text snippet again without &lt's :
   <!-- Attributes understood by com.randomnoun.common.webapp.taglib.StandardHtmlTag. -->
   <attribute><name>accesskey</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>styleClass</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>contenteditable</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>dir</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>disabled</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>lang</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>language</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>maxlength</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>size</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>style</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>title</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>width</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>onactivate</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>onafterupdate</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>onbeforeactvate</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>onbeforecut</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>onbeforedeactivate</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>onbeforeeditfocus</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>onbeforepaste</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>onbeforeupdate</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>onblur</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>onchange</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>onclick</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>oncontextmenu</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>oncontrolselect</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>oncut</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>ondblclick</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>ondeactivate</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>ondrag</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>ondragend</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>ondragenter</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>ondragleave</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>ondragover</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>ondragstart</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>ondrop</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>onerrorupdate</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>onfilterchange</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>onfocus</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>onfocusin</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>onfocusout</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>onhelp</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>onkeydown</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>onkeypress</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>onkeyup</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>onlosecapture</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>onmousedown</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>onmouseenter</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>onmouseleave</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>onmousemove</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>onmouseout</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>onmouseover</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>onmouseup</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>onmousewheel</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>onmove</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>onmoveend</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>onmovestart</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>onpaste</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>onpropertychange</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>onreadystatechange</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>onresize</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>onresizeend</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>onresizestart</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>onselect</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>onselectstart</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>ontimeerror</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>hidefocus</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>readonly</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>tabindex</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>unselectable</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
   <attribute><name>extraAttributes</name><required>false</required><rtexprvalue>true</rtexprvalue></attribute>
 */
@SuppressWarnings("serial")
public abstract class StandardHtmlTag
    extends TagSupport {
    
    
    protected Map<String, String> attributes = new HashMap<String, String>();
    protected String extraAttributes = null;

    public void setAccessKey(String accessKey) { attributes.put("accessKey", accessKey); }
    public void setStyleClass(String clazz) { attributes.put("class", clazz); }   
    public void setContenteditable(String contentEditable) { attributes.put("contenteditable", contentEditable); }   
    public void setDir(String dir) { attributes.put("dir", dir); }   
    public void setDisabled(String disabled) { attributes.put("disabled", disabled); }   
    public void setLang(String lang) { attributes.put("lang", lang); }   
    public void setLanguage(String language) { attributes.put("language", language); }   
    public void setMaxlength(String maxLength) { attributes.put("maxlength", maxLength); }   
    public void setSize(String size) { attributes.put("size", size); }   
    public void setStyle(String style) { attributes.put("style", style); }   
    public void setTitle(String title) { attributes.put("title", title); }   
    public void setWidth(String width) { attributes.put("width", width); }   
    public void setOnactivate(String onactivate) { attributes.put("onactivate", onactivate); }   
    public void setOnafterupdate(String onafterupdate) { attributes.put("onafterupdate", onafterupdate); }   
    public void setOnbeforeactvate(String onbeforeactvate) { attributes.put("onbeforeactvate", onbeforeactvate); }   
    public void setOnbeforecut(String onbeforecut) { attributes.put("onbeforecut", onbeforecut); }   
    public void setOnbeforedeactivate(String onbeforedeactivate) { attributes.put("onbeforedeactivate", onbeforedeactivate); }   
    public void setOnbeforeeditfocus(String onbeforeeditfocus) { attributes.put("onbeforeeditfocus", onbeforeeditfocus); }   
    public void setOnbeforepaste(String onbeforepaste) { attributes.put("onbeforepaste", onbeforepaste); }   
    public void setOnbeforeupdate(String onbeforeupdate) { attributes.put("onbeforeupdate", onbeforeupdate); }   
    public void setOnblur(String onblur) { attributes.put("onblur", onblur); }   
    public void setOnchange(String onchange) { attributes.put("onchange", onchange); }   
    public void setOnclick(String onclick) { attributes.put("onclick", onclick); }   
    public void setOncontextmenu(String oncontextmenu) { attributes.put("oncontextmenu", oncontextmenu); }   
    public void setOncontrolselect(String oncontrolselect) { attributes.put("oncontrolselect", oncontrolselect); }   
    public void setOncut(String oncut) { attributes.put("oncut", oncut); }   
    public void setOndblclick(String ondblclick) { attributes.put("ondblclick", ondblclick); }   
    public void setOndeactivate(String ondeactivate) { attributes.put("ondeactivate", ondeactivate); }   
    public void setOndrag(String ondrag) { attributes.put("ondrag", ondrag); }   
    public void setOndragend(String ondragend) { attributes.put("ondragend", ondragend); }   
    public void setOndragenter(String ondragenter) { attributes.put("ondragenter", ondragenter); }   
    public void setOndragleave(String ondragleave) { attributes.put("ondragleave", ondragleave); }   
    public void setOndragover(String ondragover) { attributes.put("ondragover", ondragover); }   
    public void setOndragstart(String ondragstart) { attributes.put("ondragstart", ondragstart); }   
    public void setOndrop(String ondrop) { attributes.put("ondrop", ondrop); }   
    public void setOnerrorupdate(String onerrorupdate) { attributes.put("onerrorupdate", onerrorupdate); }   
    public void setOnfilterchange(String onfilterchange) { attributes.put("onfilterchange", onfilterchange); }   
    public void setOnfocus(String onfocus) { attributes.put("onfocus", onfocus); }   
    public void setOnfocusin(String onfocusin) { attributes.put("onfocusin", onfocusin); }   
    public void setOnfocusout(String onfocusout) { attributes.put("onfocusout", onfocusout); }   
    public void setOnhelp(String onhelp) { attributes.put("onhelp", onhelp); }   
    public void setOnkeydown(String onkeydown) { attributes.put("onkeydown", onkeydown); }   
    public void setOnkeypress(String onkeypress) { attributes.put("onkeypress", onkeypress); }   
    public void setOnkeyup(String onkeyup) { attributes.put("onkeyup", onkeyup); }   
    public void setOnlosecapture(String onlosecapture) { attributes.put("onlosecapture", onlosecapture); }   
    public void setOnmousedown(String onmousedown) { attributes.put("onmousedown", onmousedown); }   
    public void setOnmouseenter(String onmouseenter) { attributes.put("onmouseenter", onmouseenter); }   
    public void setOnmouseleave(String onmouseleave) { attributes.put("onmouseleave", onmouseleave); }   
    public void setOnmousemove(String onmousemove) { attributes.put("onmousemove", onmousemove); }   
    public void setOnmouseout(String onmouseout) { attributes.put("onmouseout", onmouseout); }   
    public void setOnmouseover(String onmouseover) { attributes.put("onmouseover", onmouseover); }   
    public void setOnmouseup(String onmouseup) { attributes.put("onmouseup", onmouseup); }   
    public void setOnmousewheel(String onmousewheel) { attributes.put("onmousewheel", onmousewheel); }   
    public void setOnmove(String onmove) { attributes.put("onmove", onmove); }   
    public void setOnmoveend(String onmoveend) { attributes.put("onmoveend", onmoveend); }   
    public void setOnmovestart(String onmovestart) { attributes.put("onmovestart", onmovestart); }   
    public void setOnpaste(String onpaste) { attributes.put("onpaste", onpaste); }   
    public void setOnpropertychange(String onpropertychange) { attributes.put("onpropertychange", onpropertychange); }   
    public void setOnreadystatechange(String onreadystatechange) { attributes.put("onreadystatechange", onreadystatechange); }   
    public void setOnresize(String onresize) { attributes.put("onresize", onresize); }   
    public void setOnresizeend(String onresizeend) { attributes.put("onresizeend", onresizeend); }   
    public void setOnresizestart(String onresizestart) { attributes.put("onresizestart", onresizestart); }   
    public void setOnselect(String onselect) { attributes.put("onselect", onselect); }   
    public void setOnselectstart(String onselectstart) { attributes.put("onselectstart", onselectstart); }   
    public void setOntimeerror(String ontimeerror) { attributes.put("ontimeerror", ontimeerror); }   
    public void setHidefocus(String hideFocus) { attributes.put("hidefocus", hideFocus); }   
    public void setReadonly(String readOnly) { attributes.put("readonly", readOnly); }   
    public void setTabindex(String tabIndex) { attributes.put("tabindex", tabIndex); }   
    public void setUnselectable(String unselectable) { attributes.put("unselectable", unselectable); }   
    public void setExtraAttributes(String extraAttributes) { this.extraAttributes = extraAttributes; }   
    
    protected Map<String, String> getAttributes() { return attributes; }   
    protected String getExtraAttributes() { return extraAttributes; } 

    /** Allow JSTL EL expressions within all attributes.
     *
     * @throws JspException An exception occurred evaluating the EL.
     */
    protected void evaluateAttributes()
        throws JspException {
        // assume the tag is the tag Class name, with the final 'Tag' removed, e.g.
        //    com.randomnoun.common.webapp.taglib.SelectTag
        // implements the custom tag
        //    select
        String tagName = this.getClass().getName();

        tagName = tagName.substring(tagName.lastIndexOf('.') + 1);
        tagName = tagName.toLowerCase();

        if (tagName.endsWith("Tag")) {
            tagName = tagName.substring(0, tagName.length() - 3);
        }

        /*
        try {
            Object object = ExpressionUtil.evalNotNull(
              tagName, "extraAttributes", extraAttributes, Object.class, this, pageContext);

            // how can this ever return null !?
            if (object != null) {
                extraAttributes = object.toString();
            }
        } catch (NullAttributeException ex) {
            // ignore
        }
        
        for (Iterator<String> i = attributes.keySet().iterator(); i.hasNext();) {
            String key = (String) i.next();
            String value = (String) attributes.get(key);

            try {
                value = ExpressionUtil.evalNotNull( 
                  tagName, key, value, Object.class, this, pageContext).toString();
                attributes.put(key, value);
            } catch (NullAttributeException ex) {
                // ignore
            }
        }
        */

        if ("false".equals(attributes.get("disabled"))) {
            attributes.remove("disabled");
        }

    }

    /** Returns a string with contains all attributes assigned to this tag, in key="value"
     * format.
     * 
     * <p>(NB: values are not currently quote-escaped here) 
     *
     * @return All attributes assigned to this tag.
     */
    protected String getAttributeString() {
        StringBuffer sb = new StringBuffer();
        for (Iterator<Map.Entry<String, String>> i = attributes.entrySet().iterator(); i.hasNext();) {
            Map.Entry<String, String> entry = i.next();
            sb.append(entry.getKey() + "=\"" + entry.getValue() + "\" ");
        }
        return sb.toString() + (extraAttributes == null ? "" : extraAttributes);
    }

    /** Call this function in your tag's doEnd() method, to clear all stored attributes
     *  from this tag. */
    protected void clearAttributes() {
        this.extraAttributes = null;
        attributes.clear();
    }
}
