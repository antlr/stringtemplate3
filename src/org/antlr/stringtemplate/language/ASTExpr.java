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
package org.antlr.stringtemplate.language;

import antlr.RecognitionException;
import antlr.collections.AST;
import org.antlr.stringtemplate.*;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.*;
import java.util.*;

/** A single string template expression enclosed in $...; separator=...$
 *  parsed into an AST chunk to be evaluated.
 */
public class ASTExpr extends Expr {
    public static final String DEFAULT_ATTRIBUTE_NAME = "it";
    public static final String DEFAULT_ATTRIBUTE_NAME_DEPRECATED = "attr";
	public static final String DEFAULT_INDEX_VARIABLE_NAME = "i";
	public static final String DEFAULT_INDEX0_VARIABLE_NAME = "i0";
	public static final String DEFAULT_MAP_VALUE_NAME = "_default_";
	public static final String DEFAULT_MAP_KEY_NAME = "key";

	/** Used to indicate "default:key" in maps within groups */
	public static final StringTemplate MAP_KEY_VALUE = new StringTemplate();

	// used temporarily for checking obj.prop cache
	public static int totalObjPropRefs = 0;
	public static int totalReflectionLookups = 0;

	AST exprTree = null;

    /** store separator etc... */
    Map options = null;

    public ASTExpr(StringTemplate enclosingTemplate, AST exprTree, Map options) {
		super(enclosingTemplate);
        this.exprTree = exprTree;
        this.options = options;
    }

	/** Return the tree interpreted when this template is written out. */
	public AST getAST() {
		return exprTree;
	}

    /** To write out the value of an ASTExpr, invoke the evaluator in eval.g
     *  to walk the tree writing out the values.  For efficiency, don't
     *  compute a bunch of strings and then pack them together.  Write out directly.
     */
    public int write(StringTemplate self, StringTemplateWriter out) throws IOException {
        if ( exprTree==null || self==null || out==null ) {
            return 0;
        }
        out.pushIndentation(getIndentation());
        //System.out.println("evaluating tree: "+exprTree.toStringList());
        ActionEvaluator eval =
                new ActionEvaluator(self,this,out);
		int n = 0;
        try {
            n = eval.action(exprTree); // eval and write out tree
        }
        catch (RecognitionException re) {
            self.error("can't evaluate tree: "+exprTree.toStringList(), re);
        }
        out.popIndentation();
		return n;
    }

    // HELP ROUTINES CALLED BY EVALUATOR TREE WALKER

	/** For <names,phones:{n,p | ...}> treat the names, phones as lists
	 *  to be walked in lock step as n=names[i], p=phones[i].
	 */
	public Object applyTemplateToListOfAttributes(StringTemplate self,
												  List attributes,
												  StringTemplate templateToApply)
	{
		if ( attributes==null || templateToApply==null || attributes.size()==0 ) {
			return null; // do not apply if missing templates or empty values
		}
		Map argumentContext = null;
		// indicate it's an ST-created list
		List results = new StringTemplate.STAttributeList();

		// convert all attributes to iterators even if just one value
		for (int a = 0; a < attributes.size(); a++) {
			Object o = (Object) attributes.get(a);
			if ( o!=null ) {
				o = convertAnythingToIterator(o);
				attributes.set(a, o); // alter the list in place
			}
		}

		int numAttributes = attributes.size();

		// ensure arguments line up
		Map formalArguments = templateToApply.getFormalArguments();
		if ( formalArguments==null || formalArguments.size()==0 ) {
			self.error("missing arguments in anonymous"+
					   " template in context "+self.getEnclosingInstanceStackString());
			return null;
		}
		Object[] formalArgumentNames = formalArguments.keySet().toArray();
		if ( formalArgumentNames.length!=numAttributes ) {
			self.error("number of arguments "+formalArguments.keySet()+
					   " mismatch between attribute list and anonymous"+
					   " template in context "+self.getEnclosingInstanceStackString());
			// truncate arg list to match smaller size
			int shorterSize = Math.min(formalArgumentNames.length, numAttributes);
			numAttributes = shorterSize;
			Object[] newFormalArgumentNames = new Object[shorterSize];
			System.arraycopy(formalArgumentNames, 0,
							 newFormalArgumentNames, 0,
							 shorterSize);
			formalArgumentNames = newFormalArgumentNames;
		}

		// keep walking while at least one attribute has values
		int i = 0; // iteration number from 0
		while ( true ) {
			argumentContext = new HashMap();
			// get a value for each attribute in list; put into arg context
			// to simulate template invocation of anonymous template
			int numEmpty = 0;
			for (int a = 0; a < numAttributes; a++) {
				Iterator it = (Iterator) attributes.get(a);
				if ( it!=null && it.hasNext() ) {
					String argName = (String)formalArgumentNames[a];
					Object iteratedValue = it.next();
					argumentContext.put(argName, iteratedValue);
				}
				else {
					numEmpty++;
				}
			}
			if ( numEmpty==numAttributes ) {
				break;
			}
			argumentContext.put(DEFAULT_INDEX_VARIABLE_NAME, new Integer(i+1));
			argumentContext.put(DEFAULT_INDEX0_VARIABLE_NAME, new Integer(i));
			StringTemplate embedded = templateToApply.getInstanceOf();
			embedded.setEnclosingInstance(self);
			embedded.setArgumentContext(argumentContext);
			results.add(embedded);
			i++;
		}

		return results;
	}

