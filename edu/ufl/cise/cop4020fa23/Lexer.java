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
        pos = previous;


        while (true) {

            ch = (pos < chars.length) ? chars[pos] : '\0';
            if (ch == '\0') {
                return new Token(EOF, 0, 0, null, new SourceLocation(1, 1));
            }
            else {


                switch (state) {
                    case START -> {

                        switch (ch) {
                            case '\t', ' ' -> {
                                column++;
                                pos++;
                                previous=pos;

                            }
                            case '\n' -> {
                                line++;
                                column = 1;
                            }
                            case ',' -> {
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


