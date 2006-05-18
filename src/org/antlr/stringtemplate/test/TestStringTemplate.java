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

import org.antlr.stringtemplate.*;
import org.antlr.stringtemplate.language.AngleBracketTemplateLexer;
import org.antlr.stringtemplate.language.DefaultTemplateLexer;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/** Test the various functionality of StringTemplate. Seems to run only
 *  on unix due to \r\n vs \n issue.  David Scurrah says:
 *
 * "I don't think you were necessarily sloppy with your newlines, but Java make it very difficult to be consistant.
The stringtemplate library used unix end of lines for writing toString methods and the like,
while the testing was using the system local end of line. The other problem with end of lines was any template
file used in the testing will also have a specific end of line ( this case unix) and when read into a string that can the unique problem
of having end of line unix and local system end of line in the on  line.

My solution was not very elegant but I think it required the least changes and only to the testing.
I simply converted all strings to use unix end of line characters inside the assertTrue and then compared them.
The only other problem I found was writing a file out to the /tmp directory won't work on windows so I used the
system property  java.io.tmpdir to get a temp directory."

 * I'll fix later.
 */
public class TestStringTemplate extends TestSuite {
    final String newline = System.getProperty("line.separator");

	public void runTests() throws Throwable {
		TestRig.runAllTests(this.getClass(), this);
		/*
		System.out.println("num obj.prop refs: "+ ASTExpr.totalObjPropRefs);
		System.out.println("num obj.prop refs: "+ ASTExpr.totalObjPropComputations);
		*/
	}

	public TestStringTemplate() {
    }

	static class ErrorBuffer implements StringTemplateErrorListener {
		StringBuffer errorOutput = new StringBuffer(500);
		int n = 0;
		public void error(String msg, Throwable e) {
			n++;
			if ( n>1 ) {
				errorOutput.append('\n');
			}
			if ( e!=null ) {
				StringWriter duh = new StringWriter();
				e.printStackTrace(new PrintWriter(duh));
				errorOutput.append(msg+": "+duh.toString());
			}
			else {
				errorOutput.append(msg);
			}
		}
		public void warning(String msg) {
			n++;
			errorOutput.append(msg);
		}
		public void debug(String msg) {
			n++;
			errorOutput.append(msg);
		}
        public boolean equals(Object o) {
            String me = toString();
            String them = o.toString();
            return me.equals(them);
        }
		public String toString() {
			return errorOutput.toString();
		}
	}

	public void testInterfaceFileFormat() throws Exception {
		String groupI =
				"interface test;" +newline+
				"t();" +newline+
				"bold(item);"+newline+
				"optional duh(a,b,c);"+newline;
		StringTemplateGroupInterface I =
				new StringTemplateGroupInterface(new StringReader(groupI));

		String expecting =
			"interface test;\n" +
			"t();\n" +
			"bold(item);\n" +
			"optional duh(a, b, c);\n";
		assertEqual(I.toString(), expecting);
	}

	public void testNoGroupLoader() throws Exception {
		// this also tests the group loader
		StringTemplateErrorListener errors = new ErrorBuffer();
		String tmpdir = System.getProperty("java.io.tmpdir");

		String templates =
			"group testG implements blort;" +newline+
			"t() ::= <<foo>>" +newline+
			"bold(item) ::= <<foo>>"+newline+
			"duh(a,b,c) ::= <<foo>>"+newline;

		writeFile(tmpdir, "testG.stg", templates);

		StringTemplateGroup group =
				new StringTemplateGroup(new FileReader(tmpdir+"/testG.stg"), errors);

		String expecting = "no group loader registered";
		assertEqual(errors.toString(), expecting);
	}

	public void testCannotFindInterfaceFile() throws Exception {
		// this also tests the group loader
		StringTemplateErrorListener errors = new ErrorBuffer();
		String tmpdir = System.getProperty("java.io.tmpdir");
		StringTemplateGroup.registerGroupLoader(new PathGroupLoader(tmpdir,errors));

		String templates =
			"group testG implements blort;" +newline+
			"t() ::= <<foo>>" +newline+
			"bold(item) ::= <<foo>>"+newline+
			"duh(a,b,c) ::= <<foo>>"+newline;

		writeFile(tmpdir, "testG.stg", templates);

		StringTemplateGroup group =
				new StringTemplateGroup(new FileReader(tmpdir+"/testG.stg"), errors);

		String expecting = "no such interface file blort.sti";
		assertEqual(errors.toString(), expecting);
	}

	public void testMultiDirGroupLoading() throws Exception {
		// this also tests the group loader
		StringTemplateErrorListener errors = new ErrorBuffer();
		String tmpdir = System.getProperty("java.io.tmpdir");
		if ( !(new File(tmpdir+"/sub").exists()) ) {
			if ( !(new File(tmpdir+"/sub").mkdir()) ) { // create a subdir
				System.err.println("can't make subdir in test");
				return;
			}
		}
		StringTemplateGroup.registerGroupLoader(
			new PathGroupLoader(tmpdir+":"+tmpdir+"/sub",errors)
		);

		String templates =
			"group testG2;" +newline+
			"t() ::= <<foo>>" +newline+
			"bold(item) ::= <<foo>>"+newline+
			"duh(a,b,c) ::= <<foo>>"+newline;

		writeFile(tmpdir+"/sub", "testG2.stg", templates);

		StringTemplateGroup group =
			StringTemplateGroup.loadGroup("testG2");
		String expecting = "group testG2;\n" +
			"bold(item) ::= <<foo>>\n" +
			"duh(a,b,c) ::= <<foo>>\n" +
			"t() ::= <<foo>>\n";
		assertEqual(group.toString(), expecting);
	}

	public void testGroupSatisfiesSingleInterface() throws Exception {
		// this also tests the group loader
		StringTemplateErrorListener errors = new ErrorBuffer();
		String tmpdir = System.getProperty("java.io.tmpdir");
		StringTemplateGroup.registerGroupLoader(new PathGroupLoader(tmpdir,errors));
		String groupI =
				"interface testI;" +newline+
				"t();" +newline+
				"bold(item);"+newline+
				"optional duh(a,b,c);"+newline;
		writeFile(tmpdir, "testI.sti", groupI);

		String templates =
			"group testG implements testI;" +newline+
			"t() ::= <<foo>>" +newline+
			"bold(item) ::= <<foo>>"+newline+
			"duh(a,b,c) ::= <<foo>>"+newline;

		writeFile(tmpdir, "testG.stg", templates);

		StringTemplateGroup group =
				new StringTemplateGroup(new FileReader(tmpdir+"/testG.stg"), errors);

		String expecting = ""; // should be no errors
		assertEqual(errors.toString(), expecting);
	}

	public void testGroupExtendsSuperGroup() throws Exception {
		// this also tests the group loader
		StringTemplateErrorListener errors = new ErrorBuffer();
		String tmpdir = System.getProperty("java.io.tmpdir");
		StringTemplateGroup.registerGroupLoader(
			new PathGroupLoader(tmpdir,errors)
		);
		String superGroup =
				"group superG;" +newline+
				"bold(item) ::= <<*$item$*>>;\n"+newline;
		writeFile(tmpdir, "superG.stg", superGroup);

		String templates =
			"group testG : superG;" +newline+
			"main(x) ::= <<$bold(x)$>>"+newline;

		writeFile(tmpdir, "testG.stg", templates);

		StringTemplateGroup group =
				new StringTemplateGroup(new FileReader(tmpdir+"/testG.stg"),
										DefaultTemplateLexer.class,
										errors);
		StringTemplate st = group.getInstanceOf("main");
		st.setAttribute("x", "foo");

		String expecting = "*foo*";
		assertEqual(st.toString(), expecting);
	}

	public void testMissingInterfaceTemplate() throws Exception {
		// this also tests the group loader
		StringTemplateErrorListener errors = new ErrorBuffer();
		String tmpdir = System.getProperty("java.io.tmpdir");
		StringTemplateGroup.registerGroupLoader(new PathGroupLoader(tmpdir,errors));
		String groupI =
				"interface testI;" +newline+
				"t();" +newline+
				"bold(item);"+newline+
				"optional duh(a,b,c);"+newline;
		writeFile(tmpdir, "testI.sti", groupI);

		String templates =
			"group testG implements testI;" +newline+
			"t() ::= <<foo>>" +newline+
			"duh(a,b,c) ::= <<foo>>"+newline;

		writeFile(tmpdir, "testG.stg", templates);

		StringTemplateGroup group =
				new StringTemplateGroup(new FileReader(tmpdir+"/testG.stg"), errors);

		String expecting = "group testG does not satisfy interface testI: missing templates [bold]";
		assertEqual(errors.toString(), expecting);
	}

	public void testMissingOptionalInterfaceTemplate() throws Exception {
		// this also tests the group loader
		StringTemplateErrorListener errors = new ErrorBuffer();
		String tmpdir = System.getProperty("java.io.tmpdir");
		StringTemplateGroup.registerGroupLoader(new PathGroupLoader(tmpdir,errors));
		String groupI =
				"interface testI;" +newline+
				"t();" +newline+
				"bold(item);"+newline+
				"optional duh(a,b,c);"+newline;
		writeFile(tmpdir, "testI.sti", groupI);

		String templates =
			"group testG implements testI;" +newline+
			"t() ::= <<foo>>" +newline+
			"bold(item) ::= <<foo>>";

		writeFile(tmpdir, "testG.stg", templates);

		StringTemplateGroup group =
				new StringTemplateGroup(new FileReader(tmpdir+"/testG.stg"), errors);

		String expecting = ""; // should be NO errors
		assertEqual(errors.toString(), expecting);
	}

	public void testMismatchedInterfaceTemplate() throws Exception {
		// this also tests the group loader
		StringTemplateErrorListener errors = new ErrorBuffer();
		String tmpdir = System.getProperty("java.io.tmpdir");
		StringTemplateGroup.registerGroupLoader(new PathGroupLoader(tmpdir,errors));
		String groupI =
				"interface testI;" +newline+
				"t();" +newline+
				"bold(item);"+newline+
				"optional duh(a,b,c);"+newline;
		writeFile(tmpdir, "testI.sti", groupI);

		String templates =
			"group testG implements testI;" +newline+
			"t() ::= <<foo>>" +newline+
			"bold(item) ::= <<foo>>"+newline+
			"duh(a,c) ::= <<foo>>"+newline;

		writeFile(tmpdir, "testG.stg", templates);

		StringTemplateGroup group =
				new StringTemplateGroup(new FileReader(tmpdir+"/testG.stg"), errors);

		String expecting = "group testG does not satisfy interface testI: mismatched arguments on these templates [optional duh(a, b, c)]";
		assertEqual(errors.toString(), expecting);
	}

