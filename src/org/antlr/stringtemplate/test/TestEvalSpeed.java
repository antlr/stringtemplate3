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
package org.antlr.stringtemplate.test;

import org.antlr.stringtemplate.StringTemplateGroup;
import org.antlr.stringtemplate.StringTemplate;

import java.io.StringReader;

/** A semi-nonserious attempt to gauge speed of StringTemplate.  Given
 *  the problems associated with using the java system clock as a time
 *  measure, I have run this with -Xint (interpreter only mode) to see
 *  the relative performance of StringTemplate.  I'm just getting started
 *  on this.
 */
public class TestEvalSpeed extends TestSuite {
	public static final int N = 1000;
    final String newline = System.getProperty("line.separator");

	StringTemplateGroup group;

	String gString =
			"group speed;\n" +
			"literal() : <<A template with just a single longish literal in it>>\n" +
			"bold() : << <b>$attr$</b> >>\n"+
			"st1(name) : <<A template with just a single attr $name$ in it>>\n"+
	        "st2(names) : <<$names:bold()$>>\n";

    public TestEvalSpeed() {
		group = new StringTemplateGroup(new StringReader(gString));
		/*
        // warm up the on-the-fly compiler
		for (int i=1; i<=2*N; i++) {
			testBaselineLiteral();
			testSingleLocalAttributeReference();
			testApplyTemplateToList();
		}
        */
    }

	public void runTests() throws Throwable {
		time("testBaselineLiteral", N);
		time("testSingleLocalAttributeReference", N);
		time("testApplyTemplateToList", N);
    }

	public void testBaselineLiteral() {
		StringTemplate st = group.getInstanceOf("literal");
		st.toString();
	}

	public void testSingleLocalAttributeReference() {
		StringTemplate st = group.getInstanceOf("st1");
		st.setAttribute("name", "Ter");
		st.toString();
	}

	public void testApplyTemplateToList() {
		StringTemplate t = group.getInstanceOf("st2");
		t.setAttribute("names", "Terence");
		t.setAttribute("names", "Tom");
		t.setAttribute("names", "Sriram");
		t.setAttribute("names", "Mike");
		t.toString();
	}
}
