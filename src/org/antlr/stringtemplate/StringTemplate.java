/*
 [The "BSD licence"]
 Copyright (c) 2003-2005 Terence Parr
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:
 1. Redistributions of source code must retain the above copyright
    notice, this list of conditions and the following disclaimer.
 2. Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.
 3. The name of the author may not be used to endorse or promote products
    derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package org.antlr.stringtemplate;

import java.io.*;
import java.util.*;
import java.lang.reflect.Constructor;

import org.antlr.stringtemplate.language.*;
import org.antlr.stringtemplate.language.*;
import org.antlr.stringtemplate.language.AttributeReflectionController;
import antlr.*;
import antlr.collections.AST;

/** A <TT>StringTemplate</TT> is a "document" with holes in it where you can stick
 *  values.  <TT>StringTemplate</TT> breaks up your template into chunks of text and
 *  attribute expressions, which are by default enclosed in angle brackets:
 * <TT>&lt;</TT><em>attribute-expression</em><TT>&gt;</TT>.  <TT>StringTemplate</TT>
 * ignores everything outside of attribute expressions, treating it as just text to spit
 * out when you call <TT>StringTemplate.toString()</TT>.
 *
 *  <P><TT>StringTemplate</TT> is not a "system" or "engine" or "server"; it's a lib
rary with two classes of interest: <TT>StringTemplate</TT> and <TT>StringTemplat
eGroup</TT>.  You can directly create a <TT>StringTemplate</TT> in Java code or
you can load a template from a file.
<P>
A StringTemplate describes an output pattern/language like an exemplar.
 *  <p>
 *  StringTemplate and associated code is released under the BSD licence.  See
 *  source.  <br><br>
 *  Copyright (c) 2003-2005 Terence Parr<br><br>

 *  A particular instance of a template may have a set of attributes that
 *  you set programmatically.  A template refers to these single or multi-
 *  valued attributes when writing itself out.  References within a
 *  template conform to a simple language with attribute references and
 *  references to other, embedded, templates.  The references are surrounded
 *  by user-defined start/stop strings (default of <...>, but $...$ works
 *  well when referencing attributes in HTML to distinguish from tags).
 *
 *  <p>StringTemplateGroup is a self-referential group of StringTemplate
 *  objects kind of like a grammar.  It is very useful for keeping a
 *  group of templates together.  For example, jGuru.com's premium and
 *  guest sites are completely separate sets of template files organized
 *  with a StringTemplateGroup.  Changing "skins" is a simple matter of
 *  switching groups.  Groups know where to load templates by either
 *  looking under a rootDir you can specify for the group or by simply
 *  looking for a resource file in the current class path.  If no rootDir
 *  is specified, template files are assumed to be resources.  So, if
 *  you reference template foo() and you have a rootDir, it looks for
 *  file rootDir/foo.st.  If you don't have a rootDir, it looks for
 *  file foo.st in the CLASSPATH.  note that you can use org/antlr/misc/foo()
 *  (qualified template names) as a template ref.
 *
 *  <p>StringTemplateErrorListener is an interface you can implement to
 *  specify where StringTemplate reports errors.  Setting the listener
 *  for a group automatically makes all associated StringTemplate
 *  objects use the same listener.  For example,
 *
 *  <font size=2><pre>
 *  StringTemplateGroup group = new StringTemplateGroup("loutSyndiags");
 *  group.setErrorListener(
 *     new StringTemplateErrorListener() {
 *        public void error(String msg, Exception e) {
 *           System.err.println("StringTemplate error: "+
 *               msg+((e!=null)?": "+e.getMessage():""));
 *        }
 *    }
 *  );
 *  </pre></font>
 *
 *  <p>IMPLEMENTATION
 *
 *  <p>A StringTemplate is both class and instance like in Self.  Given
 *  any StringTemplate (even one with attributes filled in), you can
 *  get a new "blank" instance of it.
 *
 *  <p>When you define a template, the string pattern is parsed and
 *  broken up into chunks of either String or attribute/template actions.
 *  These are typically just attribute references.  If a template is
 *  embedded within another template either via setAttribute or by
 *  implicit inclusion by referencing a template within a template, it
 *  inherits the attribute scope of the enclosing StringTemplate instance.
 *  All StringTemplate instances with the same pattern point to the same
 *  list of chunks since they are immutable there is no reason to have
 *  a copy in every instance of that pattern.  The only thing that differs
 *  is that every StringTemplate Java object can have its own set of
 *  attributes.  Each chunk points back at the original StringTemplate
 *  Java object whence they were constructed.  So, there are multiple
 *  pointers to the list of chunks (one for each instance with that
 *  pattern) and only one back ptr from a chunk to the original pattern
 *  object.  This is used primarily to get the grcoup of that original
 *  so new templates can be loaded into that group.
 *
 *  <p>To write out a template, the chunks are walked in order and asked to
 *  write themselves out.  String chunks are obviously just written out,
 *  but the attribute expressions/actions are evaluated in light of the
 *  attributes in that object and possibly in an enclosing instance.
 */
public class StringTemplate {
    public static final String VERSION = "2.2b3";

