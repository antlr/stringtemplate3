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

import java.util.Stack;
import java.io.Writer;
import java.io.IOException;

/** Essentially a char filter that knows how to auto-indent output
 *  by maintaining a stack of indent levels.  I set a flag upon newline
 *  and then next nonwhitespace char resets flag and spits out indention.
 *  The indent stack is a stack of strings so we can repeat original indent
 *  not just the same number of columns (don't have to worry about tabs vs
 *  spaces then).
 *
 *  This is a filter on a Writer.
 *
 *  It may be screwed up for '\r' '\n' on PC's.
 */
public class AutoIndentWriter implements StringTemplateWriter {
    public static final int BUFFER_SIZE = 50;
    protected Stack indents = new Stack();
    protected Writer out = null;
    protected boolean atStartOfLine = true;

    public AutoIndentWriter(Writer out) {
        this.out = out;
        indents.push(null); // start with no indent
    }

    /** Push even blank (null) indents as they are like scopes; must
     *  be able to pop them back off stack.
     */
    public void pushIndentation(String indent) {
        indents.push(indent);
    }

    public String popIndentation() {
        return (String)indents.pop();
    }

    public int write(String str) throws IOException {
        //System.out.println("write("+str+"); indents="+indents);
		int n = 0;
        for (int i=0; i<str.length(); i++) {
            char c = str.charAt(i);
            if ( c=='\n' ) {
				atStartOfLine = true;
            }
            else {
                if ( atStartOfLine ) {
                    n+=indent();
                    atStartOfLine = false;
                }
            }
			n++;
            out.write(c);
        }
		return n;
    }

    public int indent() throws IOException {
		int n = 0;
        for (int i=0; i<indents.size(); i++) {
            String ind = (String)indents.get(i);
            if ( ind!=null ) {
                n+=ind.length();
				out.write(ind);
            }
        }
		return n;
    }
}