	public void testGroupFileFormat() throws Exception {
		String templates =
				"group test;" +newline+
				"t() ::= \"literal template\"" +newline+
				"bold(item) ::= \"<b>$item$</b>\""+newline+
				"duh() ::= <<"+newline+"xx"+newline+">>"+newline;
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates),
										DefaultTemplateLexer.class);

		String expecting = "group test;" +newline+
				"bold(item) ::= <<<b>$item$</b>>>" +newline+
				"duh() ::= <<xx>>" +newline+
				"t() ::= <<literal template>>"+newline;
		assertEqual(group.toString(), expecting);

		StringTemplate a = group.getInstanceOf("t");
		expecting = "literal template";
		assertEqual(a.toString(), expecting);

		StringTemplate b = group.getInstanceOf("bold");
		b.setAttribute("item", "dork");
		expecting = "<b>dork</b>";
		assertEqual(b.toString(), expecting);
	}

	public void testEscapedTemplateDelimiters() throws Exception {
		String templates =
				"group test;" +newline+
				"t() ::= <<$\"literal\":{a|$a$\\}}$ template\n>>" +newline+
				"bold(item) ::= <<<b>$item$</b\\>>>"+newline+
				"duh() ::= <<"+newline+"xx"+newline+">>"+newline;
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates),
										DefaultTemplateLexer.class);

		String expecting = "group test;" +newline+
				"bold(item) ::= <<<b>$item$</b>>>" +newline+
				"duh() ::= <<xx>>" +newline+
				"t() ::= <<$\"literal\":{a|$a$\\}}$ template>>"+newline;
		assertEqual(group.toString(), expecting);

		StringTemplate b = group.getInstanceOf("bold");
		b.setAttribute("item", "dork");
		expecting = "<b>dork</b>";
		assertEqual(b.toString(), expecting);

		StringTemplate a = group.getInstanceOf("t");
		expecting = "literal} template";
		assertEqual(a.toString(), expecting);
	}

    /** Check syntax and setAttribute-time errors */
    public void testTemplateParameterDecls() throws Exception {
        String templates =
                "group test;" +newline+
                "t() ::= \"no args but ref $foo$\"" +newline+
                "t2(item) ::= \"decl but not used is ok\""+newline +
                "t3(a,b,c,d) ::= <<$a$ $d$>>"+newline+
                "t4(a,b,c,d) ::= <<$a$ $b$ $c$ $d$>>"+newline
                ;
        StringTemplateGroup group =
                new StringTemplateGroup(new StringReader(templates),
										DefaultTemplateLexer.class);

        // check setting unknown arg in empty formal list
        StringTemplate a = group.getInstanceOf("t");
        String error = null;
        try {
            a.setAttribute("foo", "x"); // want NoSuchElementException
        }
        catch (NoSuchElementException e) {
            error = e.getMessage();
        }
        String expecting = "no such attribute: foo in template context [t]";
        assertEqual(error, expecting);

        // check setting known arg
        a = group.getInstanceOf("t2");
        a.setAttribute("item", "x"); // shouldn't get exception

        // check setting unknown arg in nonempty list of formal args
        a = group.getInstanceOf("t3");
        a.setAttribute("b", "x");
    }

    public void testTemplateRedef() throws Exception {
        String templates =
                "group test;" +newline+
                "a() ::= \"x\"" +newline+
                "b() ::= \"y\"" +newline+
                "a() ::= \"z\"" +newline;
        StringTemplateErrorListener errors = new ErrorBuffer();
        StringTemplateGroup group =
                new StringTemplateGroup(new StringReader(templates), errors);
        String expecting = "redefinition of template: a";
        assertEqual(errors.toString(), expecting);
    }

    public void testMissingInheritedAttribute() throws Exception {
        String templates =
                "group test;" +newline+
                "page(title,font) ::= <<"+newline +
                "<html>"+newline +
                "<body>"+newline +
                "$title$<br>"+newline +
                "$body()$"+newline +
                "</body>"+newline +
                "</html>"+newline +
                ">>"+newline +
                "body() ::= \"<font face=$font$>my body</font>\"" +newline;
        StringTemplateGroup group =
                new StringTemplateGroup(new StringReader(templates),
										DefaultTemplateLexer.class);
        StringTemplate t = group.getInstanceOf("page");
        t.setAttribute("title","my title");
        t.setAttribute("font","Helvetica"); // body() will see it
        t.toString(); // should be no problem
    }

    public void testFormalArgumentAssignment() throws Exception {
        String templates =
                "group test;" +newline+
                "page() ::= <<$body(font=\"Times\")$>>"+newline +
                "body(font) ::= \"<font face=$font$>my body</font>\"" +newline;
        StringTemplateGroup group =
                new StringTemplateGroup(new StringReader(templates),
										DefaultTemplateLexer.class);
        StringTemplate t = group.getInstanceOf("page");
        String expecting = "<font face=Times>my body</font>";
        assertEqual(t.toString(), expecting);
    }

    public void testUndefinedArgumentAssignment() throws Exception {
        String templates =
                "group test;" +newline+
                "page(x) ::= <<$body(font=x)$>>"+newline +
                "body() ::= \"<font face=$font$>my body</font>\"" +newline;
        StringTemplateGroup group =
                new StringTemplateGroup(new StringReader(templates),
										DefaultTemplateLexer.class);
        StringTemplate t = group.getInstanceOf("page");
        t.setAttribute("x","Times");
        String error = "";
        try {
            t.toString();
        }
        catch (NoSuchElementException iae) {
            error = iae.getMessage();
        }
        String expecting = "template body has no such attribute: font in template context [page <invoke body arg context>]";
        assertEqual(error, expecting);
    }

    public void testFormalArgumentAssignmentInApply() throws Exception {
        String templates =
                "group test;" +newline+
                "page(name) ::= <<$name:bold(font=\"Times\")$>>"+newline +
                "bold(font) ::= \"<font face=$font$><b>$it$</b></font>\"" +newline;
        StringTemplateGroup group =
                new StringTemplateGroup(new StringReader(templates),
										DefaultTemplateLexer.class);
        StringTemplate t = group.getInstanceOf("page");
        t.setAttribute("name", "Ter");
        String expecting = "<font face=Times><b>Ter</b></font>";
        assertEqual(t.toString(), expecting);
    }

    public void testUndefinedArgumentAssignmentInApply() throws Exception {
        String templates =
                "group test;" +newline+
                "page(name,x) ::= <<$name:bold(font=x)$>>"+newline +
                "bold() ::= \"<font face=$font$><b>$it$</b></font>\"" +newline;
        StringTemplateGroup group =
                new StringTemplateGroup(new StringReader(templates),
										DefaultTemplateLexer.class);
        StringTemplate t = group.getInstanceOf("page");
        t.setAttribute("x","Times");
        t.setAttribute("name", "Ter");
        String error = "";
        try {
            t.toString();
        }
        catch (NoSuchElementException iae) {
            error = iae.getMessage();
        }
        String expecting = "template bold has no such attribute: font in template context [page <invoke bold arg context>]";
        assertEqual(error, expecting);
    }

    public void testUndefinedAttributeReference() throws Exception {
        String templates =
                "group test;" +newline+
                "page() ::= <<$bold()$>>"+newline +
                "bold() ::= \"$name$\"" +newline;
        StringTemplateGroup group =
                new StringTemplateGroup(new StringReader(templates),
										DefaultTemplateLexer.class);
        StringTemplate t = group.getInstanceOf("page");
        String error = "";
        try {
            t.toString();
        }
        catch (NoSuchElementException iae) {
            error = iae.getMessage();
        }
        String expecting = "no such attribute: name in template context [page bold]";
        assertEqual(error, expecting);
    }

    public void testUndefinedDefaultAttributeReference() throws Exception {
        String templates =
                "group test;" +newline+
                "page() ::= <<$bold()$>>"+newline +
                "bold() ::= \"$it$\"" +newline;
        StringTemplateGroup group =
                new StringTemplateGroup(new StringReader(templates),
										DefaultTemplateLexer.class);
        StringTemplate t = group.getInstanceOf("page");
        String error = "";
        try {
            t.toString();
        }
        catch (NoSuchElementException nse) {
            error = nse.getMessage();
        }
        String expecting = "no such attribute: it in template context [page bold]";
        assertEqual(error, expecting);
    }

    public void testAngleBracketsWithGroupFile() throws Exception {
        String templates =
                "group test;" +newline+
                "a(s) ::= \"<s:{case <i> : <it> break;}>\""+newline +
                "b(t) ::= \"<t; separator=\\\",\\\">\"" +newline+
                "c(t) ::= << <t; separator=\",\"> >>" +newline;
        // mainly testing to ensure we don't get parse errors of above
        StringTemplateGroup group =
                new StringTemplateGroup(
                        new StringReader(templates));
        StringTemplate t = group.getInstanceOf("a");
        t.setAttribute("s","Test");
        String expecting = "case 1 : Test break;";
        assertEqual(t.toString(), expecting);
    }

    public void testAngleBracketsNoGroup() throws Exception {
        StringTemplate st =new StringTemplate(
                "Tokens : <rules; separator=\"|\"> ;",
                AngleBracketTemplateLexer.class);
        st.setAttribute("rules", "A");
        st.setAttribute("rules", "B");
        String expecting = "Tokens : A|B ;";
        assertEqual(st.toString(), expecting);
    }

	public void testRegionRef() throws Exception {
		String templates =
				"group test;" +newline+
				"a() ::= \"X$@r()$Y\"" +newline;
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates),
										DefaultTemplateLexer.class);
		StringTemplate st = group.getInstanceOf("a");
		String result = st.toString();
		String expecting = "XY";
		assertEqual(result, expecting);
	}

	public void testEmbeddedRegionRef() throws Exception {
		String templates =
				"group test;" +newline+
				"a() ::= \"X$@r$blort$@end$Y\"" +newline;
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates),
										DefaultTemplateLexer.class);
		StringTemplate st = group.getInstanceOf("a");
		String result = st.toString();
		String expecting = "XblortY";
		assertEqual(result, expecting);
	}

	public void testRegionRefAngleBrackets() throws Exception {
		String templates =
				"group test;" +newline+
				"a() ::= \"X<@r()>Y\"" +newline;
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates));
		StringTemplate st = group.getInstanceOf("a");
		String result = st.toString();
		String expecting = "XY";
		assertEqual(result, expecting);
	}

	public void testEmbeddedRegionRefAngleBrackets() throws Exception {
		String templates =
				"group test;" +newline+
				"a() ::= \"X<@r>blort<@end>Y\"" +newline;
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates));
		StringTemplate st = group.getInstanceOf("a");
		String result = st.toString();
		String expecting = "XblortY";
		assertEqual(result, expecting);
	}

	public void testEmbeddedRegionRefWithNewlinesAngleBrackets() throws Exception {
		String templates =
				"group test;" +newline+
				"a() ::= \"X<@r>" +newline+
				"blort" +newline+
				"<@end>" +newline+
				"Y\"" +newline;
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates));
		StringTemplate st = group.getInstanceOf("a");
		String result = st.toString();
		String expecting = "XblortY";
		assertEqual(result, expecting);
	}

	public void testRegionRefWithDefAngleBrackets() throws Exception {
		String templates =
				"group test;" +newline+
				"a() ::= \"X<@r()>Y\"" +newline+
				"@a.r() ::= \"foo\"" +newline;
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates));
		StringTemplate st = group.getInstanceOf("a");
		String result = st.toString();
		String expecting = "XfooY";
		assertEqual(result, expecting);
	}

	public void testRegionRefWithDefInConditional() throws Exception {
		String templates =
				"group test;" +newline+
				"a(v) ::= \"X<if(v)>A<@r()>B<endif>Y\"" +newline+
				"@a.r() ::= \"foo\"" +newline;
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates));
		StringTemplate st = group.getInstanceOf("a");
		st.setAttribute("v", "true");
		String result = st.toString();
		String expecting = "XAfooBY";
		assertEqual(result, expecting);
	}

	public void testRegionRefWithImplicitDefInConditional() throws Exception {
		String templates =
				"group test;" +newline+
				"a(v) ::= \"X<if(v)>A<@r>yo<@end>B<endif>Y\"" +newline+
				"@a.r() ::= \"foo\"" +newline;
		StringTemplateErrorListener errors = new ErrorBuffer();
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates),
										errors);
		StringTemplate st = group.getInstanceOf("a");
		st.setAttribute("v", "true");
		String result = st.toString();
		String expecting = "XAyoBY";
		assertEqual(result, expecting);

		String err_result = errors.toString();
		String err_expecting = "group test line 3: redefinition of template region: @a.r";
		assertEqual(err_result, err_expecting);
	}

	public void testRegionOverride() throws Exception {
		String templates1 =
				"group super;" +newline+
				"a() ::= \"X<@r()>Y\"" +
				"@a.r() ::= \"foo\"" +newline;
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates1));

		String templates2 =
				"group sub;" +newline+
				"@a.r() ::= \"foo\"" +newline;
		StringTemplateGroup subGroup =
				new StringTemplateGroup(new StringReader(templates2),
										AngleBracketTemplateLexer.class,
										null,
										group);

		StringTemplate st = subGroup.getInstanceOf("a");
		String result = st.toString();
		String expecting = "XfooY";
		assertEqual(result, expecting);
	}

	public void testRegionOverrideRefSuperRegion() throws Exception {
		String templates1 =
				"group super;" +newline+
				"a() ::= \"X<@r()>Y\"" +
				"@a.r() ::= \"foo\"" +newline;
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates1));

		String templates2 =
				"group sub;" +newline+
				"@a.r() ::= \"A<@super.r()>B\"" +newline;
		StringTemplateGroup subGroup =
				new StringTemplateGroup(new StringReader(templates2),
										AngleBracketTemplateLexer.class,
										null,
										group);

		StringTemplate st = subGroup.getInstanceOf("a");
		String result = st.toString();
		String expecting = "XAfooBY";
		assertEqual(result, expecting);
	}

	public void testRegionOverrideRefSuperRegion3Levels() throws Exception {
		// Bug: This was causing infinite recursion:
		// getInstanceOf(super::a)
		// getInstanceOf(sub::a)
		// getInstanceOf(subsub::a)
		// getInstanceOf(subsub::region__a__r)
		// getInstanceOf(subsub::super.region__a__r)
		// getInstanceOf(subsub::super.region__a__r)
		// getInstanceOf(subsub::super.region__a__r)
		// ...
		// Somehow, the ref to super in subsub is not moving up the chain
		// to the @super.r(); oh, i introduced a bug when i put setGroup
		// into STG.getInstanceOf()!

		String templates1 =
				"group super;" +newline+
				"a() ::= \"X<@r()>Y\"" +
				"@a.r() ::= \"foo\"" +newline;
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates1));

		String templates2 =
				"group sub;" +newline+
				"@a.r() ::= \"<@super.r()>2\"" +newline;
		StringTemplateGroup subGroup =
				new StringTemplateGroup(new StringReader(templates2),
										AngleBracketTemplateLexer.class,
										null,
										group);

		String templates3 =
				"group subsub;" +newline+
				"@a.r() ::= \"<@super.r()>3\"" +newline;
		StringTemplateGroup subSubGroup =
				new StringTemplateGroup(new StringReader(templates3),
										AngleBracketTemplateLexer.class,
										null,
										subGroup);

		StringTemplate st = subSubGroup.getInstanceOf("a");
		String result = st.toString();
		String expecting = "Xfoo23Y";
		assertEqual(result, expecting);
	}

	public void testRegionOverrideRefSuperImplicitRegion() throws Exception {
		String templates1 =
				"group super;" +newline+
				"a() ::= \"X<@r>foo<@end>Y\""+newline;
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates1));

		String templates2 =
				"group sub;" +newline+
				"@a.r() ::= \"A<@super.r()>\"" +newline;
		StringTemplateGroup subGroup =
				new StringTemplateGroup(new StringReader(templates2),
										AngleBracketTemplateLexer.class,
										null,
										group);

		StringTemplate st = subGroup.getInstanceOf("a");
		String result = st.toString();
		String expecting = "XAfooY";
		assertEqual(result, expecting);
	}

	public void testEmbeddedRegionRedefError() throws Exception {
		// cannot define an embedded template within group
		String templates =
				"group test;" +newline+
				"a() ::= \"X<@r>dork<@end>Y\"" +
				"@a.r() ::= \"foo\"" +newline;
		StringTemplateErrorListener errors = new ErrorBuffer();
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates),
										errors);
		StringTemplate st = group.getInstanceOf("a");
		st.toString();
		String result = errors.toString();
		String expecting = "group test line 2: redefinition of template region: @a.r";
		assertEqual(result, expecting);
	}

	public void testImplicitRegionRedefError() throws Exception {
		// cannot define an implicitly-defined template more than once
		String templates =
				"group test;" +newline+
				"a() ::= \"X<@r()>Y\"" +newline+
				"@a.r() ::= \"foo\"" +newline+
				"@a.r() ::= \"bar\"" +newline;
		StringTemplateErrorListener errors = new ErrorBuffer();
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates),
										errors);
		StringTemplate st = group.getInstanceOf("a");
		st.toString();
		String result = errors.toString();
		String expecting = "group test line 4: redefinition of template region: @a.r";
		assertEqual(result, expecting);
	}

	public void testImplicitOverriddenRegionRedefError() throws Exception {
		String templates1 =
			"group super;" +newline+
			"a() ::= \"X<@r()>Y\"" +
			"@a.r() ::= \"foo\"" +newline;
		StringTemplateGroup group =
			new StringTemplateGroup(new StringReader(templates1));

		String templates2 =
			"group sub;" +newline+
			"@a.r() ::= \"foo\"" +newline+
			"@a.r() ::= \"bar\"" +newline;
		StringTemplateErrorListener errors = new ErrorBuffer();
		StringTemplateGroup subGroup =
				new StringTemplateGroup(new StringReader(templates2),
										AngleBracketTemplateLexer.class,
										errors,
										group);

		StringTemplate st = subGroup.getInstanceOf("a");
		String result = errors.toString();
		String expecting = "group sub line 3: redefinition of template region: @a.r";
		assertEqual(result, expecting);
	}

	public void testUnknownRegionDefError() throws Exception {
		// cannot define an implicitly-defined template more than once
		String templates =
				"group test;" +newline+
				"a() ::= \"X<@r()>Y\"" +newline+
				"@a.q() ::= \"foo\"" +newline;
		StringTemplateErrorListener errors = new ErrorBuffer();
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates),
										errors);
		StringTemplate st = group.getInstanceOf("a");
		st.toString();
		String result = errors.toString();
		String expecting = "group test line 3: template a has no region called q";
		assertEqual(result, expecting);
	}

	public void testSuperRegionRefError() throws Exception {
		String templates1 =
			"group super;" +newline+
			"a() ::= \"X<@r()>Y\"" +
			"@a.r() ::= \"foo\"" +newline;
		StringTemplateGroup group =
			new StringTemplateGroup(new StringReader(templates1));

		String templates2 =
			"group sub;" +newline+
			"@a.r() ::= \"A<@super.q()>B\"" +newline;
		StringTemplateErrorListener errors = new ErrorBuffer();
		StringTemplateGroup subGroup =
				new StringTemplateGroup(new StringReader(templates2),
										AngleBracketTemplateLexer.class,
										errors,
										group);

		StringTemplate st = subGroup.getInstanceOf("a");
		String result = errors.toString();
		String expecting = "template a has no region called q";
		assertEqual(result, expecting);
	}

	public void testMissingEndRegionError() throws Exception {
		// cannot define an implicitly-defined template more than once
		String templates =
				"group test;" +newline+
				"a() ::= \"X$@r$foo\"" +newline;
		StringTemplateErrorListener errors = new ErrorBuffer();
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates),
										DefaultTemplateLexer.class,
										errors,
										null);
		StringTemplate st = group.getInstanceOf("a");
		st.toString();
		String result = errors.toString();
		String expecting = "missing region r $@end$ tag";
		assertEqual(result, expecting);
	}

	public void testMissingEndRegionErrorAngleBrackets() throws Exception {
		// cannot define an implicitly-defined template more than once
		String templates =
				"group test;" +newline+
				"a() ::= \"X<@r>foo\"" +newline;
		StringTemplateErrorListener errors = new ErrorBuffer();
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates),
										errors);
		StringTemplate st = group.getInstanceOf("a");
		st.toString();
		String result = errors.toString();
		String expecting = "missing region r <@end> tag";
		assertEqual(result, expecting);
	}

    public void testSimpleInheritance() throws Exception {
		// make a bold template in the super group that you can inherit from sub
		StringTemplateGroup supergroup = new StringTemplateGroup("super");
		StringTemplateGroup subgroup = new StringTemplateGroup("sub");
		StringTemplate bold = supergroup.defineTemplate("bold", "<b>$it$</b>");
		subgroup.setSuperGroup(supergroup);
		StringTemplateErrorListener errors = new ErrorBuffer();
		subgroup.setErrorListener(errors);
		supergroup.setErrorListener(errors);
		StringTemplate duh = new StringTemplate(subgroup, "$name:bold()$");
		duh.setAttribute("name", "Terence");
		String expecting = "<b>Terence</b>";
		assertEqual(duh.toString(), expecting);
	}

	public void testOverrideInheritance() throws Exception {
		// make a bold template in the super group and one in sub group
		StringTemplateGroup supergroup = new StringTemplateGroup("super");
		StringTemplateGroup subgroup = new StringTemplateGroup("sub");
		supergroup.defineTemplate("bold", "<b>$it$</b>");
		subgroup.defineTemplate("bold", "<strong>$it$</strong>");
		subgroup.setSuperGroup(supergroup);
		StringTemplateErrorListener errors = new ErrorBuffer();
		subgroup.setErrorListener(errors);
		supergroup.setErrorListener(errors);
		StringTemplate duh = new StringTemplate(subgroup, "$name:bold()$");
		duh.setAttribute("name", "Terence");
		String expecting = "<strong>Terence</strong>";
		assertEqual(duh.toString(), expecting);
	}

	public void testMultiLevelInheritance() throws Exception {
		// must loop up two levels to find bold()
		StringTemplateGroup rootgroup = new StringTemplateGroup("root");
		StringTemplateGroup level1 = new StringTemplateGroup("level1");
		StringTemplateGroup level2 = new StringTemplateGroup("level2");
		rootgroup.defineTemplate("bold", "<b>$it$</b>");
		level1.setSuperGroup(rootgroup);
		level2.setSuperGroup(level1);
		StringTemplateErrorListener errors = new ErrorBuffer();
		rootgroup.setErrorListener(errors);
		level1.setErrorListener(errors);
		level2.setErrorListener(errors);
		StringTemplate duh = new StringTemplate(level2, "$name:bold()$");
		duh.setAttribute("name", "Terence");
		String expecting = "<b>Terence</b>";
		assertEqual(duh.toString(), expecting);
	}

	public void testComplicatedInheritance() throws Exception {
		// in super: decls invokes labels
		// in sub:   overridden decls which calls super.decls
		//           overridden labels
		// Bug: didn't see the overridden labels.  In other words,
		// the overridden decls called super which called labels, but
		// didn't get the subgroup overridden labels--it calls the
		// one in the superclass.  Ouput was "DL" not "DSL"; didn't
		// invoke sub's labels().
		String basetemplates =
			"group base;" +newline+
			"decls() ::= \"D<labels()>\""+newline+
			"labels() ::= \"L\"" +newline
			;
		StringTemplateGroup base =
			new StringTemplateGroup(new StringReader(basetemplates));
		String subtemplates =
			"group sub;" +newline+
			"decls() ::= \"<super.decls()>\""+newline+
			"labels() ::= \"SL\"" +newline
			;
		StringTemplateGroup sub =
				new StringTemplateGroup(new StringReader(subtemplates));
		sub.setSuperGroup(base);
		StringTemplate st = sub.getInstanceOf("decls");
		String expecting = "DSL";
		String result = st.toString();
		assertEqual(result, expecting);
	}

	public void test3LevelSuperRef() throws Exception {
		String templates1 =
				"group super;" +newline+
				"r() ::= \"foo\"" +newline;
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates1));

		String templates2 =
				"group sub;" +newline+
				"r() ::= \"<super.r()>2\"" +newline;
		StringTemplateGroup subGroup =
				new StringTemplateGroup(new StringReader(templates2),
										AngleBracketTemplateLexer.class,
										null,
										group);

		String templates3 =
				"group subsub;" +newline+
				"r() ::= \"<super.r()>3\"" +newline;
		StringTemplateGroup subSubGroup =
				new StringTemplateGroup(new StringReader(templates3),
										AngleBracketTemplateLexer.class,
										null,
										subGroup);

		StringTemplate st = subSubGroup.getInstanceOf("r");
		String result = st.toString();
		String expecting = "foo23";
		assertEqual(result, expecting);
	}

	public void testExprInParens() throws Exception {
		// specify a template to apply to an attribute
		// Use a template group so we can specify the start/stop chars
		StringTemplateGroup group =
			new StringTemplateGroup("dummy", ".");
		StringTemplate bold = group.defineTemplate("bold", "<b>$it$</b>");
		StringTemplate duh = new StringTemplate(group, "$(\"blort: \"+(list)):bold()$");
		duh.setAttribute("list", "a");
		duh.setAttribute("list", "b");
		duh.setAttribute("list", "c");
		// System.out.println(duh);
		String expecting = "<b>blort: abc</b>";
		assertEqual(duh.toString(), expecting);
	}

    public void testMultipleAdditions() throws Exception {
        // specify a template to apply to an attribute
        // Use a template group so we can specify the start/stop chars
        StringTemplateGroup group =
            new StringTemplateGroup("dummy", ".");
        group.defineTemplate("link", "<a href=\"$url$\"><b>$title$</b></a>");
        StringTemplate duh =
            new StringTemplate(group,
                "$link(url=\"/member/view?ID=\"+ID+\"&x=y\"+foo, title=\"the title\")$");
        duh.setAttribute("ID", "3321");
        duh.setAttribute("foo", "fubar");
        String expecting = "<a href=\"/member/view?ID=3321&x=yfubar\"><b>the title</b></a>";
        assertEqual(duh.toString(), expecting);
    }

    public void testCollectionAttributes() throws Exception {
        StringTemplateGroup group =
                new StringTemplateGroup("test");
        StringTemplate bold = group.defineTemplate("bold", "<b>$it$</b>");
        StringTemplate t =
            new StringTemplate(group, "$data$, $data:bold()$, "+
                                      "$list:bold():bold()$, $array$, $a2$, $a3$, $a4$");
        Vector v = new Vector();
        v.addElement("1");
        v.addElement("2");
        v.addElement("3");
        List list = new ArrayList();
        list.add("a");
        list.add("b");
        list.add("c");
        t.setAttribute("data", v);
        t.setAttribute("list", list);
        t.setAttribute("array", new String[] {"x","y"});
        t.setAttribute("a2", new int[] {10,20});
        t.setAttribute("a3", new float[] {1.2f,1.3f});
        t.setAttribute("a4", new double[] {8.7,9.2});
        //System.out.println(t);
        String expecting="123, <b>1</b><b>2</b><b>3</b>, "+
            "<b><b>a</b></b><b><b>b</b></b><b><b>c</b></b>, xy, 1020, 1.21.3, 8.79.2";
        assertEqual(t.toString(), expecting);
    }

    public void testParenthesizedExpression() throws Exception {
        StringTemplateGroup group =
                new StringTemplateGroup("test");
        StringTemplate bold = group.defineTemplate("bold", "<b>$it$</b>");
        StringTemplate t = new StringTemplate(group, "$(f+l):bold()$");
        t.setAttribute("f", "Joe");
        t.setAttribute("l", "Schmoe");
        //System.out.println(t);
        String expecting="<b>JoeSchmoe</b>";
        assertEqual(t.toString(), expecting);
    }

	public void testApplyTemplateNameExpression() throws Exception {
        StringTemplateGroup group =
                new StringTemplateGroup("test");
        StringTemplate bold = group.defineTemplate("foobar", "foo$attr$bar");
        StringTemplate t = new StringTemplate(group, "$data:(name+\"bar\")()$");
        t.setAttribute("data", "Ter");
        t.setAttribute("data", "Tom");
        t.setAttribute("name", "foo");
        //System.out.println(t);
        String expecting="fooTerbarfooTombar";
        assertEqual(t.toString(), expecting);
    }

    public void testTemplateNameExpression() throws Exception {
        StringTemplateGroup group =
                new StringTemplateGroup("test");
        StringTemplate foo = group.defineTemplate("foo", "hi there!");
        StringTemplate t = new StringTemplate(group, "$(name)()$");
        t.setAttribute("name", "foo");
        //System.out.println(t);
        String expecting="hi there!";
        assertEqual(t.toString(), expecting);
    }

    public void testMissingEndDelimiter() throws Exception {
        StringTemplateGroup group =
                new StringTemplateGroup("test");
		StringTemplateErrorListener errors = new ErrorBuffer();
        group.setErrorListener(errors);
        StringTemplate t = new StringTemplate(group, "stuff $a then more junk etc...");
        String expectingError="problem parsing template 'anonymous': line 1:31: expecting '$', found '<EOF>'";
        //System.out.println("error: '"+errors+"'");
        //System.out.println("expecting: '"+expectingError+"'");
        assertTrue(errors.toString().startsWith(expectingError));
    }

    public void testSetButNotRefd() throws Exception {
        StringTemplate.setLintMode(true);
        StringTemplateGroup group =
                new StringTemplateGroup("test");
        StringTemplate t = new StringTemplate(group, "$a$ then $b$ and $c$ refs.");
        t.setAttribute("a", "Terence");
        t.setAttribute("b", "Terence");
        t.setAttribute("cc", "Terence"); // oops...should be 'c'
        final String newline = System.getProperty("line.separator");
		StringTemplateErrorListener errors = new ErrorBuffer();
        group.setErrorListener(errors);
        String expectingError="anonymous: set but not used: cc";
        String result = t.toString();    // result is irrelevant
        //System.out.println("result error: '"+errors+"'");
        //System.out.println("expecting: '"+expectingError+"'");
        StringTemplate.setLintMode(false);
        assertEqual(errors.toString(), expectingError);
    }

    public void testNullTemplateApplication() throws Exception {
        StringTemplateGroup group =
                new StringTemplateGroup("test");
		StringTemplateErrorListener errors = new ErrorBuffer();
        group.setErrorListener(errors);
        StringTemplate t = new StringTemplate(group, "$names:bold(x=it)$");
        t.setAttribute("names", "Terence");
        //System.out.println(t);
        String expecting=null;
		String result = null;
		String error = null;
		try {
			result = t.toString();
		}
		catch (IllegalArgumentException iae) {
			error = iae.getMessage();
		}
		assertEqual(error, "Can't find template bold.st; context is [anonymous]");
    }

    public void testNullTemplateToMultiValuedApplication() throws Exception {
        StringTemplateGroup group =
                new StringTemplateGroup("test");
		StringTemplateErrorListener errors = new ErrorBuffer();
        group.setErrorListener(errors);
        StringTemplate t = new StringTemplate(group, "$names:bold(x=it)$");
        t.setAttribute("names", "Terence");
        t.setAttribute("names", "Tom");
        //System.out.println(t);
        String expecting=null; // bold not found...empty string
		String result = null;
		String error = null;
		try {
			result = t.toString();
		}
		catch (IllegalArgumentException iae) {
			error = iae.getMessage();
		}
		assertEqual(error, "Can't find template bold.st; context is [anonymous]");
    }

    public void testChangingAttrValueTemplateApplicationToVector() throws Exception {
        StringTemplateGroup group =
                new StringTemplateGroup("test");
        StringTemplate bold = group.defineTemplate("bold", "<b>$x$</b>");
        StringTemplate t = new StringTemplate(group, "$names:bold(x=it)$");
        t.setAttribute("names", "Terence");
        t.setAttribute("names", "Tom");
        //System.out.println("'"+t.toString()+"'");
        String expecting="<b>Terence</b><b>Tom</b>";
        assertEqual(t.toString(), expecting);
    }

    public void testChangingAttrValueRepeatedTemplateApplicationToVector() throws Exception {
        StringTemplateGroup group =
                new StringTemplateGroup("dummy", ".");
        StringTemplate bold = group.defineTemplate("bold", "<b>$item$</b>");
        StringTemplate italics = group.defineTemplate("italics", "<i>$it$</i>");
        StringTemplate members =
                new StringTemplate(group, "$members:bold(item=it):italics(it=it)$");
        members.setAttribute("members", "Jim");
        members.setAttribute("members", "Mike");
        members.setAttribute("members", "Ashar");
        //System.out.println("members="+members);
        String expecting = "<i><b>Jim</b></i><i><b>Mike</b></i><i><b>Ashar</b></i>";
        assertEqual(members.toString(), expecting);
    }

    public void testAlternatingTemplateApplication() throws Exception {
        StringTemplateGroup group =
                new StringTemplateGroup("dummy", ".");
        StringTemplate listItem = group.defineTemplate("listItem", "<li>$it$</li>");
        StringTemplate bold = group.defineTemplate("bold", "<b>$it$</b>");
        StringTemplate italics = group.defineTemplate("italics", "<i>$it$</i>");
        StringTemplate item =
                new StringTemplate(group, "$item:bold(),italics():listItem()$");
        item.setAttribute("item", "Jim");
        item.setAttribute("item", "Mike");
        item.setAttribute("item", "Ashar");
        //System.out.println("ITEM="+item);
        String expecting = "<li><b>Jim</b></li><li><i>Mike</i></li><li><b>Ashar</b></li>";
        assertEqual(item.toString(), expecting);
    }

    public void testExpressionAsRHSOfAssignment() throws Exception {
        StringTemplateGroup group =
                new StringTemplateGroup("test");
        StringTemplate hostname = group.defineTemplate("hostname", "$machine$.jguru.com");
        StringTemplate bold = group.defineTemplate("bold", "<b>$x$</b>");
        StringTemplate t = new StringTemplate(group, "$bold(x=hostname(machine=\"www\"))$");
        String expecting="<b>www.jguru.com</b>";
        assertEqual(t.toString(), expecting);
    }

    public void testTemplateApplicationAsRHSOfAssignment() throws Exception {
        StringTemplateGroup group =
                new StringTemplateGroup("test");
        StringTemplate hostname = group.defineTemplate("hostname", "$machine$.jguru.com");
        StringTemplate bold = group.defineTemplate("bold", "<b>$x$</b>");
        StringTemplate italics = group.defineTemplate("italics", "<i>$it$</i>");
        StringTemplate t = new StringTemplate(group, "$bold(x=hostname(machine=\"www\"):italics())$");
        String expecting="<b><i>www.jguru.com</i></b>";
        assertEqual(t.toString(), expecting);
    }

    public void testParameterAndAttributeScoping() throws Exception {
        StringTemplateGroup group =
                new StringTemplateGroup("test");
        StringTemplate italics = group.defineTemplate("italics", "<i>$x$</i>");
        StringTemplate bold = group.defineTemplate("bold", "<b>$x$</b>");
        StringTemplate t = new StringTemplate(group, "$bold(x=italics(x=name))$");
        t.setAttribute("name", "Terence");
        //System.out.println(t);
        String expecting="<b><i>Terence</i></b>";
        assertEqual(t.toString(), expecting);
    }

    public void testComplicatedSeparatorExpr() throws Exception {
        StringTemplateGroup group =
                new StringTemplateGroup("test");
        StringTemplate bold = group.defineTemplate("bulletSeparator", "</li>$foo$<li>");
        // make separator a complicated expression with args passed to included template
        StringTemplate t =
            new StringTemplate(group,
                               "<ul>$name; separator=bulletSeparator(foo=\" \")+\"&nbsp;\"$</ul>");
        t.setAttribute("name", "Ter");
        t.setAttribute("name", "Tom");
        t.setAttribute("name", "Mel");
        //System.out.println(t);
        String expecting = "<ul>Ter</li> <li>&nbsp;Tom</li> <li>&nbsp;Mel</ul>";
        assertEqual(t.toString(), expecting);
    }

    public void testAttributeRefButtedUpAgainstEndifAndWhitespace() throws Exception {
        StringTemplateGroup group =
                new StringTemplateGroup("test");
        StringTemplate a = new StringTemplate(group,
                                              "$if (!firstName)$$email$$endif$");
        a.setAttribute("email", "parrt@jguru.com");
        String expecting = "parrt@jguru.com";
        assertEqual(a.toString(), expecting);
    }

	public void testStringCatenationOnSingleValuedAttributeViaTemplateLiteral() throws Exception {
		StringTemplateGroup group =
				new StringTemplateGroup("test");
		StringTemplate bold = group.defineTemplate("bold", "<b>$it$</b>");
		//StringTemplate a = new StringTemplate(group, "$\" Parr\":bold()$");
		StringTemplate b = new StringTemplate(group, "$bold(it={$name$ Parr})$");
		//a.setAttribute("name", "Terence");
		b.setAttribute("name", "Terence");
		String expecting = "<b>Terence Parr</b>";
		//assertEqual(a.toString(), expecting);
		assertEqual(b.toString(), expecting);
	}

	public void testStringCatenationOpOnArg() throws Exception {
		StringTemplateGroup group =
				new StringTemplateGroup("test");
		StringTemplate bold = group.defineTemplate("bold", "<b>$it$</b>");
		StringTemplate b = new StringTemplate(group, "$bold(it=name+\" Parr\")$");
		//a.setAttribute("name", "Terence");
		b.setAttribute("name", "Terence");
		String expecting = "<b>Terence Parr</b>";
		//assertEqual(a.toString(), expecting);
		assertEqual(b.toString(), expecting);
	}

	public void testStringCatenationOpOnArgWithEqualsInString() throws Exception {
		StringTemplateGroup group =
				new StringTemplateGroup("test");
		StringTemplate bold = group.defineTemplate("bold", "<b>$it$</b>");
		StringTemplate b = new StringTemplate(group, "$bold(it=name+\" Parr=\")$");
		//a.setAttribute("name", "Terence");
		b.setAttribute("name", "Terence");
		String expecting = "<b>Terence Parr=</b>";
		//assertEqual(a.toString(), expecting);
		assertEqual(b.toString(), expecting);
	}

    public void testApplyingTemplateFromDiskWithPrecompiledIF()
            throws Exception
    {
        String newline = System.getProperty("line.separator");
        // write the template files first to /tmp
        FileWriter fw = new FileWriter("/tmp/page.st");
        fw.write("<html><head>"+newline);
        //fw.write("  <title>PeerScope: $title$</title>"+newline);
        fw.write("</head>"+newline);
        fw.write("<body>"+newline);
        fw.write("$if(member)$User: $member:terse()$$endif$"+newline);
        fw.write("</body>"+newline);
        fw.write("</head>"+newline);
        fw.close();

        fw = new FileWriter("/tmp/terse.st");
        fw.write("$it.firstName$ $it.lastName$ (<tt>$it.email$</tt>)"+newline);
        fw.close();

        // specify a template to apply to an attribute
        // Use a template group so we can specify the start/stop chars
        StringTemplateGroup group =
                new StringTemplateGroup("dummy", "/tmp");

        StringTemplate a = group.getInstanceOf("page");
        a.setAttribute("member", new Connector());
        String expecting = "<html><head>"+newline+
                "</head>"+newline+
                "<body>"+newline+
                "User: Terence Parr (<tt>parrt@jguru.com</tt>)"+newline+
                "</body>"+newline+
                "</head>";
        //System.out.println("'"+a+"'");
        assertEqual(a.toString(), expecting);
    }

    public void testMultiValuedAttributeWithAnonymousTemplateUsingIndexVariableI()
            throws Exception
    {
        StringTemplateGroup tgroup =
                new StringTemplateGroup("dummy", ".");
        StringTemplate t =
                new StringTemplate(tgroup,
                                   " List:"+newline+"  "+newline+"foo"+newline+newline+
                                   "$names:{<br>$i$. $it$"+newline+
								   "}$");
        t.setAttribute("names", "Terence");
        t.setAttribute("names", "Jim");
        t.setAttribute("names", "Sriram");
        String newline = System.getProperty("line.separator");
        //System.out.println(t);
        String expecting =
                " List:"+newline+
                "  "+newline+
                "foo"+newline+newline+
                "<br>1. Terence"+newline+
                "<br>2. Jim"+newline+
                "<br>3. Sriram"+newline;
        assertEqual(t.toString(), expecting);
    }

    public void testFindTemplateInCLASSPATH() throws Exception {
        // Look for templates in CLASSPATH as resources
        StringTemplateGroup mgroup =
                new StringTemplateGroup("method stuff",
										AngleBracketTemplateLexer.class);
        StringTemplate m = mgroup.getInstanceOf("org/antlr/stringtemplate/test/method");
        // "method.st" references body() so "body.st" will be loaded too
        m.setAttribute("visibility", "public");
        m.setAttribute("name", "foobar");
        m.setAttribute("returnType", "void");
        m.setAttribute("statements", "i=1;"); // body inherits these from method
        m.setAttribute("statements", "x=i;");
        String newline = System.getProperty("line.separator");
        String expecting =
                "public void foobar() {"+newline+
                "\t// start of a body"+newline+
                "\ti=1;"+newline+
				"\tx=i;"+newline+
                "\t// end of a body"+newline+
                "}";
		//System.out.println(m);
        assertEqual(m.toString(), expecting);
    }

    public void testApplyTemplateToSingleValuedAttribute() throws Exception {
        StringTemplateGroup group =
                new StringTemplateGroup("test");
        StringTemplate bold = group.defineTemplate("bold", "<b>$x$</b>");
        StringTemplate name = new StringTemplate(group, "$name:bold(x=name)$");
        name.setAttribute("name", "Terence");
        assertEqual(name.toString(), "<b>Terence</b>");
    }

    public void testStringLiteralAsAttribute() throws Exception {
        StringTemplateGroup group =
                new StringTemplateGroup("test");
        StringTemplate bold = group.defineTemplate("bold", "<b>$it$</b>");
        StringTemplate name = new StringTemplate(group, "$\"Terence\":bold()$");
        assertEqual(name.toString(), "<b>Terence</b>");
    }

    public void testApplyTemplateToSingleValuedAttributeWithDefaultAttribute() throws Exception {
        StringTemplateGroup group =
                new StringTemplateGroup("test");
        StringTemplate bold = group.defineTemplate("bold", "<b>$it$</b>");
        StringTemplate name = new StringTemplate(group, "$name:bold()$");
        name.setAttribute("name", "Terence");
        assertEqual(name.toString(), "<b>Terence</b>");
    }

    public void testApplyAnonymousTemplateToSingleValuedAttribute() throws Exception {
        // specify a template to apply to an attribute
        // Use a template group so we can specify the start/stop chars
        StringTemplateGroup group =
                new StringTemplateGroup("dummy", ".");
        StringTemplate item =
                new StringTemplate(group, "$item:{<li>$it$</li>}$");
        item.setAttribute("item", "Terence");
        assertEqual(item.toString(), "<li>Terence</li>");
    }

    public void testApplyAnonymousTemplateToMultiValuedAttribute() throws Exception {
        // specify a template to apply to an attribute
        // Use a template group so we can specify the start/stop chars
        StringTemplateGroup group =
                new StringTemplateGroup("dummy", ".");
        StringTemplate list =
                new StringTemplate(group, "<ul>$items$</ul>");
        // demonstrate setting arg to anonymous subtemplate
        StringTemplate item =
                new StringTemplate(group, "$item:{<li>$it$</li>}; separator=\",\"$");
        item.setAttribute("item", "Terence");
        item.setAttribute("item", "Jim");
        item.setAttribute("item", "John");
        list.setAttribute("items", item); // nested template
        assertEqual(list.toString(), "<ul><li>Terence</li>,<li>Jim</li>,<li>John</li></ul>");
    }

    public void testApplyAnonymousTemplateToAggregateAttribute() throws Exception {
        StringTemplate st =
                new StringTemplate("$items:{$it.lastName$, $it.firstName$\n}$");
		// also testing wacky spaces in aggregate spec
		st.setAttribute("items.{ firstName ,lastName}", "Ter", "Parr");
        st.setAttribute("items.{firstName, lastName }", "Tom", "Burns");
        String expecting =
                "Parr, Ter"+newline +
                "Burns, Tom"+newline;
        assertEqual(st.toString(), expecting);
    }

    public void testRepeatedApplicationOfTemplateToSingleValuedAttribute() throws Exception {
        StringTemplateGroup group =
                new StringTemplateGroup("dummy", ".");
        StringTemplate search = group.defineTemplate("bold", "<b>$it$</b>");
        StringTemplate item =
                new StringTemplate(group, "$item:bold():bold()$");
        item.setAttribute("item", "Jim");
        assertEqual(item.toString(), "<b><b>Jim</b></b>");
    }

    public void testRepeatedApplicationOfTemplateToMultiValuedAttributeWithSeparator() throws Exception {
        StringTemplateGroup group =
                new StringTemplateGroup("dummy", ".");
        StringTemplate search = group.defineTemplate("bold", "<b>$it$</b>");
        StringTemplate item =
                new StringTemplate(group, "$item:bold():bold(); separator=\",\"$");
        item.setAttribute("item", "Jim");
        item.setAttribute("item", "Mike");
        item.setAttribute("item", "Ashar");
        // first application of template must yield another vector!
        //System.out.println("ITEM="+item);
        assertEqual(item.toString(), "<b><b>Jim</b></b>,<b><b>Mike</b></b>,<b><b>Ashar</b></b>");
    }

    // ### NEED A TEST OF obj ASSIGNED TO ARG?

    public void testMultiValuedAttributeWithSeparator() throws Exception {
        StringTemplate query;

        // if column can be multi-valued, specify a separator
        StringTemplateGroup group =
            new StringTemplateGroup("dummy", ".", AngleBracketTemplateLexer.class);
        query = new StringTemplate(group, "SELECT <distinct> <column; separator=\", \"> FROM <table>;");
        query.setAttribute("column", "name");
        query.setAttribute("column", "email");
        query.setAttribute("table", "User");
        // uncomment next line to make "DISTINCT" appear in output
        // query.setAttribute("distince", "DISTINCT");
        // System.out.println(query);
        assertEqual(query.toString(), "SELECT  name, email FROM User;");
    }

    public void testSingleValuedAttributes() throws Exception {
        // all attributes are single-valued:
        StringTemplate query =
                new StringTemplate("SELECT $column$ FROM $table$;");
        query.setAttribute("column", "name");
        query.setAttribute("table", "User");
        // System.out.println(query);
        assertEqual(query.toString(), "SELECT name FROM User;");
    }

	public void testIFTemplate() throws Exception {
		StringTemplateGroup group =
			new StringTemplateGroup("dummy", ".", AngleBracketTemplateLexer.class);
		StringTemplate t =
			new StringTemplate(group,
					  "SELECT <column> FROM PERSON "+
					  "<if(cond)>WHERE ID=<id><endif>;");
		t.setAttribute("column", "name");
		t.setAttribute("cond", "true");
		t.setAttribute("id", "231");
		assertEqual(t.toString(), "SELECT name FROM PERSON WHERE ID=231;");
	}

	public void testIFCondWithParensTemplate() throws Exception {
		StringTemplateGroup group =
			new StringTemplateGroup("dummy", ".", AngleBracketTemplateLexer.class);
		StringTemplate t =
			new StringTemplate(group,
				"<if(map.(type))><type> <prop>=<map.(type)>;<endif>");
		HashMap map = new HashMap();
		map.put("int","0");
		t.setAttribute("map", map);
		t.setAttribute("prop", "x");
		t.setAttribute("type", "int");
		assertEqual(t.toString(), "int x=0;");
	}

	public void testIFCondWithParensDollarDelimsTemplate() throws Exception {
		StringTemplateGroup group =
			new StringTemplateGroup("dummy", ".");
		StringTemplate t =
			new StringTemplate(group,
				"$if(map.(type))$$type$ $prop$=$map.(type)$;$endif$");
		HashMap map = new HashMap();
		map.put("int","0");
		t.setAttribute("map", map);
		t.setAttribute("prop", "x");
		t.setAttribute("type", "int");
		assertEqual(t.toString(), "int x=0;");
	}

	/** As of 2.0, you can test a boolean value */
	public void testIFBoolean() throws Exception {
		StringTemplateGroup group =
			new StringTemplateGroup("dummy", ".");
		StringTemplate t =
			new StringTemplate(group,
					  "$if(b)$x$endif$ $if(!b)$y$endif$");
		t.setAttribute("b", new Boolean(true));
		assertEqual(t.toString(), "x ");

		t = t.getInstanceOf();
		t.setAttribute("b", new Boolean(false));
		assertEqual(t.toString(), " y");
	}

    public void testNestedIFTemplate() throws Exception {
        String newline = System.getProperty("line.separator");
        StringTemplateGroup group =
            new StringTemplateGroup("dummy", ".", AngleBracketTemplateLexer.class);
        StringTemplate t =
            new StringTemplate(group,
                "ack<if(a)>"+newline+
                "foo"+newline+
                "<if(!b)>stuff<endif>"+newline+
                "<if(b)>no<endif>"+newline+
                "junk"+newline+
                "<endif>"
            );
        t.setAttribute("a", "blort");
        // leave b as null
        //System.out.println("t="+t);
        String expecting =
                "ackfoo"+newline+
                "stuff"+newline+
                "junk";
        assertEqual(t.toString(), expecting);
    }

    public class Connector {
        public int getID() { return 1; }
        public String getFirstName() { return "Terence"; }
        public String getLastName() { return "Parr"; }
        public String getEmail() { return "parrt@jguru.com"; }
        public String getBio() { return "Superhero by night..."; }
		/** As of 2.0, booleans work as you expect.  In 1.x,
		 *  a missing value simulated a boolean.
		 */
        public boolean getCanEdit() { return false; }
    }

	public class Connector2 {
		public int getID() { return 2; }
		public String getFirstName() { return "Tom"; }
		public String getLastName() { return "Burns"; }
		public String getEmail() { return "tombu@jguru.com"; }
		public String getBio() { return "Superhero by day..."; }
		public Boolean getCanEdit() { return new Boolean(true); }
	}

    public void testObjectPropertyReference() throws Exception {
        StringTemplateGroup group =
                new StringTemplateGroup("dummy", ".");
        String newline = System.getProperty("line.separator");
        StringTemplate t =
                new StringTemplate(
                        group,
                        "<b>Name: $p.firstName$ $p.lastName$</b><br>"+newline+
                        "<b>Email: $p.email$</b><br>"+newline+
                        "$p.bio$"
                );
        t.setAttribute("p", new Connector());
        //System.out.println("t is "+t.toString());
        String expecting =
                "<b>Name: Terence Parr</b><br>"+newline+
                "<b>Email: parrt@jguru.com</b><br>"+newline+
                "Superhero by night...";
        assertEqual(t.toString(), expecting);
    }

    public void testApplyRepeatedAnonymousTemplateWithForeignTemplateRefToMultiValuedAttribute() throws Exception {
        // specify a template to apply to an attribute
        // Use a template group so we can specify the start/stop chars
        StringTemplateGroup group =
            new StringTemplateGroup("dummy", ".");
        group.defineTemplate("link", "<a href=\"$url$\"><b>$title$</b></a>");
        StringTemplate duh =
            new StringTemplate(group,
        "start|$p:{$link(url=\"/member/view?ID=\"+it.ID, title=it.firstName)$ $if(it.canEdit)$canEdit$endif$}:"+
        "{$it$<br>\n}$|end");
        duh.setAttribute("p", new Connector());
        duh.setAttribute("p", new Connector2());
        String newline = System.getProperty("line.separator");
        //System.out.println(duh);
        String expecting = "start|<a href=\"/member/view?ID=1\"><b>Terence</b></a> <br>"+newline+
            "<a href=\"/member/view?ID=2\"><b>Tom</b></a> canEdit<br>"+newline+
            "|end";
        assertEqual(duh.toString(), expecting);
    }

	public static class Tree {
		protected List children = new LinkedList();
		protected String text;
		public Tree(String t) {
			text = t;
		}
		public String getText() {
			return text;
		}
		public void addChild(Tree c) {
			children.add(c);
		}
		public Tree getFirstChild() {
			if ( children.size()==0 ) {
				return null;
			}
			return (Tree)children.get(0);
		}
		public List getChildren() {
			return children;
		}
	}

	public void testRecursion() throws Exception {
		StringTemplateGroup group =
			new StringTemplateGroup("dummy", ".", AngleBracketTemplateLexer.class);
		group.defineTemplate("tree",
		"<if(it.firstChild)>"+
		  "( <it.text> <it.children:tree(); separator=\" \"> )" +
		"<else>" +
		  "<it.text>" +
		"<endif>");
		StringTemplate tree = group.getInstanceOf("tree");
		// build ( a b (c d) e )
		Tree root = new Tree("a");
		root.addChild(new Tree("b"));
		Tree subtree = new Tree("c");
		subtree.addChild(new Tree("d"));
		root.addChild(subtree);
		root.addChild(new Tree("e"));
		tree.setAttribute("it", root);
		String expecting = "( a b ( c d ) e )";
		assertEqual(tree.toString(), expecting);
	}

    public void testNestedAnonymousTemplates() throws Exception {
        StringTemplateGroup group =
                new StringTemplateGroup("dummy", ".");
        String newline = System.getProperty("line.separator");
        StringTemplate t =
                new StringTemplate(
                        group,
                        "$A:{" + newline +
                          "<i>$it:{" + newline +
                            "<b>$it$</b>" + newline +
                          "}$</i>" + newline +
                        "}$"
                );
        t.setAttribute("A", "parrt");
        String expecting = newline +
            "<i>" + newline +
            "<b>parrt</b>" + newline +
            "</i>" + newline;
        assertEqual(t.toString(), expecting);
    }

    public void testAnonymousTemplateAccessToEnclosingAttributes() throws Exception {
        StringTemplateGroup group =
                new StringTemplateGroup("dummy", ".");
        String newline = System.getProperty("line.separator");
        StringTemplate t =
                new StringTemplate(
                        group,
                        "$A:{" + newline +
                          "<i>$it:{" + newline +
                            "<b>$it$, $B$</b>" + newline +
                          "}$</i>" + newline +
                        "}$"
                );
        t.setAttribute("A", "parrt");
        t.setAttribute("B", "tombu");
        String expecting = newline +
            "<i>" + newline +
            "<b>parrt, tombu</b>" + newline +
            "</i>" + newline;
        assertEqual(t.toString(), expecting);
    }

    public void testNestedAnonymousTemplatesAgain() throws Exception {

        StringTemplateGroup group =
                new StringTemplateGroup("dummy", ".");
        String newline = System.getProperty("line.separator");
        StringTemplate t =
                new StringTemplate(
                        group,
                        "<table>"+newline +
                        "$names:{<tr>$it:{<td>$it:{<b>$it$</b>}$</td>}$</tr>}$"+newline +
                        "</table>"+newline
                );
        t.setAttribute("names", "parrt");
        t.setAttribute("names", "tombu");
        String expecting =
                "<table>" + newline +
                "<tr><td><b>parrt</b></td></tr><tr><td><b>tombu</b></td></tr>" + newline +
                "</table>" + newline;
        assertEqual(t.toString(), expecting);
    }

	public void testEscapes() throws Exception {
		StringTemplateGroup group =
				new StringTemplateGroup("dummy", ".");
		String newline = System.getProperty("line.separator");
		group.defineTemplate("foo", "$x$ && $it$");
		StringTemplate t =
				new StringTemplate(
						group,
						"$A:foo(x=\"dog\\\"\\\"\")$" // $A:foo("dog\"\"")$
				);
		StringTemplate u =
				new StringTemplate(
						group,
						"$A:foo(x=\"dog\\\"g\")$" // $A:foo(x="dog\"g")$
				);
		StringTemplate v =
				new StringTemplate(
						group,
						// $A:{$attr:foo(x="\{dog\}\"")$ is cool}$
						"$A:{$it:foo(x=\"\\{dog\\}\\\"\")$ is cool}$"
				);
		t.setAttribute("A", "ick");
		u.setAttribute("A", "ick");
		v.setAttribute("A", "ick");
		//System.out.println("t is '"+t.toString()+"'");
		//System.out.println("u is '"+u.toString()+"'");
		//System.out.println("v is '"+v.toString()+"'");
		String expecting = "dog\"\" && ick";
		assertEqual(t.toString(), expecting);
		expecting = "dog\"g && ick";
		assertEqual(u.toString(), expecting);
		expecting = "{dog}\" && ick is cool";
		assertEqual(v.toString(), expecting);
	}

    public void testEscapesOutsideExpressions() throws Exception {
        StringTemplate b = new StringTemplate("It\\'s ok...\\$; $a:{\\'hi\\', $it$}$");
        b.setAttribute("a", "Ter");
        String expecting ="It\\'s ok...$; \\'hi\\', Ter";
        String result = b.toString();
        assertEqual(result, expecting);
    }

    public void testElseClause() throws Exception {
        StringTemplate e = new StringTemplate(
                "$if(title)$"+newline +
                "foo"+newline +
                "$else$"+newline +
                "bar"+newline +
                "$endif$"
            );
        e.setAttribute("title", "sample");
        String expecting = "foo";
        assertEqual(e.toString(), expecting);

        e = e.getInstanceOf();
        expecting = "bar";
        assertEqual(e.toString(), expecting);
    }

	public void testNestedIF() throws Exception {
		StringTemplate e = new StringTemplate(
				"$if(title)$"+newline +
				"foo"+newline +
				"$else$"+newline +
				"$if(header)$"+newline +
				"bar"+newline +
				"$else$"+newline +
				"blort"+newline +
				"$endif$"+newline +
				"$endif$"
			);
		e.setAttribute("title", "sample");
		String expecting = "foo";
		assertEqual(e.toString(), expecting);

		e = e.getInstanceOf();
		e.setAttribute("header", "more");
		expecting = "bar";
		assertEqual(e.toString(), expecting);

		e = e.getInstanceOf();
		expecting = "blort";
		assertEqual(e.toString(), expecting);
	}

	public void testEmbeddedMultiLineIF() throws Exception {
		StringTemplateGroup group =
				new StringTemplateGroup("test");
		StringTemplate main = new StringTemplate(group, "$sub$");
		StringTemplate sub = new StringTemplate(group,
				"begin" + newline +
				"$if(foo)$" + newline +
				"$foo$" + newline +
				"$else$" +newline +
				"blort" + newline +
				"$endif$" + newline
			);
		sub.setAttribute("foo", "stuff");
		main.setAttribute("sub", sub);
		String expecting =
			"begin"+newline+
			"stuff";
		assertEqual(main.toString(), expecting);

		main = new StringTemplate(group, "$sub$");
		sub = sub.getInstanceOf();
		main.setAttribute("sub", sub);
		expecting =
			"begin"+newline+
			"blort";
		assertEqual(main.toString(), expecting);
	}

    public void testSimpleIndentOfAttributeList()
            throws Exception
    {
        String templates =
                "group test;" +newline+
                "list(names) ::= <<" +
                "  $names; separator=\"\n\"$"+newline+
                ">>"+newline;
        StringTemplateErrorListener errors = new ErrorBuffer();
        StringTemplateGroup group =
                new StringTemplateGroup(new StringReader(templates),
										DefaultTemplateLexer.class,
										errors);
        StringTemplate t = group.getInstanceOf("list");
        t.setAttribute("names", "Terence");
        t.setAttribute("names", "Jim");
        t.setAttribute("names", "Sriram");
        String expecting =
                "  Terence"+newline+
                "  Jim"+newline+
                "  Sriram";
        assertEqual(t.toString(), expecting);
    }

    public void testIndentOfMultilineAttributes()
            throws Exception
    {
        String templates =
                "group test;" +newline+
                "list(names) ::= <<" +
                "  $names; separator=\"\n\"$"+newline+
                ">>"+newline;
        StringTemplateErrorListener errors = new ErrorBuffer();
        StringTemplateGroup group =
                new StringTemplateGroup(new StringReader(templates),
										DefaultTemplateLexer.class,
										errors);
        StringTemplate t = group.getInstanceOf("list");
        t.setAttribute("names", "Terence\nis\na\nmaniac");
        t.setAttribute("names", "Jim");
        t.setAttribute("names", "Sriram\nis\ncool");
        String expecting =
                "  Terence"+newline+
                "  is"+newline+
                "  a"+newline+
                "  maniac"+newline+
                "  Jim"+newline+
                "  Sriram"+newline+
                "  is"+newline+
                "  cool";
        assertEqual(t.toString(), expecting);
    }

	public void testIndentOfMultipleBlankLines()
			throws Exception
	{
		String templates =
				"group test;" +newline+
				"list(names) ::= <<" +
				"  $names$"+newline+
				">>"+newline;
		StringTemplateErrorListener errors = new ErrorBuffer();
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates),
										DefaultTemplateLexer.class,
										errors);
		StringTemplate t = group.getInstanceOf("list");
		t.setAttribute("names", "Terence\n\nis a maniac");
		String expecting =
				"  Terence"+newline+
				""+newline+ // no indent on blank line
				"  is a maniac";
		assertEqual(t.toString(), expecting);
	}

    public void testIndentBetweenLeftJustifiedLiterals()
            throws Exception
    {
        String templates =
                "group test;" +newline+
                "list(names) ::= <<" +
                "Before:"+newline +
                "  $names; separator=\"\\n\"$"+newline+
                "after" +newline+
                ">>"+newline;
        StringTemplateErrorListener errors = new ErrorBuffer();
        StringTemplateGroup group =
                new StringTemplateGroup(new StringReader(templates),
										DefaultTemplateLexer.class,
										errors);
        StringTemplate t = group.getInstanceOf("list");
        t.setAttribute("names", "Terence");
        t.setAttribute("names", "Jim");
        t.setAttribute("names", "Sriram");
        String expecting =
                "Before:" +newline+
                "  Terence"+newline+
                "  Jim"+newline+
                "  Sriram"+newline+
                "after";
        assertEqual(t.toString(), expecting);
    }

    public void testNestedIndent()
            throws Exception
    {
        String templates =
                "group test;" +newline+
                "method(name,stats) ::= <<" +
                "void $name$() {"+newline +
                "\t$stats; separator=\"\\n\"$"+newline+
                "}" +newline+
                ">>"+newline+
                "ifstat(expr,stats) ::= <<"+newline +
                "if ($expr$) {"+newline +
                "  $stats; separator=\"\\n\"$"+newline +
                "}" +
                ">>"+newline +
                "assign(lhs,expr) ::= <<$lhs$=$expr$;>>"+newline
                ;
        StringTemplateErrorListener errors = new ErrorBuffer();
        StringTemplateGroup group =
                new StringTemplateGroup(new StringReader(templates),
										DefaultTemplateLexer.class,
										errors);
        StringTemplate t = group.getInstanceOf("method");
        t.setAttribute("name", "foo");
        StringTemplate s1 = group.getInstanceOf("assign");
        s1.setAttribute("lhs", "x");
        s1.setAttribute("expr", "0");
        StringTemplate s2 = group.getInstanceOf("ifstat");
        s2.setAttribute("expr", "x>0");
        StringTemplate s2a = group.getInstanceOf("assign");
        s2a.setAttribute("lhs", "y");
        s2a.setAttribute("expr", "x+y");
        StringTemplate s2b = group.getInstanceOf("assign");
        s2b.setAttribute("lhs", "z");
        s2b.setAttribute("expr", "4");
        s2.setAttribute("stats", s2a);
        s2.setAttribute("stats", s2b);
        t.setAttribute("stats", s1);
        t.setAttribute("stats", s2);
        String expecting =
                "void foo() {"+newline+
                "\tx=0;"+newline+
                "\tif (x>0) {"+newline+
                "\t  y=x+y;"+newline+
                "\t  z=4;"+newline+
                "\t}"+newline+
                "}";
        assertEqual(t.toString(), expecting);
    }

	public void testAlternativeWriter() throws Exception {
		final StringBuffer buf = new StringBuffer();
		StringTemplateWriter w = new StringTemplateWriter() {
			public void pushIndentation(String indent) {
			}
			public String popIndentation() {
				return null;
			}
			public int write(String str) throws IOException {
				buf.append(str); // just pass thru
				return str.length();
			}
		};
		StringTemplateGroup group =
				new StringTemplateGroup("test");
		group.defineTemplate("bold", "<b>$x$</b>");
		StringTemplate name = new StringTemplate(group, "$name:bold(x=name)$");
		name.setAttribute("name", "Terence");
		name.write(w);
		assertEqual(buf.toString(), "<b>Terence</b>");
	}

	public void testApplyAnonymousTemplateToMapAndSet() throws Exception {
		StringTemplate st =
				new StringTemplate("$items:{<li>$it$</li>}$");
		Map m = new HashMap();
		m.put("a", "1");
		m.put("b", "2");
		m.put("c", "3");
		st.setAttribute("items", m);
		String expecting = "<li>1</li><li>3</li><li>2</li>";
		assertEqual(st.toString(), expecting);

		st = st.getInstanceOf();
		Set s = new HashSet();
		s.add("1");
		s.add("2");
		s.add("3");
		st.setAttribute("items", s);
		expecting = "<li>3</li><li>2</li><li>1</li>";
		assertEqual(st.toString(), expecting);
	}

	public void testDumpMapAndSet() throws Exception {
		StringTemplate st =
				new StringTemplate("$items; separator=\",\"$");
		Map m = new HashMap();
		m.put("a", "1");
		m.put("b", "2");
		m.put("c", "3");
		st.setAttribute("items", m);
		String expecting = "1,3,2";
		assertEqual(st.toString(), expecting);

		st = st.getInstanceOf();
		Set s = new HashSet();
		s.add("1");
		s.add("2");
		s.add("3");
		st.setAttribute("items", s);
		expecting = "3,2,1";
		assertEqual(st.toString(), expecting);
	}

	public class Connector3 {
		public int[] getValues() { return new int[] {1,2,3}; }
		public Map getStuff() {
			Map m = new HashMap(); m.put("a","1"); m.put("b","2"); return m;
		}
	}

	public void testApplyAnonymousTemplateToArrayAndMapProperty() throws Exception {
		StringTemplate st =
				new StringTemplate("$x.values:{<li>$it$</li>}$");
		st.setAttribute("x", new Connector3());
		String expecting = "<li>1</li><li>2</li><li>3</li>";
		assertEqual(st.toString(), expecting);

		st = new StringTemplate("$x.stuff:{<li>$it$</li>}$");
		st.setAttribute("x", new Connector3());
		expecting = "<li>1</li><li>2</li>";
		assertEqual(st.toString(), expecting);
	}

    public void testSuperTemplateRef()
            throws Exception
    {
        // you can refer to a template defined in a super group via super.t()
        StringTemplateGroup group = new StringTemplateGroup("super");
        StringTemplateGroup subGroup = new StringTemplateGroup("sub");
        subGroup.setSuperGroup(group);
        group.defineTemplate("page", "$font()$:text");
        group.defineTemplate("font", "Helvetica");
        subGroup.defineTemplate("font", "$super.font()$ and Times");
        StringTemplate st = subGroup.getInstanceOf("page");
        String expecting =
                "Helvetica and Times:text";
        assertEqual(st.toString(), expecting);
    }

    public void testApplySuperTemplateRef()
            throws Exception
    {
        StringTemplateGroup group = new StringTemplateGroup("super");
        StringTemplateGroup subGroup = new StringTemplateGroup("sub");
        subGroup.setSuperGroup(group);
        group.defineTemplate("bold", "<b>$it$</b>");
        subGroup.defineTemplate("bold", "<strong>$it$</strong>");
        subGroup.defineTemplate("page", "$name:super.bold()$");
        StringTemplate st = subGroup.getInstanceOf("page");
        st.setAttribute("name", "Ter");
        String expecting =
                "<b>Ter</b>";
        assertEqual(st.toString(), expecting);
    }

    public void testLazyEvalOfSuperInApplySuperTemplateRef()
            throws Exception
    {
        StringTemplateGroup group = new StringTemplateGroup("base");
        StringTemplateGroup subGroup = new StringTemplateGroup("sub");
        subGroup.setSuperGroup(group);
        group.defineTemplate("bold", "<b>$it$</b>");
        subGroup.defineTemplate("bold", "<strong>$it$</strong>");
        // this is the same as testApplySuperTemplateRef() test
        // 'cept notice that here the supergroup defines page
        // As long as you create the instance via the subgroup,
        // "super." will evaluate lazily (i.e., not statically
        // during template compilation) to the templates
        // getGroup().superGroup value.  If I create instance
        // of page in group not subGroup, however, I will get
        // an error as superGroup is null for group "group".
        group.defineTemplate("page", "$name:super.bold()$");
        StringTemplate st = subGroup.getInstanceOf("page");
        st.setAttribute("name", "Ter");
		String error = null;
		try {
			st.toString();
		}
		catch (IllegalArgumentException iae) {
			error = iae.getMessage();
		}
		String expectingError = "base has no super group; invalid template: super.bold";
		assertEqual(error, expectingError);
    }

    public void testTemplatePolymorphism()
            throws Exception
    {
        StringTemplateGroup group = new StringTemplateGroup("super");
        StringTemplateGroup subGroup = new StringTemplateGroup("sub");
        subGroup.setSuperGroup(group);
        // bold is defined in both super and sub
        // if you create an instance of page via the subgroup,
        // then bold() should evaluate to the subgroup not the super
        // even though page is defined in the super.  Just like polymorphism.
        group.defineTemplate("bold", "<b>$it$</b>");
        group.defineTemplate("page", "$name:bold()$");
        subGroup.defineTemplate("bold", "<strong>$it$</strong>");
        StringTemplate st = subGroup.getInstanceOf("page");
        st.setAttribute("name", "Ter");
        String expecting =
                "<strong>Ter</strong>";
        assertEqual(st.toString(), expecting);
    }

    public void testListOfEmbeddedTemplateSeesEnclosingAttributes() throws Exception {
        String templates =
                "group test;" +newline+
                "output(cond,items) ::= <<page: $items$>>" +newline+
                "mybody() ::= <<$font()$stuff>>" +newline+
                "font() ::= <<$if(cond)$this$else$that$endif$>>"
                ;
        StringTemplateErrorListener errors = new ErrorBuffer();
        StringTemplateGroup group =
                new StringTemplateGroup(new StringReader(templates),
										DefaultTemplateLexer.class,
										errors);
        StringTemplate outputST = group.getInstanceOf("output");
        StringTemplate bodyST1 = group.getInstanceOf("mybody");
        StringTemplate bodyST2 = group.getInstanceOf("mybody");
        StringTemplate bodyST3 = group.getInstanceOf("mybody");
        outputST.setAttribute("items", bodyST1);
        outputST.setAttribute("items", bodyST2);
        outputST.setAttribute("items", bodyST3);
        String expecting = "page: thatstuffthatstuffthatstuff";
        assertEqual(outputST.toString(), expecting);
    }

    public void testInheritArgumentFromRecursiveTemplateApplication() throws Exception {
        // do not inherit attributes through formal args
        String templates =
                "group test;" +newline+
                "block(stats) ::= \"<stats>\"" +
                "ifstat(stats) ::= \"IF true then <stats>\""+newline
                ;
        StringTemplateGroup group =
                new StringTemplateGroup(new StringReader(templates));
        StringTemplate b = group.getInstanceOf("block");
        b.setAttribute("stats", group.getInstanceOf("ifstat"));
        b.setAttribute("stats", group.getInstanceOf("ifstat"));
        String expecting = "IF true then IF true then ";
        String result = b.toString();
        //System.err.println("result='"+result+"'");
        assertEqual(result, expecting);
    }


    public void testDeliberateRecursiveTemplateApplication() throws Exception {
        // This test will cause infinite loop.  block contains a stat which
        // contains the same block.  Must be in lintMode to detect
        String templates =
                "group test;" +newline+
                "block(stats) ::= \"<stats>\"" +
                "ifstat(stats) ::= \"IF true then <stats>\""+newline
                ;
        StringTemplate.setLintMode(true);
        StringTemplate.resetTemplateCounter();
        StringTemplateGroup group =
                new StringTemplateGroup(new StringReader(templates));
        StringTemplate b = group.getInstanceOf("block");
        StringTemplate ifstat = group.getInstanceOf("ifstat");
        b.setAttribute("stats", ifstat); // block has if stat
        ifstat.setAttribute("stats", b); // but make "if" contain block
        String expectingError =
                "infinite recursion to <ifstat([stats])@4> referenced in <block([stats])@3>; stack trace:"+newline +
                "<ifstat([stats])@4>, attributes=[stats=<block()@3>]>"+newline +
                "<block([stats])@3>, attributes=[stats=<ifstat()@4>], references=[stats]>"+newline +
                "<ifstat([stats])@4> (start of recursive cycle)"+newline +
                "...";
        // note that attributes attribute doesn't show up in ifstat() because
        // recursion detection traps the problem before it writes out the
        // infinitely-recursive template; I set the attributes attribute right
        // before I render.
        String errors = "";
        try {
            String result = b.toString();
        }
        catch (IllegalStateException ise) {
            errors = ise.getMessage();
        }
        //System.err.println("errors="+errors+"'");
        //System.err.println("expecting="+expectingError+"'");
        StringTemplate.setLintMode(false);
        assertEqual(errors, expectingError);
    }


    public void testImmediateTemplateAsAttributeLoop() throws Exception {
        // even though block has a stats value that refers to itself,
        // there is no recursion because each instance of block hides
        // the stats value from above since it's a formal arg.
        String templates =
                "group test;" +newline+
                "block(stats) ::= \"{<stats>}\""
                ;
        StringTemplateGroup group =
                new StringTemplateGroup(new StringReader(templates));
        StringTemplate b = group.getInstanceOf("block");
        b.setAttribute("stats", group.getInstanceOf("block"));
        String expecting ="{{}}";
        String result = b.toString();
        //System.err.println(result);
        assertEqual(result, expecting);
    }


    public void testTemplateAlias() throws Exception {
        String templates =
                "group test;" +newline+
                "page(name) ::= \"name is <name>\"" +
                "other ::= page"+newline
                ;
        StringTemplateGroup group =
                new StringTemplateGroup(new StringReader(templates));
        StringTemplate b = group.getInstanceOf("other");  // alias for page
        b.setAttribute("name", "Ter");
        String expecting ="name is Ter";
        String result = b.toString();
        assertEqual(result, expecting);
    }

    public void testTemplateGetPropertyGetsAttribute() throws Exception {
        // This test will cause infinite loop if missing attribute no
        // properly caught in getAttribute
        String templates =
                "group test;"+newline+
                "Cfile(funcs) ::= <<"+newline +
                "#include \\<stdio.h>"+newline+
                "<funcs:{public void <it.name>(<it.args>);}; separator=\"\\n\">"+newline+
                "<funcs; separator=\"\\n\">"+newline+
                ">>"+newline +
                "func(name,args,body) ::= <<"+newline+
                "public void <name>(<args>) {<body>}"+newline +
                ">>"+newline
                ;
        StringTemplateGroup group =
                new StringTemplateGroup(new StringReader(templates));
        StringTemplate b = group.getInstanceOf("Cfile");
        StringTemplate f1 = group.getInstanceOf("func");
        StringTemplate f2 = group.getInstanceOf("func");
        f1.setAttribute("name", "f");
        f1.setAttribute("args", "");
        f1.setAttribute("body", "i=1;");
        f2.setAttribute("name", "g");
        f2.setAttribute("args", "int arg");
        f2.setAttribute("body", "y=1;");
        b.setAttribute("funcs",f1);
        b.setAttribute("funcs",f2);
        String expecting = "#include <stdio.h>" +newline+
                "public void f();"+newline+
				"public void g(int arg);" +newline+
                "public void f() {i=1;}"+newline+
                "public void g(int arg) {y=1;}";
        assertEqual(b.toString(), expecting);
    }

    public static class Decl {
        String name;
        String type;
        public Decl(String name, String type) {this.name=name; this.type=type;}
        public String getName() {return name;}
        public String getType() {return type;}
    }

	public void testComplicatedIndirectTemplateApplication() throws Exception {
		String templates =
				"group Java;"+newline +
				""+newline +
				"file(variables) ::= <<" +
				"<variables:{ v | <v.decl:(v.format)()>}; separator=\"\\n\">"+newline +
				">>"+newline+
				"intdecl(decl) ::= \"int <decl.name> = 0;\""+newline +
				"intarray(decl) ::= \"int[] <decl.name> = null;\""+newline
				;
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates));
		StringTemplate f = group.getInstanceOf("file");
		f.setAttribute("variables.{decl,format}", new Decl("i","int"), "intdecl");
		f.setAttribute("variables.{decl,format}", new Decl("a","int-array"), "intarray");
		//System.out.println("f='"+f+"'");
		String expecting = "int i = 0;" +newline+
				"int[] a = null;";
		assertEqual(f.toString(), expecting);
	}

	public void testIndirectTemplateApplication() throws Exception {
		String templates =
				"group dork;"+newline +
				""+newline +
				"test(name) ::= <<" +
				"<(name)()>"+newline +
				">>"+newline+
				"first() ::= \"the first\""+newline +
				"second() ::= \"the second\""+newline
				;
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates));
		StringTemplate f = group.getInstanceOf("test");
		f.setAttribute("name", "first");
		String expecting = "the first";
		assertEqual(f.toString(), expecting);
	}

	public void testIndirectTemplateWithArgsApplication() throws Exception {
		String templates =
				"group dork;"+newline +
				""+newline +
				"test(name) ::= <<" +
				"<(name)(a=\"foo\")>"+newline +
				">>"+newline+
				"first(a) ::= \"the first: <a>\""+newline +
				"second(a) ::= \"the second <a>\""+newline
				;
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates));
		StringTemplate f = group.getInstanceOf("test");
		f.setAttribute("name", "first");
		String expecting = "the first: foo";
		assertEqual(f.toString(), expecting);
	}

	public void testNullIndirectTemplateApplication() throws Exception {
		String templates =
				"group dork;"+newline +
				""+newline +
				"test(names) ::= <<" +
				"<names:(ind)()>"+newline +
				">>"+newline+
				"ind() ::= \"[<it>]\""+newline;
				;
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates));
		StringTemplate f = group.getInstanceOf("test");
		f.setAttribute("names", "me");
		f.setAttribute("names", "you");
		String expecting = "";
		assertEqual(f.toString(), expecting);
	}

	public void testNullIndirectTemplate() throws Exception {
		String templates =
				"group dork;"+newline +
				""+newline +
				"test(name) ::= <<" +
				"<(name)()>"+newline +
				">>"+newline+
				"first() ::= \"the first\""+newline +
				"second() ::= \"the second\""+newline
				;
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates));
		StringTemplate f = group.getInstanceOf("test");
		//f.setAttribute("name", "first");
		String expecting = "";
		assertEqual(f.toString(), expecting);
	}

	public void testHashMapPropertyFetch() throws Exception {
		StringTemplate a = new StringTemplate("$stuff.prop$");
		HashMap map = new HashMap();
		a.setAttribute("stuff", map);
		map.put("prop", "Terence");
		String results = a.toString();
		//System.out.println(results);
		String expecting = "Terence";
		assertEqual(results, expecting);
	}

	public void testHashMapPropertyFetchEmbeddedStringTemplate() throws Exception {
		StringTemplate a = new StringTemplate("$stuff.prop$");
		HashMap map = new HashMap();
		a.setAttribute("stuff", map);
		a.setAttribute("title", "ST rocks");
		map.put("prop", new StringTemplate("embedded refers to $title$"));
		String results = a.toString();
		//System.out.println(results);
		String expecting = "embedded refers to ST rocks";
		assertEqual(results, expecting);
	}

	public void testEmbeddedComments() throws Exception {
		StringTemplate st = new StringTemplate(
				"Foo $! ignore !$bar" +newline
				);
		String expecting ="Foo bar"+newline;
		String result = st.toString();
		assertEqual(result, expecting);

		st = new StringTemplate(
				"Foo $! ignore" +newline+
				" and a line break!$" +newline+
				"bar" +newline
				);
		expecting ="Foo "+newline+"bar"+newline;
		result = st.toString();
		assertEqual(result, expecting);

		st = new StringTemplate(
				"$! start of line $ and $! ick" +newline+
				"!$boo"+newline
				);
		expecting ="boo"+newline;
		result = st.toString();
		assertEqual(result, expecting);

		st = new StringTemplate(
			"$! start of line !$" +newline+
			"$! another to ignore !$" +newline+
			"$! ick" +newline+
			"!$boo"+newline
		);
		expecting ="boo"+newline;
		result = st.toString();
		assertEqual(result, expecting);

		st = new StringTemplate(
			"$! back !$$! to back !$" +newline+ // can't detect; leaves \n
			"$! ick" +newline+
			"!$boo"+newline
		);
		expecting =newline+"boo"+newline;
		result = st.toString();
		assertEqual(result, expecting);
	}

	public void testEmbeddedCommentsAngleBracketed() throws Exception {
		StringTemplate st = new StringTemplate(
				"Foo <! ignore !>bar" +newline,
				AngleBracketTemplateLexer.class
				);
		String expecting ="Foo bar"+newline;
		String result = st.toString();
		assertEqual(result, expecting);

		st = new StringTemplate(
				"Foo <! ignore" +newline+
				" and a line break!>" +newline+
				"bar" +newline,
				AngleBracketTemplateLexer.class
				);
		expecting ="Foo "+newline+"bar"+newline;
		result = st.toString();
		assertEqual(result, expecting);

		st = new StringTemplate(
				"<! start of line $ and <! ick" +newline+
				"!>boo"+newline,
				AngleBracketTemplateLexer.class
				);
		expecting ="boo"+newline;
		result = st.toString();
		assertEqual(result, expecting);

		st = new StringTemplate(
			"<! start of line !>" +
			"<! another to ignore !>" +
			"<! ick" +newline+
			"!>boo"+newline,
			AngleBracketTemplateLexer.class
		);
		expecting ="boo"+newline;
		result = st.toString();
		//System.out.println(result);
		assertEqual(result, expecting);

		st = new StringTemplate(
			"<! back !><! to back !>" +newline+ // can't detect; leaves \n
			"<! ick" +newline+
			"!>boo"+newline,
			AngleBracketTemplateLexer.class
		);
		expecting =newline+"boo"+newline;
		result = st.toString();
		assertEqual(result, expecting);
	}

	public void testCharLiterals() throws Exception {
		StringTemplate st = new StringTemplate(
				"Foo <\\n><\\t> bar" +newline,
				AngleBracketTemplateLexer.class
				);
		String expecting ="Foo "+newline+"\t bar"+newline;
		String result = st.toString();
		assertEqual(result, expecting);

		st = new StringTemplate(
				"Foo $\\n$$\\t$ bar" +newline);
		expecting ="Foo "+newline+"\t bar"+newline;
		result = st.toString();
		assertEqual(result, expecting);

		st = new StringTemplate(
				"Foo$\\ $bar$\\n$");
		expecting ="Foo bar"+newline;
		result = st.toString();
		assertEqual(result, expecting);
	}

	public void testEmptyIteratedValueGetsSeparator() throws Exception {
		StringTemplateGroup group =
				new StringTemplateGroup("test");
		StringTemplateErrorListener errors = new ErrorBuffer();
		group.setErrorListener(errors);
		StringTemplate t = new StringTemplate(group, "$names; separator=\",\"$");
		t.setAttribute("names", "Terence");
		t.setAttribute("names", "");
		t.setAttribute("names", "");
		t.setAttribute("names", "Tom");
		t.setAttribute("names", "Frank");
		t.setAttribute("names", "");
		// empty values get separator still
		String expecting="Terence,,,Tom,Frank,";
		String result = t.toString();
		assertEqual(result, expecting);
	}

	public void testEmptyIteratedConditionalValueGetsNoSeparator() throws Exception {
		StringTemplateGroup group =
				new StringTemplateGroup("test");
		StringTemplateErrorListener errors = new ErrorBuffer();
		group.setErrorListener(errors);
		StringTemplate t = new StringTemplate(group,
			"$users:{$if(it.ok)$$it.name$$endif$}; separator=\",\"$");
		t.setAttribute("users.{name,ok}", "Terence", new Boolean(true));
		t.setAttribute("users.{name,ok}", "Tom", new Boolean(false));
		t.setAttribute("users.{name,ok}", "Frank", new Boolean(true));
		t.setAttribute("users.{name,ok}", "Johnny", new Boolean(false));
		// empty conditional values get no separator
		String expecting="Terence,Frank,"; // haven't solved the last empty value problem yet
		String result = t.toString();
		assertEqual(result, expecting);
	}

	public void testEmptyIteratedConditionalWithElseValueGetsSeparator() throws Exception {
		StringTemplateGroup group =
				new StringTemplateGroup("test");
		StringTemplateErrorListener errors = new ErrorBuffer();
		group.setErrorListener(errors);
		StringTemplate t = new StringTemplate(group,
			"$users:{$if(it.ok)$$it.name$$else$$endif$}; separator=\",\"$");
		t.setAttribute("users.{name,ok}", "Terence", new Boolean(true));
		t.setAttribute("users.{name,ok}", "Tom", new Boolean(false));
		t.setAttribute("users.{name,ok}", "Frank", new Boolean(true));
		t.setAttribute("users.{name,ok}", "Johnny", new Boolean(false));
		// empty conditional values get no separator
		String expecting="Terence,,Frank,"; // haven't solved the last empty value problem yet
		String result = t.toString();
		assertEqual(result, expecting);
	}

	public void testWhiteSpaceAtEndOfTemplate() throws Exception {
		StringTemplateGroup group = new StringTemplateGroup("group");
		StringTemplate pageST = group.getInstanceOf("org/antlr/stringtemplate/test/page");
		StringTemplate listST = group.getInstanceOf("org/antlr/stringtemplate/test/users.list");
		// users.list references row.st which has a single blank line at the end.
		// I.e., there are 2 \n in a row at the end
		// ST should eat all whitespace at end
		listST.setAttribute("users", new Connector());
		listST.setAttribute("users", new Connector2());
		pageST.setAttribute("title", "some title");
		pageST.setAttribute("body", listST);
		String expecting ="some title" +newline+
			"Terence parrt@jguru.comTom tombu@jguru.com";
		String result = pageST.toString();
		//System.out.println("'"+result+"'");
		assertEqual(result, expecting);
	}

	static class Duh {
		public List users = new ArrayList();
	}

	public void testSizeZeroButNonNullListGetsNoOutput() throws Exception {
		StringTemplateGroup group =
				new StringTemplateGroup("test");
		StringTemplateErrorListener errors = new ErrorBuffer();
		group.setErrorListener(errors);
		StringTemplate t = new StringTemplate(group,
			"begin\n" +
			"$duh.users:{name: $it$}; separator=\", \"$\n" +
			"end\n");
		t.setAttribute("duh", new Duh());
		String expecting="begin\nend\n";
		String result = t.toString();
		assertEqual(result, expecting);
	}

	public void testNullListGetsNoOutput() throws Exception {
		StringTemplateGroup group =
				new StringTemplateGroup("test");
		StringTemplateErrorListener errors = new ErrorBuffer();
		group.setErrorListener(errors);
		StringTemplate t = new StringTemplate(group,
			"begin\n" +
			"$users:{name: $it$}; separator=\", \"$\n" +
			"end\n");
		//t.setAttribute("users", new Duh());
		String expecting="begin\nend\n";
		String result = t.toString();
		assertEqual(result, expecting);
	}

	public void testEmptyListGetsNoOutput() throws Exception {
		StringTemplateGroup group =
				new StringTemplateGroup("test");
		StringTemplateErrorListener errors = new ErrorBuffer();
		group.setErrorListener(errors);
		StringTemplate t = new StringTemplate(group,
			"begin\n" +
			"$users:{name: $it$}; separator=\", \"$\n" +
			"end\n");
		t.setAttribute("users", new ArrayList());
		String expecting="begin\nend\n";
		String result = t.toString();
		assertEqual(result, expecting);
	}

	public void testEmptyListNoIteratorGetsNoOutput() throws Exception {
		StringTemplateGroup group =
				new StringTemplateGroup("test");
		StringTemplateErrorListener errors = new ErrorBuffer();
		group.setErrorListener(errors);
		StringTemplate t = new StringTemplate(group,
			"begin\n" +
			"$users; separator=\", \"$\n" +
			"end\n");
		t.setAttribute("users", new ArrayList());
		String expecting="begin\nend\n";
		String result = t.toString();
		assertEqual(result, expecting);
	}

	public void testEmptyExprAsFirstLineGetsNoOutput() throws Exception {
		StringTemplateGroup group =
				new StringTemplateGroup("test");
		StringTemplateErrorListener errors = new ErrorBuffer();
		group.setErrorListener(errors);
		group.defineTemplate("bold", "<b>$it$</b>");
		StringTemplate t = new StringTemplate(group,
			"$users$\n" +
			"end\n");
		String expecting="end\n";
		String result = t.toString();
		assertEqual(result, expecting);
	}

	public void testSizeZeroOnLineByItselfGetsNoOutput() throws Exception {
		StringTemplateGroup group =
				new StringTemplateGroup("test");
		StringTemplateErrorListener errors = new ErrorBuffer();
		group.setErrorListener(errors);
		StringTemplate t = new StringTemplate(group,
			"begin\n"+
			"$name$\n"+
			"$users:{name: $it$}$\n"+
			"$users:{name: $it$}; separator=\", \"$\n"+
			"end\n");
		String expecting="begin\nend\n";
		String result = t.toString();
		assertEqual(result, expecting);
	}

	public void testSizeZeroOnLineWithIndentGetsNoOutput() throws Exception {
		StringTemplateGroup group =
				new StringTemplateGroup("test");
		StringTemplateErrorListener errors = new ErrorBuffer();
		group.setErrorListener(errors);
		StringTemplate t = new StringTemplate(group,
			"begin\n"+
			"  $name$\n"+
			"	$users:{name: $it$}$\n"+
			"	$users:{name: $it$$\\n$}$\n"+
			"end\n");
		String expecting="begin\nend\n";
		String result = t.toString();
		assertEqual(result, expecting);
	}

	public void testSimpleAutoIndent() throws Exception {
		StringTemplate a = new StringTemplate(
			"$title$: {\n" +
			"	$name; separator=\"\n\"$\n" +
			"}");
		a.setAttribute("title", "foo");
		a.setAttribute("name", "Terence");
		a.setAttribute("name", "Frank");
		String results = a.toString();
		//System.out.println(results);
		String expecting =
			"foo: {\n" +
			"	Terence\n" +
			"	Frank\n" +
			"}";
		assertEqual(results, expecting);
	}

	public void testComputedPropertyName() throws Exception {
		StringTemplateGroup group =
				new StringTemplateGroup("test");
		StringTemplateErrorListener errors = new ErrorBuffer();
		group.setErrorListener(errors);
		StringTemplate t = new StringTemplate(group,
			"variable property $propName$=$v.(propName)$");
		t.setAttribute("v", new Decl("i","int"));
		t.setAttribute("propName", "type");
		String expecting="variable property type=int";
		String result = t.toString();
		assertEqual(errors.toString(), "");
		assertEqual(result, expecting);
	}

	public void testNonNullButEmptyIteratorTestsFalse() throws Exception {
		StringTemplateGroup group =
				new StringTemplateGroup("test");
		StringTemplate t = new StringTemplate(group,
			"$if(users)$\n" +
			"Users: $users:{$it.name$ }$\n" +
			"$endif$");
		t.setAttribute("users", new LinkedList());
		String expecting="";
		String result = t.toString();
		assertEqual(result, expecting);
	}

	public void testDoNotInheritAttributesThroughFormalArgs() throws Exception {
		String templates =
				"group test;" +newline+
				"method(name) ::= \"<stat()>\"" +newline+
				"stat(name) ::= \"x=y; // <name>\""+newline
				;
		// name is not visible in stat because of the formal arg called name.
		// somehow, it must be set.
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates));
		StringTemplate b = group.getInstanceOf("method");
		b.setAttribute("name", "foo");
		String expecting = "x=y; // ";
		String result = b.toString();
		//System.err.println("result='"+result+"'");
		assertEqual(result, expecting);
	}

	public void testArgEvaluationContext() throws Exception {
		String templates =
				"group test;" +newline+
				"method(name) ::= \"<stat(name=name)>\"" +newline+
				"stat(name) ::= \"x=y; // <name>\""+newline
				;
		// attribute name is not visible in stat because of the formal
		// arg called name in template stat.  However, we can set it's value
		// with an explicit name=name.  This looks weird, but makes total
		// sense as the rhs is evaluated in the context of method and the lhs
		// is evaluated in the context of stat's arg list.
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates));
		StringTemplate b = group.getInstanceOf("method");
		b.setAttribute("name", "foo");
		String expecting = "x=y; // foo";
		String result = b.toString();
		//System.err.println("result='"+result+"'");
		assertEqual(result, expecting);
	}

	public void testPassThroughAttributes() throws Exception {
		String templates =
				"group test;" +newline+
				"method(name) ::= \"<stat(...)>\"" +newline+
				"stat(name) ::= \"x=y; // <name>\""+newline
				;
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates));
		StringTemplate b = group.getInstanceOf("method");
		b.setAttribute("name", "foo");
		String expecting = "x=y; // foo";
		String result = b.toString();
		//System.err.println("result='"+result+"'");
		assertEqual(result, expecting);
	}

	public void testPassThroughAttributes2() throws Exception {
		String templates =
				"group test;" +newline+
				"method(name) ::= <<"+newline+
				"<stat(value=\"34\",...)>" +newline+
				">>"+newline+
				"stat(name,value) ::= \"x=<value>; // <name>\""+newline
				;
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates));
		StringTemplate b = group.getInstanceOf("method");
		b.setAttribute("name", "foo");
		String expecting = "x=34; // foo";
		String result = b.toString();
		//System.err.println("result='"+result+"'");
		assertEqual(result, expecting);
	}

	public void testDefaultArgument() throws Exception {
		String templates =
				"group test;" +newline+
				"method(name) ::= <<"+newline+
				"<stat(...)>" +newline+
				">>"+newline+
				"stat(name,value=\"99\") ::= \"x=<value>; // <name>\""+newline
				;
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates));
		StringTemplate b = group.getInstanceOf("method");
		b.setAttribute("name", "foo");
		String expecting = "x=99; // foo";
		String result = b.toString();
		//System.err.println("result='"+result+"'");
		assertEqual(result, expecting);
	}

	public void testDefaultArgument2() throws Exception {
		String templates =
				"group test;" +newline+
				"stat(name,value=\"99\") ::= \"x=<value>; // <name>\""+newline
				;
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates));
		StringTemplate b = group.getInstanceOf("stat");
		b.setAttribute("name", "foo");
		String expecting = "x=99; // foo";
		String result = b.toString();
		//System.err.println("result='"+result+"'");
		assertEqual(result, expecting);
	}

	public void testDefaultArgumentAsTemplate() throws Exception {
		String templates =
				"group test;" +newline+
				"method(name,size) ::= <<"+newline+
				"<stat(...)>" +newline+
				">>"+newline+
				"stat(name,value={<name>}) ::= \"x=<value>; // <name>\""+newline
				;
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates));
		StringTemplate b = group.getInstanceOf("method");
		b.setAttribute("name", "foo");
		b.setAttribute("size", "2");
		String expecting = "x=foo; // foo";
		String result = b.toString();
		//System.err.println("result='"+result+"'");
		assertEqual(result, expecting);
	}

	public void testDefaultArgumentAsTemplate2() throws Exception {
		String templates =
				"group test;" +newline+
				"method(name,size) ::= <<"+newline+
				"<stat(...)>" +newline+
				">>"+newline+
				"stat(name,value={ [<name>] }) ::= \"x=<value>; // <name>\""+newline
				;
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates));
		StringTemplate b = group.getInstanceOf("method");
		b.setAttribute("name", "foo");
		b.setAttribute("size", "2");
		String expecting = "x= [foo] ; // foo";
		String result = b.toString();
		//System.err.println("result='"+result+"'");
		assertEqual(result, expecting);
	}

	public void testDoNotUseDefaultArgument() throws Exception {
		String templates =
				"group test;" +newline+
				"method(name) ::= <<"+newline+
				"<stat(value=\"34\",...)>" +newline+
				">>"+newline+
				"stat(name,value=\"99\") ::= \"x=<value>; // <name>\""+newline
				;
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates));
		StringTemplate b = group.getInstanceOf("method");
		b.setAttribute("name", "foo");
		String expecting = "x=34; // foo";
		String result = b.toString();
		assertEqual(result, expecting);
	}

	public void testArgumentsAsTemplates() throws Exception {
		String templates =
				"group test;" +newline+
				"method(name,size) ::= <<"+newline+
				"<stat(value={<size>})>" +newline+
				">>"+newline+
				"stat(value) ::= \"x=<value>;\""+newline
				;
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates));
		StringTemplate b = group.getInstanceOf("method");
		b.setAttribute("name", "foo");
		b.setAttribute("size", "34");
		String expecting = "x=34;";
		String result = b.toString();
		assertEqual(result, expecting);
	}

	public void testArgumentsAsTemplatesDefaultDelimiters() throws Exception {
		String templates =
				"group test;" +newline+
				"method(name,size) ::= <<"+newline+
				"$stat(value={$size$})$" +newline+
				">>"+newline+
				"stat(value) ::= \"x=$value$;\""+newline
				;
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates),
										DefaultTemplateLexer.class);
		StringTemplate b = group.getInstanceOf("method");
		b.setAttribute("name", "foo");
		b.setAttribute("size", "34");
		String expecting = "x=34;";
		String result = b.toString();
		assertEqual(result, expecting);
	}

	public void testDefaultArgsWhenNotInvoked() throws Exception {
		String templates =
				"group test;" +newline+
				"b(name=\"foo\") ::= \".<name>.\""+newline
				;
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates));
		StringTemplate b = group.getInstanceOf("b");
		String expecting = ".foo.";
		String result = b.toString();
		assertEqual(result, expecting);
	}

	public class DateRenderer implements AttributeRenderer {
		public String toString(Object o) {
			SimpleDateFormat f = new SimpleDateFormat ("yyyy.MM.dd");
			return f.format(((Calendar)o).getTime());
		}
	}

	public class DateRenderer2 implements AttributeRenderer {
		public String toString(Object o) {
			SimpleDateFormat f = new SimpleDateFormat ("MM/dd/yyyy");
			return f.format(((Calendar)o).getTime());
		}
	}

	public void testRendererForST() throws Exception {
		StringTemplate st =new StringTemplate(
				"date: <created>",
				AngleBracketTemplateLexer.class);
		st.setAttribute("created",
						new GregorianCalendar(2005, 07-1, 05));
		st.registerRenderer(GregorianCalendar.class, new DateRenderer());
		String expecting = "date: 2005.07.05";
		String result = st.toString();
		assertEqual(result, expecting);
	}

	public void testEmbeddedRendererSeesEnclosing() throws Exception {
		// st is embedded in outer; set renderer on outer, st should
		// still see it.
		StringTemplate outer =new StringTemplate(
				"X: <x>",
				AngleBracketTemplateLexer.class);
		StringTemplate st =new StringTemplate(
				"date: <created>",
				AngleBracketTemplateLexer.class);
		st.setAttribute("created",
						new GregorianCalendar(2005, 07-1, 05));
		outer.setAttribute("x", st);
		outer.registerRenderer(GregorianCalendar.class, new DateRenderer());
		String expecting = "X: date: 2005.07.05";
		String result = outer.toString();
		assertEqual(result, expecting);
	}

	public void testRendererForGroup() throws Exception {
		String templates =
				"group test;" +newline+
				"dateThing(created) ::= \"date: <created>\""+newline
				;
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates));
		StringTemplate st = group.getInstanceOf("dateThing");
		st.setAttribute("created",
						new GregorianCalendar(2005, 07-1, 05));
		group.registerRenderer(GregorianCalendar.class, new DateRenderer());
		String expecting = "date: 2005.07.05";
		String result = st.toString();
		assertEqual(result, expecting);
	}

	public void testOverriddenRenderer() throws Exception {
		String templates =
				"group test;" +newline+
				"dateThing(created) ::= \"date: <created>\""+newline
				;
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates));
		StringTemplate st = group.getInstanceOf("dateThing");
		st.setAttribute("created",
						new GregorianCalendar(2005, 07-1, 05));
		group.registerRenderer(GregorianCalendar.class, new DateRenderer());
		st.registerRenderer(GregorianCalendar.class, new DateRenderer2());
		String expecting = "date: 07/05/2005";
		String result = st.toString();
		assertEqual(result, expecting);
	}

	public void testMap() throws Exception {
		String templates =
				"group test;" +newline+
				"typeInit ::= [\"int\":\"0\", \"float\":\"0.0\"] "+newline+
				"var(type,name) ::= \"<type> <name> = <typeInit.(type)>;\""+newline
				;
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates));
		StringTemplate st = group.getInstanceOf("var");
		st.setAttribute("type", "int");
		st.setAttribute("name", "x");
		String expecting = "int x = 0;";
		String result = st.toString();
		assertEqual(result, expecting);
	}

	public void testMapValuesAreTemplates() throws Exception {
		String templates =
				"group test;" +newline+
				"typeInit ::= [\"int\":\"0<w>\", \"float\":\"0.0<w>\"] "+newline+
				"var(type,w,name) ::= \"<type> <name> = <typeInit.(type)>;\""+newline
				;
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates));
		StringTemplate st = group.getInstanceOf("var");
		st.setAttribute("w", "L");
		st.setAttribute("type", "int");
		st.setAttribute("name", "x");
		String expecting = "int x = 0L;";
		String result = st.toString();
		assertEqual(result, expecting);
	}

	public void testMapMissingDefaultValueIsEmpty() throws Exception {
		String templates =
				"group test;" +newline+
				"typeInit ::= [\"int\":\"0\", \"float\":\"0.0\"] "+newline+
				"var(type,w,name) ::= \"<type> <name> = <typeInit.(type)>;\""+newline
				;
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates));
		StringTemplate st = group.getInstanceOf("var");
		st.setAttribute("w", "L");
		st.setAttribute("type", "double"); // double not in typeInit map
		st.setAttribute("name", "x");
		String expecting = "double x = ;"; // weird, but tests default value is key
		String result = st.toString();
		assertEqual(result, expecting);
	}

	public void testMapHiddenByFormalArg() throws Exception {
		String templates =
				"group test;" +newline+
				"typeInit ::= [\"int\":\"0\", \"float\":\"0.0\"] "+newline+
				"var(typeInit,type,name) ::= \"<type> <name> = <typeInit.(type)>;\""+newline
				;
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates));
		StringTemplate st = group.getInstanceOf("var");
		st.setAttribute("type", "int");
		st.setAttribute("name", "x");
		String expecting = "int x = ;";
		String result = st.toString();
		assertEqual(result, expecting);
	}

	public void testMapEmptyValueAndAngleBracketStrings() throws Exception {
		String templates =
				"group test;" +newline+
				"typeInit ::= [\"int\":\"0\", \"float\":, \"double\":<<0.0L>>] "+newline+
				"var(type,name) ::= \"<type> <name> = <typeInit.(type)>;\""+newline
				;
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates));
		StringTemplate st = group.getInstanceOf("var");
		st.setAttribute("type", "float");
		st.setAttribute("name", "x");
		String expecting = "float x = ;";
		String result = st.toString();
		assertEqual(result, expecting);
	}

	public void testMapDefaultValue() throws Exception {
		String templates =
				"group test;" +newline+
				"typeInit ::= [\"int\":\"0\", \"default\":\"null\"] "+newline+
				"var(type,name) ::= \"<type> <name> = <typeInit.(type)>;\""+newline
				;
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates));
		StringTemplate st = group.getInstanceOf("var");
		st.setAttribute("type", "UserRecord");
		st.setAttribute("name", "x");
		String expecting = "UserRecord x = null;";
		String result = st.toString();
		assertEqual(result, expecting);
	}

	public void testMapEmptyDefaultValue() throws Exception {
		String templates =
				"group test;" +newline+
				"typeInit ::= [\"int\":\"0\", \"default\":] "+newline+
				"var(type,name) ::= \"<type> <name> = <typeInit.(type)>;\""+newline
				;
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates));
		StringTemplate st = group.getInstanceOf("var");
		st.setAttribute("type", "UserRecord");
		st.setAttribute("name", "x");
		String expecting = "UserRecord x = ;";
		String result = st.toString();
		assertEqual(result, expecting);
	}

	public void testMapEmptyDefaultValueIsKey() throws Exception {
		String templates =
				"group test;" +newline+
				"typeInit ::= [\"int\":\"0\", \"default\":key] "+newline+
				"var(type,name) ::= \"<type> <name> = <typeInit.(type)>;\""+newline
				;
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates));
		StringTemplate st = group.getInstanceOf("var");
		st.setAttribute("type", "UserRecord");
		st.setAttribute("name", "x");
		String expecting = "UserRecord x = UserRecord;";
		String result = st.toString();
		assertEqual(result, expecting);
	}

	public void testMapViaEnclosingTemplates() throws Exception {
		String templates =
				"group test;" +newline+
				"typeInit ::= [\"int\":\"0\", \"float\":\"0.0\"] "+newline+
				"intermediate(type,name) ::= \"<var(...)>\""+newline+
				"var(type,name) ::= \"<type> <name> = <typeInit.(type)>;\""+newline
				;
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates));
		StringTemplate st = group.getInstanceOf("intermediate");
		st.setAttribute("type", "int");
		st.setAttribute("name", "x");
		String expecting = "int x = 0;";
		String result = st.toString();
		assertEqual(result, expecting);
	}

	public void testMapViaEnclosingTemplates2() throws Exception {
		String templates =
				"group test;" +newline+
				"typeInit ::= [\"int\":\"0\", \"float\":\"0.0\"] "+newline+
				"intermediate(stuff) ::= \"<stuff>\""+newline+
				"var(type,name) ::= \"<type> <name> = <typeInit.(type)>;\""+newline
				;
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates));
		StringTemplate interm = group.getInstanceOf("intermediate");
		StringTemplate var = group.getInstanceOf("var");
		var.setAttribute("type", "int");
		var.setAttribute("name", "x");
		interm.setAttribute("stuff", var);
		String expecting = "int x = 0;";
		String result = interm.toString();
		assertEqual(result, expecting);
	}

	public void testEmptyGroupTemplate() throws Exception {
		String templates =
				"group test;" +newline+
				"foo() ::= \"\""+newline
				;
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates));
		StringTemplate a = group.getInstanceOf("foo");
		String expecting = "";
		String result = a.toString();
		assertEqual(result, expecting);
	}

	public void test8BitEuroChars() throws Exception {
		StringTemplate e = new StringTemplate(
				"Danish: Å char"
			);
		e = e.getInstanceOf();
		String expecting = "Danish: Å char";
		assertEqual(e.toString(), expecting);
	}

	public void testFirstOp() throws Exception {
		StringTemplate e = new StringTemplate(
				"$first(names)$"
			);
		e = e.getInstanceOf();
		e.setAttribute("names", "Ter");
		e.setAttribute("names", "Tom");
		e.setAttribute("names", "Sriram");
		String expecting = "Ter";
		assertEqual(e.toString(), expecting);
	}

	public void testRestOp() throws Exception {
		StringTemplate e = new StringTemplate(
				"$rest(names); separator=\", \"$"
			);
		e = e.getInstanceOf();
		e.setAttribute("names", "Ter");
		e.setAttribute("names", "Tom");
		e.setAttribute("names", "Sriram");
		String expecting = "Tom, Sriram";
		assertEqual(e.toString(), expecting);
	}

	public void testLastOp() throws Exception {
		StringTemplate e = new StringTemplate(
				"$last(names)$"
			);
		e = e.getInstanceOf();
		e.setAttribute("names", "Ter");
		e.setAttribute("names", "Tom");
		e.setAttribute("names", "Sriram");
		String expecting = "Sriram";
		assertEqual(e.toString(), expecting);
	}

	public void testCombinedOp() throws Exception {
		// replace first of yours with first of mine
		StringTemplate e = new StringTemplate(
				"$[first(mine),rest(yours)]; separator=\", \"$"
			);
		e = e.getInstanceOf();
		e.setAttribute("mine", "1");
		e.setAttribute("mine", "2");
		e.setAttribute("mine", "3");
		e.setAttribute("yours", "a");
		e.setAttribute("yours", "b");
		String expecting = "1, b";
		assertEqual(e.toString(), expecting);
	}

	public void testCatListAndSingleAttribute() throws Exception {
		// replace first of yours with first of mine
		StringTemplate e = new StringTemplate(
				"$[mine,yours]; separator=\", \"$"
			);
		e = e.getInstanceOf();
		e.setAttribute("mine", "1");
		e.setAttribute("mine", "2");
		e.setAttribute("mine", "3");
		e.setAttribute("yours", "a");
		String expecting = "1, 2, 3, a";
		assertEqual(e.toString(), expecting);
	}

	public void testCatListAndEmptyAttributes() throws Exception {
		// + is overloaded to be cat strings and cat lists so the
		// two operands (from left to right) determine which way it
		// goes.  In this case, x+mine is a list so everything from their
		// to the right becomes list cat.
		StringTemplate e = new StringTemplate(
				"$[x,mine,y,yours,z]; separator=\", \"$"
			);
		e = e.getInstanceOf();
		e.setAttribute("mine", "1");
		e.setAttribute("mine", "2");
		e.setAttribute("mine", "3");
		e.setAttribute("yours", "a");
		String expecting = "1, 2, 3, a";
		assertEqual(e.toString(), expecting);
	}

	public void testNestedOp() throws Exception {
		StringTemplate e = new StringTemplate(
				"$first(rest(names))$" // gets 2nd element
			);
		e = e.getInstanceOf();
		e.setAttribute("names", "Ter");
		e.setAttribute("names", "Tom");
		e.setAttribute("names", "Sriram");
		String expecting = "Tom";
		assertEqual(e.toString(), expecting);
	}

	public void testFirstWithOneAttributeOp() throws Exception {
		StringTemplate e = new StringTemplate(
				"$first(names)$"
			);
		e = e.getInstanceOf();
		e.setAttribute("names", "Ter");
		String expecting = "Ter";
		assertEqual(e.toString(), expecting);
	}

	public void testLastWithOneAttributeOp() throws Exception {
		StringTemplate e = new StringTemplate(
				"$last(names)$"
			);
		e = e.getInstanceOf();
		e.setAttribute("names", "Ter");
		String expecting = "Ter";
		assertEqual(e.toString(), expecting);
	}

	public void testLastWithLengthOneListAttributeOp() throws Exception {
		StringTemplate e = new StringTemplate(
				"$last(names)$"
			);
		e = e.getInstanceOf();
		e.setAttribute("names", new ArrayList() {{add("Ter");}});
		String expecting = "Ter";
		assertEqual(e.toString(), expecting);
	}

	public void testRestWithOneAttributeOp() throws Exception {
		StringTemplate e = new StringTemplate(
				"$rest(names)$"
			);
		e = e.getInstanceOf();
		e.setAttribute("names", "Ter");
		String expecting = "";
		assertEqual(e.toString(), expecting);
	}

	public void testRestWithLengthOneListAttributeOp() throws Exception {
		StringTemplate e = new StringTemplate(
				"$rest(names)$"
			);
		e = e.getInstanceOf();
		e.setAttribute("names", new ArrayList() {{add("Ter");}});
		String expecting = "";
		assertEqual(e.toString(), expecting);
	}

	public void testRepeatedRestOp() throws Exception {
		StringTemplate e = new StringTemplate(
				"$rest(names)$, $rest(names)$" // gets 2nd element
			);
		e = e.getInstanceOf();
		e.setAttribute("names", "Ter");
		e.setAttribute("names", "Tom");
		String expecting = "Tom, Tom";
		assertEqual(e.toString(), expecting);
	}

	/** BUG!  Fix this.  Iterator is not reset from first to second $x$
	 *  Either reset the iterator or pass an attribute that knows to get
	 *  the iterator each time.  Seems like first, tail do not
	 *  have same problem as they yield objects.
	 *
	 *  Maybe make a RestIterator like I have CatIterator.
	 */
	public void testRepeatedRestOpAsArg() throws Exception {
		String templates =
				"group test;" +newline+
				"root(names) ::= \"$other(rest(names))$\""+newline+
				"other(x) ::= \"$x$, $x$\""+newline
				;
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates),
										DefaultTemplateLexer.class);
		StringTemplate e = group.getInstanceOf("root");
		e.setAttribute("names", "Ter");
		e.setAttribute("names", "Tom");
		String expecting = "Tom, Tom";
		assertEqual(e.toString(), expecting);
	}

	public void testApplyTemplateWithSingleFormalArgs() throws Exception {
		String templates =
				"group test;" +newline+
				"test(names) ::= <<<names:bold(item=it); separator=\", \"> >>"+newline+
				"bold(item) ::= <<*<item>*>>"+newline
				;
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates));
		StringTemplate e = group.getInstanceOf("test");
		e.setAttribute("names", "Ter");
		e.setAttribute("names", "Tom");
		String expecting = "*Ter*, *Tom* ";
		String result = e.toString();
		assertEqual(result, expecting);
	}

	public void testApplyTemplateWithNoFormalArgs() throws Exception {
		String templates =
				"group test;" +newline+
				"test(names) ::= <<<names:bold(); separator=\", \"> >>"+newline+
				"bold() ::= <<*<it>*>>"+newline
				;
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates),
						AngleBracketTemplateLexer.class);
		StringTemplate e = group.getInstanceOf("test");
		e.setAttribute("names", "Ter");
		e.setAttribute("names", "Tom");
		String expecting = "*Ter*, *Tom* ";
		String result = e.toString();
		assertEqual(result, expecting);
	}

	public void testAnonTemplateArgs() throws Exception {
		StringTemplate e = new StringTemplate(
				"$names:{n| $n$}; separator=\", \"$"
			);
		e = e.getInstanceOf();
		e.setAttribute("names", "Ter");
		e.setAttribute("names", "Tom");
		String expecting = "Ter, Tom";
		assertEqual(e.toString(), expecting);
	}

	public void testAnonTemplateArgs2() throws Exception {
		StringTemplate e = new StringTemplate(
				"$names:{n| .$n$.}:{ n | _$n$_}; separator=\", \"$"
			);
		e = e.getInstanceOf();
		e.setAttribute("names", "Ter");
		e.setAttribute("names", "Tom");
		String expecting = "_.Ter._, _.Tom._";
		assertEqual(e.toString(), expecting);
	}

	public void testFirstWithCatAttribute() throws Exception {
		StringTemplate e = new StringTemplate(
				"$first([names,phones])$"
			);
		e = e.getInstanceOf();
		e.setAttribute("names", "Ter");
		e.setAttribute("names", "Tom");
		e.setAttribute("phones", "1");
		e.setAttribute("phones", "2");
		String expecting = "Ter";
		assertEqual(e.toString(), expecting);
	}

	public void testJustCat() throws Exception {
		StringTemplate e = new StringTemplate(
				"$[names,phones]$"
			);
		e = e.getInstanceOf();
		e.setAttribute("names", "Ter");
		e.setAttribute("names", "Tom");
		e.setAttribute("phones", "1");
		e.setAttribute("phones", "2");
		String expecting = "TerTom12";
		assertEqual(e.toString(), expecting);
	}

	public void testCat2Attributes() throws Exception {
		StringTemplate e = new StringTemplate(
				"$[names,phones]; separator=\", \"$"
			);
		e = e.getInstanceOf();
		e.setAttribute("names", "Ter");
		e.setAttribute("names", "Tom");
		e.setAttribute("phones", "1");
		e.setAttribute("phones", "2");
		String expecting = "Ter, Tom, 1, 2";
		assertEqual(e.toString(), expecting);
	}

	public void testCat2AttributesWithApply() throws Exception {
		StringTemplate e = new StringTemplate(
				"$[names,phones]:{a|$a$.}$"
			);
		e = e.getInstanceOf();
		e.setAttribute("names", "Ter");
		e.setAttribute("names", "Tom");
		e.setAttribute("phones", "1");
		e.setAttribute("phones", "2");
		String expecting = "Ter.Tom.1.2.";
		assertEqual(e.toString(), expecting);
	}

	public void testCat3Attributes() throws Exception {
		StringTemplate e = new StringTemplate(
				"$[names,phones,salaries]; separator=\", \"$"
			);
		e = e.getInstanceOf();
		e.setAttribute("names", "Ter");
		e.setAttribute("names", "Tom");
		e.setAttribute("phones", "1");
		e.setAttribute("phones", "2");
		e.setAttribute("salaries", "big");
		e.setAttribute("salaries", "huge");
		String expecting = "Ter, Tom, 1, 2, big, huge";
		assertEqual(e.toString(), expecting);
	}

	public void testListAsTemplateArgument() throws Exception {
		String templates =
				"group test;" +newline+
				"test(names,phones) ::= \"<foo([names,phones])>\""+newline+
				"foo(items) ::= \"<items:{a | *<a>*}>\""+newline
				;
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates),
						AngleBracketTemplateLexer.class);
		StringTemplate e = group.getInstanceOf("test");
		e.setAttribute("names", "Ter");
		e.setAttribute("names", "Tom");
		e.setAttribute("phones", "1");
		e.setAttribute("phones", "2");
		String expecting = "*Ter**Tom**1**2*";
		String result = e.toString();
		assertEqual(result, expecting);
	}

	public void testSingleExprTemplateArgument() throws Exception {
		String templates =
				"group test;" +newline+
				"test(name) ::= \"<bold(name)>\""+newline+
				"bold(item) ::= \"*<item>*\""+newline
				;
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates),
						AngleBracketTemplateLexer.class);
		StringTemplate e = group.getInstanceOf("test");
		e.setAttribute("name", "Ter");
		String expecting = "*Ter*";
		String result = e.toString();
		assertEqual(result, expecting);
	}

	public void testSingleExprTemplateArgumentInApply() throws Exception {
		// when you specify a single arg on a template application
		// it overrides the setting of the iterated value "it" to that
		// same single formal arg.  Your arg hides the implicitly set "it".
		String templates =
				"group test;" +newline+
				"test(names,x) ::= \"<names:bold(x)>\""+newline+
				"bold(item) ::= \"*<item>*\""+newline
				;
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates),
						AngleBracketTemplateLexer.class);
		StringTemplate e = group.getInstanceOf("test");
		e.setAttribute("names", "Ter");
		e.setAttribute("names", "Tom");
		e.setAttribute("x", "ick");
		String expecting = "*ick**ick*";
		String result = e.toString();
		assertEqual(result, expecting);
	}

	public void testSoleFormalTemplateArgumentInMultiApply() throws Exception {
		String templates =
				"group test;" +newline+
				"test(names) ::= \"<names:bold(),italics()>\""+newline+
				"bold(x) ::= \"*<x>*\""+newline+
				"italics(y) ::= \"_<y>_\""+newline
				;
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates),
						AngleBracketTemplateLexer.class);
		StringTemplate e = group.getInstanceOf("test");
		e.setAttribute("names", "Ter");
		e.setAttribute("names", "Tom");
		String expecting = "*Ter*_Tom_";
		String result = e.toString();
		assertEqual(result, expecting);
	}

	public void testSingleExprTemplateArgumentError() throws Exception {
		String templates =
				"group test;" +newline+
				"test(name) ::= \"<bold(name)>\""+newline+
				"bold(item,ick) ::= \"*<item>*\""+newline
				;
		StringTemplateErrorListener errors = new ErrorBuffer();
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates),
						AngleBracketTemplateLexer.class, errors);
		StringTemplate e = group.getInstanceOf("test");
		e.setAttribute("name", "Ter");
		String result = e.toString();
		String expecting = "template bold must have exactly one formal arg in template context [test <invoke bold arg context>]";
		assertEqual(errors.toString(), expecting);
	}

	public void testInvokeIndirectTemplateWithSingleFormalArgs() throws Exception {
		String templates =
				"group test;" +newline+
				"test(templateName,arg) ::= \"<(templateName)(arg)>\""+newline+
				"bold(x) ::= <<*<x>*>>"+newline+
				"italics(y) ::= <<_<y>_>>"+newline
				;
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates));
		StringTemplate e = group.getInstanceOf("test");
		e.setAttribute("templateName", "italics");
		e.setAttribute("arg", "Ter");
		String expecting = "_Ter_";
		String result = e.toString();
		assertEqual(result, expecting);
	}

	public void testParallelAttributeIteration() throws Exception {
		StringTemplate e = new StringTemplate(
				"$names,phones,salaries:{n,p,s | $n$@$p$: $s$\n}$"
			);
		e = e.getInstanceOf();
		e.setAttribute("names", "Ter");
		e.setAttribute("names", "Tom");
		e.setAttribute("phones", "1");
		e.setAttribute("phones", "2");
		e.setAttribute("salaries", "big");
		e.setAttribute("salaries", "huge");
		String expecting = "Ter@1: big"+newline+"Tom@2: huge"+newline;
		assertEqual(e.toString(), expecting);
	}

	public void testParallelAttributeIterationWithDifferentSizes() throws Exception {
		StringTemplate e = new StringTemplate(
				"$names,phones,salaries:{n,p,s | $n$@$p$: $s$}; separator=\", \"$"
			);
		e = e.getInstanceOf();
		e.setAttribute("names", "Ter");
		e.setAttribute("names", "Tom");
		e.setAttribute("names", "Sriram");
		e.setAttribute("phones", "1");
		e.setAttribute("phones", "2");
		e.setAttribute("salaries", "big");
		String expecting = "Ter@1: big, Tom@2: , Sriram@: ";
		assertEqual(e.toString(), expecting);
	}

	public void testParallelAttributeIterationWithSingletons() throws Exception {
		StringTemplate e = new StringTemplate(
				"$names,phones,salaries:{n,p,s | $n$@$p$: $s$}; separator=\", \"$"
			);
		e = e.getInstanceOf();
		e.setAttribute("names", "Ter");
		e.setAttribute("phones", "1");
		e.setAttribute("salaries", "big");
		String expecting = "Ter@1: big";
		assertEqual(e.toString(), expecting);
	}

	public void testParallelAttributeIterationWithMismatchArgListSizes() throws Exception {
		StringTemplateErrorListener errors = new ErrorBuffer();
		StringTemplate e = new StringTemplate(
				"$names,phones,salaries:{n,p | $n$@$p$}; separator=\", \"$"
			);
		e.setErrorListener(errors);
		e = e.getInstanceOf();
		e.setAttribute("names", "Ter");
		e.setAttribute("names", "Tom");
		e.setAttribute("phones", "1");
		e.setAttribute("phones", "2");
		e.setAttribute("salaries", "big");
		String expecting = "Ter@1, Tom@2";
		assertEqual(e.toString(), expecting);
		String errorExpecting = "number of arguments [n, p] mismatch between attribute list and anonymous template in context [anonymous]";
		assertEqual(errors.toString(), errorExpecting);
	}

	public void testParallelAttributeIterationWithMissingArgs() throws Exception {
		StringTemplateErrorListener errors = new ErrorBuffer();
		StringTemplate e = new StringTemplate(
				"$names,phones,salaries:{$n$@$p$}; separator=\", \"$"
			);
		e.setErrorListener(errors);
		e = e.getInstanceOf();
		e.setAttribute("names", "Tom");
		e.setAttribute("phones", "2");
		e.setAttribute("salaries", "big");
		e.toString(); // generate the error
		String errorExpecting = "missing arguments in anonymous template in context [anonymous]";
		assertEqual(errors.toString(), errorExpecting);
	}

	public void testParallelAttributeIterationWithDifferentSizesTemplateRefInsideToo() throws Exception {
		String templates =
				"group test;" +newline+
				"page(names,phones,salaries) ::= "+newline+
				"	<<$names,phones,salaries:{n,p,s | $value(n)$@$value(p)$: $value(s)$}; separator=\", \"$>>"+newline +
				"value(x=\"n/a\") ::= \"$x$\"" +newline;
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates),
										DefaultTemplateLexer.class);
		StringTemplate p = group.getInstanceOf("page");
		p.setAttribute("names", "Ter");
		p.setAttribute("names", "Tom");
		p.setAttribute("names", "Sriram");
		p.setAttribute("phones", "1");
		p.setAttribute("phones", "2");
		p.setAttribute("salaries", "big");
		String expecting = "Ter@1: big, Tom@2: n/a, Sriram@n/a: n/a";
		assertEqual(p.toString(), expecting);
	}

	public void testAnonTemplateOnLeftOfApply() throws Exception {
		StringTemplate e = new StringTemplate(
				"${foo}:{($it$)}$"
			);
		String expecting = "(foo)";
		assertEqual(e.toString(), expecting);
	}

	public void testOverrideThroughConditional() throws Exception {
		String templates =
			"group base;" +newline+
			"body(ick) ::= \"<if(ick)>ick<f()><else><f()><endif>\"" +
			"f() ::= \"foo\""+newline
			;
		StringTemplateGroup group =
				new StringTemplateGroup(new StringReader(templates));
		String templates2 =
				"group sub;" +newline+
				"f() ::= \"bar\""+newline
			;
		StringTemplateGroup subgroup =
			new StringTemplateGroup(new StringReader(templates2),
									AngleBracketTemplateLexer.class,
									null,
									group);

		StringTemplate b = subgroup.getInstanceOf("body");
		String expecting ="bar";
		String result = b.toString();
		assertEqual(result, expecting);
	}

	public static class NonPublicProperty {

	}

	public void testNonPublicPropertyAccess() throws Exception {
		StringTemplate st =
				new StringTemplate("$x.foo$:$x.bar$");
		Object o = new Object() {
			public int foo = 9;
			public int getBar() { return 34; }
		};

		st.setAttribute("x", o);
		String expecting = "9:34";
		assertEqual(st.toString(), expecting);
	}

	public void testIndexVar() throws Exception {
		StringTemplateGroup group =
				new StringTemplateGroup("dummy", ".");
		String newline = System.getProperty("line.separator");
		StringTemplate t =
				new StringTemplate(
						group,
						"$A:{$i$. $it$}; separator=\"\\n\"$"
				);
		t.setAttribute("A", "parrt");
		t.setAttribute("A", "tombu");
		String expecting =
			"1. parrt" +newline+
			"2. tombu";
		assertEqual(t.toString(), expecting);
	}

	public void testIndex0Var() throws Exception {
		StringTemplateGroup group =
				new StringTemplateGroup("dummy", ".");
		String newline = System.getProperty("line.separator");
		StringTemplate t =
				new StringTemplate(
						group,
						"$A:{$i0$. $it$}; separator=\"\\n\"$"
				);
		t.setAttribute("A", "parrt");
		t.setAttribute("A", "tombu");
		String expecting =
			"0. parrt" +newline+
			"1. tombu";
		assertEqual(t.toString(), expecting);
	}

	public void testIndexVarWithMultipleExprs() throws Exception {
		StringTemplateGroup group =
				new StringTemplateGroup("dummy", ".");
		String newline = System.getProperty("line.separator");
		StringTemplate t =
				new StringTemplate(
						group,
						"$A,B:{a,b|$i$. $a$@$b$}; separator=\"\\n\"$"
				);
		t.setAttribute("A", "parrt");
		t.setAttribute("A", "tombu");
		t.setAttribute("B", "x5707");
		t.setAttribute("B", "x5000");
		String expecting =
			"1. parrt@x5707" +newline+
			"2. tombu@x5000";
		assertEqual(t.toString(), expecting);
	}

	public void testIndex0VarWithMultipleExprs() throws Exception {
		StringTemplateGroup group =
				new StringTemplateGroup("dummy", ".");
		String newline = System.getProperty("line.separator");
		StringTemplate t =
				new StringTemplate(
						group,
						"$A,B:{a,b|$i0$. $a$@$b$}; separator=\"\\n\"$"
				);
		t.setAttribute("A", "parrt");
		t.setAttribute("A", "tombu");
		t.setAttribute("B", "x5707");
		t.setAttribute("B", "x5000");
		String expecting =
			"0. parrt@x5707" +newline+
			"1. tombu@x5000";
		assertEqual(t.toString(), expecting);
	}

	public void testArgumentContext() throws Exception {
		// t is referenced within foo and so will be evaluated in that
		// context.  it can therefore see name.
		StringTemplateGroup group =
				new StringTemplateGroup("test");
		StringTemplate main = group.defineTemplate("main", "$foo(t={Hi, $name$}, name=\"parrt\")$");
		StringTemplate foo = group.defineTemplate("foo", "$t$");
		String expecting="Hi, parrt";
		assertEqual(main.toString(), expecting);
	}

	/** Use when super.attr name is implemented
	public void testArgumentContext2() throws Exception {
		// t is referenced within foo and so will be evaluated in that
		// context.  it can therefore see name.
		StringTemplateGroup group =
				new StringTemplateGroup("test");
		StringTemplate main = group.defineTemplate("main", "$foo(t={Hi, $super.name$}, name=\"parrt\")$");
		main.setAttribute("name", "tombu");
		StringTemplate foo = group.defineTemplate("foo", "$t$");
		String expecting="Hi, parrt";
		assertEqual(main.toString(), expecting);
	}
	 */

	public static void writeFile(String dir, String fileName, String content) {
		try {
			File f = new File(dir, fileName);
			FileWriter w = new FileWriter(f);
			BufferedWriter bw = new BufferedWriter(w);
			bw.write(content);
			bw.close();
			w.close();
		}
		catch (IOException ioe) {
			System.err.println("can't write file");
			ioe.printStackTrace(System.err);
		}
	}

}