	public Object applyListOfAlternatingTemplates(StringTemplate self,
                                                  Object attributeValue,
                                                  List templatesToApply)
    {
        if ( attributeValue==null || templatesToApply==null || templatesToApply.size()==0 ) {
            return null; // do not apply if missing templates or empty value
        }
        StringTemplate embedded = null;
        Map argumentContext = null;

        // normalize collections and such to use iterators
        // anything iteratable can be used for "APPLY"
		attributeValue = convertAnythingIteratableToIterator(attributeValue);

        if ( attributeValue instanceof Iterator ) {
			// results can be treated list an attribute, indicate ST created list
			List resultVector = new StringTemplate.STAttributeList();
            Iterator iter = (Iterator)attributeValue;
            int i = 0;
            while ( iter.hasNext() ) {
                Object ithValue = iter.next();
                if ( ithValue==null ) {
                    // weird...a null value in the list; ignore
                    continue;
                }
                int templateIndex = i % templatesToApply.size(); // rotate through
                embedded = (StringTemplate)templatesToApply.get(templateIndex);
                // template to apply is an actual StringTemplate (created in
                // eval.g), but that is used as the examplar.  We must create
                // a new instance of the embedded template to apply each time
                // to get new attribute sets etc...
                StringTemplateAST args = embedded.getArgumentsAST();
                embedded = embedded.getInstanceOf(); // make new instance
                embedded.setEnclosingInstance(self);
                embedded.setArgumentsAST(args);
                argumentContext = new HashMap();
				Map formalArgs = embedded.getFormalArguments();
				boolean isAnonymous =
					embedded.getName() == StringTemplate.ANONYMOUS_ST_NAME;
				setSoleFormalArgumentToIthValue(embedded, argumentContext, ithValue);
				// if it's an anonymous template with a formal arg, don't set it/attr
				if ( !(isAnonymous && formalArgs!=null && formalArgs.size()>0) ) {
					argumentContext.put(DEFAULT_ATTRIBUTE_NAME, ithValue);
	                argumentContext.put(DEFAULT_ATTRIBUTE_NAME_DEPRECATED, ithValue);
				}
				argumentContext.put(DEFAULT_INDEX_VARIABLE_NAME, new Integer(i+1));
				argumentContext.put(DEFAULT_INDEX0_VARIABLE_NAME, new Integer(i));
                embedded.setArgumentContext(argumentContext);
                evaluateArguments(embedded);
                /*
				System.err.println("i="+i+": applyTemplate("+embedded.getName()+
                        ", args="+argumentContext+
                        " to attribute value "+ithValue);
                */
				resultVector.add(embedded);
                i++;
            }
			if ( resultVector.size()==0 ) {
				resultVector = null;
			}
			return resultVector;
        }
        else {
            /*
            System.out.println("setting attribute "+DEFAULT_ATTRIBUTE_NAME+" in arg context of "+
            embedded.getName()+
            " to "+attributeValue);
            */
            embedded = (StringTemplate)templatesToApply.get(0);
            argumentContext = new HashMap();
			Map formalArgs = embedded.getFormalArguments();
			StringTemplateAST args = embedded.getArgumentsAST();
			setSoleFormalArgumentToIthValue(embedded, argumentContext, attributeValue);
			boolean isAnonymous =
				embedded.getName() == StringTemplate.ANONYMOUS_ST_NAME;
			// if it's an anonymous template with a formal arg, don't set it/attr
			if ( !(isAnonymous && formalArgs!=null && formalArgs.size()>0) ) {
				argumentContext.put(DEFAULT_ATTRIBUTE_NAME, attributeValue);
				argumentContext.put(DEFAULT_ATTRIBUTE_NAME_DEPRECATED, attributeValue);
			}
			argumentContext.put(DEFAULT_INDEX_VARIABLE_NAME, new Integer(1));
            embedded.setArgumentContext(argumentContext);
            evaluateArguments(embedded);
            return embedded;
        }
    }

