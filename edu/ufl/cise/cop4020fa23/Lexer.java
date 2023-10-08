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

import java.util.Arrays;
import java.util.HashMap;

import edu.ufl.cise.cop4020fa23.exceptions.LexicalException;

/**
 * 
 */
public class Lexer implements ILexer {

	String input;
	char[] inputChars;

	int pos;  //position of next char to consider
	char ch;  // ch = inputChars[pos]
	int line;
	int column;

	public Lexer(String input) {
		this.input = input;
		pos = 0;
		inputChars = Arrays.copyOf(input.toCharArray(), input.length() + 1);
		ch = inputChars[pos];
		line = 1;
		column = 1;
	}

	@Override
	public IToken next() throws LexicalException {
		return scanToken();
	}

//	public ILexer backup() {
//		pos--;
//		ch = inputChars[pos];
//		return this;
//	}

	private enum State {
		START, IN_IDENT, IN_NUM_LIT, IN_STRING, HAVE_AND, HAVE_OR, HAVE_LT, HAVE_GT, HAVE_HASH, HAVE_TIMES, HAVE_MINUS,
		HAVE_COLON, HAVE_EQ, HAVE_LSQUARE, IN_COMMENT;
	}

	void nextChar() {
		++pos;
		++column;
		ch = inputChars[pos];
		if (ch == '\n') {
			line++;
			column = 0;
		}
	}
/*
 * 	RES_image, 
	RES_pixel,
	RES_int,
	RES_string,
	RES_void,
	RES_boolean,
	RES_nil,
	RES_write,
	RES_height,
	RES_width,
	RES_if,
	RES_fi,
	RES_do,
	RES_od,
	RES_red,
	RES_green,
	RES_blue,
	CONST, // Z | BLACK | BLUE | CYAN | DARK_GRAY | GRAY | GREEN | LIGHT_GRAY | MAGENTA | ORANGE | PINK | RED | WHITE | YELLOW
	BOOLEAN_LIT,// TRUE, FALSE
 */
	private static HashMap<String, Kind> reserved;
	static {
		reserved = new HashMap<>();
		reserved.put("image", RES_image);
		reserved.put("pixel", RES_pixel);
		reserved.put("int", RES_int);
		reserved.put("string", RES_string);
		reserved.put("void", RES_void);
		reserved.put("boolean", RES_boolean);
		reserved.put("nil", RES_nil);
		reserved.put("write", RES_write);
		reserved.put("height", RES_height);
		reserved.put("width", RES_width);
		reserved.put("if", RES_if);
		reserved.put("fi", RES_fi);
		reserved.put("do", RES_do);
		reserved.put("od", RES_od);
		reserved.put("red", RES_red);
		reserved.put("green", RES_green);
		reserved.put("blue", RES_blue);
		reserved.put("Z", CONST);
		reserved.put("BLACK", CONST);
		reserved.put("BLUE", CONST);
		reserved.put("CYAN", CONST);
		reserved.put("DARK_GRAY", CONST);
		reserved.put("GRAY", CONST);
		reserved.put("GREEN", CONST);
		reserved.put("LIGHT_GRAY", CONST);
		reserved.put("MAGENTA", CONST);
		reserved.put("ORANGE", CONST);
		reserved.put("PINK", CONST);
		reserved.put("RED", CONST);
		reserved.put("WHITE", CONST);
		reserved.put("YELLOW", CONST);
		reserved.put("TRUE", BOOLEAN_LIT);
		reserved.put("FALSE", BOOLEAN_LIT);
	}
	private static HashMap<Character, Kind> singleCharTokenKinds;
	static {
		singleCharTokenKinds = new HashMap<>();
		singleCharTokenKinds.put('0', NUM_LIT);
//		singleCharTokenKinds.put('.', DOT);
		singleCharTokenKinds.put(';', SEMI);
		singleCharTokenKinds.put('+', PLUS);
		singleCharTokenKinds.put('/', DIV);
		singleCharTokenKinds.put('%', MOD);
//		singleCharTokenKinds.put('{', LCURLY);
//		singleCharTokenKinds.put('}', RCURLY);
		singleCharTokenKinds.put('[', LSQUARE);
		singleCharTokenKinds.put(']', RSQUARE);
		singleCharTokenKinds.put('(', LPAREN);
		singleCharTokenKinds.put(')', RPAREN);
		singleCharTokenKinds.put(',', COMMA);
		singleCharTokenKinds.put('?', QUESTION);
		singleCharTokenKinds.put('!', BANG);
		singleCharTokenKinds.put(';', SEMI);
		singleCharTokenKinds.put('^', RETURN);
	}

