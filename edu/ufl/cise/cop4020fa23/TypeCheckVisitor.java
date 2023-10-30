package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.ast.*;
import edu.ufl.cise.cop4020fa23.exceptions.PLCCompilerException;
import edu.ufl.cise.cop4020fa23.exceptions.TypeCheckException;

import java.util.*;

public class TypeCheckVisitor implements ASTVisitor {

    // Symbol Table Class
    // k = key, v = value
    public static class SymbolTable {
        private Stack<Integer> scopeStack = new Stack<>();
        private int currentScopeID = 0;
        private Map<String, LinkedList<Entry>> map = new HashMap<>();

        public static class Entry {
            int scopeID;
            NameDef nameDef;
            Entry previous;

            Entry(int scopeID, NameDef nameDef, Entry previous) {
                this.scopeID = scopeID;
                this.nameDef = nameDef;
                this.previous = previous;
            }
        }

        public SymbolTable() {
            enterScope();
        }

        public void enterScope() {
            currentScopeID++;
            scopeStack.push(currentScopeID);
        }

        public void leaveScope() {
            scopeStack.pop();
        }

        public NameDef lookup(String name) {
            LinkedList<Entry> entries = map.get(name);
            if (entries == null) return null;

            for (Entry entry : entries) {
                if (scopeStack.contains(entry.scopeID)) {
                    return entry.nameDef;
                }
            }
            return null;
        }

        public void insert(NameDef nameDef) {
            String name = nameDef.getName();
            Entry entry = new Entry(currentScopeID, nameDef, map.containsKey(name) ? map.get(name).peekFirst() : null);
            map.computeIfAbsent(name, k -> new LinkedList<>()).addFirst(entry);
        }
    }

    private SymbolTable st = new SymbolTable();
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
       else throw new TypeCheckException("Type mismatch in assignment statement. Type 1: " + type1 + ", Type2: " + type2);
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
        // visit expr first
        Expr expr = declaration.getInitializer();
        Type exprType = null;
        if (expr != null) {
            exprType = (Type) expr.visit(this, arg);
        }

        NameDef nameDef = declaration.getNameDef();
        nameDef.visit(this, arg);
        Type nameDefType = nameDef.getType();

        // checking conditions
        if (expr == null || exprType == nameDefType || (exprType == Type.STRING && nameDefType == Type.IMAGE)) {
            // insert to symbol table
            st.insert(nameDef);
            return nameDefType;
        } else {
            throw new TypeCheckException("Type mismatch in declaration: " + declaration);
        }
    }

    // code from PowerPoint
    // edited because we do not have check() function
    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws PLCCompilerException {
        Type typeW = (Type) dimension.getWidth().visit(this, arg);
        if(typeW != Type.INT){
            throw new TypeCheckException("image width must be int");
        }
        Type typeH = (Type) dimension.getHeight().visit(this, arg);
        if(typeH != Type.INT){
            throw new TypeCheckException("image height must be int");
        }
        return dimension;
    }

    @Override
    public Object visitDoStatement(DoStatement doStatement, Object arg) throws PLCCompilerException {
        return null;
    }

    @Override
    public Object visitExpandedPixelExpr(ExpandedPixelExpr expandedPixelExpr, Object arg) throws PLCCompilerException {
        Expr redExpr = expandedPixelExpr.getRed();
        Expr greenExpr = expandedPixelExpr.getGreen();
        Expr blueExpr = expandedPixelExpr.getBlue();

        // validating red, green, and blue expression types
        Type redType = (Type) redExpr.visit(this, arg);
        Type greenType = (Type) greenExpr.visit(this, arg);
        Type blueType = (Type) blueExpr.visit(this, arg);

        if (redType != Type.INT || greenType != Type.INT || blueType != Type.INT) {
            throw new TypeCheckException("Components of an ExpandedPixelExpr must be of type INT");
        }

        // set type pixel
        expandedPixelExpr.setType(Type.PIXEL);
        return Type.PIXEL;
    }

    @Override
    public Object visitGuardedBlock(GuardedBlock guardedBlock, Object arg) throws PLCCompilerException {
        guardedBlock.getGuard().visit(this, arg);
        guardedBlock.getBlock().visit(this, arg);
        return null;
    }

    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws PLCCompilerException {
        NameDef identNameDef = st.lookup(identExpr.getName());

        if (identNameDef == null) {
            throw new TypeCheckException("Undeclared identifier: " + identExpr.getName());
        }

        identExpr.setNameDef(identNameDef);
        identExpr.setType(identNameDef.getType());

        return identNameDef.getType();
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
        Dimension dimension = nameDef.getDimension();
        Type type = nameDef.getType();

        if(dimension != null){
            if(type != Type.IMAGE){
                throw new TypeCheckException("Expected image type for NameDef with a dimension but instead got: " + type);
            }
            nameDef.getDimension().visit(this, arg);
        } else {
            if (!(type == Type.INT || type == Type.BOOLEAN || type == Type.STRING || type == Type.PIXEL || type == Type.IMAGE)) {
                throw new TypeCheckException("Invalid type for NameDef without a dimension: " + type);
            }
        }

        // checking if the variable is already declared in the current scope
        if (st.lookup(nameDef.getName()) != null) {
            throw new TypeCheckException("Variable '" + nameDef.getName() + "' is already declared in the current scope.");
        }

        System.out.println("type in NameDef: " + nameDef.getType());

        // insert the NameDef into the symbol table
        st.insert(nameDef);

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
        System.out.println("in pixsele");
        Expr xExpr = pixelSelector.xExpr();
        Expr yExpr = pixelSelector.yExpr();

        if (xExpr instanceof IdentExpr) {
            IdentExpr identX = (IdentExpr) xExpr;
            identX.visit(this, arg);
            if (st.lookup(identX.getName()) == null) {
                st.insert(identX.getNameDef());
            }
        } else if (!(xExpr instanceof NumLitExpr)) {
            throw new TypeCheckException("ExprxExpr must be an IdentExp or NumLitExpr.");
        }

        if (yExpr instanceof IdentExpr) {
            IdentExpr identY = (IdentExpr) yExpr;
            identY.visit(this, arg);
            if (st.lookup(identY.getName()) == null) {
                st.insert(identY.getNameDef());
            }
        } else if (!(yExpr instanceof NumLitExpr)) {
            throw new TypeCheckException("ExpryExpr must be an IdentExp or NumLitExpr.");
        }

        // validating x and y types
        Type xType = (Type) xExpr.visit(this, arg);
        Type yType = (Type) yExpr.visit(this, arg);

        if (xType != Type.INT || yType != Type.INT) {
            throw new TypeCheckException("PixelSelector coordinates must be of type INT.");
        }

        return Type.PIXEL;
    }


    @Override
    public Object visitPostfixExpr(PostfixExpr postfixExpr, Object arg) throws PLCCompilerException {
        Type exprType = (Type) postfixExpr.primary().visit(this, arg);
        PixelSelector pixel = postfixExpr.pixel();
        ChannelSelector channel = postfixExpr.channel();
        postfixExpr.pixel().visit(this, arg);
        postfixExpr.channel().visit(this, arg);

        System.out.println("pixel: " + pixel + " channel: " + channel + " type: " + exprType);
        if (pixel != null && channel != null) {
            return Type.INT;
        } else if (pixel != null) {
            return Type.PIXEL;
        } else if (channel != null) {
            return Type.IMAGE;
        } else {
            return exprType;
        }
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