	protected void setSoleFormalArgumentToIthValue(StringTemplate embedded, Map argumentContext, Object ithValue) {
		Map formalArgs = embedded.getFormalArguments();
		if ( formalArgs!=null ) {
			String soleArgName = null;
			boolean isAnonymous =
				embedded.getName() == StringTemplate.ANONYMOUS_ST_NAME;
			if ( formalArgs.size()==1 || (isAnonymous&&formalArgs.size()>0) ) {
				if ( isAnonymous && formalArgs.size()>1 ) {
					embedded.error("too many arguments on {...} template: "+formalArgs);
				}
				// if exactly 1 arg or anonymous, give that the value of
				// "it" as a convenience like they said
				// $list:template(arg=it)$
				Set argNames = formalArgs.keySet();
				soleArgName = (String)argNames.toArray()[0];
				argumentContext.put(soleArgName, ithValue);
			}
		}
	}

	/** Return o.getPropertyName() given o and propertyName.  If o is
     *  a stringtemplate then access it's attributes looking for propertyName
     *  instead (don't check any of the enclosing scopes; look directly into
     *  that object).  Also try isXXX() for booleans.  Allow Map
	 *  as special case (grab value for key).
	 *
	 *  Cache repeated requests for obj.prop within same group.
     */
	public Object getObjectProperty(StringTemplate self,
									final Object o,
									final String propertyName) {
		if ( o==null || propertyName==null ) {
			return null;
		}
		totalObjPropRefs++;
		/*
		// see if property is cached in group's cache
		Object cachedValue =
			self.getGroup().getCachedObjectProperty(o,propertyName);
		if ( cachedValue!=null ) {
			return cachedValue;
		}
		Object value = rawGetObjectProperty(self, o, propertyName);
		// take care of array properties...convert to a List so we can
		// apply templates to the elements etc...
		value = convertArrayToList(value);
		self.getGroup().cacheObjectProperty(o,propertyName,value);
		*/
		Object value = rawGetObjectProperty(self, o, propertyName);
		// take care of array properties...convert to a List so we can
		// apply templates to the elements etc...
		value = convertArrayToList(value);
		return value;
	}

