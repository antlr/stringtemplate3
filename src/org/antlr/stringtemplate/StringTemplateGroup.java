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

import org.antlr.stringtemplate.language.DefaultTemplateLexer;
import org.antlr.stringtemplate.language.GroupLexer;
import org.antlr.stringtemplate.language.GroupParser;

import java.util.*;
import java.io.*;
import java.lang.reflect.Constructor;

/** Manages a group of named mutually-referential StringTemplate objects.
 *  Currently the templates must all live under a directory so that you
 *  can reference them as foo.st or gutter/header.st.  To refresh a
 *  group of templates, just create a new StringTemplateGroup and start
 *  pulling templates from there.  Or, set the refresh interval.
 *
 *  Use getInstanceOf(template-name) to get a string template
 *  to fill in.
 *
 *  The name of a template is the file name minus ".st" ending if present
 *  unless you name it as you load it.
 */
public class StringTemplateGroup {
	/** What is the group name */
	protected String name;

	/** Maps template name to StringTemplate object */
	protected Map templates = new HashMap();

	/** Maps map names to HashMap objects.  This is the list of maps
	 *  defined by the user like typeInitMap ::= ["int":"0"]
	 */
	protected Map maps = new HashMap();

    /** How to pull apart a template into chunks? */
    protected Class templateLexerClass = DefaultTemplateLexer.class;

	/** Under what directory should I look for templates?  If null,
	 *  to look into the CLASSPATH for templates as resources.
     */
    protected String rootDir = null;

	/** Track all groups by name; maps name to StringTemplateGroup */
	protected static Map nameToGroupMap = new HashMap();

	/** Are we derived from another group?  Templates not found in this group
	 *  will be searched for in the superGroup recursively.
	 */
	protected StringTemplateGroup superGroup = null;

    /** When templates are files on the disk, the refresh interval is used
     *  to know when to reload.  When a Reader is passed to the ctor,
     *  it is a stream full of template definitions.  The former is used
     *  for web development, but the latter is most likely used for source
     *  code generation for translators; a refresh is unlikely.  Anyway,
     *  I decided to track the source of templates in case such info is useful
     *  in other situations than just turning off refresh interval.  I just
     *  found another: don't ever look on the disk for individual templates
     *  if this group is a group file...immediately look into any super group.
     *  If not in the super group, report no such template.
     */
    protected boolean templatesDefinedInGroupFile = false;

	/** Normally AutoIndentWriter is used to filter output, but user can
	 *  specify a new one.
	 */
	protected Class userSpecifiedWriter;

	/** A Map<Class,Object> that allows people to register a renderer for
	 *  a particular kind of object to be displayed for any template in this
	 *  group.  For example, a date should be formatted differently depending
	 *  on the locale.  You can set Date.class to an object whose
	 *  toString(Object) method properly formats a Date attribute
	 *  according to locale.  Or you can have a different renderer object
	 *  for each locale.
	 *
	 *  These render objects are used way down in the evaluation chain
	 *  right before an attribute's toString() method would normally be
	 *  called in ASTExpr.write().
 	 */
	protected Map attributeRenderers;

	/** Where to report errors.  All string templates in this group
	 *  use this error handler by default.
	 */
	protected StringTemplateErrorListener listener = DEFAULT_ERROR_LISTENER;

    public static StringTemplateErrorListener DEFAULT_ERROR_LISTENER =
        new StringTemplateErrorListener() {
            public void error(String s, Throwable e) {
                System.err.println(s);
                if ( e!=null ) {
                    e.printStackTrace(System.err);
                }
            }
            public void warning(String s) {
                System.out.println(s);
            }
            public void debug(String s) {
                System.out.println(s);
            }
        };

	/** Used to indicate that the template doesn't exist.
	 *  We don't have to check disk for it; we know it's not there.
	 */
	protected static final StringTemplate NOT_FOUND_ST =
		new StringTemplate();

    /** How long before tossing out all templates in seconds. */
    protected int refreshIntervalInSeconds = Integer.MAX_VALUE/1000; // default: no refreshing from disk
    protected long lastCheckedDisk = 0L;

	/** How are the files encode (ascii, UTF8, ...)?  You might want to read
	 *  UTF8 for example on a ascii machine.
	 */
	String fileCharEncoding = System.getProperty("file.encoding");

    /** Create a group manager for some templates, all of which are
     *  at or below the indicated directory.
     */
	public StringTemplateGroup(String name, String rootDir) {
        this(name,rootDir,DefaultTemplateLexer.class);
    }

    public StringTemplateGroup(String name, String rootDir, Class lexer) {
        this.name = name;
		this.rootDir = rootDir;
        lastCheckedDisk = System.currentTimeMillis();
		nameToGroupMap.put(name, this);
        this.templateLexerClass = lexer;
    }