    /** An automatically created aggregate of properties.
     *
     *  I often have lists of things that need to be formatted, but the list
     *  items are actually pieces of data that are not already in an object.  I
     *  need ST to do something like:
     *
     *  Ter=3432
     *  Tom=32234
     *  ....
     *
     *  using template:
     *
     *  $items:{$attr.name$=$attr.type$}$
     *
     *  This example will call getName() on the objects in items attribute, but
     *  what if they aren't objects?  I have perhaps two parallel arrays
     *  instead of a single array of objects containing two fields.  One
     *  solution is allow Maps to be handled like properties so that it.name
     *  would fail getName() but then see that it's a Map and do
     *  it.get("name") instead.
     *
     *  This very clean approach is espoused by some, but the problem is that
     *  it's a hole in my separation rules.  People can put the logic in the
     *  view because you could say: "go get bob's data" in the view:
     *
     *  Bob's Phone: $db.bob.phone$
     *
     *  A view should not be part of the program and hence should never be able
     *  to go ask for a specific person's data.
     *
     *  After much thought, I finally decided on a simple solution.  I've
     *  added setAttribute variants that pass in multiple property values,
     *  with the property names specified as part of the name using a special
     *  attribute name syntax: "name.{propName1,propName2,...}".  This
     *  object is a special kind of HashMap that hopefully prevents people
     *  from passing a subclass or other variant that they have created as
     *  it would be a loophole.  Anyway, the ASTExpr.getObjectProperty()
     *  method looks for Aggregate as a special case and does a get() instead
     *  of getPropertyName.
     */
    public static final class Aggregate {
        protected HashMap properties = new HashMap();
        /** Allow StringTemplate to add values, but prevent the end
         *  user from doing so.
         */
        protected void put(String propName, Object propValue) {
            properties.put(propName, propValue);
        }
        public Object get(String propName) {
            return properties.get(propName);
        }
    }

    static boolean debugMode = false;

    /** track probable issues like setting attribute that is not referenced. */
    static boolean lintMode = false;

    protected List referencedAttributes = null;

	/** What's the name of this template? */
    protected String name = "anonymous";

    private static int templateCounter=0;
    private static synchronized int getNextTemplateCounter() {
        templateCounter++;
        return templateCounter;
    }
    /** reset the template ID counter to 0; public so that testing routine
     *  can access but not really of interest to the user.
     */
    public static void resetTemplateCounter() {
        templateCounter = 0;
    }

    protected int templateID = getNextTemplateCounter();

    /** Enclosing instance if I'm embedded within another template.
     *  IF-subtemplates are considered embedded as well.
     */
    protected StringTemplate enclosingInstance = null;

    /** A list of embedded templates */
    protected List embeddedInstances = null;

    /** If this template is an embedded template such as when you apply
     *  a template to an attribute, then the arguments passed to this
     *  template represent the argument context--a set of values
     *  computed by walking the argument assignment list.  For example,
     *  <name:bold(item=name, foo="x")> would result in an
     *  argument context of {[item=name], [foo="x"]} for this
     *  template.  This template would be the bold() template and
     *  the enclosingInstance would point at the template that held
     *  that <name:bold(...)> template call.  When you want to get
     *  an attribute value, you first check the attributes for the
     *  'self' template then the arg context then the enclosingInstance
     *  like resolving variables in pascal-like language with nested
     *  procedures.
     *
     *  With multi-valued attributes such as <faqList:briefFAQDisplay()>
     *  attribute "i" is set to 1..n.
     */
    protected Map argumentContext = null;

    /** If this template is embedded in another template, the arguments
     *  must be evaluated just before each application when applying
     *  template to a list of values.  The "it" attribute must change
     *  with each application so that $names:bold(item=it)$ works.  If
     *  you evaluate once before starting the application loop then it
     *  has a single fixed value.  Eval.g saves the AST rather than evaluating
     *  before invoking applyListOfAlternatingTemplates().  Each iteration
     *  of a template application to a multi-valued attribute, these args
     *  are re-evaluated with an initial context of {[it=...], [i=...]}.
     */
    protected StringTemplateAST argumentsAST = null;

    /** When templates are defined in a group file format, the attribute
     *  list is provided including information about attribute cardinality
     *  such as present, optional, ...  When this information is available,
     *  rawSetAttribute should do a quick existence check as should the
     *  invocation of other templates.  So if you ref bold(item="foo") but
     *  item is not defined in bold(), then an exception should be thrown.
     *  When actually rendering the template, the cardinality is checked.
     *  This is a Map<String,FormalArgument>.
     */
    protected Map formalArguments = FormalArgument.UNKNOWN;

	protected int numberOfDefaultArgumentValues = 0;

	protected boolean passThroughAttributes = false;

	/** What group originally defined the prototype for this template?
	 *  This affects the set of templates I can refer to.
	 */
	protected StringTemplateGroup group;

	/** Where to report errors */
    StringTemplateErrorListener listener = null;

	/** The original, immutable pattern/language (not really used again after
	 *  initial "compilation", setup/parsing).
	 */
    protected String pattern;

	/** Map an attribute name to its value(s).  These values are set by outside
	 *  code via st.setAttribute(name, value).  StringTemplate is like self in
     *  that a template is both the "class def" and "instance".  When you
     *  create a StringTemplate or setTemplate, the text is broken up into chunks
     *  (i.e., compiled down into a series of chunks that can be evaluated later).
     *  You can have multiple
	 */
    protected Map attributes;

	/** A list of alternating string and ASTExpr references.
	 *  This is compiled to when the template is loaded/defined and walked to
	 *  write out a template instance.
	 */
    protected List chunks;

	protected static StringTemplateGroup defaultGroup =
		new StringTemplateGroup("defaultGroup", ".");

	/** Create a blank template with no pattern and no attributes */
	public StringTemplate() {
		group = defaultGroup; // make sure has a group even if default
        if ( debugMode ) debug("new StringTemplate():"+getTemplateID());
    }

	/** Create an anonymous template.  It has no name just
	 *  chunks (which point to this anonymous template) and attributes.
	 */
    public StringTemplate(String template) {
        this(null, template);
        if ( debugMode ) debug("new StringTemplate(template):"+getTemplateID());
    }

