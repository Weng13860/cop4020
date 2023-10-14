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
		List<NameDef> params = new ArrayList<>();

		if (isKind(t.kind())) {
			params.add(nameDef());
			while (t.kind() == COMMA) {
				consume();
				params.add(nameDef());
			}
		}

		return params;
	}
	public NameDef nameDef() throws PLCCompilerException{
		IToken typeToken = t;
		consume();
		Dimension dimension = null;
		if (t.kind() == LSQUARE) {
			dimension = Dim();
		}
		if (t.kind() != IDENT) {
			throw new SyntaxException("Expected IDENT token for name definition.");
		}
		IToken identToken = t;
		consume();
		return new NameDef(t, typeToken, dimension, identToken);
	}
	public Dimension Dim() throws PLCCompilerException{
		Expr width = null;
		Expr height = null;

		if (t.kind() != Kind.LSQUARE) {
			throw new SyntaxException("Expected '[' at the start of a dimension.");
		}

		consume();

		// Parse width expression.
		ExpressionParser widthParser = new ExpressionParser(lexer);
		width = widthParser.parse();

		if (t.kind() != Kind.COMMA) {
			throw new SyntaxException("Expected ',' between width and height in the dimension.");
		}

		consume();

		// Parse height expression.
		ExpressionParser heightParser = new ExpressionParser(lexer);
		height = heightParser.parse();

		if (t.kind() != Kind.RSQUARE) {
			throw new SyntaxException("Expected ']' at the end of a dimension.");
		}

		consume();

		return new Dimension(t, width, height);
	}
	public Block block() throws PLCCompilerException{
		List<Block.BlockElem> statements = new ArrayList<>();

		consume();
		while (t.kind() != BLOCK_CLOSE) {
			//statements.add(Block.BlockElem(t));
		}
		consume();

		return new Block(t, statements);
	}
//	public  Statement statement() throws PLCCompilerException{
//		return new Statement();
//	}
	public Declaration decl() throws PLCCompilerException{
		NameDef nameDef = nameDef();
		Expr initializer = null;
		if (t.kind() == ASSIGN) {
			consume();
			ExpressionParser parser = new ExpressionParser(lexer);
			initializer = parser.parse();
		}
		return new Declaration(t, nameDef, initializer);
	}
	public PostfixExpr postfix() throws PLCCompilerException{
		ExpressionParser primaryParser = new ExpressionParser(lexer);
		Expr primary = primaryParser.parse();
		PixelSelector pixel = null;
		ChannelSelector channel = null;
		if (t.kind() == LSQUARE) {
			pixel = pixsele();
		}
		if (t.kind() == COMMA) {
			consume();
			channel = channelsele();
		}
		return new PostfixExpr(t, primary, pixel, channel);
	}
	public LValue lv() throws PLCCompilerException{
		IToken name = t;
		consume();
		PixelSelector pixelSelector = null;
		ChannelSelector channelSelector = null;
		if (t.kind() == LSQUARE) {
			pixelSelector = pixsele();
		}
		if (t.kind() == COMMA) {
			consume();
			channelSelector = channelsele();
		}
		return new LValue(t, name, pixelSelector, channelSelector);
	}
	public ChannelSelector channelsele() throws PLCCompilerException{
		if (t.kind() == IDENT) {
			IToken color = t;
			consume();
			return new ChannelSelector(t, color);
		}
		throw new SyntaxException("Expected IDENT token for channel selector.");
	}
	public PixelSelector pixsele() throws PLCCompilerException{
		consume(); // consume '['
		ExpressionParser parser = new ExpressionParser(lexer);
		Expr xExpr = parser.parse();
		if (t.kind() != COMMA) {
			throw new SyntaxException("Expected ',' in pixel selector.");
		}
		consume();
		Expr yExpr = parser.parse();
		if (t.kind() != RSQUARE) {
			throw new SyntaxException("Expected ']' in pixel selector.");
		}
		consume();
		return new PixelSelector(t, xExpr, yExpr);
	}
	public ExpandedPixelExpr expanpix() throws PLCCompilerException{
		ExpressionParser parser = new ExpressionParser(lexer);
		Expr red = parser.parse();
		consume();
		Expr grn = parser.parse();
		consume();
		Expr blu = parser.parse();
		consume();
		return new ExpandedPixelExpr(t, red, grn, blu);
	}
	public GuardedBlock guardBlo() throws PLCCompilerException{
		ExpressionParser parser = new ExpressionParser(lexer);
		Expr guard = parser.parse();
		consume();
		Block block = block();
		return new GuardedBlock(t, guard, block);
	}
	public StatementBlock blockst() throws PLCCompilerException{
		Block block = block();
		return new StatementBlock(t, block);
	}


	private AST program() throws PLCCompilerException {
		List<IToken> params = new ArrayList<IToken>();
		consume();
		// move to next token only if the token is valid
		if(isKind(t.kind())){
			consume();

			// next token must be ident
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