    /** Create a group manager for some templates, all of which are
	 *  loaded as resources via the classloader.
	 */
    public StringTemplateGroup(String name) {
        this(name,null,DefaultTemplateLexer.class);
    }

    public StringTemplateGroup(String name, Class lexer) {
        this(name,null,lexer);
    }

    /** Create a group from the template group defined by a input stream.
     *  The name is pulled from the file.  The format is
     *
     *  group name;
     *
     *  t1(args) : "..."
     *  t2 : <<
     *  >>
     *  ...
     */
    public StringTemplateGroup(Reader r) {
        this(r,DefaultTemplateLexer.class,DEFAULT_ERROR_LISTENER);
    }

    public StringTemplateGroup(Reader r, StringTemplateErrorListener errors) {
        this(r,DefaultTemplateLexer.class,errors);
    }

    public StringTemplateGroup(Reader r, Class lexer) {
        this(r,lexer,null);
    }

    /** Create a group from the input stream, but use a nondefault lexer
     *  to break the templates up into chunks.  This is usefor changing
     *  the delimiter from the default $...$ to <...>, for example.
     */
    public StringTemplateGroup(Reader r,
                               Class lexer,
                               StringTemplateErrorListener errors)
    {
        this.templatesDefinedInGroupFile = true;
        this.templateLexerClass = lexer;
        this.listener = errors;
        parseGroup(r);
    }

    public Class getTemplateLexerClass() {
        return templateLexerClass;
    }

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setSuperGroup(StringTemplateGroup superGroup) {
		this.superGroup = superGroup;
	}

	public void setSuperGroup(String groupName) {
		this.superGroup = (StringTemplateGroup)nameToGroupMap.get(groupName);
	}

	public StringTemplateGroup getSuperGroup() {
		return superGroup;
	}

	public String getRootDir() {
		return rootDir;
	}

	public void setRootDir(String rootDir) {
		this.rootDir = rootDir;
	}

	/** StringTemplate object factory; each group can have its own. */
	public StringTemplate createStringTemplate() {
		return new StringTemplate();
	}

    public StringTemplate getInstanceOf(String name) throws IllegalArgumentException {
		StringTemplate st = lookupTemplate(name);
		return st.getInstanceOf();
	}

	public StringTemplate getEmbeddedInstanceOf(StringTemplate enclosingInstance,
                                                String name)
    	throws IllegalArgumentException
	{
        StringTemplate st = getInstanceOf(name);
        st.setEnclosingInstance(enclosingInstance);
        return st;
    }

	/** Get the template called 'name' from the group.  If not found,
     *  attempt to load.  If not found on disk, then try the superGroup
	 *  if any.  If not even there, then record that it's
     *  NOT_FOUND so we don't waste time looking again later.  If we've gone
     *  past refresh interval, flush and look again.
     */
    public StringTemplate lookupTemplate(String name)
		throws IllegalArgumentException
	{
        if ( name.startsWith("super.") ) {
            if ( superGroup!=null ) {
                int dot = name.indexOf('.');
                name = name.substring(dot+1,name.length());
                return superGroup.lookupTemplate(name);
            }
            throw new IllegalArgumentException(getName()+
                    " has no super group; invalid template: "+name);
        }
        checkRefreshInterval();
        StringTemplate st = (StringTemplate)templates.get(name);
        if ( st==null ) {
            // not there?  Attempt to load
            if ( !templatesDefinedInGroupFile ) {
                // only check the disk for individual template
                st = loadTemplateFromBeneathRootDirOrCLASSPATH(getFileNameFromTemplateName(name));
            }
            if ( st==null && superGroup!=null ) {
                // try to resolve in super group
                st = superGroup.getInstanceOf(name);
                if ( st!=null ) {
                    st.setGroup(this);
                }
            }
            if ( st!=null ) { // found in superGroup
                // insert into this group; refresh will allow super
                // to change it's def later or this group to add
                // an override.
                templates.put(name, st);
            }
            else {
                // not found; remember that this sucker doesn't exist
                templates.put(name, NOT_FOUND_ST);
				throw new IllegalArgumentException("Can't load template "+getFileNameFromTemplateName(name));
            }
        }
        else if ( st==NOT_FOUND_ST ) {
            return null;
        }

		return st;
	}

    protected void checkRefreshInterval() {
        if ( templatesDefinedInGroupFile ) {
            return;
        }
        boolean timeToFlush=refreshIntervalInSeconds==0 ||
                (System.currentTimeMillis()-lastCheckedDisk)>=refreshIntervalInSeconds*1000;
        if ( timeToFlush ) {
            // throw away all pre-compiled references
            templates.clear();
            lastCheckedDisk = System.currentTimeMillis();
        }
    }

