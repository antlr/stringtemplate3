// $ANTLR 2.7.5rc2 (2005-01-08): "action.g" -> "ActionParser.java"$

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
import antlr.collections.AST;
import java.util.Hashtable;
import antlr.ASTFactory;
import antlr.ASTPair;
import antlr.collections.impl.ASTArray;

/** Parse the individual attribute expressions */
public class ActionParser extends antlr.LLkParser       implements ActionParserTokenTypes
 {

    protected StringTemplate self = null;

    public ActionParser(TokenStream lexer, StringTemplate self) {
        this(lexer, 2);
        this.self = self;
    }

	public void reportError(RecognitionException e) {
		self.error("template parse error", e);
	}

protected ActionParser(TokenBuffer tokenBuf, int k) {
  super(tokenBuf,k);
  tokenNames = _tokenNames;
  buildTokenTypeASTClassMap();
  astFactory = new ASTFactory(getTokenTypeToASTClassMap());
}

public ActionParser(TokenBuffer tokenBuf) {
  this(tokenBuf,2);
}

protected ActionParser(TokenStream lexer, int k) {
  super(lexer,k);
  tokenNames = _tokenNames;
  buildTokenTypeASTClassMap();
  astFactory = new ASTFactory(getTokenTypeToASTClassMap());
}

public ActionParser(TokenStream lexer) {
  this(lexer,2);
}

public ActionParser(ParserSharedInputState state) {
  super(state,2);
  tokenNames = _tokenNames;
  buildTokenTypeASTClassMap();
  astFactory = new ASTFactory(getTokenTypeToASTClassMap());
}

	public final Map  action() throws RecognitionException, TokenStreamException {
		Map opts=null;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		org.antlr.stringtemplate.language.StringTemplateAST action_AST = null;
		
		try {      // for error handling
			switch ( LA(1)) {
			case LPAREN:
			case ID:
			case LITERAL_super:
			case STRING:
			case INT:
			{
				templatesExpr();
				astFactory.addASTChild(currentAST, returnAST);
				{
				switch ( LA(1)) {
				case SEMI:
				{
					match(SEMI);
					opts=optionList();
					astFactory.addASTChild(currentAST, returnAST);
					break;
				}
				case EOF:
				{
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				action_AST = (org.antlr.stringtemplate.language.StringTemplateAST)currentAST.root;
				break;
			}
			case CONDITIONAL:
			{
				org.antlr.stringtemplate.language.StringTemplateAST tmp2_AST = null;
				tmp2_AST = (org.antlr.stringtemplate.language.StringTemplateAST)astFactory.create(LT(1));
				astFactory.makeASTRoot(currentAST, tmp2_AST);
				match(CONDITIONAL);
				match(LPAREN);
				ifCondition();
				astFactory.addASTChild(currentAST, returnAST);
				match(RPAREN);
				action_AST = (org.antlr.stringtemplate.language.StringTemplateAST)currentAST.root;
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_0);
			} else {
			  throw ex;
			}
		}
		returnAST = action_AST;
		return opts;
	}
	
	public final void templatesExpr() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		org.antlr.stringtemplate.language.StringTemplateAST templatesExpr_AST = null;
		Token  c = null;
		org.antlr.stringtemplate.language.StringTemplateAST c_AST = null;
		
		try {      // for error handling
			expr();
			astFactory.addASTChild(currentAST, returnAST);
			{
			_loop8:
			do {
				if ((LA(1)==COLON)) {
					c = LT(1);
					c_AST = (org.antlr.stringtemplate.language.StringTemplateAST)astFactory.create(c);
					astFactory.makeASTRoot(currentAST, c_AST);
					match(COLON);
					if ( inputState.guessing==0 ) {
						c_AST.setType(APPLY);
					}
					template();
					astFactory.addASTChild(currentAST, returnAST);
					{
					_loop7:
					do {
						if ((LA(1)==COMMA)) {
							match(COMMA);
							template();
							astFactory.addASTChild(currentAST, returnAST);
						}
						else {
							break _loop7;
						}
						
					} while (true);
					}
				}
				else {
					break _loop8;
				}
				
			} while (true);
			}
			templatesExpr_AST = (org.antlr.stringtemplate.language.StringTemplateAST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_1);
			} else {
			  throw ex;
			}
		}
		returnAST = templatesExpr_AST;
	}
	
	public final Map  optionList() throws RecognitionException, TokenStreamException {
		Map opts=new HashMap();
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		org.antlr.stringtemplate.language.StringTemplateAST optionList_AST = null;
		org.antlr.stringtemplate.language.StringTemplateAST e_AST = null;
		
		try {      // for error handling
			match(LITERAL_separator);
			org.antlr.stringtemplate.language.StringTemplateAST tmp7_AST = null;
			tmp7_AST = (org.antlr.stringtemplate.language.StringTemplateAST)astFactory.create(LT(1));
			match(ASSIGN);
			expr();
			e_AST = (org.antlr.stringtemplate.language.StringTemplateAST)returnAST;
			if ( inputState.guessing==0 ) {
				opts.put("separator",e_AST);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_0);
			} else {
			  throw ex;
			}
		}
		returnAST = optionList_AST;
		return opts;
	}
	
	public final void ifCondition() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		org.antlr.stringtemplate.language.StringTemplateAST ifCondition_AST = null;
		
		try {      // for error handling
			switch ( LA(1)) {
			case LPAREN:
			case ID:
			case LITERAL_super:
			case STRING:
			case INT:
			{
				ifAtom();
				astFactory.addASTChild(currentAST, returnAST);
				ifCondition_AST = (org.antlr.stringtemplate.language.StringTemplateAST)currentAST.root;
				break;
			}
			case NOT:
			{
				org.antlr.stringtemplate.language.StringTemplateAST tmp8_AST = null;
				tmp8_AST = (org.antlr.stringtemplate.language.StringTemplateAST)astFactory.create(LT(1));
				astFactory.makeASTRoot(currentAST, tmp8_AST);
				match(NOT);
				ifAtom();
				astFactory.addASTChild(currentAST, returnAST);
				ifCondition_AST = (org.antlr.stringtemplate.language.StringTemplateAST)currentAST.root;
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_2);
			} else {
			  throw ex;
			}
		}
		returnAST = ifCondition_AST;
	}
	
	public final void expr() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		org.antlr.stringtemplate.language.StringTemplateAST expr_AST = null;
		
		try {      // for error handling
			primaryExpr();
			astFactory.addASTChild(currentAST, returnAST);
			{
			_loop13:
			do {
				if ((LA(1)==PLUS)) {
					org.antlr.stringtemplate.language.StringTemplateAST tmp9_AST = null;
					tmp9_AST = (org.antlr.stringtemplate.language.StringTemplateAST)astFactory.create(LT(1));
					astFactory.makeASTRoot(currentAST, tmp9_AST);
					match(PLUS);
					primaryExpr();
					astFactory.addASTChild(currentAST, returnAST);
				}
				else {
					break _loop13;
				}
				
			} while (true);
			}
			expr_AST = (org.antlr.stringtemplate.language.StringTemplateAST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_3);
			} else {
			  throw ex;
			}
		}
		returnAST = expr_AST;
	}
	
	public final void template() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		org.antlr.stringtemplate.language.StringTemplateAST template_AST = null;
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case LPAREN:
			case ID:
			case LITERAL_super:
			{
				namedTemplate();
				astFactory.addASTChild(currentAST, returnAST);
				break;
			}
			case ANONYMOUS_TEMPLATE:
			{
				anonymousTemplate();
				astFactory.addASTChild(currentAST, returnAST);
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			if ( inputState.guessing==0 ) {
				template_AST = (org.antlr.stringtemplate.language.StringTemplateAST)currentAST.root;
				template_AST = (org.antlr.stringtemplate.language.StringTemplateAST)astFactory.make( (new ASTArray(2)).add((org.antlr.stringtemplate.language.StringTemplateAST)astFactory.create(TEMPLATE)).add(template_AST));
				currentAST.root = template_AST;
				currentAST.child = template_AST!=null &&template_AST.getFirstChild()!=null ?
					template_AST.getFirstChild() : template_AST;
				currentAST.advanceChildToEnd();
			}
			template_AST = (org.antlr.stringtemplate.language.StringTemplateAST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_3);
			} else {
			  throw ex;
			}
		}
		returnAST = template_AST;
	}
	
	public final void ifAtom() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		org.antlr.stringtemplate.language.StringTemplateAST ifAtom_AST = null;
		
		try {      // for error handling
			expr();
			astFactory.addASTChild(currentAST, returnAST);
			ifAtom_AST = (org.antlr.stringtemplate.language.StringTemplateAST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_2);
			} else {
			  throw ex;
			}
		}
		returnAST = ifAtom_AST;
	}
	
	public final void primaryExpr() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		org.antlr.stringtemplate.language.StringTemplateAST primaryExpr_AST = null;
		
		try {      // for error handling
			if ((LA(1)==ID||LA(1)==STRING||LA(1)==INT) && (_tokenSet_4.member(LA(2)))) {
				atom();
				astFactory.addASTChild(currentAST, returnAST);
				{
				_loop17:
				do {
					if ((LA(1)==DOT)) {
						org.antlr.stringtemplate.language.StringTemplateAST tmp10_AST = null;
						tmp10_AST = (org.antlr.stringtemplate.language.StringTemplateAST)astFactory.create(LT(1));
						astFactory.makeASTRoot(currentAST, tmp10_AST);
						match(DOT);
						{
						switch ( LA(1)) {
						case ID:
						{
							org.antlr.stringtemplate.language.StringTemplateAST tmp11_AST = null;
							tmp11_AST = (org.antlr.stringtemplate.language.StringTemplateAST)astFactory.create(LT(1));
							astFactory.addASTChild(currentAST, tmp11_AST);
							match(ID);
							break;
						}
						case LPAREN:
						{
							valueExpr();
							astFactory.addASTChild(currentAST, returnAST);
							break;
						}
						default:
						{
							throw new NoViableAltException(LT(1), getFilename());
						}
						}
						}
					}
					else {
						break _loop17;
					}
					
				} while (true);
				}
				primaryExpr_AST = (org.antlr.stringtemplate.language.StringTemplateAST)currentAST.root;
			}
			else {
				boolean synPredMatched19 = false;
				if (((LA(1)==LPAREN||LA(1)==ID||LA(1)==LITERAL_super) && (_tokenSet_5.member(LA(2))))) {
					int _m19 = mark();
					synPredMatched19 = true;
					inputState.guessing++;
					try {
						{
						templateInclude();
						}
					}
					catch (RecognitionException pe) {
						synPredMatched19 = false;
					}
					rewind(_m19);
					inputState.guessing--;
				}
				if ( synPredMatched19 ) {
					templateInclude();
					astFactory.addASTChild(currentAST, returnAST);
					primaryExpr_AST = (org.antlr.stringtemplate.language.StringTemplateAST)currentAST.root;
				}
				else if ((LA(1)==LPAREN) && (_tokenSet_6.member(LA(2)))) {
					valueExpr();
					astFactory.addASTChild(currentAST, returnAST);
					primaryExpr_AST = (org.antlr.stringtemplate.language.StringTemplateAST)currentAST.root;
				}
				else {
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
			}
			catch (RecognitionException ex) {
				if (inputState.guessing==0) {
					reportError(ex);
					recover(ex,_tokenSet_7);
				} else {
				  throw ex;
				}
			}
			returnAST = primaryExpr_AST;
		}
		
	public final void atom() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		org.antlr.stringtemplate.language.StringTemplateAST atom_AST = null;
		
		try {      // for error handling
			switch ( LA(1)) {
			case ID:
			{
				org.antlr.stringtemplate.language.StringTemplateAST tmp12_AST = null;
				tmp12_AST = (org.antlr.stringtemplate.language.StringTemplateAST)astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp12_AST);
				match(ID);
				atom_AST = (org.antlr.stringtemplate.language.StringTemplateAST)currentAST.root;
				break;
			}
			case STRING:
			{
				org.antlr.stringtemplate.language.StringTemplateAST tmp13_AST = null;
				tmp13_AST = (org.antlr.stringtemplate.language.StringTemplateAST)astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp13_AST);
				match(STRING);
				atom_AST = (org.antlr.stringtemplate.language.StringTemplateAST)currentAST.root;
				break;
			}
			case INT:
			{
				org.antlr.stringtemplate.language.StringTemplateAST tmp14_AST = null;
				tmp14_AST = (org.antlr.stringtemplate.language.StringTemplateAST)astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp14_AST);
				match(INT);
				atom_AST = (org.antlr.stringtemplate.language.StringTemplateAST)currentAST.root;
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_4);
			} else {
			  throw ex;
			}
		}
		returnAST = atom_AST;
	}
	
	public final void valueExpr() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		org.antlr.stringtemplate.language.StringTemplateAST valueExpr_AST = null;
		Token  eval = null;
		org.antlr.stringtemplate.language.StringTemplateAST eval_AST = null;
		
		try {      // for error handling
			eval = LT(1);
			eval_AST = (org.antlr.stringtemplate.language.StringTemplateAST)astFactory.create(eval);
			astFactory.makeASTRoot(currentAST, eval_AST);
			match(LPAREN);
			templatesExpr();
			astFactory.addASTChild(currentAST, returnAST);
			match(RPAREN);
			if ( inputState.guessing==0 ) {
				eval_AST.setType(VALUE); eval_AST.setText("value");
			}
			valueExpr_AST = (org.antlr.stringtemplate.language.StringTemplateAST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_4);
			} else {
			  throw ex;
			}
		}
		returnAST = valueExpr_AST;
	}
	
	public final void templateInclude() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		org.antlr.stringtemplate.language.StringTemplateAST templateInclude_AST = null;
		Token  qid = null;
		org.antlr.stringtemplate.language.StringTemplateAST qid_AST = null;
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case ID:
			{
				org.antlr.stringtemplate.language.StringTemplateAST tmp16_AST = null;
				tmp16_AST = (org.antlr.stringtemplate.language.StringTemplateAST)astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp16_AST);
				match(ID);
				argList();
				astFactory.addASTChild(currentAST, returnAST);
				break;
			}
			case LITERAL_super:
			{
				match(LITERAL_super);
				match(DOT);
				qid = LT(1);
				qid_AST = (org.antlr.stringtemplate.language.StringTemplateAST)astFactory.create(qid);
				astFactory.addASTChild(currentAST, qid_AST);
				match(ID);
				if ( inputState.guessing==0 ) {
					qid_AST.setText("super."+qid_AST.getText());
				}
				argList();
				astFactory.addASTChild(currentAST, returnAST);
				break;
			}
			case LPAREN:
			{
				indirectTemplate();
				astFactory.addASTChild(currentAST, returnAST);
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			if ( inputState.guessing==0 ) {
				templateInclude_AST = (org.antlr.stringtemplate.language.StringTemplateAST)currentAST.root;
				templateInclude_AST = (org.antlr.stringtemplate.language.StringTemplateAST)astFactory.make( (new ASTArray(2)).add((org.antlr.stringtemplate.language.StringTemplateAST)astFactory.create(INCLUDE,"include")).add(templateInclude_AST));
				currentAST.root = templateInclude_AST;
				currentAST.child = templateInclude_AST!=null &&templateInclude_AST.getFirstChild()!=null ?
					templateInclude_AST.getFirstChild() : templateInclude_AST;
				currentAST.advanceChildToEnd();
			}
			templateInclude_AST = (org.antlr.stringtemplate.language.StringTemplateAST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_7);
			} else {
			  throw ex;
			}
		}
		returnAST = templateInclude_AST;
	}
	
	public final void nonAlternatingTemplateExpr() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		org.antlr.stringtemplate.language.StringTemplateAST nonAlternatingTemplateExpr_AST = null;
		Token  c = null;
		org.antlr.stringtemplate.language.StringTemplateAST c_AST = null;
		
		try {      // for error handling
			expr();
			astFactory.addASTChild(currentAST, returnAST);
			{
			_loop23:
			do {
				if ((LA(1)==COLON)) {
					c = LT(1);
					c_AST = (org.antlr.stringtemplate.language.StringTemplateAST)astFactory.create(c);
					astFactory.makeASTRoot(currentAST, c_AST);
					match(COLON);
					if ( inputState.guessing==0 ) {
						c_AST.setType(APPLY);
					}
					template();
					astFactory.addASTChild(currentAST, returnAST);
				}
				else {
					break _loop23;
				}
				
			} while (true);
			}
			nonAlternatingTemplateExpr_AST = (org.antlr.stringtemplate.language.StringTemplateAST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_8);
			} else {
			  throw ex;
			}
		}
		returnAST = nonAlternatingTemplateExpr_AST;
	}
	
	public final void namedTemplate() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		org.antlr.stringtemplate.language.StringTemplateAST namedTemplate_AST = null;
		Token  qid = null;
		org.antlr.stringtemplate.language.StringTemplateAST qid_AST = null;
		
		try {      // for error handling
			switch ( LA(1)) {
			case ID:
			{
				org.antlr.stringtemplate.language.StringTemplateAST tmp19_AST = null;
				tmp19_AST = (org.antlr.stringtemplate.language.StringTemplateAST)astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp19_AST);
				match(ID);
				argList();
				astFactory.addASTChild(currentAST, returnAST);
				namedTemplate_AST = (org.antlr.stringtemplate.language.StringTemplateAST)currentAST.root;
				break;
			}
			case LITERAL_super:
			{
				match(LITERAL_super);
				match(DOT);
				qid = LT(1);
				qid_AST = (org.antlr.stringtemplate.language.StringTemplateAST)astFactory.create(qid);
				astFactory.addASTChild(currentAST, qid_AST);
				match(ID);
				if ( inputState.guessing==0 ) {
					qid_AST.setText("super."+qid_AST.getText());
				}
				argList();
				astFactory.addASTChild(currentAST, returnAST);
				namedTemplate_AST = (org.antlr.stringtemplate.language.StringTemplateAST)currentAST.root;
				break;
			}
			case LPAREN:
			{
				indirectTemplate();
				astFactory.addASTChild(currentAST, returnAST);
				namedTemplate_AST = (org.antlr.stringtemplate.language.StringTemplateAST)currentAST.root;
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_3);
			} else {
			  throw ex;
			}
		}
		returnAST = namedTemplate_AST;
	}
	
	public final void anonymousTemplate() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		org.antlr.stringtemplate.language.StringTemplateAST anonymousTemplate_AST = null;
		Token  t = null;
		org.antlr.stringtemplate.language.StringTemplateAST t_AST = null;
		
		try {      // for error handling
			t = LT(1);
			t_AST = (org.antlr.stringtemplate.language.StringTemplateAST)astFactory.create(t);
			astFactory.addASTChild(currentAST, t_AST);
			match(ANONYMOUS_TEMPLATE);
			if ( inputState.guessing==0 ) {
				
				StringTemplate anonymous = new StringTemplate();
				anonymous.setGroup(self.getGroup());
				anonymous.setEnclosingInstance(self);
				anonymous.setTemplate(t.getText());
				t_AST.setStringTemplate(anonymous);
				
			}
			anonymousTemplate_AST = (org.antlr.stringtemplate.language.StringTemplateAST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_3);
			} else {
			  throw ex;
			}
		}
		returnAST = anonymousTemplate_AST;
	}
	
	public final void argList() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		org.antlr.stringtemplate.language.StringTemplateAST argList_AST = null;
		
		try {      // for error handling
			if ((LA(1)==LPAREN) && (LA(2)==RPAREN)) {
				match(LPAREN);
				match(RPAREN);
				if ( inputState.guessing==0 ) {
					argList_AST = (org.antlr.stringtemplate.language.StringTemplateAST)currentAST.root;
					argList_AST = (org.antlr.stringtemplate.language.StringTemplateAST)astFactory.create(ARGS,"ARGS");
					currentAST.root = argList_AST;
					currentAST.child = argList_AST!=null &&argList_AST.getFirstChild()!=null ?
						argList_AST.getFirstChild() : argList_AST;
					currentAST.advanceChildToEnd();
				}
			}
			else if ((LA(1)==LPAREN) && (LA(2)==ID)) {
				match(LPAREN);
				argumentAssignment();
				astFactory.addASTChild(currentAST, returnAST);
				{
				_loop34:
				do {
					if ((LA(1)==COMMA)) {
						match(COMMA);
						argumentAssignment();
						astFactory.addASTChild(currentAST, returnAST);
					}
					else {
						break _loop34;
					}
					
				} while (true);
				}
				match(RPAREN);
				if ( inputState.guessing==0 ) {
					argList_AST = (org.antlr.stringtemplate.language.StringTemplateAST)currentAST.root;
					argList_AST = (org.antlr.stringtemplate.language.StringTemplateAST)astFactory.make( (new ASTArray(2)).add((org.antlr.stringtemplate.language.StringTemplateAST)astFactory.create(ARGS,"ARGS")).add(argList_AST));
					currentAST.root = argList_AST;
					currentAST.child = argList_AST!=null &&argList_AST.getFirstChild()!=null ?
						argList_AST.getFirstChild() : argList_AST;
					currentAST.advanceChildToEnd();
				}
				argList_AST = (org.antlr.stringtemplate.language.StringTemplateAST)currentAST.root;
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_7);
			} else {
			  throw ex;
			}
		}
		returnAST = argList_AST;
	}
	