	protected Object rawGetObjectProperty(StringTemplate self, Object o, String propertyName) {
		Class c = o.getClass();
        Object value = null;

        // Special case: our automatically created Aggregates via
        // attribute name: "{obj.{prop1,prop2}}"
        if ( c==StringTemplate.Aggregate.class ) {
            value = ((StringTemplate.Aggregate)o).get(propertyName);
			return value;
        }

        // Special case: if it's a template, pull property from
        // it's attribute table.
        // TODO: TJP just asked himself why we can't do inherited attr here?
        else if ( c==StringTemplate.class ) {
            Map attributes = ((StringTemplate)o).getAttributes();
            if ( attributes!=null ) {
                value = attributes.get(propertyName);
				return value;
            }
        }

        // Special case: if it's a Map then pull using
        // key not the property method.
        if ( o instanceof Map ) {
            Map map = (Map)o;
			if ( map.containsKey(propertyName) ) {
				value = map.get(propertyName);
			}
			else {
				if ( map.containsKey(DEFAULT_MAP_VALUE_NAME) ) {
					value = map.get(DEFAULT_MAP_VALUE_NAME);
				}
			}
			if ( value == MAP_KEY_VALUE ) {
				value = propertyName;
			}
			return value;
        }

		// try getXXX and isXXX properties

		// check cache
		Method m = null;
		Member cachedMember =
			self.getGroup().getCachedClassProperty(c,propertyName);
		if ( cachedMember!=null ) {
			try {
				if ( cachedMember instanceof Method ) {
					m = (Method)cachedMember;
					value = invokeMethod(m, o, value);
				}
				else {
					// must be a field
					Field f = (Field)cachedMember;
					value = accessField(f, o, value);
				}
			}
			catch (Exception e) {
				self.error("Can't get property "+propertyName+
					" from "+c.getName()+" instance", e);
			}
			return value;
		}

		// must look up using reflection
		String methodSuffix = Character.toUpperCase(propertyName.charAt(0))+
			propertyName.substring(1,propertyName.length());
		totalReflectionLookups++;
		m = getMethod(c,"get"+methodSuffix);
		if ( m==null ) {
			totalReflectionLookups++;
			m = getMethod(c, "is"+methodSuffix);
		}
		if ( m != null ) {
			// save to avoid lookup later
			self.getGroup().cacheClassProperty(c,propertyName,m);
			try {
				value = invokeMethod(m, o, value);
			}
			catch (Exception e) {
				self.error("Can't get property "+propertyName+" using method get/is"+methodSuffix+
					" from "+c.getName()+" instance", e);
			}
		}
		else {
			// try for a visible field
			try {
				totalReflectionLookups++;
				Field f = c.getField(propertyName);
				self.getGroup().cacheClassProperty(c,propertyName,f);
				try {
					value = accessField(f, o, value);
				}
				catch (IllegalAccessException iae) {
					self.error("Can't access property "+propertyName+" using method get/is"+methodSuffix+
						" or direct field access from "+c.getName()+" instance", iae);
				}
			}
			catch (NoSuchFieldException nsfe) {
				self.error("Class "+c.getName()+" has no such attribute: "+propertyName+
					" in template context "+self.getEnclosingInstanceStackString(), nsfe);
			}
		}

        return value;
    }

	protected Object accessField(Field f, Object o, Object value) throws IllegalAccessException {
		try {
			// make sure it's accessible (stupid java)
			f.setAccessible(true);
		}
		catch (SecurityException se) {
			; // oh well; security won't let us
		}
		value = f.get(o);
		return value;
	}

	protected Object invokeMethod(Method m, Object o, Object value) throws IllegalAccessException, InvocationTargetException {
		try {
			// make sure it's accessible (stupid java)
			m.setAccessible(true);
		}
		catch (SecurityException se) {
			; // oh well; security won't let us
		}
		value = m.invoke(o,(Object[])null);
		return value;
	}

	protected Method getMethod(Class c, String methodName) {
		Method m;
		try {
			m = c.getMethod(methodName, (Class[])null);
		}
		catch (NoSuchMethodException nsme) {
			m = null;
		}
		return m;
	}

	/** Normally StringTemplate tests presence or absence of attributes
	 *  for adherence to my principles of separation, but some people
	 *  disagree and want to change.
	 *
	 *  For 2.0, if the object is a boolean, do something special. $if(boolean)$
	 *  will actually test the value.  Now, this breaks my rules of entanglement
	 *  listed in my paper, but it truly surprises programmers to have booleans
	 *  always true.  Further, the key to isolating logic in the model is avoiding
	 *  operators (for which you need attribute values).  But, no operator is
	 *  needed to use boolean values.  Well, actually I guess "!" (not) is
	 *  an operator.  Regardless, for practical reasons, I'm going to technically
	 *  violate my rules as I currently have them defined.  Perhaps for a future
	 *  version of the paper I will refine the rules.
	 *
	 *  Post 2.1, I added a check for non-null Iterators, Collections, ...
	 *  with size==0 to return false. TJP 5/1/2005
	 */
	public boolean testAttributeTrue(Object a) {
		if ( a==null ) {
			return false;
		}
		if ( a instanceof Boolean ) {
			return ((Boolean)a).booleanValue();
		}
		if ( a instanceof Collection ) {
			return ((Collection)a).size()>0;
		}
		if ( a instanceof Map ) {
			return ((Map)a).size()>0;
		}
		if ( a instanceof Iterator ) {
			return ((Iterator)a).hasNext();
		}
		return true; // any other non-null object, return true--it's present
	}

