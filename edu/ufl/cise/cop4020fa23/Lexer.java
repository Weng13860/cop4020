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
    int column=0;
    int pos =0;
    char[] chars;
    char ch;
    char[] chs;
    int previous = 0;

    public enum STATE {
        START,
        HAVE_ZERO,
        HAVE_NUMBERSIGN,
        HAVE_BITOR,
        HAVE_COLON,
        HAVE_BITAND,
        HAVE_MINUS,
        HAVE_TIMES,
        HAVE_LSQUARE,
        HAVE_LT,
        HAVE_ASSIGN,
        HAVE_STRING,
        IDENT,
        NUM,
        HAVE_GT,
    }


    public Lexer(String input) {

        this.chars = input.toCharArray();


    }
    public boolean checklineend(char isend){
        if(isend=='\n'){return true;}
        else {return false;}

    }

    // function to identify constants, reserved words, etc
    private Kind getKindForIdent(String ident) {
        switch (ident) {
            case "RED", "BLACK", "Z", "BLUE", "CYAN", "DARK_GRAY", "GRAY", "GREEN", "LIGHT_GRAY", "MAGENTA", "ORANGE", "PINK", "WHITE", "YELLOW": return CONST;
            case "if": return RES_if;
            case "fi": return RES_fi;
            case "od": return RES_od;
            case "do": return RES_do;
            case "red": return RES_red;
            case "blue": return RES_blue;
            case "green": return RES_green;
            case "nil": return RES_nil;
            case "image": return RES_image;
            case "int": return RES_int;
            case "string": return RES_string;
            case "pixel": return RES_pixel;
            case "boolean": return RES_boolean;
            case "void": return RES_void;
            case "width": return RES_width;
            case "height": return RES_height;
            case "write": return RES_write;
            case "FALSE", "TRUE": return BOOLEAN_LIT;
            default: return IDENT;
        }
    }

    @Override
    public IToken next() throws LexicalException {
        //renew position everytime call the next()func.
        int count=0;
        STATE state = STATE.START;

        while (true) {

            count++;
            column++;

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
                            // white space
                            case '\t', ' ' -> {
                                pos++;
                                count--;
                            }
                            // new lines
                            case '\n', '\r' -> {
                                //new line, but remain pos.
                                line++;
                                column = 0;
                                pos++;
                                count--;
                            }
                            // operators
                            case'?'->{
                                char[] source = Arrays.copyOfRange(chars, pos, pos+count);
                                pos++;
                                return new Token(QUESTION, 0, 1, source, new SourceLocation(line, column));

                            }
                            case'('->{
                                char[] source = Arrays.copyOfRange(chars, pos, pos+count);
                                pos++;
                                return new Token(LPAREN, 0, 1, source, new SourceLocation(line, column));
                            }

                           case '#'-> {

                                state = STATE.HAVE_NUMBERSIGN;}
                            case '&' -> {
                                state = STATE.HAVE_BITAND;
                            }
                            case '/'->{
                                char[] source = Arrays.copyOfRange(chars,pos, pos+count);
                                pos++;
                                return new Token(DIV, 0, 1, source, new SourceLocation(line, column));

                            }
                            case'!'->{

                                char[] source = Arrays.copyOfRange(chars, pos, pos+count);
                                pos++;
                                return new Token(BANG, 0, 1, source, new SourceLocation(line, column));}
                            case';'->{

                                char[] source = Arrays.copyOfRange(chars, pos, pos+count);
                                pos++;
                                return new Token(SEMI, 0, 1, source, new SourceLocation(line, column));}
                            case')'->{

                                char[] source = Arrays.copyOfRange(chars, pos, pos+count);
                                pos++;
                                return new Token(RPAREN, 0, 1, source, new SourceLocation(line, column));
                            }
                            case ',' -> {
                                //before return token, need to count pos, get source, and renew previous
                                //if the state is start, no need to renew state.
                                //else need set state as start for next()call.

                                char[] source = Arrays.copyOfRange(chars, pos, pos+count);
                                pos++;

                                return new Token(COMMA,0, 1, source, new SourceLocation(line, column));

                            }
                            case '[' -> {

                                state = STATE.HAVE_LSQUARE;

                            }
                            case ']' -> {

                                char[] source = Arrays.copyOfRange(chars, pos, pos+count);
                                pos++;
                                return new Token(RSQUARE, 0, 1, source, new SourceLocation(line, column));
                            }
                            case '%' -> {

                                char[] source = Arrays.copyOfRange(chars, pos,pos+count);
                                pos++;
                                return new Token(MOD, 0, 1, source, new SourceLocation(line, column));
                            }

                            case '+' -> {
                                char[] source = Arrays.copyOfRange(chars, pos,pos+count);
                                pos++;
                                return new Token(PLUS, 0, 1, source, new SourceLocation(line, column));

                            }
                            case '<'->{

                                state=STATE.HAVE_LT;
                            }

                            case'>'->{
                                state=STATE.HAVE_GT;

                            }
                            case '*'->{
                                state=STATE.HAVE_TIMES;

                            }
                            case '^'->{
                                char[] source = Arrays.copyOfRange(chars, pos,pos+count);
                                pos++;
                                return new Token(RETURN, 0, 1, source, new SourceLocation(line, column));
                            }
                            case'='->{
                                state=STATE.HAVE_ASSIGN;
                            }
                            case'-'->{
                                state=STATE.HAVE_MINUS;
                            }
                            case '"'->{
                                state=STATE.HAVE_STRING;
                            }
                            // identifiers, reserved words
                            case 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r','_', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'->{
                                state=STATE.IDENT;
                            }
                            // digits
                            case '1', '2', '3', '4', '5', '6', '7', '8', '9'->{
                                state=STATE.NUM;
                            }
                            // numbers cannot start with 0
                            case '0'->{
                                state=STATE.HAVE_ZERO;
                                char[] source = Arrays.copyOfRange(chars, pos,pos+count);
                                pos++;
                                return new Token(NUM_LIT, 0, 1, source, new SourceLocation(line, column));
                            }
                            case '@'->{
                                throw new LexicalException("Unrecognized symbol");
                            }
                            case ':'->{
                                state=STATE.HAVE_COLON;
                            }
                            case '|'->{
                                state=STATE.HAVE_BITOR;
                            }

                        }


                    }

                    // check for block close, else colon
                    case HAVE_COLON -> {
                        ch=chars[pos+1];
                        if(ch=='>'){
                            column++;
                            char[] source = Arrays.copyOfRange(chars, pos, pos+2);
                            pos=pos+2;
                            return new Token(BLOCK_CLOSE,0,2,source,new SourceLocation(line,column));
                        }
                        else{

                            char[] source = Arrays.copyOfRange(chars, pos,pos+count);
                            pos++;
                            return new Token(COLON, 0, 1, source, new SourceLocation(line, column));}


                    }

                    case HAVE_BITOR -> {
                        ch=chars[pos+1];
                        if(ch=='|'){
                            column++;
                            char[] source = Arrays.copyOfRange(chars, pos, pos+2);
                            pos=pos+2;
                            return new Token(OR,0,2,source,new SourceLocation(line,column));
                        }
                        else{

                            char[] source = Arrays.copyOfRange(chars, pos,pos+count);
                            pos++;
                            return new Token(BITOR, 0, 1, source, new SourceLocation(line, column));}

                    }

                    case NUM->{
                        StringBuilder sb = new StringBuilder();
                        while(Character.isDigit(ch)) {
                            sb.append(ch);
                            pos++;
                            column++;
                            ch = (pos < chars.length) ? chars[pos] : '\0';
                        }
                        String numberStr = sb.toString();
                        char[] source = numberStr.toCharArray();

                        // Check if the number exceeds Integer.MAX_VALUE and throw exception
                        try {
                            int value = Integer.parseInt(numberStr);
                        }
                        catch(NumberFormatException e) {
                            // if the number is greater than int limit
                            throw new LexicalException("Num exceeds integer limit at " + new SourceLocation(line, column));
                        }

                        return new Token(NUM_LIT, 0, numberStr.length(), source, new SourceLocation(line, column-numberStr.length()-1));
                    }

                    case IDENT->{
                        column--;
                        StringBuilder sb = new StringBuilder();
                        while (Character.isLetter(ch) || ch == '_' || Character.isDigit(ch)) {  // capture full identifier
                            sb.append(ch);
                            pos++;
                            ch = (pos < chars.length) ? chars[pos] : '\0';
                        }
                        String identStr = sb.toString();
                        column+=identStr.length()-1;
                        Kind kind = getKindForIdent(identStr);  // check if identifier is keyword
                        char[] source = identStr.toCharArray();
                        return new Token(kind, 0, identStr.length(), source, new SourceLocation(line, column-identStr.length()+1));
                    }

                    case HAVE_STRING -> {
                        if(chars[pos+1] == '"'){
                            char[] source = Arrays.copyOfRange(chars, pos, pos+count);
                            return new Token(STRING_LIT, 0, count, source, new SourceLocation(line, column-1));
                        }
                        while(pos + count < chars.length && chars[pos+count] != '"') {
                            count++;
                        }
                        if (pos + count >= chars.length || chars[pos + count] != '"') {
                            throw new LexicalException("Missing punctuation");
                        }
                        count++;
                        char[] source = Arrays.copyOfRange(chars, pos, pos+count);
                        pos = pos + count;
                        return new Token(STRING_LIT, 0, count, source, new SourceLocation(line, column-1));
                    }

                    case HAVE_TIMES -> {
                        ch=chars[pos+1];
                        if (ch == '*') {
                            char[] source = Arrays.copyOfRange(chars, pos, pos+count);
                            pos=pos+count;
                            return new Token(EXP, 0, 2, source, new SourceLocation(line, column));
                        } else {
                            char[] source = Arrays.copyOfRange(chars, pos, pos+count-1);
                            column--;
                            pos++;
                            return new Token(TIMES, 0, 1, source, new SourceLocation(line, column));

                        }
                    }

                    // check for arrow
                    case HAVE_MINUS -> {
                        if(chars[pos+1]=='>'){
                            ch=chars[pos+1];
                            char[] source = Arrays.copyOfRange(chars, pos, pos+count);
                            pos=pos+count;
                            return new Token(RARROW, 0, count, source, new SourceLocation(line, column-1));}

                        else {
                            count--;
                            char[] source = Arrays.copyOfRange(chars, pos,pos+count);
                            pos++;
                            column--;

                            return new Token(MINUS, 0, 1, source, new SourceLocation(line, column));
                        }

                    }

                    // check for double =
                    case HAVE_ASSIGN -> {
                        if(chars[pos+1]=='='){
                            ch=chars[pos+1];
                            char[] source = Arrays.copyOfRange(chars, pos, pos+count);
                            pos=pos+count;
                            return new Token(EQ, 0, count, source, new SourceLocation(line, column-1));}

                        else {
                            count--;
                            char[] source = Arrays.copyOfRange(chars, pos,pos+count);
                            pos++;
                            column--;

                            return new Token(ASSIGN, 0, 1, source, new SourceLocation(line, column));
                        }

                    }

                   case HAVE_NUMBERSIGN -> {
                        ch = (pos++ < chars.length) ? chars[pos++] : '\0';
                        if(ch=='#'){
                            while(ch!='\n'){
                                pos++;
                                ch = (pos < chars.length) ? chars[pos] : '\0';
                                if(ch=='\0'){
                                    return new Token(EOF,pos,0,null,new SourceLocation(line,column));
                                }
                                column++;
                            }
                            line++;
                            column=1;
                            pos++;

                            state=STATE.START;

                        }
                        else{

                            pos++;
                            throw new LexicalException();}
                    }

                    // check for double &
                    case HAVE_BITAND -> {
                        if(chars[pos+1]=='&'){
                                ch=chars[pos+1];
                                char[] source = Arrays.copyOfRange(chars, pos, pos+count);
                                pos=pos+count;
                                return new Token(AND, 0, count, source, new SourceLocation(line, column-1));}

                        else {
                            count--;
                            char[] source = Arrays.copyOfRange(chars, pos,pos+count);
                            pos++;
                            column--;

                            return new Token(BITAND, 0, 1, source, new SourceLocation(line, column));
                        }
                    }

                    // check for box
                    case HAVE_LSQUARE -> {
                        ch=chars[pos+1];
                        if (ch == ']') {
                            char[] source = Arrays.copyOfRange(chars, pos, pos+count);
                            pos=pos+count;
                            return new Token(BOX, 0, 2, source, new SourceLocation(line, column));
                        } else {
                            char[] source = Arrays.copyOfRange(chars, pos, pos+count-1);
                            column--;
                            pos++;
                            return new Token(LSQUARE, 0, 1, source, new SourceLocation(line, column));

                        }
                    }

                    // check for GE
                    case HAVE_GT -> {
                        ch=chars[pos+1];
                        if(ch=='='){
                            column++;
                            char[] source = Arrays.copyOfRange(chars, pos, pos+2);
                            pos=pos+2;
                            return new Token(GE,0,2,source,new SourceLocation(line,column));
                        }
                        else{

                        char[] source = Arrays.copyOfRange(chars, pos,pos+count);
                        pos++;
                        return new Token(GT, 0, 1, source, new SourceLocation(line, column));}
                    }

                    // check for block open, LE
                    case HAVE_LT -> {

                        ch=chars[pos+1];
                       if( ch=='='){
                            column++;
                            char[] source = Arrays.copyOfRange(chars, pos, pos+2);
                            pos=pos+2;
                            return new Token(LE,0,2,source,new SourceLocation(line,column));
                        }
                        else if(ch==':'){
                            column++;
                            char[] source = Arrays.copyOfRange(chars, pos, pos+2);
                            pos=pos+2;
                            return new Token(BLOCK_OPEN,0,2,source,new SourceLocation(line,column));
                        }
                        else {
                            char[] source = Arrays.copyOfRange(chars, pos, pos+1);
                            pos++;
                            return new Token(LT,0,1,source,new SourceLocation(line,column));
                        }
                    }
                }
            }
        }


    }
    }