    public StringTemplate(String template, Class lexer) {
        this();
        setGroup(new StringTemplateGroup("angleBracketsGroup", lexer));
        setTemplate(template);
    }

	/** Create an anonymous template with no name, but with a group */
	public StringTemplate(StringTemplateGroup group, String template) {
		this();
        if ( debugMode ) debug("new StringTemplate(group, ["+template+"]):"+getTemplateID());
		if ( group!=null ) {
			setGroup(group);
		}
		setTemplate(template);
   }

    /** Make the 'to' template look exactly like the 'from' template
     *  except for the attributes.  This is like creating an instance
     *  of a class in that the executable code is the same (the template
     *  chunks), but the instance data is blank (the attributes).  Do
     *  not copy the enclosingInstance pointer since you will want this
     *  template to eval in a context different from the examplar.
     */
    protected void dup(StringTemplate from, StringTemplate to) {
        if ( debugMode ) debug("dup template ID "+from.getTemplateID()+" to get "+to.getTemplateID());
        to.pattern = from.pattern;
		to.chunks = from.chunks;
        to.formalArguments = from.formalArguments;
		to.numberOfDefaultArgumentValues = from.numberOfDefaultArgumentValues;
		to.name = from.name;
		to.group = from.group;
		to.listener = from.listener;
    }

    /** Make an instance of this template; it contains an exact copy of
     *  everything (except the attributes and enclosing instance pointer).
     *  So the new template refers to the previously compiled chunks of this
     *  template but does not have any attribute values.
     */
    public StringTemplate getInstanceOf() {
		if ( debugMode ) debug("getInstanceOf("+getName()+")");
        StringTemplate t = group.createStringTemplate();
		dup(this, t);
		return t;
    }

    public StringTemplate getEnclosingInstance() {
        return enclosingInstance;
    }

    public void setEnclosingInstance(StringTemplate enclosingInstance) {
        if ( this==enclosingInstance ) {
            throw new IllegalArgumentException("cannot embed template "+getName()+" in itself");
        }
        // set the parent for this template
        this.enclosingInstance = enclosingInstance;
        // make the parent track this template as an embedded template
        if ( enclosingInstance!=null ) {
			this.enclosingInstance.addEmbeddedInstance(this);
		}
    }

    public void addEmbeddedInstance(StringTemplate embeddedInstance) {
        if ( this.embeddedInstances==null ) {
            this.embeddedInstances = new LinkedList();
        }
        this.embeddedInstances.add(embeddedInstance);
    }

    public Map getArgumentContext() {
        return argumentContext;
    }

    public void setArgumentContext(Map ac) {
        argumentContext = ac;
    }

    public StringTemplateAST getArgumentsAST() {
        return argumentsAST;
    }

    public void setArgumentsAST(StringTemplateAST argumentsAST) {
        this.argumentsAST = argumentsAST;
    }

    public String getName() {
        return name;
    }

    public String getOutermostName() {
        if ( enclosingInstance!=null ) {
            return enclosingInstance.getOutermostName();
        }
        return getName();
    }

    public void setName(String name) {
        this.name = name;
    }

	public StringTemplateGroup getGroup() {
		return group;
	}

	public void setGroup(StringTemplateGroup group) {
		this.group = group;
	}

    public void setTemplate(String template) {
		this.pattern = template;
		breakTemplateIntoChunks();
    }

    public String getTemplate() {
		return pattern;
    }

	public void setErrorListener(StringTemplateErrorListener listener) {
		this.listener = listener;
	}

    public StringTemplateErrorListener getErrorListener() {
        if ( listener==null ) {
            return group.getErrorListener();
        }
        return listener;
    }

    public void reset() {
		attributes = new HashMap(); // just throw out table and make new one
    }

    public void setPredefinedAttributes() {
        if ( !inLintMode() ) {
            return; // only do this method so far in lint mode
        }
        if ( formalArguments!=FormalArgument.UNKNOWN ) {
            // must define self before setting if the user has defined
            // formal arguments to the template
            defineFormalArgument(ASTExpr.REFLECTION_ATTRIBUTES);
        }
        if ( attributes==null ) {
            attributes = new HashMap();
        }
        rawSetAttribute(
                attributes,
                ASTExpr.REFLECTION_ATTRIBUTES,
                new AttributeReflectionController(this).toString());
    }

	public void removeAttribute(String name) {
		attributes.remove(name);
    }

	/** Set an attribute for this template.  If you set the same
	 *  attribute more than once, you get a multi-valued attribute.
	 *  If you send in a StringTemplate object as a value, it's
	 *  enclosing instance (where it will inherit values from) is
	 *  set to 'this'.  This would be the normal case, though you
	 *  can set it back to null after this call if you want.
	 *  If you send in a List plus other values to the same
	 *  attribute, they all get flattened into one List of values.
     *  If you send in an array, it is converted to a List.  Works
     *  with arrays of objects and arrays of {int,float,double}.
	 */
	public void setAttribute(String name, Object value) {
		if ( debugMode ) debug(getName()+".setAttribute("+name+", "+value+")");
		if ( value==null ) {
			return;
		}
        if ( attributes==null ) {
            attributes = new HashMap();
        }

        if ( value instanceof StringTemplate ) {
            ((StringTemplate)value).setEnclosingInstance(this);
        }
		else {
			// convert value if array
			value = ASTExpr.convertArrayToList(value);
		}

		// convert plain collections
		// get exactly in this scope (no enclosing)
		Object o = this.attributes.get(name);
		if ( o!=null ) { // it's a multi-value attribute
			//System.out.println("exists: "+name+"="+o);
			ArrayList v = null;
			if ( o instanceof ArrayList ) { // already a List
				v = (ArrayList)o;
				if ( value instanceof List ) {
					// flatten incoming list into existing
					// (do nothing if same List to avoid trouble)
					List v2 = (List)value;
					for (int i = 0; v!=v2 && i < v2.size(); i++) {
						// System.out.println("flattening "+name+"["+i+"]="+v2.elementAt(i)+" into existing value");
						v.add(v2.get(i));
					}
				}
				else {
					v.add(value);
				}
			}
			else {
				// second attribute, must convert existing to ArrayList
				v = new ArrayList(); // make list to hold multiple values
				// make it point to list now
				rawSetAttribute(this.attributes, name, v);
				v.add(o);  // add previous single-valued attribute
				if ( value instanceof List ) {
					// flatten incoming list into existing
					List v2 = (List)value;
					for (int i = 0; i < v2.size(); i++) {
						v.add(v2.get(i));
					}
				}
				else {
					v.add(value);
				}
			}
		}
		else {
			rawSetAttribute(this.attributes, name, value);
		}
	}