    /** For now, we can only add two objects as strings; convert objects to
	 *  Strings then cat.
     */
    public Object add(Object a, Object b) {
        if ( a==null ) { // a null value means don't do cat, just return other value
            return b;
        }
        else if ( b==null ) {
            return a;
        }
		return a.toString() + b.toString();
    }

    /** Call a string template with args and return result.  Do not convert
     *  to a string yet.  It may need attributes that will be available after
     *  this is inserted into another template.
     */
	public StringTemplate getTemplateInclude(StringTemplate enclosing,
											 String templateName,
											 StringTemplateAST argumentsAST)
    {
		//System.out.println("getTemplateInclude: look up "+enclosing.getGroup().getName()+"::"+templateName);
        StringTemplateGroup group = enclosing.getGroup();
        StringTemplate embedded = group.getEmbeddedInstanceOf(enclosing, templateName);
        if ( embedded==null ) {
            enclosing.error("cannot make embedded instance of "+templateName+
                    " in template "+enclosing.getName());
            return null;
        }
        embedded.setArgumentsAST(argumentsAST);
        evaluateArguments(embedded);
        return embedded;
    }

    /** How to spit out an object.  If it's not a StringTemplate nor a
     *  List, just do o.toString().  If it's a StringTemplate,
     *  do o.write(out).  If it's a Vector, do a write(out,
     *  o.elementAt(i)) for all elements.  Note that if you do
     *  something weird like set the values of a multivalued tag
     *  to be vectors, it will effectively flatten it.
     *
     *  If self is an embedded template, you might have specified
     *  a separator arg; used when is a vector.
     */
    public int writeAttribute(StringTemplate self, Object o, StringTemplateWriter out) {
        Object separator = null;
        if ( options!=null ) {
            separator = options.get("separator");
        }
        return write(self,o,out,separator);
    }

	protected int write(StringTemplate self,
						Object o,
						StringTemplateWriter out,
						Object separator)
    {
        if ( o==null ) {
            return 0;
        }
		int n = 0;
        try {
            if ( o instanceof StringTemplate ) {
                StringTemplate stToWrite = (StringTemplate)o;
				// failsafe: perhaps enclosing instance not set
				// Or, it could be set to another context!  This occurs
				// when you store a template instance as an attribute of more
				// than one template (like both a header file and C file when
				// generating C code).  It must execute within the context of
				// the enclosing template.
				stToWrite.setEnclosingInstance(self);
                // if self is found up the enclosing instance chain, then
                // infinite recursion
                if ( StringTemplate.inLintMode() &&
                     StringTemplate.isRecursiveEnclosingInstance(stToWrite) )
                {
                    // throw exception since sometimes eval keeps going
                    // even after I ignore this write of o.
                    throw new IllegalStateException("infinite recursion to "+
                            stToWrite.getTemplateDeclaratorString()+" referenced in "+
                            stToWrite.getEnclosingInstance().getTemplateDeclaratorString()+
                            "; stack trace:\n"+stToWrite.getEnclosingInstanceStackTrace());
                }
                else {
                    n = stToWrite.write(out);
                }
                return n;
            }
            // normalize anything iteratable to iterator
			o = convertAnythingIteratableToIterator(o);
			if ( o instanceof Iterator ) {
				Iterator iter = (Iterator)o;
                String separatorString = null;
                if ( separator!=null ) {
                    separatorString = computeSeparator(self, out, separator);
                }
                while ( iter.hasNext() ) {
                    Object iterValue = iter.next();
                    int charWrittenForValue =
						write(self, iterValue, out, separator);
					n += charWrittenForValue;
                    if ( iter.hasNext() ) {
						boolean valueIsPureConditional = false;
						if ( iterValue instanceof StringTemplate ) {
							StringTemplate iterValueST = (StringTemplate)iterValue;
							List chunks = (List)iterValueST.getChunks();
							Expr firstChunk = (Expr)chunks.get(0);
							valueIsPureConditional =
								firstChunk instanceof ConditionalExpr &&
								((ConditionalExpr)firstChunk).getElseSubtemplate()==null;
						}
						boolean emptyIteratedValue =
							valueIsPureConditional && charWrittenForValue==0;
                        if ( !emptyIteratedValue && separator!=null ) {
							n += out.write(separatorString);
                        }
                    }
                }
            }
            else {
				AttributeRenderer renderer =
					self.getAttributeRenderer(o.getClass());
				if ( renderer!=null ) {
					n = out.write( renderer.toString(o) );
				}
				else {
                	n = out.write( o.toString() );
				}
				return n;
            }
        }
        catch (IOException io) {
            self.error("problem writing object: "+o, io);
        }
		return n;
    }

