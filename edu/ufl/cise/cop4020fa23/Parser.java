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

import edu.ufl.cise.cop4020fa23.ast.*;
import edu.ufl.cise.cop4020fa23.exceptions.LexicalException;
import edu.ufl.cise.cop4020fa23.exceptions.PLCCompilerException;
import edu.ufl.cise.cop4020fa23.exceptions.SyntaxException;

import javax.swing.plaf.synth.SynthButtonUI;

import static edu.ufl.cise.cop4020fa23.Kind.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class Parser implements IParser {

	final ILexer lexer;
	private IToken t;

	public Parser(ILexer lexer) throws LexicalException {
		super();
		this.lexer = lexer;
	}

	// moves lexer to next token
	private void consume() throws LexicalException {
		t = lexer.next();
	}

	// checks if token's kind matches valid program identifier
	private boolean isKind(Kind kind){
		if(kind == RES_image || kind == RES_boolean || kind == RES_int || kind == RES_pixel || kind == RES_void || kind == RES_string){
			return true;
		}
		else{
			return false;
		}
	}

	@Override
	public AST parse() throws PLCCompilerException {
		AST e = program();
		return e;
	}

	private AST program() throws PLCCompilerException {
		consume();
		// move to next token only if the token is valid
		if(isKind(t.kind())){
			consume();

			// next token must be "program"
			if(t.text() == "program"){
				consume();

				if(t.kind() == BLOCK_OPEN) {
					consume();

					while (t.kind() != BLOCK_CLOSE) {
						consume();
					}
					consume();
					if (t != null) {
						throw new SyntaxException("Unexpected token after block close");
					}
					// return something
				}
				else{
					throw new SyntaxException("missing block close");
				}
			}
			else{
				throw new SyntaxException("expected \"program\"");
			}
		}
		else{
			throw new SyntaxException("invalid kind before program()");
		}
		throw new UnsupportedOperationException();
	}


/* Notes
   * program can be: RES_image, RES_pixel, RES_int, RES_string, RES_void, RES_boolean,
   * example: int program()
   * has to have block open and block close
   * tokens outside of block should throw a SyntaxException
*/
}