    /** Convenience method to box ints */
    public void setAttribute(String name, int value) {
        setAttribute(name, new Integer(value));
    }

    /** Set an aggregate attribute with two values.  The attribute name
     *  must have the format: "name.{propName1,propName2}".
     */
    public void setAttribute(String aggrSpec, Object v1, Object v2) {
        setAttribute(aggrSpec, new Object[] {v1,v2});
    }

    public void setAttribute(String aggrSpec, Object v1, Object v2, Object v3) {
        setAttribute(aggrSpec, new Object[] {v1,v2,v3});
    }

    public void setAttribute(String aggrSpec, Object v1, Object v2, Object v3, Object v4) {
        setAttribute(aggrSpec, new Object[] {v1,v2,v3,v4});
    }

    public void setAttribute(String aggrSpec, Object v1, Object v2, Object v3, Object v4, Object v5) {
        setAttribute(aggrSpec, new Object[] {v1,v2,v3,v4,v5});
    }

    /** Create an aggregate from the list of properties in aggrSpec and fill
     *  with values from values array.  This is not publically visible because
     *  it conflicts semantically with setAttribute("foo",new Object[] {...});
     */
    protected void setAttribute(String aggrSpec, Object[] values) {
        List properties = new ArrayList();
        String aggrName = parseAggregateAttributeSpec(aggrSpec, properties);
        if ( values==null || properties.size()==0 ) {
            throw new IllegalArgumentException("missing properties or values for '"+aggrSpec+"'");
        }
        if ( values.length != properties.size() ) {
            throw new IllegalArgumentException("number of properties in '"+aggrSpec+"' != number of values");
        }
        Aggregate aggr = new Aggregate();
        for (int i = 0; i < values.length; i++) {
            Object value = values[i];
			if ( value instanceof StringTemplate ) {
				((StringTemplate)value).setEnclosingInstance(this);
			}
			else {
				value = ASTExpr.convertArrayToList(value);
			}
            aggr.put((String)properties.get(i), value);
        }
        setAttribute(aggrName, aggr);
    }

    /** Split "aggrName.{propName1,propName2}" into list [propName1,propName2]
     *  and the aggrName.
     */
    protected String parseAggregateAttributeSpec(String aggrSpec, List properties) {
        int dot = aggrSpec.indexOf('.');
        if ( dot<=0 ) {
            throw new IllegalArgumentException("invalid aggregate attribute format: "+
                    aggrSpec);
        }
        String aggrName = aggrSpec.substring(0, dot);
        String propString = aggrSpec.substring(dot+1, aggrSpec.length());
        boolean error = true;
        StringTokenizer tokenizer = new StringTokenizer(propString, "{,}", true);
        match:
        if ( tokenizer.hasMoreTokens() ) {
            String token = tokenizer.nextToken(); // advance to {
            if ( token.equals("{") ) {
                token = tokenizer.nextToken();    // advance to first prop name
                properties.add(token);
                token = tokenizer.nextToken();    // advance to a comma
                while ( token.equals(",") ) {
                    token = tokenizer.nextToken();    // advance to a prop name
                    properties.add(token);
                    token = tokenizer.nextToken();    // advance to a "," or "}"
                }
                if ( token.equals("}") ) {
                    error = false;
                }
            }
        }
        if ( error ) {
            throw new IllegalArgumentException("invalid aggregate attribute format: "+
                    aggrSpec);
        }
        return aggrName;
    }

    /** Map a value to a named attribute.  Throw NoSuchElementException if
     *  the named attribute is not formally defined in self's specific template
     *  and a formal argument list exists.
     */
	protected void rawSetAttribute(Map attributes,
								   String name,
								   Object value)
	{
		if ( debugMode ) debug(getName()+".rawSetAttribute("+name+", "+value+")");
		if ( formalArguments!=FormalArgument.UNKNOWN &&
			getFormalArgument(name)==null )
		{
			// a normal call to setAttribute with unknown attribute
			throw new NoSuchElementException("no such attribute: "+name+
											 " in template context "+
											 getEnclosingInstanceStackString());
		}
		if ( value == null ) {
			return;
		}
		attributes.put(name, value);
	}