	/** A separator is normally just a string literal, but is still an AST that
     *  we must evaluate.  The separator can be any expression such as a template
     *  include or string cat expression etc...
     */
    protected String computeSeparator(StringTemplate self,
									  StringTemplateWriter out,
									  Object separator)
	{
        if ( separator==null ) {
            return null;
        }
        if ( separator instanceof StringTemplateAST ) {
            StringTemplateAST separatorTree = (StringTemplateAST)separator;
            // must evaluate, writing to a string so we can hand on to it
            ASTExpr e = new ASTExpr(getEnclosingTemplate(),separatorTree,null);
            StringWriter buf = new StringWriter();
			// create a new instance of whatever StringTemplateWriter
			// implementation they are using.  Default is AutoIndentWriter.
			// Defalut behavior is to indent but without
            // any prior indents surrounding this attribute expression
			StringTemplateWriter sw = null;
			Class writerClass = out.getClass();
			try {
				Constructor ctor =
						writerClass.getConstructor(new Class[] {Writer.class});
				sw = (StringTemplateWriter)ctor.newInstance(new Object[] {buf});
			}
			catch (Exception exc) {
				// default new AutoIndentWriter(buf)
				self.error("cannot make implementation of StringTemplateWriter",exc);
				sw = new AutoIndentWriter(buf);
			}

			try {
				e.write(self,sw);
            }
            catch (IOException ioe) {
                self.error("can't evaluate separator expression", ioe);
            }
            return buf.toString();
        }
        else {
            // just in case we expand in the future and it's something else
            return separator.toString();
        }
    }

	/** Evaluate an argument list within the context of the enclosing
	 *  template but store the values in the context of self, the
	 *  new embedded template.  For example, bold(item=item) means
	 *  that bold.item should get the value of enclosing.item.
	 */
    protected void evaluateArguments(StringTemplate self) {
        StringTemplateAST argumentsAST = self.getArgumentsAST();
        if ( argumentsAST==null || argumentsAST.getFirstChild()==null )	{
            // return immediately if missing tree or no actual args
            return;
        }

		// Evaluate args in the context of the enclosing template, but we
		// need the predefined args like 'it', 'attr', and 'i' to be
		// available as well so we put a dummy ST between the enclosing
		// context and the embedded context.  The dummy has the predefined
		// context as does the embedded.
		StringTemplate enclosing = self.getEnclosingInstance();
		StringTemplate argContextST = new StringTemplate(self.getGroup(), "");
		argContextST.setName("<invoke "+self.getName()+" arg context>");
		argContextST.setEnclosingInstance(enclosing);
		argContextST.setArgumentContext(self.getArgumentContext());

        ActionEvaluator eval =
                new ActionEvaluator(argContextST,this,null);
		/*
		System.out.println("eval args: "+argumentsAST.toStringList());
		System.out.println("ctx is "+self.getArgumentContext());
		*/
        try {
            // using any initial argument context (such as when obj is set),
            // evaluate the arg list like bold(item=obj).  Since we pass
            // in any existing arg context, that context gets filled with
            // new values.  With bold(item=obj), context becomes:
            // {[obj=...],[item=...]}.
            Map ac = eval.argList(argumentsAST, self, self.getArgumentContext());
            self.setArgumentContext(ac);
        }
        catch (RecognitionException re) {
            self.error("can't evaluate tree: "+argumentsAST.toStringList(), re);
        }
	}

