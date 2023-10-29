package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.ast.*;
import edu.ufl.cise.cop4020fa23.exceptions.PLCCompilerException;
import java.util.*;

public class TypeCheckVisitor implements ASTVisitor {

    // Symbol Table Class
    // k = key, v = value
    // k, v may be changed once I know what to have as parameters
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
        // right side of statement
        Type rhsType = (Type) assignmentStatement.getE().visit(this, arg);

        // left side of statement
        LValue lhs = assignmentStatement.getlValue();
        Type lhsType = st.lookup(lhs.getNameDef());

        // error if no left side
        if (lhsType == null) {
            throw new PLCCompilerException("Undeclared variable: " + lhs);
        }

        // checking type compatibility
        if (lhsType != rhsType) {
            throw new PLCCompilerException("Type mismatch in assignment. Expected " + lhsType + " but found " + rhsType);
        }

        return rhsType;
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
        return statementBlock.getBlock().visit(this, arg);
    }

    @Override
    public Object visitChannelSelector(ChannelSelector channelSelector, Object arg) throws PLCCompilerException {
        Kind selectedColor = channelSelector.color();

        // make sure valid color
        if (selectedColor != Kind.RES_red && selectedColor != Kind.RES_green && selectedColor != Kind.RES_blue) {
            throw new PLCCompilerException("Invalid channel selected: " + selectedColor);
        }

        return Type.INT;
    }

    @Override
    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws PLCCompilerException {
        return null;
    }

    @Override
    public Object visitDeclaration(Declaration declaration, Object arg) throws PLCCompilerException {
        NameDef nameDef = declaration.getNameDef();
        Type type = declaration.getNameDef().getType();
        st.insert(nameDef, type);
        return type;
    }

    // code from PowerPoint
    // edited because we do not have check() function
    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws PLCCompilerException {
        Type typeW = (Type) dimension.getWidth().visit(this, arg);
        Type typeH = (Type) dimension.getHeight().visit(this, arg);
        if(typeW == Type.INT && typeH == Type.INT){
            return dimension;
        }
        return "incorrect dim";
    }

    @Override
    public Object visitDoStatement(DoStatement doStatement, Object arg) throws PLCCompilerException {
        List<GuardedBlock> guardedBlocks = doStatement.getGuardedBlocks();

        // checking each guarded block
        for (GuardedBlock guardedBlock : guardedBlocks) {
            guardedBlock.visit(this, arg);
        }

        return null;
    }

    @Override
    public Object visitExpandedPixelExpr(ExpandedPixelExpr expandedPixelExpr, Object arg) throws PLCCompilerException {
        return null;
    }

    @Override
    public Object visitGuardedBlock(GuardedBlock guardedBlock, Object arg) throws PLCCompilerException {
        return null;
    }

    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws PLCCompilerException {
        Type identType = st.lookup(identExpr.getNameDef());
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
        return null;
    }

    @Override
    public Object visitNameDef(NameDef nameDef, Object arg) throws PLCCompilerException {
        return null;
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
        return null;
    }

    @Override
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws PLCCompilerException {
        return null;
    }

    @Override
    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws PLCCompilerException {
        return null;
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws PLCCompilerException {
        return null;
    }

    // from PowerPoint/Slack
    @Override
    public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws PLCCompilerException {
        writeStatement.getExpr().visit(this, arg);
        return writeStatement;
    }

    @Override
    public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws PLCCompilerException {
        return null;
    }

    @Override
    public Object visitConstExpr(ConstExpr constExpr, Object arg) throws PLCCompilerException {
        return null;
    }
}
