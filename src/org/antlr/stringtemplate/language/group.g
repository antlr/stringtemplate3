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
class GroupParser extends Parser;

options {
    k=3;
}

{
protected StringTemplateGroup group;

public void reportError(RecognitionException e) {
	group.error("template parse error", e);
}
}

group[StringTemplateGroup g]
	:	"group" name:ID {g.setName(name.getText());} SEMI
	    (template[g])*
    ;

template[StringTemplateGroup g]
{
    Map formalArgs = null;
    StringTemplate st = null;
    boolean ignore = false;
}
	:	name:ID
	    {
	    if ( g.isDefinedInThisGroup(name.getText()) ) {
	        g.error("redefinition of template: "+name.getText());
	        st = new StringTemplate(); // create bogus template to fill in
	    }
	    else {
	        st = g.defineTemplate(name.getText(), null);
	    }
	    }
	    LPAREN
	        (args[st]|{st.defineEmptyFormalArgumentList();})
	    RPAREN
	    DEFINED_TO_BE t:TEMPLATE {st.setTemplate(t.getText());}

	|   alias:ID DEFINED_TO_BE target:ID
	    {g.defineTemplateAlias(alias.getText(), target.getText());}
	;

args[StringTemplate st]
{
	String defaultValue = null;
	StringTemplate defaultValueST = null;

}
    :	name:ID ( ASSIGN s:TEMPLATE {defaultValue=s.getText();} )?
    	{
    	st.defineFormalArgument(name.getText(), defaultValue);
    	}
        ( COMMA name2:ID ( ASSIGN s2:TEMPLATE {defaultValue=s2.getText();} )?
          {st.defineFormalArgument(name2.getText(), defaultValue);}
        )*
	;

/*
suffix returns [int cardinality=FormalArgument.REQUIRED]
    :   OPTIONAL {cardinality=FormalArgument.OPTIONAL;}
    |   STAR     {cardinality=FormalArgument.ZERO_OR_MORE;}
    |   PLUS     {cardinality=FormalArgument.ONE_OR_MORE;}
	|
    ;
    */

class GroupLexer extends Lexer;

options {
	k=2;
	charVocabulary = '\u0000'..'\uFFFE';
}

ID	:	('a'..'z'|'A'..'Z') ('a'..'z'|'A'..'Z'|'0'..'9'|'-'|'_')*
	;

TEMPLATE
	:	'"'! ( '\\'! '"' | '\\' ~'"' | ~'"' )+ '"'!
	|	"<<"!
	 	(options {greedy=true;}:('\r'!)?'\n'! {newline();})? // consume 1st \n
		(	options {greedy=false;}  // stop when you see the >>
		:	{LA(3)=='>'&&LA(4)=='>'}? '\r'! '\n'! {newline();} // kill last \r\n
		|	{LA(2)=='>'&&LA(3)=='>'}? '\n'! {newline();}       // kill last \n
		|	('\r')? '\n' {newline();}                          // else keep
		|	.
		)*
        ">>"!
	;

LPAREN: '(' ;
RPAREN: ')' ;
COMMA:  ',' ;
DEFINED_TO_BE:  "::=" ;
SEMI:   ';' ;
STAR:   '*' ;
PLUS:   '+' ;
ASSIGN:   '=' ;
OPTIONAL : '?' ;

// Single-line comments
SL_COMMENT
    :   "//"
        (~('\n'|'\r'))* (('\r')? '\n')?
        {$setType(Token.SKIP); newline();}
    ;

// multiple-line comments
ML_COMMENT
    :   "/*"
        (   
            options {
                greedy=false;
            }
        :   ('\r')? '\n'    {newline();}
        |   .
        )*
        "*/"
        {$setType(Token.SKIP);}
    ;

// Whitespace -- ignored
WS  :   (   ' '
        |   '\t'
        |   '\f'
        |   ('\r')? '\n' { newline(); }
        )+
        { $setType(Token.SKIP); }
    ;
