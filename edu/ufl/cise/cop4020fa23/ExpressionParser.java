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
	public AST parse() throws PLCCompilerException {
		Expr e = expr();
		return e;
	}


	private Expr expr() throws PLCCompilerException {
		IToken firstToken = t;
		if(isPrimaryExprToken(firstToken)){
			return PrimaryExpr();
		}
			else{
				throw new UnsupportedOperationException("THE PARSER HAS NOT BEEN IMPLEMENTED YET");
			}
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