    protected StringTemplate loadTemplate(String name, BufferedReader r)
            throws IOException
    {
        String line;
        String nl = System.getProperty("line.separator");
        StringBuffer buf = new StringBuffer(300);
        while ((line = r.readLine()) != null) {
            buf.append(line);
            buf.append(nl);
        }
		// strip newlines etc.. from front/back since filesystem
		// may add newlines etc...
		String pattern = buf.toString().trim();
		if ( pattern.length()==0 ) {
			error("no text in template '"+name+"'");
			return null;
		}
        return defineTemplate(name, pattern);
    }

    /** Load a template whose name is derived from the template filename.
     *  If there is no root directory, try to load the template from
     *  the classpath.  If there is a rootDir, try to load the file
     *  from there.
     */
    protected StringTemplate loadTemplateFromBeneathRootDirOrCLASSPATH(String fileName)
    {
		StringTemplate template = null;
        String name = getTemplateNameFromFileName(fileName);
        // if no rootDir, try to load as a resource in CLASSPATH
		if ( rootDir==null ) {
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			InputStream is = cl.getResourceAsStream(fileName);
			if ( is==null ) {
				return null;
			}
			BufferedReader br = null;
			try {
				br = new BufferedReader(getInputStreamReader(is));
			    template = loadTemplate(name, br);
				br.close();
				br=null;
			}
			catch (IOException ioe) {
                if ( br!=null ) {
					try {
						br.close();
					}
					catch (IOException ioe2) {
						error("Cannot close template file: "+fileName);
					}
				}
			}
			return template;
		}
        // load via rootDir
		template = loadTemplate(name, rootDir+"/"+fileName);
		return template;
    }

    /** (public so that people can override behavior; not a general
     *  purpose method)
     */
	public String getFileNameFromTemplateName(String templateName) {
		return templateName+".st";
	}

	/** Convert a filename relativePath/name.st to relativePath/name.
     *  (public so that people can override behavior; not a general
     *  purpose method)
     */
	public String getTemplateNameFromFileName(String fileName) {
		String name = fileName;
		int suffix = name.lastIndexOf(".st");
		if ( suffix>=0 ) {
			name = name.substring(0, suffix);
		}
		return name;
	}

	protected StringTemplate loadTemplate(String name, String fileName)
    {
		BufferedReader br = null;
		StringTemplate template = null;
		try {
			InputStream fin = new FileInputStream(fileName);
			InputStreamReader isr = getInputStreamReader(fin);
			br = new BufferedReader(isr);
			template = loadTemplate(name, br);
			br.close();
			br = null;
		}
		catch (IOException ioe) {
			if ( br!=null ) {
				try {
					br.close();
				}
				catch (IOException ioe2) {
					error("Cannot close template file: "+fileName);
				}
			}
		}
		return template;
	}

	protected InputStreamReader getInputStreamReader(InputStream in) {
		InputStreamReader isr = null;
		try {
			 isr = new InputStreamReader(in, fileCharEncoding);
		}
		catch (UnsupportedEncodingException uee) {
			error("Invalid file character encoding: "+fileCharEncoding);
		}
		return isr;
	}

	public String getFileCharEncoding() {
		return fileCharEncoding;
	}

	public void setFileCharEncoding(String fileCharEncoding) {
		this.fileCharEncoding = fileCharEncoding;
	}

	/** Define an examplar template; precompiled and stored
     *  with no attributes.  Remove any previous definition.
     */
    public StringTemplate defineTemplate(String name,
                                         String template)
    {
        StringTemplate st = createStringTemplate();
        st.setName(name);
		st.setGroup(this);
        st.setTemplate(template);
		st.setErrorListener(listener);
        templates.put(name, st);
        return st;
    }

    /** Make name and alias for target.  Replace any previous def of name */
    public StringTemplate defineTemplateAlias(String name, String target) {
        StringTemplate targetST=getTemplateDefinition(target);
        if ( targetST==null ){
            error("cannot alias "+name+" to undefined template: "+target);
            return null;
        }
        templates.put(name, targetST);
        return targetST;
    }

    public boolean isDefinedInThisGroup(String name) {
        return templates.get(name)!=null;
    }

    /** Get the ST for 'name' in this group only */
	public StringTemplate getTemplateDefinition(String name) {
        return (StringTemplate)templates.get(name);
    }

	/** Is there *any* definition for template 'name' in this template
	 *  or above it in the group hierarchy?
	 */
	public boolean isDefined(String name) {
		try {
			lookupTemplate(name);
			return true;
		}
		catch (IllegalArgumentException iae) {
			return false;
		}
	}

