header {
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
}

/** Parse the individual attribute expressions */
class ActionParser extends Parser;
options {
	k = 2;
    buildAST = true;
    ASTLabelType = "org.antlr.stringtemplate.language.StringTemplateAST";
}

tokens {
    APPLY;  // template application
    ARGS;   // subtree is a list of (possibly empty) arguments
    INCLUDE;// isolated template include (no attribute)
    CONDITIONAL="if";
    VALUE;  // used for (foo): #(VALUE foo)
    TEMPLATE;
}

{
    protected StringTemplate self = null;

    public ActionParser(TokenStream lexer, StringTemplate self) {
        this(lexer, 2);
        this.self = self;
    }

	public void reportError(RecognitionException e) {
		self.error("template parse error", e);
	}
}

action returns [Map opts=null]
	:	templatesExpr (SEMI! opts=optionList)?
	|	"if"^ LPAREN! ifCondition RPAREN!
	;

optionList! returns [Map opts=new HashMap()]
    :   "separator" ASSIGN e:expr {opts.put("separator",#e);}
    ;

templatesExpr
    :   expr ( c:COLON^ {#c.setType(APPLY);} template (COMMA! template)* )*
    ;

ifCondition
	:   ifAtom
	|   NOT^ ifAtom
	;

ifAtom
    :   expr
    ;

expr:   primaryExpr (PLUS^ primaryExpr)*
    ;

primaryExpr
    :	atom
    	( DOT^
     	  ( ID
          | valueExpr
          )
     	)*
    |   (templateInclude)=>templateInclude  // (see past parens to arglist)
    |   valueExpr
    ;

valueExpr
	:   eval:LPAREN^ templatesExpr RPAREN!
        {#eval.setType(VALUE); #eval.setText("value");}
    ;

nonAlternatingTemplateExpr
    :   expr ( c:COLON^ {#c.setType(APPLY);} template )*
    ;

template
    :   (   namedTemplate       // foo()
        |   anonymousTemplate   // {foo}
        )
        {#template = #(#[TEMPLATE],template);}
    ;

namedTemplate
	:	ID argList
	|   "super"! DOT! qid:ID {#qid.setText("super."+#qid.getText());} argList
    |   indirectTemplate
	;

anonymousTemplate
	:	t:ANONYMOUS_TEMPLATE
        {
        StringTemplate anonymous = new StringTemplate();
        anonymous.setGroup(self.getGroup());
        anonymous.setEnclosingInstance(self);
        anonymous.setTemplate(t.getText());
        #t.setStringTemplate(anonymous);
        }
	;

atom:   ID
	|	STRING
    |   INT
    ;

templateInclude
	:	(   ID argList
	    |   "super"! DOT! qid:ID {#qid.setText("super."+#qid.getText());} argList
        |   indirectTemplate
	    )
        {#templateInclude = #(#[INCLUDE,"include"], templateInclude);}
    ;

/** Match (foo)() and (foo+".terse")()
    breaks encapsulation
 */
indirectTemplate!
    :   LPAREN e:expr RPAREN args:argList
        {#indirectTemplate = #(#[VALUE,"value"],e,args);}
	;

argList
	:!	LPAREN! RPAREN! {#argList = #[ARGS,"ARGS"];}
	|	LPAREN! argumentAssignment (COMMA! argumentAssignment)* RPAREN!
        {#argList = #(#[ARGS,"ARGS"],#argList);}
	;

argumentAssignment
	:	ID ASSIGN^ nonAlternatingTemplateExpr
	|	DOTDOTDOT
	;


class ActionLexer extends Lexer;

options {
    k=2;
	charVocabulary = '\003'..'\uFFFE';
    testLiterals=false;
}

ID
options {
    testLiterals=true;
}
    :	('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'_'|'/')*
	;

INT :   ('0'..'9')+ ;

STRING
	:	'"'! (ESC_CHAR[true] | ~'"')* '"'!
	;

ANONYMOUS_TEMPLATE
	:	'{'! (ESC_CHAR[false] | NESTED_ANONYMOUS_TEMPLATE | ~'}')* '}'!
	;

protected
NESTED_ANONYMOUS_TEMPLATE
	:	'{' (ESC_CHAR[false] | NESTED_ANONYMOUS_TEMPLATE | ~'}')* '}'
	;

protected
ESC_CHAR[boolean doEscape]
	:	'\\'
		(	options {generateAmbigWarnings=false;} // . ambig with others
		:   'n' {if (doEscape) { $setText("\n"); }}
		|	'r' {if (doEscape) { $setText("\r"); }}
		|	't' {if (doEscape) { $setText("\t"); }}
		|	'b' {if (doEscape) { $setText("\b"); }}
		|	'f' {if (doEscape) { $setText("\f"); }}
		|   c:. {if (doEscape) {$setText(String.valueOf(c));}}
		)
	;

LPAREN : '(' ;
RPAREN : ')' ;
COMMA  : ',' ;
DOT    : '.' ;
ASSIGN : '=' ;
COLON  : ':' ;
PLUS   : '+' ;
SEMI   : ';' ;
NOT	   : '!' ;
DOTDOTDOT : "..." ;

WS	:	(' '|'\t'|'\r'|'\n'{newline();})+ {$setType(Token.SKIP);}
	;