	/** Do a standard conversion of array attributes to Lists.  Create
	 *  StringTemplate.STAttributeList objects so we know they are ST
	 *  controlled not user lists.
	 */
	public static Object convertArrayToList(Object value) {
		if ( value==null ) {
			return null;
		}
		if ( value instanceof Object[] ) {
			Object[] list = (Object[])value;
			List v = new StringTemplate.STAttributeList(list.length);
			for (int i = 0; i < list.length; i++) {
				Object elem = list[i];
				v.add(elem);
			}
			value = v;
		}
		else if ( value instanceof int[] ) {
			int[] list = (int[])value;
			List v = new StringTemplate.STAttributeList(list.length);
			for (int i = 0; i < list.length; i++) {
				int elem = list[i];
				v.add(new Integer(elem));
			}
			value = v;
		}
		else if ( value instanceof long[] ) {
			long[] list = (long[])value;
			List v = new StringTemplate.STAttributeList(list.length);
			for (int i = 0; i < list.length; i++) {
				long elem = list[i];
				v.add(new Long(elem));
			}
			value = v;
		}
		else if ( value instanceof float[] ) {
			float[] list = (float[])value;
			List v = new StringTemplate.STAttributeList(list.length);
			for (int i = 0; i < list.length; i++) {
				float elem = list[i];
				v.add(new Float(elem));
			}
			value = v;
		}

		else if ( value instanceof double[] ) {
			double[] list = (double[])value;
			List v = new StringTemplate.STAttributeList(list.length);
			for (int i = 0; i < list.length; i++) {
				double elem = list[i];
				v.add(new Double(elem));
			}
			value = v;
		}
		return value;
	}

	private static Object convertAnythingIteratableToIterator(Object o) {
		Iterator iter = null;
		if ( o instanceof Collection ) {
			iter = ((Collection)o).iterator();
		}
		else if ( o instanceof Map ) {
			iter = ((Map)o).values().iterator();
		}
		else if ( o instanceof Iterator ) {
			iter = (Iterator)o;
		}
		if ( iter==null ) {
			return o;
		}
		return iter;
	}

	static Iterator convertAnythingToIterator(Object o) {
		Iterator iter = null;
		if ( o instanceof Collection ) {
			iter = ((Collection)o).iterator();
		}
		else if ( o instanceof Map ) {
			iter = ((Map)o).values().iterator();
		}
		else if ( o instanceof Iterator ) {
			iter = (Iterator)o;
		}
		if ( iter==null ) {
			List singleton = new StringTemplate.STAttributeList(1);
			singleton.add(o);
			return singleton.iterator();
		}
		return iter;
	}

	/** Return the first attribute if multiple valued or the attribute
	 *  itself if single-valued.  Used in <names:first()>
	 */
	public Object first(Object attribute) {
		if ( attribute==null ) {
			return null;
		}
		Object f = attribute;
		attribute = convertAnythingIteratableToIterator(attribute);
		if ( attribute instanceof Iterator ) {
			Iterator it = (Iterator)attribute;
			if ( it.hasNext() ) {
				f = it.next();
			}
		}

		return f;
	}

	/** Return the everything but the first attribute if multiple valued
	 *  or null if single-valued.  Used in <names:rest()>.
	 */
	public Object rest(Object attribute) {
		if ( attribute==null ) {
			return null;
		}
		Object theRest = attribute;
		attribute = convertAnythingIteratableToIterator(attribute);
		if ( attribute instanceof Iterator ) {
			Iterator it = (Iterator)attribute;
			if ( !it.hasNext() ) {
				return null; // if not even one value return null
			}
			it.next(); // ignore first value
			if ( !it.hasNext() ) {
				return null; // if not more than one value, return null
			}
			theRest = it;    // return suitably altered iterator
		}
		else {
			theRest = null;  // rest of single-valued attribute is null
		}

		return theRest;
	}

	/** Return the last attribute if multiple valued or the attribute
	 *  itself if single-valued.  Used in <names:last()>.  This is pretty
	 *  slow as it iterates until the last element.  Ultimately, I could
	 *  make a special case for a List or Vector.
	 */
	public Object last(Object attribute) {
		if ( attribute==null ) {
			return null;
		}
		Object last = attribute;
		attribute = convertAnythingIteratableToIterator(attribute);
		if ( attribute instanceof Iterator ) {
			Iterator it = (Iterator)attribute;
			while ( it.hasNext() ) {
				last = it.next();
			}
		}

		return last;
	}

	public String toString() {
		return exprTree.toStringList();
	}
}
