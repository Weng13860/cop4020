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

import edu.ufl.cise.cop4020fa23.Token.*;
import edu.ufl.cise.cop4020fa23.exceptions.LexicalException;

import java.util.Arrays;


public class Lexer implements ILexer {

    String input;
    int previous;
    char[] chars;
    int line = 1;
    int column = 1;
    char ch;

    // states for numerical equations or tokens
    private enum STATE {
        START,
        IN_IDENT,
        HAVE_ZERO,
        HAVE_DOT,
        IN_STRING,
        IN_FLOAT, IN_NUM, HAVE_EQ, HAVE_MINUS, HAVE_BITand
    }

    // creates an array of chars from input for easier access to tokens
    public Lexer(String input) {
        chars = input.toCharArray();
        previous = 0;
        ch = (chars.length > 0) ? chars[previous] : '\0';
    }

    /*
     * loop through input, parsing by space
     * assess each token as a type
     * if it has a space, split
     *
     */

    // ensures that number is within integer range
    public static void validateNumLit(String numStr) throws LexicalException {
        try {
            Integer.parseInt(numStr);
        } catch (NumberFormatException e) {
            throw new LexicalException();
        }
    }
    // used to go through tokens one at a time
    @Override
    public IToken next() throws LexicalException {
        STATE state = STATE.START;
        int pos = 0;

        while (true) {

            switch (state) {
                case START -> {
                    previous = pos;
                    switch (ch) {
                        case '\0' -> {
                            return new Token(EOF, previous, 0, null, new SourceLocation(line, column));
                        }
                        case ' ', '\t', '\r' -> {
                            pos++;
                            column++;
                        }
                        case '\n' -> {
                            line++;
                            column = 1;
                        }
                        case '-' -> {
                            state = STATE.HAVE_MINUS;
                            pos++;
                            column++;
                            ch = chars[pos];
                        }
                        case '&' -> {
                            pos++;
                            column++;
                            state = STATE.HAVE_BITand;
                        }
                        case '\"' -> {
                            state = STATE.IN_STRING;
                            ch = (pos < chars.length) ? chars[pos] : '\0';
                            pos++;
                        }
                        case 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '_', '$', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z' -> {
                            state = STATE.IN_IDENT;
                            pos++;
                            column++;
                        }
//                        case '0' -> {
//                            state = STATE.HAVE_ZERO;
//                            pos++;
//                            column++;
//                        }
                        case '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                            state = STATE.IN_NUM;
                            //previous = pos;
                            pos++;
                            column++;
                        }
                    }
                }
                case IN_IDENT -> {
                    while (Character.isLetterOrDigit(ch) || ch == '_') {
                        pos++;
                        if (pos < chars.length) {
                            ch = chars[pos];
                        } else {
                            ch = '\0';
                        }
                    }
                    String identStr = new String(chars, previous, pos - previous);
                    char[] source = Arrays.copyOfRange(chars, previous, pos);
                    return new Token(IDENT, previous, pos - previous, source, new SourceLocation(line, column));
//                    pos++;
//                    char[] source = Arrays.copyOfRange(chars, previous, pos);
//                    return new Token(IDENT, previous, pos - previous, source, new SourceLocation(line, column));
                }
                case IN_STRING -> {
                    while (ch != '\"' && pos < chars.length) {
                        pos++;
                        ch = (pos < chars.length) ? chars[pos] : '\0';
                    }
                    if (ch == '\"') {
                        pos++;  // This line moves past the closing "
                        char[] source = Arrays.copyOfRange(chars, previous, pos);
                        ch = (pos < chars.length) ? chars[pos] : '\0';  // Keep this line to set the next character, but don't skip any characters
                        return new Token(STRING_LIT, previous, pos - previous, source, new SourceLocation(line, column));
                    } else {
                        throw new LexicalException("Unterminated string literal.");
                    }
                }
                // if the number is 0
//                case HAVE_ZERO -> {
//                    char[] source = Arrays.copyOfRange(chars, previous, pos);
//                    return new Token(NUM_LIT, previous, pos-previous, source, new SourceLocation(line, column));
//
//                }


                // if the number is a decimal
                case HAVE_DOT -> {
//                    char[] source = Arrays.copyOfRange(chars, previous, pos);
//                    return new Token(NUM_LIT, previous, pos - previous, source, new SourceLocation(line, column));
                }

                // for multiple digit numbers
                case IN_NUM -> {
                    while (Character.isDigit(ch) && pos < chars.length) {
                        if(ch == '0'){ // If we encounter a 0 while in IN_NUM state, we break to handle it as a single token.
                            break;
                        }
                        pos++;
                        if (pos < chars.length) {
                            ch = chars[pos];
                        }
                    }
                    String numStr = new String(chars, previous, pos - previous);
                    validateNumLit(numStr);
                    char[] source = Arrays.copyOfRange(chars, previous, pos);
                    return new Token(NUM_LIT, previous, pos-previous, source, new SourceLocation(line, column));
                }

                // equation
                case HAVE_MINUS -> {
                    if (ch == '>') {
                        pos++;
                        column++;
                        char[] source = Arrays.copyOfRange(chars, previous, pos);
                        return new Token(RARROW, previous, 2, source, new SourceLocation(line, column));
                    }
                }
                case HAVE_BITand -> {
                    if (ch == '&') {
                        pos++;
                        column++;
                        char[] source = Arrays.copyOfRange(chars, previous, pos);

                        return new Token(AND, previous, 2, source, new SourceLocation(line, column));
                    } else {
                        char[] source = Arrays.copyOfRange(chars, previous, pos);
                        return new Token(BITAND, previous, 1, source, new SourceLocation(line, column));
                    }
                }
                case IN_FLOAT -> {

                }

            }
            pos++;
            if (pos < chars.length) {
                ch = chars[pos];
            } else {
                ch = '\0';
            }
        }

    }


}
