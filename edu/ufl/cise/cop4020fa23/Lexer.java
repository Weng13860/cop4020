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

import edu.ufl.cise.cop4020fa23.exceptions.LexicalException;
import jdk.jfr.Percentage;

import java.util.Arrays;

public class Lexer implements ILexer {

    String input;

    int line=1;
    int column=1;
    int pos = 0;
    char[] chars;
    char ch;
    char[] chs;
    int previous = 0;

    public enum STATE {
        START,
        HAVE_LSQUARE,

    }

    STATE state = STATE.START;

    public Lexer(String input) {

        this.chars = input.toCharArray();


    }


    @Override
    public IToken next() throws LexicalException {
        //renew position everytime call the next()func.
        pos = previous;


        while (true) {
            //every loop refresh ch since add previous=pos at the end of loop
            ch = (pos < chars.length) ? chars[pos] : '\0';
            //get /0 if it is last ch. and return eof
            if (ch == '\0') {
                return new Token(EOF, 0, 0, null, new SourceLocation(1, 1));
            }
            else {
                //state=start and refreshed ch in this loop
                switch (state) {
                    case START -> {

                        switch (ch) {
                            case '\t', ' ' -> {
                                // whitespace still work in this loop but need move position and col,but since it does not count as any type of result we need
                                //move previous also
                                column++;
                                pos++;
                                previous=pos;

                            }
                            case '\n' -> {
                                //new line, but remain pos.
                                line++;
                                column = 1;
                            }
                            case ',' -> {
                                //before return token, need to count pos, get source, and renew previous
                                //if the state is start, no need to renew state.
                                //else need set state as start for next()call.
                                pos++;
                                char[] source = Arrays.copyOfRange(chars, previous, pos);
                                previous = pos;
                                return new Token(COMMA, pos, 1, source, new SourceLocation(line, column));

                            }
                            case '[' -> {
                                pos++;
                                state = STATE.HAVE_LSQUARE;

                            }
                            case ']' -> {
                                pos++;
                                char[] source = Arrays.copyOfRange(chars, previous, pos);
                                previous = pos;
                                return new Token(RSQUARE, pos, 1, source, new SourceLocation(line, column));
                            }
                            case '%' -> {
                                pos++;
                                char[] source = Arrays.copyOfRange(chars, previous, pos);
                                previous = pos;
                                return new Token(MOD, pos, 1, source, new SourceLocation(line, column));
                            }
                            case '+' -> {
                                pos++;
                                char[] source = Arrays.copyOfRange(chars, previous, pos);
                                previous = pos;
                                return new Token(PLUS, pos, 1, source, new SourceLocation(line, column));
                            }
                        }


                    }
                    case HAVE_LSQUARE -> {
                        ch=chars[pos];

                        if (ch == ']') {
                            pos++;

                            char[] source = Arrays.copyOfRange(chars, previous, pos);
                            previous = pos;
                            state=STATE.START;
                            return new Token(BOX, pos, 2, source, new SourceLocation(line, column));
                        } else {

                            char[] source = Arrays.copyOfRange(chars, previous, pos);
                            previous = pos;
                            state=STATE.START;
                            return new Token(LSQUARE, pos, 1, source, new SourceLocation(line, column));

                        }
                    }


                }
            }
        }


    }
    }


