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
    import java.io.*;
}

/** A parser used to break up a single template into chunks, text literals
 *  and attribute expressions.
 */
class TemplateParser extends Parser;

{
protected StringTemplate self;

public void reportError(RecognitionException e) {
	self.error("template parse error", e);
}
}

template[StringTemplate self]
{
	this.self = self;
}
    :   (   s:LITERAL  {self.addChunk(new StringRef(self,s.getText()));}
        |   nl:NEWLINE
        	{
            if ( LA(1)!=ELSE && LA(1)!=ENDIF ) {
            	self.addChunk(new NewlineRef(self,nl.getText()));
            }
        	}
        |   action[self]
        )*
    ;

action[StringTemplate self]
    :   a:ACTION
        {
        String indent = ((ChunkToken)a).getIndentation();
        ASTExpr c = self.parseAction(a.getText());
        c.setIndentation(indent);
        self.addChunk(c);
        }

    |   i:IF
        {
        ConditionalExpr c = (ConditionalExpr)self.parseAction(i.getText());
        // create and precompile the subtemplate
        StringTemplate subtemplate =
        	new StringTemplate(self.getGroup(), null);
        subtemplate.setEnclosingInstance(self);
        subtemplate.setName(i.getText()+" subtemplate");
        self.addChunk(c);
        }

        template[subtemplate] {if ( c!=null ) c.setSubtemplate(subtemplate);}



        (   ELSE
            {
            // create and precompile the subtemplate
            StringTemplate elseSubtemplate =
         		new StringTemplate(self.getGroup(), null);
            elseSubtemplate.setEnclosingInstance(self);
            elseSubtemplate.setName("else subtemplate");
            }

            template[elseSubtemplate]
            {if ( c!=null ) c.setElseSubtemplate(elseSubtemplate);}
        )?

        ENDIF
    ;

/** Break up an input text stream into chunks of either plain text
 *  or template actions in "$...$".  Treat IF and ENDIF tokens
 *  specially.
 */
class DefaultTemplateLexer extends Lexer;

options {
    k=7; // see "$endif$"
    charVocabulary = '\u0001'..'\uFFFE';
}

{
protected String currentIndent = null;
protected StringTemplate self;

public DefaultTemplateLexer(StringTemplate self, Reader r) {
	this(r);
	this.self = self;
}

public void reportError(RecognitionException e) {
	self.error("template parse error", e);
}

protected boolean upcomingELSE(int i) throws CharStreamException {
 	return LA(i)=='$'&&LA(i+1)=='e'&&LA(i+2)=='l'&&LA(i+3)=='s'&&LA(i+4)=='e'&&
 	       LA(i+5)=='$';
}

protected boolean upcomingENDIF(int i) throws CharStreamException {
	return LA(i)=='$'&&LA(i+1)=='e'&&LA(i+2)=='n'&&LA(i+3)=='d'&&LA(i+4)=='i'&&
	       LA(i+5)=='f'&&LA(i+6)=='$';
}
}


LITERAL
    :   {LA(1)!='\r'&&LA(1)!='\n'}?
        ( options { generateAmbigWarnings=false;}
          {
          int loopStartIndex=text.length();
          int col=getColumn();
          }
        : '\\'! '$'  // allow escaped delimiter
        | '\\' ~'$'  // otherwise ignore escape char
        | ind:INDENT
          {
          if ( col==1 && LA(1)=='$' ) {
              // store indent in ASTExpr not in a literal
              currentIndent=ind.getText();
			  text.setLength(loopStartIndex); // reset length to wack text
          }
          else currentIndent=null;
          }
        | ~('$'|'\r'|'\n')
        )+
        {if (($getText).length()==0) {$setType(Token.SKIP);}} // pure indent?
    ;

NEWLINE
    :	('\r')? '\n' {newline(); currentIndent=null;}
    ;

ACTION
   	options {
   		generateAmbigWarnings=false; // $EXPR$ is ambig with $!..!$
	}
{
    int startCol = getColumn();
}
    :	"$\\n$"! {$setText('\n'); $setType(LITERAL);}
    |	"$\\r$"! {$setText('\r'); $setType(LITERAL);}
    |	"$\\t$"! {$setText('\t'); $setType(LITERAL);}
    |	"$\\ $"! {$setText(' '); $setType(LITERAL);}
    | 	COMMENT {$setType(Token.SKIP);}
    |   (
    	options {
    		generateAmbigWarnings=false; // $EXPR$ is ambig with $endif$ etc...
		}
	:	'$'! "if" (' '!)* "(" (~')')+ ")" '$'! {$setType(TemplateParser.IF);}
        ( ('\r'!)? '\n'! {newline();})? // ignore any newline right after an IF
    |   '$'! "else" '$'!         {$setType(TemplateParser.ELSE);}
        ( ('\r'!)? '\n'! {newline();})? // ignore any newline right after an ELSE
    |   '$'! "endif" '$'!        {$setType(TemplateParser.ENDIF);}
        ( {startCol==1}? ('\r'!)? '\n'! {newline();})? // ignore after ENDIF if on line by itself
    |   '$'! EXPR '$'!  // (Can't start with '!', which would mean comment)
    	)
    	{
        ChunkToken t = new ChunkToken(_ttype, $getText, currentIndent);
        $setToken(t);
    	}
    ;

protected
EXPR:   ( ESC
        | ('\r')? '\n' {newline();}
        | SUBTEMPLATE
        | ~'$'
        )+
    ;

protected
ESC :   '\\' ('$'|'n'|'t'|'\\'|'"'|'\''|':'|'{'|'}')
    ;

protected
SUBTEMPLATE
    :    '{' (SUBTEMPLATE|ESC|~'}')+ '}'
    ;

protected
INDENT
    :   ( options {greedy=true;}: ' ' | '\t')+
    ;

protected
COMMENT
{
    int startCol = getColumn();
}
    :   "$!"
    	(	options {greedy=false;}
    	:	('\r')? '\n' {newline();}
    	|	.
    	)*
    	"!$" ( {startCol==1}? ('\r')? '\n' {newline();} )?
    ;