	/** Argument evaluation such as foo(x=y), x must
	 *  be checked against foo's argument list not this's (which is
 	 *  the enclosing context).  So far, only eval.g uses arg self as
	 *  something other than this.
	 */
	public void rawSetArgumentAttribute(StringTemplate embedded,
										Map attributes,
										String name,
										Object value)
	{
		if ( debugMode ) debug(getName()+".rawSetAttribute("+name+", "+value+")");
		if ( embedded.formalArguments!=FormalArgument.UNKNOWN &&
			 embedded.getFormalArgument(name)==null )
		{
			throw new NoSuchElementException("template "+embedded.getName()+
											 " has no such attribute: "+name+
											 " in template context "+
											 getEnclosingInstanceStackString());
		}
		if ( value == null ) {
			return;
		}
		attributes.put(name, value);
	}

	public Object getAttribute(String name) {
		return get(this,name);
    }

    /** Walk the chunks, asking them to write themselves out according
	 *  to attribute values of 'this.attributes'.  This is like evaluating or
     *  interpreting the StringTemplate as a program using the
     *  attributes.  The chunks will be identical (point at same list)
     *  for all instances of this template.
	 */
	public int write(StringTemplateWriter out) throws IOException {
		int n = 0;
        setPredefinedAttributes();
		setDefaultArgumentValues();
		for (int i=0; chunks!=null && i<chunks.size(); i++) {
			Expr a = (Expr)chunks.get(i);
			int chunkN = a.write(this, out);
			// NEWLINE expr-with-no-output NEWLINE => NEWLINE
			// Indented $...$ have the indent stored with the ASTExpr
			// so the indent does not come out as a StringRef
			if ( chunkN==0 &&
				(i-1)>=0 && chunks.get(i-1) instanceof NewlineRef &&
				(i+1)<chunks.size() && chunks.get(i+1) instanceof NewlineRef )
			{
				//System.out.println("found pure \\n blank \\n pattern");
				i++; // make it skip over the next chunk, the NEWLINE
			}
			n += chunkN;
		}
        if ( lintMode ) {
            checkForTrouble();
        }
		return n;
	}

    /** Resolve an attribute reference.  It can be in three possible places:
     *
     *  1. the attribute list for the current template
     *  2. if self is an embedded template, somebody invoked us possibly
     *     with arguments--check the argument context
     *  3. if self is an embedded template, the attribute list for the enclosing
     *     instance (recursively up the enclosing instance chain)
     *
     *  Attribute references are checked for validity.  If an attribute has
     *  a value, its validity was checked before template rendering.
     *  If the attribute has no value, then we must check to ensure it is a
     *  valid reference.  Somebody could reference any random value like $xyz$;
     *  formal arg checks before rendering cannot detect this--only the ref
     *  can initiate a validity check.  So, if no value, walk up the enclosed
     *  template tree again, this time checking formal parameters not
     *  attributes Map.  The formal definition must exist even if no value.
     *
     *  To avoid infinite recursion in toString(), we have another condition
     *  to check regarding attribute values.  If your template has a formal
     *  argument, foo, then foo will hide any value available from "above"
     *  in order to prevent infinite recursion.
     *
	 *  This method is not static so people can overrided functionality.
     */
    public Object get(StringTemplate self, String attribute) {
        //System.out.println("get("+self.getEnclosingInstanceStackString()+", "+attribute+")");
		if ( self==null ) {
			return null;
		}

        if ( lintMode ) {
            self.trackAttributeReference(attribute);
        }

        // is it here?
        Object o = null;
        if ( self.attributes!=null ) {
            o = self.attributes.get(attribute);
        }

        // nope, check argument context in case embedded
        if ( o==null ) {
            Map argContext = self.getArgumentContext();
            if ( argContext!=null ) {
                o = argContext.get(attribute);
            }
        }

        if ( o==null &&
			 !self.passThroughAttributes &&
			 self.getFormalArgument(attribute)!=null )
		{
            // if you've defined attribute as formal arg for this
            // template and it has no value, do not look up the
            // enclosing dynamic scopes.  This avoids potential infinite
            // recursion.
            return null;
        }

		// not locally defined, check enclosingInstance if embedded
		if ( o==null && self.enclosingInstance!=null ) {
            /*
            System.out.println("looking for "+getName()+"."+attribute+" in super="+
                    enclosingInstance.getName());
			*/
            Object valueFromEnclosing = get(self.enclosingInstance, attribute);
            if ( valueFromEnclosing==null ) {
                checkNullAttributeAgainstFormalArguments(self, attribute);
            }
            o = valueFromEnclosing;
		}

		return o;
    }

	/** Walk a template, breaking it into a list of
	 *  chunks: Strings and actions/expressions.
	 */
	protected void breakTemplateIntoChunks() {
        //System.out.println("parsing template: "+pattern);
        if ( pattern==null ) {
            return;
        }
        try {
            // instead of creating a specific template lexer, use
            // an instance of the class specified by the user.
            // The default is DefaultTemplateLexer.
            // The only constraint is that you use an ANTLR lexer
            // so I can use the special ChunkToken.
            Class lexerClass = group.getTemplateLexerClass();
            Constructor ctor =
                    lexerClass.getConstructor(
						new Class[] {StringTemplate.class,Reader.class}
					);
            CharScanner chunkStream =
                    (CharScanner) ctor.newInstance(
						new Object[] {this,new StringReader(pattern)}
					);
            chunkStream.setTokenObjectClass("org.antlr.stringtemplate.language.ChunkToken");
            TemplateParser chunkifier = new TemplateParser(chunkStream);
            chunkifier.template(this);
        }
        catch (Exception e) {
            String name = "<unknown>";
            String outerName = getOutermostName();
            if ( getName()!=null ) {
                name = getName();
            }
            if ( outerName!=null && !name.equals(outerName) ) {
                name = name+" nested in "+outerName;
            }
            error("problem parsing template '"+name+"'", e);
        }
	}

