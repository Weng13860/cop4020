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
		} catch (Exception e) {
			System.out.println("nonononono");
		}
	}


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
			throw new SyntaxException("111");
		}else{
			consume();
			x=expr();
			if(t.kind()!=Kind.RARROW){
				throw new SyntaxException("aaa");
			}
			else {consume();
				y=expr();
				if(t.kind()!=Kind.COMMA){
					throw new SyntaxException("bbb");}
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
		while(t.kind()==Kind.BITOR||t.kind()==Kind.OR){
			IToken op=t;
			consume();
			y=LogicalAndExpr();
			x=new BinaryExpr(firstToken,x,op,y);
		}
		return x;

	}
	private Expr LogicalAndExpr()throws PLCCompilerException{
		IToken firstToken = t;
		Expr x=null;
		Expr y=null;
		x=ComparsionExpr();
		while(t.kind()==Kind.BITAND||t.kind()==Kind.AND){
			IToken op=t;
			consume();
			y= ComparsionExpr();
			x=new BinaryExpr(firstToken,x,op,y);
		}
		return x;

	}
	private Expr ComparsionExpr() throws PLCCompilerException{
		IToken firstToken = t;
		Expr x=null;
		Expr y=null;
		x=PowExpr();
		while(t.kind()==Kind.GT||t.kind()==Kind.GE||t.kind()==Kind.LE||t.kind()==Kind.LT||t.kind()==Kind.EQ){
			IToken firstleft=t;
			consume();
			y= PowExpr();
			x=new BinaryExpr(firstToken,x,firstleft,y);
		}
		return x;

	}
	private Expr PowExpr()throws PLCCompilerException{
		IToken firstToken = t;
		Expr x=null;
		Expr y=null;
		x=AdditiveExpr();
		if(x!=null){
			consume();
			if(t.kind()==Kind.EXP){
				consume();
				y=PowExpr();
				return PowExpr();
			}
			else if(t.kind()!=null){
				throw new SyntaxException("powexpr wrong1");
			}
			else{return AdditiveExpr();}
		}
		else throw new SyntaxException("powexpr wrong2");
	}
	private Expr AdditiveExpr()throws PLCCompilerException{
		IToken firstToken = t;
		Expr x=null;
		Expr y=null;
		x=MultiplicativeExpr();
		while(t.kind()==Kind.PLUS||t.kind()==Kind.MINUS){
			IToken op=t;
			consume();
			y=MultiplicativeExpr();
			x=AdditiveExpr();

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
			x=MultiplicativeExpr();

		}
		return x;
	}
	private Expr UnaryExpr()throws PLCCompilerException{
		IToken firstToken = t;
		Expr x=null;
		Expr y=null;
		if(t.kind()!=Kind.BANG||t.kind()!=Kind.MINUS||t.kind()!=Kind.RES_width||t.kind()!=Kind.RES_height){throw new SyntaxException("UnaryExpr p");}
		else {consume();
			x=UnaryExpr();
			if(x!=null){
				return x;}
			else{
				x=PostfixExpr();
				if(x!=null){return x;}
				else {
				throw new SyntaxException("UnartEXpr2");}

			}
		}
	}
	private Expr PostfixExpr()throws PLCCompilerException{
		if
	}
	private Expr ChannelSelector()throws PLCCompilerException{
		IToken firstToken = t;
		IToken color;
		Expr x=null;
		if(t.kind()!=Kind.COLON){throw new SyntaxException("pro ChannelS");}
		else{
			consume();
			if(t.kind()==Kind.RES_green||t.kind()==Kind.RES_red||t.kind()==Kind.RES_blue){
				color=IToken.Kind();


				return ChannelSelector(firstToken,);
			}
		}
	}


	private Expr ExpandedPixelExpr()throws PLCCompilerException{
		IToken firstToken=t;
		Expr x=null;
		Expr y=null;
		Expr z=null;
		if(firstToken.kind()==Kind.LSQUARE){
			consume();
			x=expr();
			if(firstToken.kind()==Kind.COMMA){
				consume();
				y=expr();
				if(firstToken.kind()==Kind.RSQUARE){
					consume();
					return new ExpandedPixelExpr(firstToken,x,y,z);
				}
				else return null;
			}
			else return null;
		}
		else return null;
	}






	public Expr PrimaryExpr() throws PLCCompilerException{
		IToken firstToken = t;
		switch (firstToken.kind()){
			case NUM_LIT -> {
				return new NumLitExpr(firstToken);
			}
			case IDENT -> {
				return new IdentExpr(firstToken);
			}
			case STRING_LIT -> {
				return new StringLitExpr(firstToken);
			}
			default -> { throw new SyntaxException("aaa");
			}
		}

	}

    

}
