// $ANTLR 2.7.5rc2 (2005-01-08): "template.g" -> "TemplateParser.java"$

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
    import java.io.*;

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

/** A parser used to break up a single template into chunks, text literals
 *  and attribute expressions.
 */
public class TemplateParser extends antlr.LLkParser       implements TemplateParserTokenTypes
 {

protected StringTemplate self;

public void reportError(RecognitionException e) {
	self.error("template parse error", e);
}

protected TemplateParser(TokenBuffer tokenBuf, int k) {
  super(tokenBuf,k);
  tokenNames = _tokenNames;
}

public TemplateParser(TokenBuffer tokenBuf) {
  this(tokenBuf,1);
}

protected TemplateParser(TokenStream lexer, int k) {
  super(lexer,k);
  tokenNames = _tokenNames;
}

public TemplateParser(TokenStream lexer) {
  this(lexer,1);
}

public TemplateParser(ParserSharedInputState state) {
  super(state,1);
  tokenNames = _tokenNames;
}

	public final void template(
		StringTemplate self
	) throws RecognitionException, TokenStreamException {
		
		Token  s = null;
		Token  nl = null;
		
			this.self = self;
		
		
		try {      // for error handling
			{
			_loop3:
			do {
				switch ( LA(1)) {
				case LITERAL:
				{
					s = LT(1);
					match(LITERAL);
					self.addChunk(new StringRef(self,s.getText()));
					break;
				}
				case NEWLINE:
				{
					nl = LT(1);
					match(NEWLINE);
					
					if ( LA(1)!=ELSE && LA(1)!=ENDIF ) {
						self.addChunk(new NewlineRef(self,nl.getText()));
					}
						
					break;
				}
				case ACTION:
				case IF:
				{
					action(self);
					break;
				}
				default:
				{
					break _loop3;
				}
				}
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_0);
		}
	}
	
	public final void action(
		StringTemplate self
	) throws RecognitionException, TokenStreamException {
		
		Token  a = null;
		Token  i = null;
		
		try {      // for error handling
			switch ( LA(1)) {
			case ACTION:
			{
				a = LT(1);
				match(ACTION);
				
				String indent = ((ChunkToken)a).getIndentation();
				ASTExpr c = self.parseAction(a.getText());
				c.setIndentation(indent);
				self.addChunk(c);
				
				break;
			}
			case IF:
			{
				i = LT(1);
				match(IF);
				
				ConditionalExpr c = (ConditionalExpr)self.parseAction(i.getText());
				// create and precompile the subtemplate
				StringTemplate subtemplate =
					new StringTemplate(self.getGroup(), null);
				subtemplate.setEnclosingInstance(self);
				subtemplate.setName(i.getText()+" subtemplate");
				self.addChunk(c);
				
				template(subtemplate);
				if ( c!=null ) c.setSubtemplate(subtemplate);
				{
				switch ( LA(1)) {
				case ELSE:
				{
					match(ELSE);
					
					// create and precompile the subtemplate
					StringTemplate elseSubtemplate =
							new StringTemplate(self.getGroup(), null);
					elseSubtemplate.setEnclosingInstance(self);
					elseSubtemplate.setName("else subtemplate");
					
					template(elseSubtemplate);
					if ( c!=null ) c.setElseSubtemplate(elseSubtemplate);
					break;
				}
				case ENDIF:
				{
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				match(ENDIF);
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_1);
		}
	}
	
	
	public static final String[] _tokenNames = {
		"<0>",
		"EOF",
		"<2>",
		"NULL_TREE_LOOKAHEAD",
		"LITERAL",
		"NEWLINE",
		"ACTION",
		"IF",
		"ELSE",
		"ENDIF",
		"EXPR",
		"ESC",
		"SUBTEMPLATE",
		"INDENT",
		"COMMENT"
	};
	
	private static final long[] mk_tokenSet_0() {
		long[] data = { 768L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_0 = new BitSet(mk_tokenSet_0());
	private static final long[] mk_tokenSet_1() {
		long[] data = { 1008L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_1 = new BitSet(mk_tokenSet_1());
	
	}