    protected void parseGroup(Reader r) {
        try {
            GroupLexer lexer = new GroupLexer(r);
            GroupParser parser = new GroupParser(lexer);
            parser.group(this);
            //System.out.println("read group\n"+this.toString());
        }
        catch (Exception e) {
            String name = "<unknown>";
            if ( getName()!=null ) {
                name = getName();
            }
            error("problem parsing group '"+name+"'", e);
        }
    }

    public int getRefreshInterval() {
        return refreshIntervalInSeconds;
    }

    /** How often to refresh all templates from disk.  This is a crude
     *  mechanism at the moment--just tosses everything out at this
     *  frequency.  Set interval to 0 to refresh constantly (no caching).
     *  Set interval to a huge number like MAX_INT to have no refreshing
     *  at all (DEFAULT); it will cache stuff.
     */
    public void setRefreshInterval(int refreshInterval) {
        this.refreshIntervalInSeconds = refreshInterval;
    }

	public void setErrorListener(StringTemplateErrorListener listener) {
		this.listener = listener;
	}

    public StringTemplateErrorListener getErrorListener() {
        return listener;
    }

	/** Specify a StringTemplateWriter implementing class to use for
	 *  filtering output
	 */
	public void setStringTemplateWriter(Class c) {
		userSpecifiedWriter = c;
	}

	/** return an instance of a StringTemplateWriter that spits output to w.
	 *  If a writer is specified, use it instead of the default.
	 */
	public StringTemplateWriter getStringTemplateWriter(Writer w) {
		StringTemplateWriter stw = null;
		if ( userSpecifiedWriter!=null ) {
			try {
				Constructor ctor =
					userSpecifiedWriter.getConstructor(new Class[] {Writer.class});
				stw = (StringTemplateWriter) ctor.newInstance(new Object[] {w});
			}
			catch (Exception e) {
				error("problems getting StringTemplateWriter",e);
			}
		}
		if ( stw==null ) {
			stw = new AutoIndentWriter(w);
		}
		return stw;
	}

	/** Specify a complete map of what object classes should map to which
	 *  renderer objects for every template in this group (that doesn't
	 *  override it per template).
	 */
	public void setAttributeRenderers(Map renderers) {
		this.attributeRenderers = renderers;
	}

	/** Register a renderer for all objects of a particular type for all
	 *  templates in this group.
	 */
	public void registerRenderer(Class attributeClassType, Object renderer) {
		if ( attributeRenderers==null ) {
			attributeRenderers = new HashMap();
		}
		attributeRenderers.put(attributeClassType, renderer);
	}

	/** What renderer is registered for this attributeClassType for
	 *  this group?  If not found, as superGroup if it has one.
	 */
	public AttributeRenderer getAttributeRenderer(Class attributeClassType) {
		if ( attributeRenderers==null ) {
			if ( superGroup==null ) {
				return null; // no renderers and no parent?  Stop.
			}
			// no renderers; consult super group
			return superGroup.getAttributeRenderer(attributeClassType);
		}

		AttributeRenderer renderer =
			(AttributeRenderer)attributeRenderers.get(attributeClassType);
		if ( renderer==null ) {
			// no renderer registered for this class, check super group
			renderer = superGroup.getAttributeRenderer(attributeClassType);
		}
		return renderer;
	}

	public Map getMap(String name) {
		if ( maps==null ) {
			if ( superGroup==null ) {
				return null;
			}
			return superGroup.getMap(name);
		}
		Map m = (Map)maps.get(name);
		if ( m==null && superGroup!=null ) {
			m = superGroup.getMap(name);
		}
		return m;
	}

	public void defineMap(String name, Map mapping) {
		maps.put(name, mapping);
	}

	public void error(String msg) {
		error(msg, null);
	}

	public void error(String msg, Exception e) {
		if ( listener!=null ) {
			listener.error(msg,e);
		}
		else {
			System.err.println("StringTemplate: "+msg+": "+e);
		}
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        Iterator iter = templates.keySet().iterator();
        buf.append("group "+getName()+";\n");
        StringTemplate formalArgs = new StringTemplate("$args;separator=\",\"$");
        while (iter.hasNext()) {
            String tname = (String) iter.next();
            StringTemplate st = (StringTemplate)templates.get(tname);
			if ( st!=NOT_FOUND_ST ) {
                formalArgs = formalArgs.getInstanceOf();
                formalArgs.setAttribute("args", st.getFormalArguments());
                buf.append(tname+"("+formalArgs+") ::= ");
                buf.append("<<");
				buf.append(st.getTemplate());
				buf.append(">>\n");
			}
        }
        return buf.toString();
    }
}
