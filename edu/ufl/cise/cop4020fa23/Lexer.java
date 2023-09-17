/*Copyright 2023 by Beverly A Sanders
* 
* This code is provided for solely for use of students in COP4020 Programming Language Concepts at the 
* University of Florida during the fall semester 2023 as part of the course project.  
* 
* No other use is authorized. 
* 
* This code may not be posted on a public web site either during or after the course.  
*/

package edu.ufl.cise.cop4020fa23;

import static edu.ufl.cise.cop4020fa23.Kind.*;
import  edu.ufl.cise.cop4020fa23.Token.*;
import edu.ufl.cise.cop4020fa23.exceptions.LexicalException;

import java.util.Arrays;


public class Lexer implements ILexer {

	String input;
	int previous;
	char[]chars;
	int line=1;
	int column=1;
	char ch;

	// states for numerical equations or tokens
	private enum STATE{
		START,
		IN_IDENT,
		HAVE_ZERO,
		HAVE_DOT,
		IN_FLOAT,IN_NUM,HAVE_EQ,HAVE_MINUS,HAVE_BITand}


	// creates an array of chars from input for easier access to tokens
	public Lexer(String input) {
		chars = input.toCharArray();
		previous =0;
		ch=(chars.length>0)?chars[previous]:'\0';
	}

	/*
	 * loop through input, parsing by space
	 * assess each token as a type
	 * if it has a space, split
	 *
	 */


	// used to go through tokens one at a time
	@Override
	public IToken next() throws LexicalException {

		/*if (previous==0){
			return new Token(IDENT,0,tokens[previous].length(),tokens[previous].toCharArray(),new SourceLocation(1,1));
		}
		else {
			return new Token(determineKind(tokens[previous]),tokens[previous].length()+1,tokens[previous].length(),tokens[previous].toCharArray(),new SourceLocation(1,1));
		}*/
		STATE state=STATE.START;
		int pos=0;

		while(true){

			switch (state) {
				case START -> {
					previous=pos;
					switch (ch) {
						case '\0'->{return new Token(EOF,previous,0,null,new SourceLocation(line,column));}
						case' ','\t','\r'->{pos++;
							column++;}
						case '\n'->{
							line++;
							column=1;}
						case '-'->{
							state=STATE.HAVE_MINUS;
							pos++;
							column++;
							ch=chars[pos];
						}
						case'&'->{
							pos++;
							column++;
							state=STATE.HAVE_BITand;
						}

					}
				}
				case IN_IDENT -> {

				}
				// if the number is 0
				case HAVE_ZERO -> {
					return new Token(NUM_LIT, pos - chars.length - 1, pos - previous, chars[pos], new SourceLocation(line, column));
					// don't increase position bc 0 can only be 0, not 00}
				}
				// if the number is a decimal
				case HAVE_DOT -> {
					return new Token(NUM_LIT, pos - chars.length-1, pos-previous, chars[pos], new SourceLocation(line, column));
					pos++;}
				// for multiple digit numbers
				case IN_NUM -> {}
				// equation
				case HAVE_MINUS -> {
					if(ch=='>') {
						pos++;
						column++;
						char[] source = Arrays.copyOfRange(chars, previous, pos);
						return new Token(RARROW,previous,2,source,new SourceLocation(line,column));}





				}
				case HAVE_BITand -> {
					if(ch=='&') {
						pos++;
						column++;
						char[] source = Arrays.copyOfRange(chars, previous, pos);

						return new Token(AND,previous,2,source,new SourceLocation(line,column));
					}
					else{char[] source = Arrays.copyOfRange(chars, previous, pos);
						return new Token(BITAND,previous,1,source,new SourceLocation(line,column));}
				}
				case IN_FLOAT -> {}

			}
			pos++;
			if(pos<chars.length) {
				ch = chars[pos];
			}
			else{ch='\0';
			}
		}

	}




}
