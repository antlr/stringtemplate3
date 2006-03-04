package org.antlr.stringtemplate;

import java.io.*;
import java.util.List;
import java.util.StringTokenizer;
import java.util.ArrayList;

/** A brain dead loader that looks only in the directory(ies) you
 *  specify in the ctor.
 *  You may specify the char encoding.
 *  NOTE: this does not work when you jar things up!  Use
 *  CommonGroupLoader instead in that case
 */
public class PathGroupLoader implements StringTemplateGroupLoader {
	/** List of ':' separated dirs to pull groups from */
	protected List dirs = null;

	protected StringTemplateErrorListener errors = null;

	/** How are the files encoded (ascii, UTF8, ...)?  You might want to read
	 *  UTF8 for example on an ascii machine.
	 */
	String fileCharEncoding = System.getProperty("file.encoding");

	public PathGroupLoader(StringTemplateErrorListener errors) {
		this.errors = errors;
	}

	/** Pass a single dir or multiple dirs separated by colons from which
	 *  to load groups/interfaces.
	 */
	public PathGroupLoader(String dirStr, StringTemplateErrorListener errors) {
		this.errors = errors;
		StringTokenizer tokenizer = new StringTokenizer(dirStr, ":", false);
		while (tokenizer.hasMoreElements()) {
			String dir = (String) tokenizer.nextElement();
			if ( dirs==null ) {
				dirs = new ArrayList();
			}
			dirs.add(dir);
			/* don't check; dirs might get created later
			if ( !(new File(dir).exists()) ) {
				error("group loader: no such dir "+dir);
			}
			else {
				dirs.add(dir);
			}
			*/
		}
	}

	public StringTemplateGroup loadGroup(String groupName,
										 StringTemplateGroup superGroup) {
		StringTemplateGroup group = null;
		try {
			BufferedReader br = locate(groupName+".stg");
			if ( br==null ) {
				error("no such group file "+groupName+".stg");
				return null;
			}
			group = new StringTemplateGroup(br, null, errors, superGroup);
		}
		catch (IOException ioe) {
			error("can't load group "+groupName, ioe);
		}
		return group;
	}

	/** Load a group with a specified superGroup.  Groups with
	 *  region definitions must know their supergroup to find templates
	 *  during parsing.
	 */
	public StringTemplateGroup loadGroup(String groupName) {
		return loadGroup(groupName, null);
	}

	public StringTemplateGroupInterface loadInterface(String interfaceName) {
		StringTemplateGroupInterface I = null;
		try {
			BufferedReader br = locate(interfaceName+".sti");
			if ( br==null ) {
				error("no such interface file "+interfaceName+".sti");
				return null;
			}
			I = new StringTemplateGroupInterface(br, errors);
		}
		catch (IOException ioe) {
			error("can't load interface "+interfaceName, ioe);
		}
		return I;
	}

	/** Look in each directory for the file called 'name'. */
	protected BufferedReader locate(String name) throws IOException {
		for (int i = 0; i < dirs.size(); i++) {
			String dir = (String) dirs.get(i);
			String fileName = dir+"/"+name;
			if ( new File(fileName).exists() ) {
				FileInputStream fis = new FileInputStream(fileName);
				InputStreamReader isr = getInputStreamReader(fis);
				return new BufferedReader(isr);
			}
		}
		return null;
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

	public void error(String msg) {
		error(msg, null);
	}

	public void error(String msg, Exception e) {
		if ( errors!=null ) {
			errors.error(msg,e);
		}
		else {
			System.err.println("StringTemplate: "+msg);
			if ( e!=null ) {
				e.printStackTrace();
			}
		}
    }
}
