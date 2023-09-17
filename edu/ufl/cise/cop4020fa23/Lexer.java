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
		IN_FLOAT,IN_NUM,HAVE_EQ,HAVE_MINUS
	}

	// creates an array of chars from input for easier access to tokens
	public Lexer(String input) {
		chars = input.toCharArray();
		previous =0;
		ch=chars[previous];
	}

	// splits tokens based on whitespace, \r, \n, etc.
	public String[] tokenize(String input){
		if(input == null){
			return new String[0];
		}
		return input.split("\\s+");
	}

	// from token, determine what kind of token it is
	public static Kind determineKind(String token) {
		switch(token) {
			case ",":
				return Kind.COMMA;
			case ";":
				return Kind.SEMI;
			case "?":
				return Kind.QUESTION;
			case ":":
				return Kind.COLON;
			case "(":
				return Kind.LPAREN;
			case ")":
				return Kind.RPAREN;
			case "<":
				return Kind.LT;
			case ">":
				return Kind.GT;
			case "[":
				return Kind.LSQUARE;
			case "]":
				return Kind.RSQUARE;
			case "=":
				return Kind.ASSIGN;
			case "==":
				return Kind.EQ;
			case "<=":
				return Kind.LE;
			case ">=":
				return Kind.GE;
			case "!":
				return Kind.BANG;
			case "&":
				return Kind.BITAND;
			case "&&":
				return Kind.AND;
			case "|":
				return Kind.BITOR;
			case "||":
				return Kind.OR;
			case "+":
				return Kind.PLUS;
			case "-":
				return Kind.MINUS;
			case "*":
				return Kind.TIMES;
			case "**":
				return Kind.EXP;
			case "/":
				return Kind.DIV;
			case "%":
				return Kind.MOD;
			case "<:":
				return Kind.BLOCK_OPEN;
			case ":>":
				return Kind.BLOCK_CLOSE;
			case "^":
				return Kind.RETURN;
			case "->":
				return Kind.RARROW;
			case "[]":
				return Kind.BOX;
			default:
				if (token.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
					// Match against all reserved words
					switch(token) {
						case "image":
							return Kind.RES_image;
						case "pixel":
							return Kind.RES_pixel;
						case "int":
							return Kind.RES_int;
						case "string":
							return Kind.RES_string;
						case "void":
							return Kind.RES_void;
						case "boolean":
							return Kind.RES_boolean;
						case "nil":
							return Kind.RES_nil;
						case "write":
							return Kind.RES_write;
						case "height":
							return Kind.RES_height;
						case "width":
							return Kind.RES_width;
						case "if":
							return Kind.RES_if;
						case "fi":
							return Kind.RES_fi;
						case "do":
							return Kind.RES_do;
						case "od":
							return Kind.RES_od;
						case "red":
							return Kind.RES_red;
						case "green":
							return Kind.RES_green;
						case "blue":
							return Kind.RES_blue;
						case "TRUE", "FALSE":
							return Kind.BOOLEAN_LIT;
						case "Z", "BLACK", "BLUE", "CYAN", "DARK_GRAY", "GRAY", "GREEN", "LIGHT_GRAY", "MAGENTA", "ORANGE", "PINK", "RED", "WHITE", "YELLOW":
							return Kind.CONST;
						default:
							return Kind.IDENT;
					}
				} else if (token.matches("^[0-9]+$")) {
					return Kind.NUM_LIT;
				} else if (token.startsWith("\"") && token.endsWith("\"")) {
					return Kind.STRING_LIT;
				}
				break;
		}
		return Kind.ERROR; // Default case
	}


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
					pos=previous;
					switch (ch) {
						case' ','\t','\r'->{pos++;
						column++;}
						case '\n'->{line++;
							column=1;}
						case '-'->{
							state=STATE.HAVE_MINUS;
							pos++;
						column++;
						ch=chars[pos];
						}

					}
				}
				case IN_IDENT -> {

				}
				case HAVE_ZERO -> {}
				case HAVE_DOT -> {}
				case IN_NUM -> {}
				case HAVE_MINUS -> {
					if(ch=='>') {
						return new Token(RARROW,pos- chars.length-1,2,chars[pos],new SourceLocation());
						state=STATE.START;
						pos++;
					column++;
					}
				}
				case HAVE_EQ -> {}
				case IN_FLOAT -> {}

			}
		}

	}




}