	private boolean isPrintable(char ch2) {
		return 32 <= ch2 && ch2 <= 126;
	}

	boolean isIdentifierStart(char ch2) {
		return ('A' <= ch && ch <= 'Z') || ('a' <= ch && ch <= 'z') || (ch == '_');
	}

	boolean isIdentifierPart(char ch2) {
		return isIdentifierStart(ch2) || ('0' <= ch2 && ch2 <= '9');
	}

	private Token scanToken() throws LexicalException {
		State state = State.START;
		int tokenStart = -1;
		SourceLocation loc = null;
		while (true) {
			switch (state) {
			case START -> {// skip over white space, read first char and change state or return accordingly
				tokenStart = pos;
				loc = new SourceLocation(line, column);
				switch (ch) {
				case 0 -> {
					// do not try to read next character, there is none.
					return new Token(EOF, tokenStart, 0, inputChars, loc);
				}
				case ' ', '\n', '\r' -> { // white space, stay in start state
					nextChar();
				}
//				case '0', '.', '+', '/', '%', '{', '}', ']', '(', ')', ',', '?', '!', ';', '^' -> { // single character
																									// tokens
				case '0',  '+', '/', '%',  ']', '(', ')', ',', '?', '!', ';', '^' -> { // single character
				// tokens
					Kind kind = singleCharTokenKinds.get(ch);
					nextChar();
					return new Token(kind, tokenStart, pos - tokenStart, inputChars, loc);
				}
				case '&' -> {
					nextChar();
					state = State.HAVE_AND;
				}
				case '|' -> {
					nextChar();
					state = State.HAVE_OR;
				}
				case '<' -> {
					nextChar();
					state = State.HAVE_LT;
				}
				case '>' -> {
					nextChar();
					state = State.HAVE_GT;
				}
				case '*' -> {
					nextChar();
					state = State.HAVE_TIMES;
				}
				case '-' -> {
					nextChar();
					state = State.HAVE_MINUS;
				}
				case ':' -> {
					nextChar();
					state = State.HAVE_COLON;
				}
				case '=' -> {
					nextChar();
					state = State.HAVE_EQ;
				}
				case '\"' -> {
					nextChar();
					state = State.IN_STRING;
				}
				case '[' -> {
					nextChar();
					state = State.HAVE_LSQUARE;
				}
				case '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
					nextChar();
					state = State.IN_NUM_LIT;
				}
				case '#' -> {
					nextChar();
					state = State.HAVE_HASH;
				}
				default -> {
					if (isIdentifierStart(ch)) {
						nextChar();
						state = State.IN_IDENT;
					} else {
						throw new LexicalException(loc, " illegal character " + ch + " with ascii value " + (int) ch);
					}
				}
				}
			}

			case HAVE_AND -> {
				switch (ch) {
				case '&' -> {
					nextChar();
					return new Token(AND, tokenStart, pos - tokenStart, inputChars, loc);
				}
				default -> {
					return new Token(BITAND, tokenStart, pos - tokenStart, inputChars, loc);
				}
				}
			}
			case HAVE_OR -> {
				switch (ch) {
				case '|' -> {
					nextChar();
					return new Token(OR, tokenStart, pos - tokenStart, inputChars, loc);
				}
				default -> {
					return new Token(BITOR, tokenStart, pos - tokenStart, inputChars, loc);
				}
				}
			}
			case HAVE_LT -> {
				switch (ch) {
				case ':' -> {
					nextChar();
					return new Token(BLOCK_OPEN, tokenStart, pos - tokenStart, inputChars, loc);
				}
				case '=' -> {
					nextChar();
					return new Token(LE, tokenStart, pos - tokenStart, inputChars, loc);
				}
				default -> {
					return new Token(LT, tokenStart, pos - tokenStart, inputChars, loc);
				}
				}
			}
			case HAVE_GT -> {
				switch (ch) {
				case '=' -> {
					nextChar();
					return new Token(GE, tokenStart, pos - tokenStart, inputChars, loc);
				}
				default -> {
					return new Token(GT, tokenStart, pos - tokenStart, inputChars, loc);
				}
				}
			}
			case HAVE_TIMES -> {
				switch (ch) {
				case '*' -> {
					nextChar();
					return new Token(EXP, tokenStart, pos - tokenStart, inputChars, loc);
				}
				default -> {
					return new Token(TIMES, tokenStart, pos - tokenStart, inputChars, loc);
				}
				}
			}
			case HAVE_MINUS -> {
				switch (ch) {
				case '>' -> {
					nextChar();
					return new Token(RARROW, tokenStart, pos - tokenStart, inputChars, loc);
				}
				default -> {
					return new Token(MINUS, tokenStart, pos - tokenStart, inputChars, loc);
				}
				}
			}
			case HAVE_COLON -> {
				switch (ch) {
				case '>' -> {
					nextChar();
					return new Token(BLOCK_CLOSE, tokenStart, pos - tokenStart, inputChars, loc);
				}
				default -> {
					return new Token(COLON, tokenStart, pos - tokenStart, inputChars, loc);
				}
				}
			}
			case HAVE_EQ -> {
				switch (ch) {
				case '=' -> {
					nextChar();
					return new Token(EQ, tokenStart, pos - tokenStart, inputChars, loc);
				}
				default -> {
					return new Token(ASSIGN, tokenStart, pos - tokenStart, inputChars, loc);
				}
				}
			}
			case HAVE_LSQUARE -> {
				switch (ch) {
				case ']' -> {
					nextChar();
					return new Token(BOX, tokenStart, pos - tokenStart, inputChars, loc);
				}
				default -> {
					return new Token(LSQUARE, tokenStart, pos - tokenStart, inputChars, loc);
				}
				}
			}
			case IN_STRING -> {
				switch (ch) {
				case '\"' -> {
					nextChar();
					return new Token(STRING_LIT, tokenStart, pos - tokenStart, inputChars, loc);
				}
				default -> {
					if (!isPrintable(ch))
						throw new LexicalException(loc, "illegal character in String");
					nextChar();
				}
				}
			}
			case IN_NUM_LIT -> {
				switch (ch) {
				case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
					nextChar();
				}
				default -> {
					Token t = new Token(NUM_LIT, tokenStart, pos - tokenStart, inputChars, loc);
					try {
						Integer.parseInt(t.text());
						return t;
					} catch (NumberFormatException e) {

						throw new LexicalException(loc, "numeric literal out of range [" + t.text() + "]");
					}
				}
				}
			}
			case HAVE_HASH -> {
				switch (ch) {
				case '#' -> {
					nextChar();
					state = State.IN_COMMENT;
				}
				default -> {
					throw new LexicalException(loc, "single # not allowed");
				}
				}
			}
			case IN_COMMENT -> {
				if (isPrintable(ch)) {
					nextChar();
				} else {
					// don't create a token here, just return to the start state
					// if this character is not printable, it is either '\r' or '\n' or an illegal
					// character.
					// if it is an illegal character, this will be discovered in the start state.
					state = State.START;
				}
			}
			case IN_IDENT -> {
				if (isIdentifierPart(ch)) {
					nextChar();
				} else {
					String identText = input.substring(tokenStart, pos); // get text of identifier
					Kind reservedKind = reserved.get(identText);
					return new Token(reservedKind == null ? IDENT : reservedKind, tokenStart, pos - tokenStart,
							inputChars, loc);

				}
			}
			}
		}
	}

}
