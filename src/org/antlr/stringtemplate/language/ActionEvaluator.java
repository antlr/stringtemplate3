// $ANTLR 2.7.5rc2 (2005-01-08): "eval.g" -> "ActionEvaluator.java"$

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
import java.io.*;
import java.lang.reflect.*;

import antlr.TreeParser;
import antlr.Token;
import antlr.collections.AST;
import antlr.RecognitionException;
import antlr.ANTLRException;
import antlr.NoViableAltException;
import antlr.MismatchedTokenException;
import antlr.SemanticException;
import antlr.collections.impl.BitSet;
import antlr.ASTPair;
import antlr.collections.impl.ASTArray;


public class ActionEvaluator extends antlr.TreeParser       implements ActionEvaluatorTokenTypes
 {

    public static class NameValuePair {
        public String name;
        public Object value;
    };

    protected StringTemplate self = null;
    protected StringTemplateWriter out = null;
    protected ASTExpr chunk = null;

    /** Create an evaluator using attributes from self */
    public ActionEvaluator(StringTemplate self, ASTExpr chunk, StringTemplateWriter out) {
        this.self = self;
        this.chunk = chunk;
        this.out = out;
    }
 
	public void reportError(RecognitionException e) {
		self.error("template parse error", e);
	}
public ActionEvaluator() {
	tokenNames = _tokenNames;
}

	public final int  action(AST _t) throws RecognitionException {
		int numCharsWritten=0;
		
		org.antlr.stringtemplate.language.StringTemplateAST action_AST_in = (_t == ASTNULL) ? null : (org.antlr.stringtemplate.language.StringTemplateAST)_t;
		
		Object e=null;
		
		
		try {      // for error handling
			e=expr(_t);
			_t = _retTree;
			numCharsWritten = chunk.writeAttribute(self,e,out);
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		_retTree = _t;
		return numCharsWritten;
	}
	
	public final Object  expr(AST _t) throws RecognitionException {
		Object value=null;
		
		org.antlr.stringtemplate.language.StringTemplateAST expr_AST_in = (_t == ASTNULL) ? null : (org.antlr.stringtemplate.language.StringTemplateAST)_t;
		
		Object a=null, b=null, e=null;
		Map argumentContext=null;
		
		
		try {      // for error handling
			if (_t==null) _t=ASTNULL;
			switch ( _t.getType()) {
			case PLUS:
			{
				AST __t3 = _t;
				org.antlr.stringtemplate.language.StringTemplateAST tmp1_AST_in = (org.antlr.stringtemplate.language.StringTemplateAST)_t;
				match(_t,PLUS);
				_t = _t.getFirstChild();
				a=expr(_t);
				_t = _retTree;
				b=expr(_t);
				_t = _retTree;
				value = chunk.add(a,b);
				_t = __t3;
				_t = _t.getNextSibling();
				break;
			}
			case APPLY:
			{
				value=templateApplication(_t);
				_t = _retTree;
				break;
			}
			case DOT:
			case ID:
			case STRING:
			case INT:
			{
				value=attribute(_t);
				_t = _retTree;
				break;
			}
			case INCLUDE:
			{
				value=templateInclude(_t);
				_t = _retTree;
				break;
			}
			case VALUE:
			{
				AST __t4 = _t;
				org.antlr.stringtemplate.language.StringTemplateAST tmp2_AST_in = (org.antlr.stringtemplate.language.StringTemplateAST)_t;
				match(_t,VALUE);
				_t = _t.getFirstChild();
				e=expr(_t);
				_t = _retTree;
				_t = __t4;
				_t = _t.getNextSibling();
				
				StringWriter buf = new StringWriter();
				Class writerClass = out.getClass();
				StringTemplateWriter sw = null;
				try {
				Constructor ctor =
					writerClass.getConstructor(new Class[] {Writer.class});
				sw = (StringTemplateWriter)ctor.newInstance(new Object[] {buf});
				}
				catch (Exception exc) {
					// default new AutoIndentWriter(buf)
					self.error("cannot make implementation of StringTemplateWriter",exc);
					sw = new AutoIndentWriter(buf);
					}
				chunk.writeAttribute(self,e,sw);
				value = buf.toString();
				
				break;
			}
			default:
			{
				throw new NoViableAltException(_t);
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		_retTree = _t;
		return value;
	}
	
/** Apply template(s) to an attribute; can be applied to another apply
 *  result.
 */
	public final Object  templateApplication(AST _t) throws RecognitionException {
		Object value=null;
		
		org.antlr.stringtemplate.language.StringTemplateAST templateApplication_AST_in = (_t == ASTNULL) ? null : (org.antlr.stringtemplate.language.StringTemplateAST)_t;
		
		Object a=null;
		Vector templatesToApply=new Vector();
		
		
		try {      // for error handling
			AST __t10 = _t;
			org.antlr.stringtemplate.language.StringTemplateAST tmp3_AST_in = (org.antlr.stringtemplate.language.StringTemplateAST)_t;
			match(_t,APPLY);
			_t = _t.getFirstChild();
			a=expr(_t);
			_t = _retTree;
			{
			int _cnt12=0;
			_loop12:
			do {
				if (_t==null) _t=ASTNULL;
				if ((_t.getType()==TEMPLATE)) {
					template(_t,templatesToApply);
					_t = _retTree;
				}
				else {
					if ( _cnt12>=1 ) { break _loop12; } else {throw new NoViableAltException(_t);}
				}
				
				_cnt12++;
			} while (true);
			}
			value = chunk.applyListOfAlternatingTemplates(self,a,templatesToApply);
			_t = __t10;
			_t = _t.getNextSibling();
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		_retTree = _t;
		return value;
	}
	
	public final Object  attribute(AST _t) throws RecognitionException {
		Object value=null;
		
		org.antlr.stringtemplate.language.StringTemplateAST attribute_AST_in = (_t == ASTNULL) ? null : (org.antlr.stringtemplate.language.StringTemplateAST)_t;
		org.antlr.stringtemplate.language.StringTemplateAST prop = null;
		org.antlr.stringtemplate.language.StringTemplateAST i3 = null;
		org.antlr.stringtemplate.language.StringTemplateAST i = null;
		org.antlr.stringtemplate.language.StringTemplateAST s = null;
		
		Object obj = null;
		String propName = null;
		Object e = null;
		
		
		try {      // for error handling
			if (_t==null) _t=ASTNULL;
			switch ( _t.getType()) {
			case DOT:
			{
				AST __t21 = _t;
				org.antlr.stringtemplate.language.StringTemplateAST tmp4_AST_in = (org.antlr.stringtemplate.language.StringTemplateAST)_t;
				match(_t,DOT);
				_t = _t.getFirstChild();
				obj=expr(_t);
				_t = _retTree;
				{
				if (_t==null) _t=ASTNULL;
				switch ( _t.getType()) {
				case ID:
				{
					prop = (org.antlr.stringtemplate.language.StringTemplateAST)_t;
					match(_t,ID);
					_t = _t.getNextSibling();
					propName = prop.getText();
					break;
				}
				case VALUE:
				{
					AST __t23 = _t;
					org.antlr.stringtemplate.language.StringTemplateAST tmp5_AST_in = (org.antlr.stringtemplate.language.StringTemplateAST)_t;
					match(_t,VALUE);
					_t = _t.getFirstChild();
					e=expr(_t);
					_t = _retTree;
					_t = __t23;
					_t = _t.getNextSibling();
					if (e!=null) {propName=e.toString();}
					break;
				}
				default:
				{
					throw new NoViableAltException(_t);
				}
				}
				}
				_t = __t21;
				_t = _t.getNextSibling();
				value = chunk.getObjectProperty(self,obj,propName);
				break;
			}
			case ID:
			{
				i3 = (org.antlr.stringtemplate.language.StringTemplateAST)_t;
				match(_t,ID);
				_t = _t.getNextSibling();
				
				try {
				value=self.getAttribute(i3.getText());
				}
				catch (NoSuchElementException nse) {
				// rethrow with more precise error message
				throw new NoSuchElementException(nse.getMessage()+" in template "+self.getName());
				}
				
				break;
			}
			case INT:
			{
				i = (org.antlr.stringtemplate.language.StringTemplateAST)_t;
				match(_t,INT);
				_t = _t.getNextSibling();
				value=new Integer(i.getText());
				break;
			}
			case STRING:
			{
				s = (org.antlr.stringtemplate.language.StringTemplateAST)_t;
				match(_t,STRING);
				_t = _t.getNextSibling();
				value=s.getText();
				break;
			}
			default:
			{
				throw new NoViableAltException(_t);
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		_retTree = _t;
		return value;
	}
	
	public final Object  templateInclude(AST _t) throws RecognitionException {
		Object value=null;
		
		org.antlr.stringtemplate.language.StringTemplateAST templateInclude_AST_in = (_t == ASTNULL) ? null : (org.antlr.stringtemplate.language.StringTemplateAST)_t;
		org.antlr.stringtemplate.language.StringTemplateAST id = null;
		org.antlr.stringtemplate.language.StringTemplateAST a1 = null;
		org.antlr.stringtemplate.language.StringTemplateAST a2 = null;
		
		StringTemplateAST args = null;
		String name = null;
		Object n = null;
		
		
		try {      // for error handling
			AST __t6 = _t;
			org.antlr.stringtemplate.language.StringTemplateAST tmp6_AST_in = (org.antlr.stringtemplate.language.StringTemplateAST)_t;
			match(_t,INCLUDE);
			_t = _t.getFirstChild();
			{
			if (_t==null) _t=ASTNULL;
			switch ( _t.getType()) {
			case ID:
			{
				id = (org.antlr.stringtemplate.language.StringTemplateAST)_t;
				match(_t,ID);
				_t = _t.getNextSibling();
				a1 = (org.antlr.stringtemplate.language.StringTemplateAST)_t;
				if ( _t==null ) throw new MismatchedTokenException();
				_t = _t.getNextSibling();
				name=id.getText(); args=a1;
				break;
			}
			case VALUE:
			{
				AST __t8 = _t;
				org.antlr.stringtemplate.language.StringTemplateAST tmp7_AST_in = (org.antlr.stringtemplate.language.StringTemplateAST)_t;
				match(_t,VALUE);
				_t = _t.getFirstChild();
				n=expr(_t);
				_t = _retTree;
				a2 = (org.antlr.stringtemplate.language.StringTemplateAST)_t;
				if ( _t==null ) throw new MismatchedTokenException();
				_t = _t.getNextSibling();
				_t = __t8;
				_t = _t.getNextSibling();
				if (n!=null) {name=n.toString();} args=a2;
				break;
			}
			default:
			{
				throw new NoViableAltException(_t);
			}
			}
			}
			_t = __t6;
			_t = _t.getNextSibling();
			
			if ( name!=null ) {
				value = chunk.getTemplateInclude(self, name, args);
			}
			
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		_retTree = _t;
		return value;
	}
	
	public final void template(AST _t,
		Vector templatesToApply
	) throws RecognitionException {
		
		org.antlr.stringtemplate.language.StringTemplateAST template_AST_in = (_t == ASTNULL) ? null : (org.antlr.stringtemplate.language.StringTemplateAST)_t;
		org.antlr.stringtemplate.language.StringTemplateAST t = null;
		org.antlr.stringtemplate.language.StringTemplateAST args = null;
		org.antlr.stringtemplate.language.StringTemplateAST anon = null;
		
		Map argumentContext = null;
		Object n = null;
		
		
		try {      // for error handling
			AST __t14 = _t;
			org.antlr.stringtemplate.language.StringTemplateAST tmp8_AST_in = (org.antlr.stringtemplate.language.StringTemplateAST)_t;
			match(_t,TEMPLATE);
			_t = _t.getFirstChild();
			{
			if (_t==null) _t=ASTNULL;
			switch ( _t.getType()) {
			case ID:
			{
				t = (org.antlr.stringtemplate.language.StringTemplateAST)_t;
				match(_t,ID);
				_t = _t.getNextSibling();
				args = (org.antlr.stringtemplate.language.StringTemplateAST)_t;
				if ( _t==null ) throw new MismatchedTokenException();
				_t = _t.getNextSibling();
				
				String templateName = t.getText();
				StringTemplateGroup group = self.getGroup();
				StringTemplate embedded = group.getEmbeddedInstanceOf(self, templateName);
				if ( embedded!=null ) {
				embedded.setArgumentsAST(args);
				templatesToApply.addElement(embedded);
				}
				
				break;
			}
			case ANONYMOUS_TEMPLATE:
			{
				anon = (org.antlr.stringtemplate.language.StringTemplateAST)_t;
				match(_t,ANONYMOUS_TEMPLATE);
				_t = _t.getNextSibling();
				
				StringTemplate anonymous = anon.getStringTemplate();
				templatesToApply.addElement(anonymous);
				
				break;
			}
			case VALUE:
			{
				AST __t16 = _t;
				org.antlr.stringtemplate.language.StringTemplateAST tmp9_AST_in = (org.antlr.stringtemplate.language.StringTemplateAST)_t;
				match(_t,VALUE);
				_t = _t.getFirstChild();
				n=expr(_t);
				_t = _retTree;
				argumentContext=argList(_t,null);
				_t = _retTree;
				_t = __t16;
				_t = _t.getNextSibling();
				
				if ( n!=null ) {
					String templateName = n.toString();
									StringTemplateGroup group = self.getGroup();
									StringTemplate embedded = group.getEmbeddedInstanceOf(self, templateName);
									if ( embedded!=null ) {
										embedded.setArgumentsAST(args);
										templatesToApply.addElement(embedded);
									}
				}
				
				break;
			}
			default:
			{
				throw new NoViableAltException(_t);
			}
			}
			}
			_t = __t14;
			_t = _t.getNextSibling();
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		_retTree = _t;
	}
	
	public final Map  argList(AST _t,
		Map initialContext
	) throws RecognitionException {
		Map argumentContext=null;
		
		org.antlr.stringtemplate.language.StringTemplateAST argList_AST_in = (_t == ASTNULL) ? null : (org.antlr.stringtemplate.language.StringTemplateAST)_t;
		
		argumentContext = initialContext;
		if ( argumentContext==null ) {
		argumentContext=new HashMap();
		}
		
		
		try {      // for error handling
			AST __t25 = _t;
			org.antlr.stringtemplate.language.StringTemplateAST tmp10_AST_in = (org.antlr.stringtemplate.language.StringTemplateAST)_t;
			match(_t,ARGS);
			_t = _t.getFirstChild();
			{
			_loop27:
			do {
				if (_t==null) _t=ASTNULL;
				if ((_t.getType()==ASSIGN)) {
					argumentAssignment(_t,argumentContext);
					_t = _retTree;
				}
				else {
					break _loop27;
				}
				
			} while (true);
			}
			_t = __t25;
			_t = _t.getNextSibling();
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		_retTree = _t;
		return argumentContext;
	}
	
	public final boolean  ifCondition(AST _t) throws RecognitionException {
		boolean value=false;
		
		org.antlr.stringtemplate.language.StringTemplateAST ifCondition_AST_in = (_t == ASTNULL) ? null : (org.antlr.stringtemplate.language.StringTemplateAST)_t;
		
		Object a=null, b=null;
		
		
		try {      // for error handling
			if (_t==null) _t=ASTNULL;
			switch ( _t.getType()) {
			case APPLY:
			case INCLUDE:
			case VALUE:
			case PLUS:
			case DOT:
			case ID:
			case STRING:
			case INT:
			{
				a=ifAtom(_t);
				_t = _retTree;
				value = chunk.testAttributeTrue(a);
				break;
			}
			case NOT:
			{
				AST __t18 = _t;
				org.antlr.stringtemplate.language.StringTemplateAST tmp11_AST_in = (org.antlr.stringtemplate.language.StringTemplateAST)_t;
				match(_t,NOT);
				_t = _t.getFirstChild();
				a=ifAtom(_t);
				_t = _retTree;
				_t = __t18;
				_t = _t.getNextSibling();
				value = !chunk.testAttributeTrue(a);
				break;
			}
			default:
			{
				throw new NoViableAltException(_t);
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		_retTree = _t;
		return value;
	}
	
	public final Object  ifAtom(AST _t) throws RecognitionException {
		Object value=null;
		
		org.antlr.stringtemplate.language.StringTemplateAST ifAtom_AST_in = (_t == ASTNULL) ? null : (org.antlr.stringtemplate.language.StringTemplateAST)_t;
		
		try {      // for error handling
			value=expr(_t);
			_t = _retTree;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		_retTree = _t;
		return value;
	}
	
	public final void argumentAssignment(AST _t,
		Map argumentContext
	) throws RecognitionException {
		
		org.antlr.stringtemplate.language.StringTemplateAST argumentAssignment_AST_in = (_t == ASTNULL) ? null : (org.antlr.stringtemplate.language.StringTemplateAST)_t;
		org.antlr.stringtemplate.language.StringTemplateAST arg = null;
		
		Object e = null;
		
		
		try {      // for error handling
			AST __t29 = _t;
			org.antlr.stringtemplate.language.StringTemplateAST tmp12_AST_in = (org.antlr.stringtemplate.language.StringTemplateAST)_t;
			match(_t,ASSIGN);
			_t = _t.getFirstChild();
			arg = (org.antlr.stringtemplate.language.StringTemplateAST)_t;
			match(_t,ID);
			_t = _t.getNextSibling();
			e=expr(_t);
			_t = _retTree;
			
				       if ( e!=null )
				           self.rawSetAttribute(argumentContext,arg.getText(),e);
				
			_t = __t29;
			_t = _t.getNextSibling();
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		_retTree = _t;
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
	
	}
	