    public ASTExpr parseAction(String action) {
		ActionLexer lexer =
			new ActionLexer(new StringReader(action.toString()));
		ActionParser parser =
			new ActionParser(lexer, this);
        parser.setASTNodeClass("org.antlr.stringtemplate.language.StringTemplateAST");
		ASTExpr a = null;
		try {
			Map options = parser.action();
            AST tree = parser.getAST();
            if ( tree!=null ) {
                if ( tree.getType()==ActionParser.CONDITIONAL ) {
                    a = new ConditionalExpr(this,tree);
                }
                else {
                    a = new ASTExpr(this,tree,options);
                }
            }
		}
		catch (RecognitionException re) {
			error("Can't parse chunk: "+action.toString(), re);
		}
		catch (TokenStreamException tse) {
			error("Can't parse chunk: "+action.toString(), tse);
		}
		return a;
	}

    public int getTemplateID() {
        return templateID;
    }

    public Map getAttributes() {
        return attributes;
    }

	/** Get a list of the strings and subtemplates and attribute
	 *  refs in a template.
	 */
	public List getChunks() {
		return chunks;
	}

    public void addChunk(Expr e) {
        if ( chunks==null ) {
            chunks = new ArrayList();
        }
        chunks.add(e);
    }

    public void setAttributes(Map attributes) {
		this.attributes = attributes;
	}

    // F o r m a l  A r g  S t u f f

	public Map getFormArguments() {
		return formalArguments;
	}

	/*
	public int getNumberOfFormArgumentsWithDefaultValues() {
		int n = 0;
		Set argNames = formalArguments.keySet();
		for (Iterator it = argNames.iterator(); it.hasNext();) {
			String argName = (String) it.next();
			FormalArgument arg = (FormalArgument)formalArguments.get(argName);
			if ( arg.defaultValueST!=null ) {
				n++;
			}
		}
		return n;
	}
	*/

    public void setFormalArguments(Map args) {
        formalArguments = args;
    }

	/** Set any default argument values that were not set by the
	 *  invoking template or by setAttribute directly.  Note
	 *  that the default values may be templates.  Their evaluation
	 *  context is the template itself and, hence, can see attributes
	 *  within the template, any arguments, and any values inherited
	 *  by the template.
	 *
	 *  Default values are stored in the argument context rather than
	 *  the template attributes table just for consistency's sake.
	 */
	public void setDefaultArgumentValues() {
		if ( numberOfDefaultArgumentValues==0 ) {
			return;
		}
		if ( argumentContext==null ) {
			argumentContext = new HashMap();
		}
		Map formalArguments = getFormArguments();
		if ( formalArguments!=FormalArgument.UNKNOWN ) {
			Set argNames = formalArguments.keySet();
			for (Iterator it = argNames.iterator(); it.hasNext();) {
				String argName = (String) it.next();
				// use the default value then
				FormalArgument arg =
					(FormalArgument)formalArguments.get(argName);
				if ( arg.defaultValueST!=null ) {
					Object existingValue = getAttribute(argName);
					if ( existingValue==null ) { // value unset?
						// evaluate default value by creating a new
						// instance enclosed within the argument context
						StringTemplate defaultEvalST =
							arg.defaultValueST.getInstanceOf();
						defaultEvalST.setEnclosingInstance(this);
						argumentContext.put(argName, defaultEvalST);
					}
				}
			}
		}
	}

	/** From this template upward in the enclosing template tree,
     *  recursively look for the formal parameter.
     */
    public FormalArgument lookupFormalArgument(String name) {
        FormalArgument arg = getFormalArgument(name);
        if ( arg==null && enclosingInstance!=null ) {
            arg = enclosingInstance.lookupFormalArgument(name);
        }
        return arg;
    }

    public FormalArgument getFormalArgument(String name) {
        return (FormalArgument)formalArguments.get(name);
    }

    public void defineEmptyFormalArgumentList() {
        setFormalArguments(new HashMap());
    }

	public void defineFormalArgument(String name) {
		defineFormalArgument(name,null);
	}

    public void defineFormalArgument(String name, String defaultValue) {
		StringTemplate defaultValueST = null;
		if ( defaultValue!=null ) {
			numberOfDefaultArgumentValues++;
			defaultValueST = new StringTemplate(getGroup(), defaultValue);
			defaultValueST.setName("<"+name+" default value subtemplate>");
		}
        FormalArgument a = new FormalArgument(name,defaultValueST);
        if ( formalArguments==FormalArgument.UNKNOWN ) {
            formalArguments = new HashMap();
        }
        formalArguments.put(name, a);
    }

	/** Normally if you call template y from x, y cannot see any attributes
	 *  of x that are defined as formal parameters of y.  Setting this
	 *  passThroughAttributes to true, will override that and allow a
	 *  template to see through the formal arg list to inherited values.
	 */
	public void setPassThroughAttributes(boolean passThroughAttributes) {
		this.passThroughAttributes = passThroughAttributes;
	}

    // U T I L I T Y  R O U T I N E S

    public void error(String msg) {
        error(msg, null);
    }

    public void warning(String msg) {
        if ( getErrorListener()!=null ) {
            getErrorListener().warning(msg);
        }
        else {
            System.err.println("StringTemplate: warning: "+msg);
        }
    }

    public void debug(String msg) {
        if ( getErrorListener()!=null ) {
            getErrorListener().debug(msg);
        }
        else {
            System.err.println("StringTemplate: debug: "+msg);
        }
    }

	public void error(String msg, Throwable e) {
		if ( getErrorListener()!=null ) {
			getErrorListener().error(msg,e);
		}
		else {
			if ( e!=null ) {
                System.err.println("StringTemplate: error: "+msg+": "+e.toString());
            }
            else {
                System.err.println("StringTemplate: error: "+msg);
            }
		}
    }

