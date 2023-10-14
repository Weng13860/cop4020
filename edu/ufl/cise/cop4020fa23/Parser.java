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
	public List<NameDef>Param() throws PLCCompilerException{

	}
	public NameDef nameDef() throws PLCCompilerException{
		IToken firstToken = t;
		Dimension x=null;
		if(isKind(t.kind())) {
			consume();
			if(t.kind()==IDENT){

				return new NameDef(firstToken,firstToken,null,t);
			}
			else {
				x=Dim();
				if(x!=null){
					return new NameDef(firstToken,firstToken,x,t);
				}
				else throw new SyntaxException("namedefaa");
			}
		}
		else throw new SyntaxException("namedefbb");
	}
	public Dimension Dim() throws PLCCompilerException{

	}
	public Block block() throws PLCCompilerException{

	}
	public  Statement statement() throws PLCCompilerException{

	}
	public Declaration decl() throws PLCCompilerException{

	}
	public PostfixExpr postfix() throws PLCCompilerException{

	}
	public LValue lv() throws PLCCompilerException{

	}
	public ChannelSelector channelsele() throws PLCCompilerException{

	}
	public PixelSelector pixsele() throws PLCCompilerException{

	}
	public ExpandedPixelExpr expanpix() throws PLCCompilerException{

	}
	public GuardedBlock guardBlo() throws PLCCompilerException{

	}
	public StatementBlock blockst() throws PLCCompilerException{

	}


	private AST program() throws PLCCompilerException {
		List<IToken> params = new ArrayList<IToken>();
		consume();
		// move to next token only if the token is valid
		if(isKind(t.kind())){
			consume();

			// next token must be "program"
			if(t.kind() == IDENT){
				System.out.println("ident: " + t.text());
				consume();

				if(t.kind() == LPAREN) {
					System.out.println("LPAREN: " + t.text());
					consume();
					if(t.kind() == RPAREN){
						consume();
					}
					else{
						while(t.kind() != RPAREN){
							System.out.println("param: " + t.text());
							params.add(t);
							consume();
						}
					}

					if (t.kind() == BLOCK_OPEN) {
						System.out.println("block_open: " + t.text());
						consume();

						while (t.kind() != BLOCK_CLOSE && t != null) {
							consume();
						}
						System.out.println("block_close: " + t.text());

						if (t == null) {
							throw new SyntaxException("missing block close");
						}

						if (lexer.next() != null) {
							throw new SyntaxException("Unexpected token after block close: " + lexer.next().text());
						}

					}
					else {
						throw new SyntaxException("expected block open");
					}
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