/** Match (foo)() and (foo+".terse")()
    breaks encapsulation
 */
	public final void indirectTemplate() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		org.antlr.stringtemplate.language.StringTemplateAST indirectTemplate_AST = null;
		org.antlr.stringtemplate.language.StringTemplateAST e_AST = null;
		org.antlr.stringtemplate.language.StringTemplateAST args_AST = null;
		
		try {      // for error handling
			org.antlr.stringtemplate.language.StringTemplateAST tmp27_AST = null;
			tmp27_AST = (org.antlr.stringtemplate.language.StringTemplateAST)astFactory.create(LT(1));
			match(LPAREN);
			expr();
			e_AST = (org.antlr.stringtemplate.language.StringTemplateAST)returnAST;
			org.antlr.stringtemplate.language.StringTemplateAST tmp28_AST = null;
			tmp28_AST = (org.antlr.stringtemplate.language.StringTemplateAST)astFactory.create(LT(1));
			match(RPAREN);
			argList();
			args_AST = (org.antlr.stringtemplate.language.StringTemplateAST)returnAST;
			if ( inputState.guessing==0 ) {
				indirectTemplate_AST = (org.antlr.stringtemplate.language.StringTemplateAST)currentAST.root;
				indirectTemplate_AST = (org.antlr.stringtemplate.language.StringTemplateAST)astFactory.make( (new ASTArray(3)).add((org.antlr.stringtemplate.language.StringTemplateAST)astFactory.create(VALUE,"value")).add(e_AST).add(args_AST));
				currentAST.root = indirectTemplate_AST;
				currentAST.child = indirectTemplate_AST!=null &&indirectTemplate_AST.getFirstChild()!=null ?
					indirectTemplate_AST.getFirstChild() : indirectTemplate_AST;
				currentAST.advanceChildToEnd();
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_7);
			} else {
			  throw ex;
			}
		}
		returnAST = indirectTemplate_AST;
	}
	
	public final void argumentAssignment() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		org.antlr.stringtemplate.language.StringTemplateAST argumentAssignment_AST = null;
		
		try {      // for error handling
			org.antlr.stringtemplate.language.StringTemplateAST tmp29_AST = null;
			tmp29_AST = (org.antlr.stringtemplate.language.StringTemplateAST)astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp29_AST);
			match(ID);
			org.antlr.stringtemplate.language.StringTemplateAST tmp30_AST = null;
			tmp30_AST = (org.antlr.stringtemplate.language.StringTemplateAST)astFactory.create(LT(1));
			astFactory.makeASTRoot(currentAST, tmp30_AST);
			match(ASSIGN);
			nonAlternatingTemplateExpr();
			astFactory.addASTChild(currentAST, returnAST);
			argumentAssignment_AST = (org.antlr.stringtemplate.language.StringTemplateAST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_8);
			} else {
			  throw ex;
			}
		}
		returnAST = argumentAssignment_AST;
	}
	
	
	public static final String[] _tokenNames = {
		"<0>",
		"EOF",
		"<2>",
		"NULL_TREE_LOOKAHEAD",
		"APPLY",
		"ARGS",
		"INCLUDE",
		"\"if\"",
		"VALUE",
		"TEMPLATE",
		"SEMI",
		"LPAREN",
		"RPAREN",
		"\"separator\"",
		"ASSIGN",
		"COLON",
		"COMMA",
		"NOT",
		"PLUS",
		"DOT",
		"ID",
		"\"super\"",
		"ANONYMOUS_TEMPLATE",
		"STRING",
		"INT",
		"NESTED_ANONYMOUS_TEMPLATE",
		"ESC_CHAR",
		"WS"
	};
	
	protected void buildTokenTypeASTClassMap() {
		tokenTypeToASTClassMap=null;
	};
	
	private static final long[] mk_tokenSet_0() {
		long[] data = { 2L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_0 = new BitSet(mk_tokenSet_0());
	private static final long[] mk_tokenSet_1() {
		long[] data = { 5122L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_1 = new BitSet(mk_tokenSet_1());
	private static final long[] mk_tokenSet_2() {
		long[] data = { 4096L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_2 = new BitSet(mk_tokenSet_2());
	private static final long[] mk_tokenSet_3() {
		long[] data = { 103426L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_3 = new BitSet(mk_tokenSet_3());
	private static final long[] mk_tokenSet_4() {
		long[] data = { 889858L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_4 = new BitSet(mk_tokenSet_4());
	private static final long[] mk_tokenSet_5() {
		long[] data = { 28837888L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_5 = new BitSet(mk_tokenSet_5());
	private static final long[] mk_tokenSet_6() {
		long[] data = { 28313600L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_6 = new BitSet(mk_tokenSet_6());
	private static final long[] mk_tokenSet_7() {
		long[] data = { 365570L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_7 = new BitSet(mk_tokenSet_7());
	private static final long[] mk_tokenSet_8() {
		long[] data = { 69632L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_8 = new BitSet(mk_tokenSet_8());
	
	}