    /** Make StringTemplate check your work as it evaluates templates.
     *  Problems are sent to error listener.   Currently warns when
     *  you set attributes that are not used.
     */
    public static void setLintMode(boolean lint) {
        StringTemplate.lintMode = lint;
    }

    public static boolean inLintMode() {
        return lintMode;
    }

    public static boolean isDebugMode() {
        return debugMode;
    }

    /** DEBUG MODE IS PRETTY MUCH USELESS AT THE MOMENT! */
	public static void setDebugMode(boolean debug) {
        StringTemplate.debugMode = debug;
    }

    /** Indicates that 'name' has been referenced in this template. */
    protected void trackAttributeReference(String name) {
        if ( referencedAttributes==null ) {
            referencedAttributes = new ArrayList();
        }
        referencedAttributes.add(name);
    }

    /** Look up the enclosing instance chain (and include this) to see
     *  if st is a template already in the enclosing instance chain.
     */
    public static boolean isRecursiveEnclosingInstance(StringTemplate st) {
        if ( st==null ) {
            return false;
        }
        StringTemplate p = st.enclosingInstance;
        if ( p==st ) {
            return true; // self-recursive
        }
        // now look for indirect recursion
        while ( p!=null ) {
            if ( p==st ) {
                return true;
            }
            p = p.enclosingInstance;
        }
        return false;
    }

    public String getEnclosingInstanceStackTrace() {
        StringBuffer buf = new StringBuffer();
        Set seen = new HashSet();
        StringTemplate p = this;
        while ( p!=null ) {
            if ( seen.contains(p) ) {
                buf.append(p.getTemplateDeclaratorString());
                buf.append(" (start of recursive cycle)");
                buf.append("\n");
                buf.append("...");
                break;
            }
            seen.add(p);
            buf.append(p.getTemplateDeclaratorString());
            if ( p.attributes!=null ) {
                buf.append(", attributes=[");
                int i = 0;
                for (Iterator iter = p.attributes.keySet().iterator(); iter.hasNext();) {
                    String attrName = (String) iter.next();
                    if ( i>0 ) {
                        buf.append(", ");
                    }
                    i++;
                    buf.append(attrName);
                    Object o = p.attributes.get(attrName);
                    if ( o instanceof StringTemplate ) {
                        StringTemplate st = (StringTemplate)o;
                        buf.append("=");
                        buf.append("<");
                        buf.append(st.getName());
                        buf.append("()@");
                        buf.append(String.valueOf(st.getTemplateID()));
                        buf.append(">");
                    }
                    else if ( o instanceof List ) {
                        buf.append("=List[..");
                        List list = (List)o;
                        int n=0;
                        for (int j = 0; j < list.size(); j++) {
                            Object listValue = list.get(j);
                            if ( listValue instanceof StringTemplate ) {
                                if ( n>0 ) {
                                    buf.append(", ");
                                }
                                n++;
                                StringTemplate st = (StringTemplate)listValue;
                                buf.append("<");
                                buf.append(st.getName());
                                buf.append("()@");
                                buf.append(String.valueOf(st.getTemplateID()));
                                buf.append(">");
                            }
                        }
                        buf.append("..]");
                    }
                }
                buf.append("]");
            }
            if ( p.referencedAttributes!=null ) {
                buf.append(", references=");
                buf.append(p.referencedAttributes);
            }
            buf.append(">\n");
            p = p.enclosingInstance;
        }
        /*
        if ( enclosingInstance!=null ) {
        buf.append(enclosingInstance.getEnclosingInstanceStackTrace());
        }
        */
        return buf.toString();
    }

    public String getTemplateDeclaratorString() {
        StringBuffer buf = new StringBuffer();
        buf.append("<");
        buf.append(getName());
        buf.append("(");
        buf.append(formalArguments.keySet());
        buf.append(")@");
        buf.append(String.valueOf(getTemplateID()));
        buf.append(">");
        return buf.toString();
    }

    /** Find "missing attribute" and "cardinality mismatch" errors.
     *  Excecuted before a template writes its chunks out.
     *  When you find a problem, throw an IllegalArgumentException.
     *  We must check the attributes as well as the incoming arguments
     *  in argumentContext.
    protected void checkAttributesAgainstFormalArguments() {
        Set args = formalArguments.keySet();
        /*
        if ( (attributes==null||attributes.size()==0) &&
             (argumentContext==null||argumentContext.size()==0) &&
             formalArguments.size()!=0 )
        {
            throw new IllegalArgumentException("missing argument(s): "+args+" in template "+getName());
        }
        Iterator iter = args.iterator();
        while ( iter.hasNext() ) {
            String argName = (String)iter.next();
            FormalArgument arg = getFormalArgument(argName);
            int expectedCardinality = arg.getCardinality();
            Object value = getAttribute(argName);
            int actualCardinality = getActualArgumentCardinality(value);
            // if intersection of expected and actual is empty, mismatch
            if ( (expectedCardinality&actualCardinality)==0 ) {
                throw new IllegalArgumentException("cardinality mismatch: "+
                        argName+"; expected "+
                        FormalArgument.getCardinalityName(expectedCardinality)+
                        " found cardinality="+getObjectLength(value));
            }
        }
    }
*/

