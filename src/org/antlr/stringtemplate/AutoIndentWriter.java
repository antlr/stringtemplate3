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

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

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
	public static final String newline = System.getProperty("line.separator");

	/** stack of indents; use List as it's much faster than Stack. Grows
	 *  from 0..n-1.
	 */
	protected List indents = new ArrayList();

	protected Writer out = null;
    protected boolean atStartOfLine = true;

	/** Track char position in the line (later we can think about tabs).
	 *  Indexed from 0.  We want to keep charPosition <= lineWidth.
	 *  This is the position we are *about* to write not the position
	 *  last written to.
	 */
	protected int charPosition = 0;
	protected int lineWidth = NO_WRAP;

	protected int charPositionOfStartOfExpr = 0;

	public AutoIndentWriter(Writer out) {
		this.out = out;
		indents.add(null); // start with no indent
    }

	public void setLineWidth(int lineWidth) {
		this.lineWidth = lineWidth;
	}

	/** Push even blank (null) indents as they are like scopes; must
     *  be able to pop them back off stack.
     */
    public void pushIndentation(String indent) {
		indents.add(indent);
    }

    public String popIndentation() {
        return (String)indents.remove(indents.size()-1);
    }

	public int getIndentationWidth() {
		int n = 0;
        for (int i=0; i<indents.size(); i++) {
            String ind = (String)indents.get(i);
            if ( ind!=null ) {
                n+=ind.length();
			}
        }
		return n;
	}

	public int getCurrentCharPositionInLine() {
		return charPosition;
	}

	public void setCurrentCharPositionOfExpr(int charPos) {
		charPositionOfStartOfExpr = charPos;
	}

	/** Write out a string literal or attribute expression or expression element.
	 *
	 *  If doing line wrap, then check wrap before emitting this str.  If
	 *  at or beyond desired line width then emit a \n and any indentation
	 *  before spitting out this str.
	 */
	public int write(String str) throws IOException {
        //System.out.println("write("+str+"); indents="+indents);
		int n = 0;

		// if want wrap and not already at start of line (last char was \n)
		// and we have hit or exceeded the threshold
		if ( lineWidth!=NO_WRAP && !atStartOfLine && charPosition>=lineWidth ) {
			out.write(newline);
			n++; // have to count the extra \n we wrote
			charPosition = 0;
			if ( charPositionOfStartOfExpr>0 ) {
				// we know absolute position, don't bother indenting
				n+=indent(charPositionOfStartOfExpr);
			}
			else {
				n+=indent();
			}
		}

		return writeWork(str, n);
	}

	protected int writeWork(String str, int n) throws IOException {
		for (int i=0; i<str.length(); i++) {
			char c = str.charAt(i);
			if ( c=='\n' ) {
				atStartOfLine = true;
				charPosition = -1; // set so the write below sets to 0
			}
			else {
				if ( atStartOfLine ) {
					n+=indent();
					atStartOfLine = false;
				}
			}
			n++;
			out.write(c);
			charPosition++;
		}
		return n;
	}

	public int writeSeparator(String str) throws IOException {
		return writeWork(str, 0);
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
		charPosition += n;
		return n;
    }

	public int indent(int spaces) throws IOException {
		for (int i=1; i<=spaces; i++) {
			out.write(' ');
		}
		charPosition += spaces;
		return spaces;
	}

}
