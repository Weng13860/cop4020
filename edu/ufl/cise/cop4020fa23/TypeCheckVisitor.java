package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.ast.*;
import edu.ufl.cise.cop4020fa23.exceptions.PLCCompilerException;
import java.util.*;

public class TypeCheckVisitor implements ASTVisitor {

    // Symbol Table Class
    // k = key, v = value
    public static class SymbolTable<K, V> {
        private Stack<HashMap<K, V>> tables;

        public SymbolTable() {
            tables = new Stack<>();
            tables.push(new HashMap<>());
        }

        // searching table
        public V lookup(K key) {
            for (int i = tables.size() - 1; i >= 0; i--) {
                if (tables.get(i).containsKey(key)) {
                    return tables.get(i).get(key);
                }
            }
            // return nothing if key isn't found
            return null;
        }

        public void insert(K key, V value) {
            tables.peek().put(key, value);
        }

        public void enterScope() {
            tables.push(new HashMap<>());
        }

        public void leaveScope() {
            if (tables.size() == 1) {
                throw new IllegalStateException("Cannot leave the global scope");
            }
            tables.pop();
        }
    }

    private SymbolTable<NameDef, Type> st = new SymbolTable<>();
    private Program root;

    // from PowerPoint/Slack
    @Override
    public Object visitProgram(Program program, Object arg) throws PLCCompilerException{
        root = program;
        Type type = Type.kind2type(program.getTypeToken().kind());
        program.setType(type);
        st.enterScope();
        List<NameDef> params = program.getParams();
        for(NameDef param : params){
            param.visit(this, arg);
        }
        program.getBlock().visit(this, arg);
        st.leaveScope();
        return type;
    }
    @Override
    public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws PLCCompilerException {
       Type type1= assignmentStatement.getlValue().getType();
       Expr typea=assignmentStatement.getE();
       Type type2=(Type) typea.visit(this,arg);
       if(AssignmentCompatible(type1,type2)){
           return type2;
           }
       else throw new PLCCompilerException("visttass");
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws PLCCompilerException {
        return null;
    }

    // from PowerPoint
    @Override
    public Object visitBlock(Block block, Object arg) throws PLCCompilerException {
        st.enterScope();
        List<Block.BlockElem> blockElems = block.getElems();
        for (Block.BlockElem elem : blockElems) {
            elem.visit(this, arg);
        }
        st.leaveScope();
        return block;
    }

    @Override
    public Object visitBlockStatement(StatementBlock statementBlock, Object arg) throws PLCCompilerException {
        return null;
    }

    @Override
    public Object visitChannelSelector(ChannelSelector channelSelector, Object arg) throws PLCCompilerException {
        return null;
    }

    @Override
    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws PLCCompilerException {
        return null;
    }

    @Override
    public Object visitDeclaration(Declaration declaration, Object arg) throws PLCCompilerException {
        NameDef nameDef = declaration.getNameDef();
        Type type = declaration.getNameDef().getType();
        System.out.println("NameDef: " + nameDef + "Type: " + type);
        st.insert(nameDef, type);
        return type;
    }

    // code from PowerPoint
    // edited because we do not have check() function
    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws PLCCompilerException {
        Type typeW = (Type) dimension.getWidth().visit(this, arg);
        if(typeW != Type.INT){
            return "image width must be int";
        }
        Type typeH = (Type) dimension.getHeight().visit(this, arg);
        if(typeH != Type.INT){
            return "image height must be int";
        }
        return dimension;
    }

    @Override
    public Object visitDoStatement(DoStatement doStatement, Object arg) throws PLCCompilerException {
        return null;
    }

    @Override
    public Object visitExpandedPixelExpr(ExpandedPixelExpr expandedPixelExpr, Object arg) throws PLCCompilerException {
        return expandedPixelExpr.getType();
    }

    @Override
    public Object visitGuardedBlock(GuardedBlock guardedBlock, Object arg) throws PLCCompilerException {
        guardedBlock.getGuard().visit(this, arg);
        guardedBlock.getBlock().visit(this, arg);
        return null;
    }

    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws PLCCompilerException {
        NameDef nameDef = identExpr.getNameDef();
        Type identType = st.lookup(nameDef);

        if (identType == null) {
            throw new PLCCompilerException("Undeclared identifier: " + identExpr);
        }

        return identType;
    }

    @Override
    public Object visitIfStatement(IfStatement ifStatement, Object arg) throws PLCCompilerException {
        return null;
    }

    @Override
    public Object visitLValue(LValue lValue, Object arg) throws PLCCompilerException {
        return lValue.getType();
    }

    @Override
    public Object visitNameDef(NameDef nameDef, Object arg) throws PLCCompilerException {
        HashMap<NameDef, Type> currentScope = st.tables.peek();
        if (currentScope.containsKey(nameDef)) {
            throw new PLCCompilerException("Variable '" + nameDef + "' is already declared in the current scope.");
        }
        st.insert(nameDef, nameDef.getType());
        return nameDef.getType();
    }

    @Override
    public Object visitNumLitExpr(NumLitExpr numLitExpr, Object arg) throws PLCCompilerException {
        Type type = Type.INT;
        numLitExpr.setType(type);
        return type;
    }

    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws PLCCompilerException {
        return null;
    }

    @Override
    public Object visitPostfixExpr(PostfixExpr postfixExpr, Object arg) throws PLCCompilerException {
        return postfixExpr.visit(this, arg);
    }

    @Override
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws PLCCompilerException {
        return returnStatement.getE().visit(this, arg);
    }

    @Override
    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws PLCCompilerException {
        Type type = Type.STRING;
        stringLitExpr.setType(type);
        return type;
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws PLCCompilerException {
        return unaryExpr.getExpr().visit(this, arg);
    }

    // from PowerPoint/Slack
    @Override
    public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws PLCCompilerException {
        writeStatement.getExpr().visit(this, arg);
        return writeStatement;
    }

    @Override
    public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws PLCCompilerException {
        return Type.BOOLEAN;
    }

    @Override
    public Object visitConstExpr(ConstExpr constExpr, Object arg) throws PLCCompilerException {
        return constExpr.getType();
    }
    public boolean AssignmentCompatible (Type type1,Type type2){
        return type1==type2||(type1==Type.PIXEL&&type2==Type.INT)||(type1==Type.IMAGE&&(type2==Type.PIXEL||type2==Type.INT||type2==Type.STRING));
    }
}
