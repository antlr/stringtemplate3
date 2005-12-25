package org.antlr.stringtemplate;

import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;

/** A brain dead loader that looks only in the directory you specify in the ctor.
 */
public class CommonGroupLoader implements StringTemplateGroupLoader {
	protected String dir = "";
	protected StringTemplateErrorListener errors = null;

	public CommonGroupLoader(StringTemplateErrorListener errors) {
	}

	public CommonGroupLoader(String dir, StringTemplateErrorListener errors) {
		this.dir = dir;
	}

	public StringTemplateGroup loadGroup(String groupName) {
		return null;
	}

	public StringTemplateGroupInterface loadInterface(String interfaceName) {
		FileReader fr = null;
		BufferedReader br = null;
		StringTemplateGroupInterface I = null;
		try {
			fr = new FileReader(dir+"/"+interfaceName+".sti");
			br = new BufferedReader(fr);
			I = new StringTemplateGroupInterface(br, errors);
		}
		catch (IOException ioe) {
			error("can't load interface "+interfaceName, ioe);
		}
		return I;
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
