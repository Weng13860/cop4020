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
import edu.ufl.cise.cop4020fa23.ExpressionParser;
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

		consume(); // Assuming the '{' token.
		while (t.kind() != BLOCK_CLOSE) {
			statements.add(statement());
		}
		consume(); // Assuming the '}' token.

		return new Block(t, statements);
	}

	public Statement statement() throws PLCCompilerException {
		// Return statement
		if (t.kind() == RETURN) {
			IToken firstToken = t;
			consume();
			Expr returnExpr = expr();
			return new ReturnStatement(firstToken, returnExpr);
		}
		// Write statement
		else if (t.kind() == RES_write) {
			IToken firstToken = t;
			consume();
			Expr writeExpr = expr();
			return new WriteStatement(firstToken, writeExpr);
		}
		// Do statement
		else if (t.kind() == RES_do) {
			IToken firstToken = t;
			consume();
			List<GuardedBlock> guardedBlocks = new ArrayList<>();
			// You'll need to specify the ending condition for GuardedBlocks based on your grammar.
			// Assuming it ends when it's not a guarded block.
			while (t.kind()!=RES_od) {
				consume();
				GuardedBlock x=guardBlo();
				if(x!=null){
					guardedBlocks.add(x);}
				consume();
				if(t.kind()==BOX){
					consume();
					GuardedBlock y=guardBlo();
					guardedBlocks.add(y);
				}
			}
			return new DoStatement(firstToken, guardedBlocks);
		}
		//If Statement
		else if (t.kind() == RES_if) {

			IToken firstToken = t;
			consume();
			List<GuardedBlock> guardedBlocks = new ArrayList<>();
			// You'll need to specify the ending condition for GuardedBlocks based on your grammar.
			// Assuming it ends when it's not a guarded block.
			while (t.kind()!=RES_fi) {
				consume();
				GuardedBlock x=guardBlo();
				if(x!=null){
					guardedBlocks.add(x);}
				consume();
				if(t.kind()==BOX){
					consume();
					GuardedBlock y=guardBlo();
					guardedBlocks.add(y);
				}
			}
			return new IfStatement(firstToken, guardedBlocks);
		}
		// Assignment statement
		else if (t.kind() == IDENT) {
			IToken firstToken = t;
			LValue lvalue = lv(); // Assuming lv() reads an LValue.
			consume();
			if (t.kind() == ASSIGN) {
				consume();
				Expr rightSide = expr();
				return new AssignmentStatement(firstToken, lvalue, rightSide);
			} else {
				throw new SyntaxException("Expected = after identifier for an assignment statement.");
			}
		}

		throw new SyntaxException("Unknown statement starting with: " + t.text());
	}

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

	public Expr expr() throws PLCCompilerException {
		IToken firstToken = t;
		if(firstToken.kind()==Kind.QUESTION){return ConditionalExpr();}

		else {return LogicalOrExpr();}}
	private Expr ConditionalExpr() throws  PLCCompilerException{
		IToken firstToken = t;
		Expr x=null;
		Expr y=null;
		Expr z=null;
		if(t.kind()!=Kind.QUESTION){
			throw new SyntaxException("No question mark");
		}else{
			consume();
			x=expr();
			if(t.kind()!=Kind.RARROW){
				throw new SyntaxException("No right arrow");
			}
			else {consume();
				y=expr();
				if(t.kind()!=Kind.COMMA){
					throw new SyntaxException("No comma");}
				else{
					consume();
					z=expr();
				}
			}
		}
		return new ConditionalExpr(firstToken,x,y,z);
	}
	private Expr LogicalOrExpr()throws PLCCompilerException{
		IToken firstToken = t;
		Expr x=null;
		Expr y=null;
		x=LogicalAndExpr();
		if(x!=null) {

			while(t.kind()==Kind.BITOR||t.kind()==Kind.OR) {
				IToken op = t;
				consume();
				y = LogicalAndExpr();
				x = new BinaryExpr(firstToken, x, op, y);
			}
			return x;
		}
		return x;

	}
	private Expr LogicalAndExpr()throws PLCCompilerException{
		IToken firstToken = t;
		Expr x=null;
		Expr y=null;
		x=ComparsionExpr();
		if(x!=null) {

			while (t.kind() == Kind.AND || t.kind() == Kind.BITAND) {
				IToken op = t;
				consume();
				y = ComparsionExpr();
				x = new BinaryExpr(firstToken, x, op, y);
			}
			return x;
		}
		return x;

	}
	private Expr ComparsionExpr() throws PLCCompilerException{
		IToken firstToken = t;
		Expr x=null;
		Expr y=null;
		x=PowExpr();
		if(x!=null){

			while(t.kind()==Kind.GT||t.kind()==Kind.GE||t.kind()==Kind.LE||t.kind()==Kind.LT||t.kind()==Kind.EQ){
				IToken op=t;
				consume();
				y=PowExpr();
				return new BinaryExpr(firstToken,x,op,y);
			}
			return x;
		}
		return x;

	}
	private Expr PowExpr()throws PLCCompilerException{
		IToken firstToken = t;
		Expr x=null;
		Expr y=null;
		x=AdditiveExpr();
		if(x!=null){

			while(t.kind()==Kind.EXP){
				IToken op=t;
				consume();
				y=PowExpr();
				return new BinaryExpr(firstToken,x,op,y);
			}
			return x;
		}
		return x;
	}
	private Expr AdditiveExpr()throws PLCCompilerException{
		IToken firstToken = t;
		Expr x=null;
		Expr y=null;
		x=MultiplicativeExpr();
		if(x!=null){
			while(t.kind()==Kind.PLUS||t.kind()==Kind.MINUS){
				IToken op=t;
				consume();
				y=MultiplicativeExpr();
				x= new BinaryExpr(firstToken,x,op,y);

			}
			return x;
		}
		return x;
	}
	private Expr MultiplicativeExpr()throws PLCCompilerException{

		IToken firstToken = t;
		Expr x=null;
		Expr y=null;
		x=UnaryExpr();
		while(t.kind()==Kind.TIMES||t.kind()==Kind.DIV||t.kind()==Kind.MOD){
			IToken op=t;
			consume();
			y=UnaryExpr();
			x=new BinaryExpr(firstToken,x,op,y);

		}
		return x;
	}
	private Expr UnaryExpr() throws PLCCompilerException {
		IToken firstToken = t;
		if (t.kind() == Kind.BANG || t.kind() == Kind.MINUS || t.kind() == Kind.RES_width || t.kind() == Kind.RES_height) {
			consume();
			Expr x = UnaryExpr();
			if (x != null) {
				return new UnaryExpr(firstToken, firstToken, x);
			}
		}
		return PostfixExpr();
	}


	private Expr PostfixExpr()throws PLCCompilerException{
		IToken firstToken=t;
		Expr x=null;
		PixelSelector y=null;
		ChannelSelector z=null;
		x=PrimaryExpr();

		if(x==null){
			return null;
		}
		else {
			consume();
			y=PixelSelector();
			if(y!=null){
				consume();
				if(t.kind()==Kind.COLON){

					z=ChannelSelector();}
				if(z!=null){
					return new PostfixExpr(firstToken,x,y,z);
				}

				else {return new PostfixExpr(firstToken,x,y,null);
				}
			}
			else {
				if(t.kind()==Kind.COLON){

					z=ChannelSelector();}
				if(z!=null){
					return new PostfixExpr(firstToken,x,null,z);
				}
				else return x;
			}
		}
	}
	private ChannelSelector ChannelSelector()throws PLCCompilerException{
		IToken firstToken = t;
		Kind color=null;
		IToken x=null;
		if(t.kind()!=Kind.COLON){throw new SyntaxException("pro ChannelS");}
		else{
			consume();
			if(t.kind()==Kind.RES_green||t.kind()==Kind.RES_red||t.kind()==Kind.RES_blue){
				x=t;
				return new ChannelSelector(firstToken,x);
			}
			else throw new SyntaxException("Channels");
		}
	}



	private Expr ExpandedPixelExpr()throws PLCCompilerException{
		IToken firstToken=t;
		Expr x=null;
		Expr y=null;
		Expr z=null;
		if(t.kind()==Kind.LSQUARE){
			consume();
			x=expr();
			if(t.kind()==Kind.COMMA){
				consume();
				y=expr();
				if(t.kind()==Kind.COMMA){
					consume();
					z=expr();
					if(t.kind()==Kind.RSQUARE){
						return new ExpandedPixelExpr(firstToken,x,y,z);
					}
					else throw new SyntaxException("Missing bracket");
				}
				else return null;
			}
			else return null;
		}
		else return null;
	}
	private PixelSelector PixelSelector() throws PLCCompilerException {
		IToken firstToken = t;
		Expr x = null;
		Expr y = null;

		if (t.kind() == Kind.LSQUARE) {
			consume();
			x = expr();

			if (t.kind() == Kind.COMMA) {
				consume();
				y = expr();

				if (t.kind() == Kind.RSQUARE) {
					return new PixelSelector(firstToken, x, y);
				} else {
					throw new SyntaxException("Missing closing bracket");
				}
			}
		}
		return null;
	}


	private Expr PrimaryExpr() throws PLCCompilerException{
		IToken firstToken = t;
		Expr y;
		switch (t.kind()){
			case NUM_LIT -> {
				y= new NumLitExpr(t);

			}
			case IDENT -> {
				y=new IdentExpr(t);
			}
			case STRING_LIT -> {
				y= new StringLitExpr(t);
			}
			case BOOLEAN_LIT -> {
				y= new BooleanLitExpr(t);
			}
			case LPAREN -> {
				consume();
				Expr x=expr();
				if(x!=null){

					if(t.kind()==Kind.RPAREN){
						return x;
					}
					else throw new SyntaxException("No right parentheses");
				}
				else throw new SyntaxException("Null expression");
			}
			case CONST -> {
				y=new ConstExpr(t);
			}
			case LSQUARE -> {
				return ExpandedPixelExpr();
			}
			default -> {
				throw new SyntaxException("Default primary expression");
			}

		}
		return y;

	}
}
/* Notes
   * program can be: RES_image, RES_pixel, RES_int, RES_string, RES_void, RES_boolean,
   * example: int program()
   * has to have block open and block close
   * tokens outside of block should throw a SyntaxException
*/