    /** A reference to an attribute with no value, must be compared against
     *  the formal parameter to see if it exists; if it exists all is well,
     *  but if not, throw an exception.
     *
     *  Don't do the check if no formal parameters exist for this template;
     *  ask enclosing.
     */
    protected void checkNullAttributeAgainstFormalArguments(
            StringTemplate self,
            String attribute)
    {
        if ( self.getFormArguments()==FormalArgument.UNKNOWN ) {
            // bypass unknown arg lists
            if ( self.enclosingInstance!=null ) {
                checkNullAttributeAgainstFormalArguments(
                        self.enclosingInstance,
                        attribute);
            }
            return;
        }
        FormalArgument formalArg = lookupFormalArgument(attribute);
        if ( formalArg == null ) {
			throw new NoSuchElementException("no such attribute: "+attribute+
											 " in template context "+getEnclosingInstanceStackString());
        }
    }

    /** Executed after evaluating a template.  For now, checks for setting
     *  of attributes not reference.
     */
    protected void checkForTrouble() {
        // we have table of set values and list of values referenced
        // compare, looking for SET BUT NOT REFERENCED ATTRIBUTES
        if ( attributes==null ) {
            return;
        }
        Set names = attributes.keySet();
        Iterator iter = names.iterator();
		// if in names and not in referenced attributes, trouble
        while ( iter.hasNext() ) {
            String name = (String)iter.next();
            if ( referencedAttributes!=null &&
                !referencedAttributes.contains(name) &&
                !name.equals(ASTExpr.REFLECTION_ATTRIBUTES) )
            {
                warning(getName()+": set but not used: "+name);
            }
        }
        // can do the reverse, but will have lots of false warnings :(
    }

	/** If an instance of x is enclosed in a y which is in a z, return
	 *  a String of these instance names in order from topmost to lowest;
	 *  here that would be "[z y x]".
	 */
    public String getEnclosingInstanceStackString() {
		List names = new LinkedList();
		StringTemplate p = this;
		while ( p!=null ) {
			names.add(0,p.getName()+(p.passThroughAttributes?"(...)":""));
			p = p.enclosingInstance;
		}
		return names.toString().replaceAll(",","");
	}

    /** Compute a bitset of valid cardinality for an actual attribute value.
     *  A length of 0, satisfies OPTIONAL|ZERO_OR_MORE
     *  whereas a length of 1 satisfies all. A length>1
     *  satisfies ZERO_OR_MORE|ONE_OR_MORE
	 *
	 *  UNUSED
    public static int getActualArgumentCardinality(Object value) {
        int actualLength = getObjectLength(value);
        if ( actualLength==0 ) {
            return FormalArgument.OPTIONAL|FormalArgument.ZERO_OR_MORE;
        }
        if ( actualLength==1 ) {
            return FormalArgument.REQUIRED|
                   FormalArgument.ONE_OR_MORE|
                   FormalArgument.ZERO_OR_MORE|
                   FormalArgument.OPTIONAL;
        }
        return FormalArgument.ZERO_OR_MORE|FormalArgument.ONE_OR_MORE;
    }
     */

	/**  UNUSED */
    /*
    protected static int getObjectLength(Object value) {
        int actualLength = 0;
        if ( value!=null ) {
            if ( value instanceof Collection ) {
                actualLength = ((Collection)value).size();
            }
            if ( value instanceof Map ) {
                actualLength = ((Map)value).size();
            }
            else {
                actualLength = 1;
            }
        }

        return actualLength;
    }
    */

    public String toDebugString() {
        StringBuffer buf = new StringBuffer();
        buf.append("template-"+getName()+":");
        buf.append("chunks=");;
        buf.append(chunks.toString());
        buf.append("attributes=[");
        Set attrNames = attributes.keySet();
        int n=0;
        for (Iterator iter = attrNames.iterator(); iter.hasNext();) {
            if ( n>0 ) {
                buf.append(',');
            }
            String name = (String) iter.next();
            buf.append(name+"=");
            Object value = attributes.get(name);
            if ( value instanceof StringTemplate ) {
                buf.append(((StringTemplate)value).toDebugString());
            }
            else {
                buf.append(value);
            }
            n++;
        }
        buf.append("]");
        return buf.toString();
    }

    public void printDebugString() {
        System.out.println("template-"+getName()+":");
        System.out.print("chunks=");
        System.out.println(chunks.toString());
        if ( attributes==null ) {
            return;
        }
        System.out.print("attributes=[");
        Set attrNames = attributes.keySet();
        int n=0;
        for (Iterator iter = attrNames.iterator(); iter.hasNext();) {
            if ( n>0 ) {
                System.out.print(',');
            }
            String name = (String) iter.next();
            Object value = attributes.get(name);
            if ( value instanceof StringTemplate ) {
                System.out.print(name+"=");
                ((StringTemplate)value).printDebugString();
            }
            else {
                if ( value instanceof List ) {
                    ArrayList alist = (ArrayList)value;
                    for (int i = 0; i < alist.size(); i++) {
                        Object o = (Object) alist.get(i);
                        System.out.print(name+"["+i+"] is "+o.getClass().getName()+"=");
                        if ( o instanceof StringTemplate ) {
                            ((StringTemplate)o).printDebugString();
                        }
                        else {
                            System.out.println(o);
                        }
                    }
                }
                else {
                    System.out.print(name+"=");
                    System.out.println(value);
                }
            }
            n++;
        }
        System.out.print("]\n");
    }

    public String toString() {
        StringWriter out = new StringWriter();
		// Write the output to a StringWriter
		// TODO seems slow to create all these objects, can I use a singleton?
		StringTemplateWriter wr = group.getStringTemplateWriter(out);
        //StringTemplateWriter buf = new AutoIndentWriter(out);
        try {
            write(wr);
        }
        catch (IOException io) {
            error("Got IOException writing to StringWriter????");
        }
        return out.toString();
    }

}
