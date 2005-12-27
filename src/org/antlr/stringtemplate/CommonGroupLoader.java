package org.antlr.stringtemplate;

import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.File;
import java.util.List;
import java.util.StringTokenizer;
import java.util.ArrayList;

/** A brain dead loader that looks only in the directory(ies) you specify in the ctor.
 */
public class CommonGroupLoader implements StringTemplateGroupLoader {
	protected List dirs = null;
	protected StringTemplateErrorListener errors = null;

	public CommonGroupLoader(StringTemplateErrorListener errors) {
		this.errors = errors;
	}

	/** Pass a single dir or multiple dirs separated by colons from which
	 *  to load groups/interfaces.
	 */
	public CommonGroupLoader(String dirStr, StringTemplateErrorListener errors) {
		this.errors = errors;
		StringTokenizer tokenizer = new StringTokenizer(dirStr, ":", false);
		while (tokenizer.hasMoreElements()) {
			String dir = (String) tokenizer.nextElement();
			if ( dirs==null ) {
				dirs = new ArrayList();
			}
			if ( !(new File(dir).exists()) ) {
				error("group loader: no such dir "+dir);
			}
			else {
				dirs.add(dir);
			}
		}
	}

	public StringTemplateGroup loadGroup(String groupName) {
		FileReader fr = null;
		BufferedReader br = null;
		StringTemplateGroup group = null;
		String fileName = locate(groupName+".stg");
		if ( fileName==null ) {
			error("no such group file "+groupName+".stg");
			return null;
		}
		try {
			fr = new FileReader(fileName);
			br = new BufferedReader(fr);
			group = new StringTemplateGroup(br, errors);
		}
		catch (IOException ioe) {
			error("can't load group "+groupName, ioe);
		}
		return group;
	}

	public StringTemplateGroupInterface loadInterface(String interfaceName) {
		FileReader fr = null;
		BufferedReader br = null;
		StringTemplateGroupInterface I = null;
		String fileName = locate(interfaceName+".sti");
		if ( fileName==null ) {
			error("no such interface file "+interfaceName+".sti");
			return null;
		}
		try {
			fr = new FileReader(fileName);
			br = new BufferedReader(fr);
			I = new StringTemplateGroupInterface(br, errors);
		}
		catch (IOException ioe) {
			error("can't load interface "+interfaceName, ioe);
		}
		return I;
	}

	protected String locate(String name) {
		for (int i = 0; i < dirs.size(); i++) {
			String dir = (String) dirs.get(i);
			String fileName = dir+"/"+name;
			if ( new File(fileName).exists() ) {
				return fileName;
			}
		}
		return null;
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
