// $ANTLR 2.7.5rc2 (2005-01-08): "group.g" -> "GroupParser.java"$

/*
 [The "BSD licence"]
 Copyright (c) 2003-2004 Terence Parr
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
import org.antlr.stringtemplate.*;
import java.util.*;

import antlr.TokenBuffer;
import antlr.TokenStreamException;
import antlr.TokenStreamIOException;
import antlr.ANTLRException;
import antlr.LLkParser;
import antlr.Token;
import antlr.TokenStream;
import antlr.RecognitionException;
import antlr.NoViableAltException;
import antlr.MismatchedTokenException;
import antlr.SemanticException;
import antlr.ParserSharedInputState;
import antlr.collections.impl.BitSet;

/** Match a group of template definitions beginning
 *  with a group name declaration.  Templates are enclosed
 *  in double-quotes or <<...>> quotes for multi-line templates.
 *  Template names have arg lists that indicate the cardinality
 *  of the attribute: present, optional, zero-or-more, one-or-more.
 *  Here is a sample group file:

	group nfa;

	// an NFA has edges and states
	nfa(states,edges) ::= <<
	digraph NFA {
	rankdir=LR;
	<states; separator="\\n">
	<edges; separator="\\n">
	}
	>>

	state(name) ::= "node [shape = circle]; <name>;"

 */
public class GroupParser extends antlr.LLkParser       implements GroupParserTokenTypes
 {

protected StringTemplateGroup group;

public void reportError(RecognitionException e) {
	group.error("template parse error", e);
}

protected GroupParser(TokenBuffer tokenBuf, int k) {
  super(tokenBuf,k);
  tokenNames = _tokenNames;
}

public GroupParser(TokenBuffer tokenBuf) {
  this(tokenBuf,3);
}

protected GroupParser(TokenStream lexer, int k) {
  super(lexer,k);
  tokenNames = _tokenNames;
}

public GroupParser(TokenStream lexer) {
  this(lexer,3);
}

public GroupParser(ParserSharedInputState state) {
  super(state,3);
  tokenNames = _tokenNames;
}

	public final void group(
		StringTemplateGroup g
	) throws RecognitionException, TokenStreamException {
		
		Token  name = null;
		
		try {      // for error handling
			match(LITERAL_group);
			name = LT(1);
			match(ID);
			g.setName(name.getText());
			match(SEMI);
			{
			_loop3:
			do {
				if ((LA(1)==ID)) {
					template(g);
				}
				else {
					break _loop3;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_0);
		}
	}
	
	public final void template(
		StringTemplateGroup g
	) throws RecognitionException, TokenStreamException {
		
		Token  name = null;
		Token  t = null;
		Token  alias = null;
		Token  target = null;
		
		Map formalArgs = null;
		StringTemplate st = null;
		boolean ignore = false;
		
		
		try {      // for error handling
			if ((LA(1)==ID) && (LA(2)==LPAREN)) {
				name = LT(1);
				match(ID);
				
					    if ( g.isDefinedInThisGroup(name.getText()) ) {
					        g.error("redefinition of template: "+name.getText());
					        st = new StringTemplate(); // create bogus template to fill in
					    }
					    else {
					        st = g.defineTemplate(name.getText(), null);
					    }
					
				match(LPAREN);
				{
				switch ( LA(1)) {
				case ID:
				{
					args(st);
					break;
				}
				case RPAREN:
				{
					st.defineEmptyFormalArgumentList();
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				match(RPAREN);
				match(DEFINED_TO_BE);
				t = LT(1);
				match(TEMPLATE);
				st.setTemplate(t.getText());
			}
			else if ((LA(1)==ID) && (LA(2)==DEFINED_TO_BE)) {
				alias = LT(1);
				match(ID);
				match(DEFINED_TO_BE);
				target = LT(1);
				match(ID);
				g.defineTemplateAlias(alias.getText(), target.getText());
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_1);
		}
	}
	
	public final void args(
		StringTemplate st
	) throws RecognitionException, TokenStreamException {
		
		Token  name = null;
		Token  name2 = null;
		
		try {      // for error handling
			name = LT(1);
			match(ID);
			st.defineFormalArgument(name.getText());
			{
			_loop8:
			do {
				if ((LA(1)==COMMA)) {
					match(COMMA);
					name2 = LT(1);
					match(ID);
					st.defineFormalArgument(name2.getText());
				}
				else {
					break _loop8;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_2);
		}
	}
	
	
	public static final String[] _tokenNames = {
		"<0>",
		"EOF",
		"<2>",
		"NULL_TREE_LOOKAHEAD",
		"\"group\"",
		"ID",
		"SEMI",
		"LPAREN",
		"RPAREN",
		"DEFINED_TO_BE",
		"TEMPLATE",
		"COMMA",
		"STAR",
		"PLUS",
		"OPTIONAL",
		"SL_COMMENT",
		"ML_COMMENT",
		"WS"
	};
	
	private static final long[] mk_tokenSet_0() {
		long[] data = { 2L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_0 = new BitSet(mk_tokenSet_0());
	private static final long[] mk_tokenSet_1() {
		long[] data = { 34L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_1 = new BitSet(mk_tokenSet_1());
	private static final long[] mk_tokenSet_2() {
		long[] data = { 256L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_2 = new BitSet(mk_tokenSet_2());
	
	}
