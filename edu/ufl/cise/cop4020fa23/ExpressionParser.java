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

/**
 Expr::=  ConditionalExpr | LogicalOrExpr
 ConditionalExpr ::=  ?  Expr  :  Expr  :  Expr
 LogicalOrExpr ::= LogicalAndExpr (    (   |   |   ||   ) LogicalAndExpr)*
 LogicalAndExpr ::=  ComparisonExpr ( (   &   |  &&   )  ComparisonExpr)*
 ComparisonExpr ::= PowExpr ( (< | > | == | <= | >=) PowExpr)*
 PowExpr ::= AdditiveExpr ** PowExpr |   AdditiveExpr
 AdditiveExpr ::= MultiplicativeExpr ( ( + | -  ) MultiplicativeExpr )*
 MultiplicativeExpr ::= UnaryExpr (( * |  /  |  % ) UnaryExpr)*
 UnaryExpr ::=  ( ! | - | length | width) UnaryExpr  |  UnaryExprPostfix
 UnaryExprPostfix::= PrimaryExpr (PixelSelector | ε ) (ChannelSelector | ε )
 PrimaryExpr ::=STRING_LIT | NUM_LIT |  IDENT | ( Expr ) | Z
 ExpandedPixel
 ChannelSelector ::= : red | : green | : blue
 PixelSelector  ::= [ Expr , Expr ]
 ExpandedPixel ::= [ Expr , Expr , Expr ]
 Dimension  ::=  [ Expr , Expr ]

 */

public class ExpressionParser implements IParser {

	final ILexer lexer;
	private IToken t;


	/**
	 * @param lexer
	 * @throws LexicalException
	 */
	public ExpressionParser(ILexer lexer) throws LexicalException {
		super();
		this.lexer = lexer;
		t = lexer.next();
	}

	private boolean isPrimaryExprToken(IToken token) {
		return token.kind() == Kind.NUM_LIT
				|| token.kind() == Kind.STRING_LIT
				|| token.kind() == Kind.BOOLEAN_LIT||token.kind() == Kind.IDENT||token.kind() == Kind.CONST;
	}


	@Override
	public Expr parse() throws PLCCompilerException {
		Expr e = expr();
		return e;
	}

	private void consume() throws PLCCompilerException {
		try {
			t = lexer.next();
		} catch (LexicalException e) {
			throw new LexicalException("No next token");
		}
	}

	//get token and see what function we choose here, if first token is ? we use conditionalExpr. so send it to the function
	//etc etc........but add exceptions help for debug..
	private Expr expr() throws PLCCompilerException {
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