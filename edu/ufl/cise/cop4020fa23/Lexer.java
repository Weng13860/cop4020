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
        HAVE_NUMBERSIGN,
        HAVE_BITAND,
        HAVE_MINUS,
        HAVE_LSQUARE,
        HAVE_LT,
        HAVE_ASSIGN,
        IDENT

    }


    public Lexer(String input) {

        this.chars = input.toCharArray();


    }
    public boolean checklineend(char isend){
        if(isend=='\n'){return true;}
        else {return false;}

    }

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

                            case '\t', ' ' -> {
                                // whitespace still work in this loop but need move position and col,but since it does not count as any type of result we need
                                //move previous also
                                pos++;
                                count--;
                            }
                            case '\n' -> {
                                //new line, but remain pos.
                                line++;
                                column = 0;
                                pos++;
                                count--;



                            }
                            case'?'->{
                                char[] source = Arrays.copyOfRange(chars, pos, pos+count);
                                pos++;
                                return new Token(QUESTION, 0, 1, source, new SourceLocation(line, column));

                            }
                            case'{'->{
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
                            case'}'->{

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
                                char[] source = Arrays.copyOfRange(chars, pos,pos+count);
                                pos++;
                                return new Token(GT, 0, 1, source, new SourceLocation(line, column));
                            }
                            case'='->{
                                state=STATE.HAVE_ASSIGN;
                            }
                            case'-'->{
                                state=STATE.HAVE_MINUS;
                            }
                            case 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'->{
                                state=STATE.IDENT;
                            }
                        }


                    }
                    case IDENT->{
                        StringBuilder sb = new StringBuilder();
                        while (Character.isLetter(ch) || ch == '_') {  // capture full identifier
                            sb.append(ch);
                            pos++;
                            ch = (pos < chars.length) ? chars[pos] : '\0';
                        }
                        String identStr = sb.toString();
                        Kind kind = getKindForIdent(identStr);  // check if identifier is keyword
                        char[] source = identStr.toCharArray();
                        return new Token(kind, 0, identStr.length(), source, new SourceLocation(line, column - identStr.length() + 1));
                    }
                    case HAVE_MINUS -> {
                        if(chars[pos+1]=='>'){
                            ch=chars[pos+1];
                            char[] source = Arrays.copyOfRange(chars, pos, pos+count);
                            pos=pos+count;
                            //char []newSource=new char[source.length+1];
                            // System.arraycopy(source, 0, newSource, 0, source.length);
                            // newSource[source.length] = '\n';
                            //source = newSource;
                            return new Token(RARROW, 0, count, source, new SourceLocation(line, column-1));}

                        else {
                            count--;
                            char[] source = Arrays.copyOfRange(chars, pos,pos+count);
                            pos++;
                            column--;

                            return new Token(MINUS, 0, 1, source, new SourceLocation(line, column));
                        }

                    }
                    case HAVE_ASSIGN -> {
                        if(chars[pos+1]=='='){
                            ch=chars[pos+1];
                            char[] source = Arrays.copyOfRange(chars, pos, pos+count);
                            pos=pos+count;
                            //char []newSource=new char[source.length+1];
                            // System.arraycopy(source, 0, newSource, 0, source.length);
                            // newSource[source.length] = '\n';
                            //source = newSource;
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
                    case HAVE_BITAND -> {
                        if(chars[pos+1]=='&'){
                                ch=chars[pos+1];
                                char[] source = Arrays.copyOfRange(chars, pos, pos+count);
                                pos=pos+count;
                                //char []newSource=new char[source.length+1];
                               // System.arraycopy(source, 0, newSource, 0, source.length);
                               // newSource[source.length] = '\n';
                                //source = newSource;
                                return new Token(AND, 0, count, source, new SourceLocation(line, column-1));}

                        else {
                            count--;
                            char[] source = Arrays.copyOfRange(chars, pos,pos+count);
                            pos++;
                            column--;

                            return new Token(BITAND, 0, 1, source, new SourceLocation(line, column));
                        }
                    }
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
                    case HAVE_LT -> {

                        ch=chars[pos+1];
                        if(ch=='<'){
                            column++;
                            ch=chars[pos+2];
                            if(ch==':'){
                                char[] source = Arrays.copyOfRange(chars, pos, pos+1);

                                pos=pos+1;
                                return new Token(LT, 0, 1, source, new SourceLocation(line, column));
                            }
                            else if(ch=='='){
                                char[] source = Arrays.copyOfRange(chars, pos, pos+1);
                                pos=pos+1;
                                return new Token(LT,0,1,source,new SourceLocation(line,column));
                            }
                            else{
                                char[] source = Arrays.copyOfRange(chars, pos, pos+1);
                                pos=pos+1;
                                return new Token(LT,0,1,source,new SourceLocation(line,column));
                            }
                        }
                        else if( ch=='='){
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
                            return new Token(LT,0,1,source,new SourceLocation(line,column));}
                    }


                }
            }
        }


    }
    }


