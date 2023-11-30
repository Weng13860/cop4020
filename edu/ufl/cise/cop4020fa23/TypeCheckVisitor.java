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
            currentScopeID--;
            int poppedScope = scopeStack.pop();

            // remove entries associated with the popped scope
            map.values().forEach(entries -> entries.removeIf(entry -> entry.scopeID == poppedScope));
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
            Entry entry = new Entry(currentScopeID, nameDef, map.containsKey(name) ? map.get(name).peek() : null);
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
            if(st.lookup(param.getName()) != null){
                throw new TypeCheckException("Variable name " + param.getName() + " already exists.");
            }
            param.visit(this, arg);
        }
        program.getBlock().visit(this, arg);
        st.leaveScope();
        return type;
    }
    @Override
    public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws PLCCompilerException {
        st.enterScope();

        LValue lValue = assignmentStatement.getlValue();

        if (lValue.getPixelSelector() != null) {
            // check x and y
            Expr xExpr = lValue.getPixelSelector().xExpr();
            Expr yExpr = lValue.getPixelSelector().yExpr();
            xExpr.setType(Type.INT);
            yExpr.setType(Type.INT);

            // if they're IdentExpr, check if they're declared or not
            if (xExpr instanceof IdentExpr) {
                String xName = ((IdentExpr) xExpr).getName();
                if (st.lookup(xName) == null) {
                    st.insert(new SyntheticNameDef(xName));
                }
            }
            if (yExpr instanceof IdentExpr) {
                String yName = ((IdentExpr) yExpr).getName();
                if (st.lookup(yName) == null) {
                    st.insert(new SyntheticNameDef(yName));
                }
            }
        }

        lValue.visit(this, arg);

        // getting lvalue type and expr type
        Type lValueType = lValue.getType();
        Type exprType = (Type) assignmentStatement.getE().visit(this, arg);

        if (!AssignmentCompatible(lValueType, exprType)) {
            throw new TypeCheckException("Type mismatch in assignment: LValue type " + lValueType + " is not compatible with Expr type " + exprType);
        }

        st.leaveScope();

        return null;
    }


    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws PLCCompilerException {
        // getting left & right expr
        Type leftType = (Type) binaryExpr.getLeftExpr().visit(this, arg);
        Type rightType = (Type) binaryExpr.getRightExpr().visit(this, arg);
        Kind opKind = binaryExpr.getOpKind();

        // handle addition of different types
        if (opKind == Kind.PLUS && (leftType != rightType)) {
            throw new TypeCheckException("Type mismatch in addition: " + leftType + " and " + rightType);
        }

        // infer type based on types
        Type resultType = inferBinaryType(leftType, opKind, rightType);

        // setting type to inferred type
        binaryExpr.setType(resultType);

        return resultType;
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
        Kind selectedChannel = channelSelector.color();
        return selectedChannel;

    }

    @Override
    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws PLCCompilerException {
        // getting types of the guard, true, and false expressions
        Type guardType = (Type) conditionalExpr.getGuardExpr().visit(this, arg);
        Type trueType = (Type) conditionalExpr.getTrueExpr().visit(this, arg);
        Type falseType = (Type) conditionalExpr.getFalseExpr().visit(this, arg);

        // checking the conditions
        if (guardType != Type.BOOLEAN) {
            throw new TypeCheckException("Guard expression in a conditional must be of type BOOLEAN.");
        }

        if (trueType != falseType) {
            throw new TypeCheckException("True and false expressions in a conditional must have the same type.");
        }

        // return cond expr type
        conditionalExpr.setType(trueType);
        return trueType;
    }

    @Override
    public Object visitConstExpr(ConstExpr constExpr, Object arg) throws PLCCompilerException {
        if (constExpr.getName().equals("Z")) {
            constExpr.setType(Type.INT);
        } else {
            constExpr.setType(Type.PIXEL);
        }

        // Return the type
        return constExpr.getType();
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

        String javaName = generateUniqueJavaName(nameDef.getName());
        // checking for redeclaration in the same scope
        if (st.lookup(nameDef.getJavaName()) != null && nameDef.getType() == st.lookup(nameDef.getName()).getType()) {
            throw new TypeCheckException("Variable name '" + nameDef.getName() + "' already exists in the current scope.");
        }

        // setting java name in nameDef
        nameDef.setJavaName(javaName);

        // insert the NameDef into the symbol table
        st.insert(nameDef);

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
    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws PLCCompilerException {
        Type typeW = (Type) dimension.getWidth().visit(this, arg);
        if(typeW != Type.INT){
            throw new TypeCheckException("dimension width must be int");
        }
        Type typeH = (Type) dimension.getHeight().visit(this, arg);
        if(typeH != Type.INT){
            throw new TypeCheckException("dimension height must be int");
        }
        return dimension;
    }

    @Override
    public Object visitDoStatement(DoStatement doStatement, Object arg) throws PLCCompilerException {
        List<GuardedBlock> guardedBlocks = doStatement.getGuardedBlocks();

        // visiting each guard block
        for (GuardedBlock guardedBlock : guardedBlocks) {
            guardedBlock.visit(this, arg);
        }

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
        // visit guard
        Type guardType = (Type) guardedBlock.getGuard().visit(this, arg);

        // type must be boolean
        if (guardType != Type.BOOLEAN) {
            throw new TypeCheckException("The guard expression in a GuardedBlock must be of type BOOLEAN.");
        }

        // visit block
        guardedBlock.getBlock().visit(this, arg);

        return guardType;
    }

    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws PLCCompilerException {
        NameDef identNameDef = st.lookup(identExpr.getName());

        if (identNameDef == null) {
            throw new TypeCheckException("Undeclared identifier: " + identExpr.getName());
        }

        identExpr.setNameDef(identNameDef);
        identExpr.setType(identNameDef.getType());

        String javaName = generateUniqueJavaName(identExpr.getNameDef().getName());

        // set the generated Java name in the identifier
        identExpr.getNameDef().setJavaName(javaName);

        return identNameDef.getType();
    }

    @Override
    public Object visitIfStatement(IfStatement ifStatement, Object arg) throws PLCCompilerException {
        List<GuardedBlock> guardedBlocks = ifStatement.getGuardedBlocks();

        // visiting each guard block
        for (GuardedBlock guardedBlock : guardedBlocks) {
            guardedBlock.visit(this, arg);
        }

        return null;
    }

    @Override
    public Object visitLValue(LValue lValue, Object arg) throws PLCCompilerException {
        // getting nameDef from table
        NameDef nameDef = st.lookup(lValue.getName());
        if (nameDef == null) {
            throw new TypeCheckException("Undeclared identifier: " + lValue.getName());
        }
        lValue.setNameDef(nameDef);

        // getting variable type from lvalue
        Type varType = lValue.getVarType();

        // checking conditions
        PixelSelector pixelSelector = lValue.getPixelSelector();
        ChannelSelector channelSelector = lValue.getChannelSelector();

        if (pixelSelector != null && varType != Type.IMAGE) {
            throw new TypeCheckException("Expected IMAGE type when PixelSelector is present.");
        }

        if (channelSelector != null && (varType != Type.PIXEL && varType != Type.IMAGE)) {
            throw new TypeCheckException("Expected PIXEL or IMAGE type when ChannelSelector is present.");
        }

        // determining the type
        Type finalType = inferLValueType(varType, pixelSelector, channelSelector);
        lValue.setType(finalType);

        return finalType;
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

        // generate java name
        String javaName = generateUniqueJavaName(nameDef.getName());

        // setting java name in nameDef
        nameDef.setJavaName(javaName);

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
        // if lvalue, only visit children
        if ("LValue".equals(arg)) {
            pixelSelector.xExpr().visit(this, arg);
            pixelSelector.yExpr().visit(this, arg);
        }
        // otherwise, check types
        else {
            Type xType = (Type) pixelSelector.xExpr().visit(this, arg);
            if (xType != Type.INT) {
                throw new TypeCheckException("PixelSelector x-coordinate must be of type INT.");
            }

            Type yType = (Type) pixelSelector.yExpr().visit(this, arg);
            if (yType != Type.INT) {
                throw new TypeCheckException("PixelSelector y-coordinate must be of type INT.");
            }
        }
        return Type.PIXEL;
    }


    @Override
    public Object visitPostfixExpr(PostfixExpr postfixExpr, Object arg) throws PLCCompilerException {
        Type exprType = (Type) postfixExpr.primary().visit(this, arg);
        Type inferredType;
        if(postfixExpr.pixel()!=null&&postfixExpr.channel()!=null){
        postfixExpr.pixel().visit(this,arg);
        // inferring postfix type based on pixel and channel
         inferredType = inferPostfixExprType(exprType, postfixExpr.pixel(), postfixExpr.channel());}
        else if(postfixExpr.pixel()==null&&postfixExpr.channel()!=null){
        // inferring postfix type based on pixel and channel
         inferredType = inferPostfixExprType(exprType, null, postfixExpr.channel());}
        else if(postfixExpr.pixel()==null&&postfixExpr.channel()==null)
        { inferredType = inferPostfixExprType(exprType, null, null);}
        else{
            postfixExpr.pixel().visit(this,arg);
         inferredType = inferPostfixExprType(exprType,postfixExpr.pixel() ,null);}
        postfixExpr.setType(inferredType);

        return inferredType;
    }

    @Override
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws PLCCompilerException {
        Type returnType = (Type) returnStatement.getE().visit(this, arg);


        // making sure it matches root
        if (returnType != root.getType()) {
            throw new TypeCheckException("Return type " + returnType + " does not match the enclosing program's type " + root.getType());
        }

        return returnType;
    }

    @Override
    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws PLCCompilerException {
        Type type = Type.STRING;
        stringLitExpr.setType(type);
        return type;
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws PLCCompilerException {
        // get expr type
        Type exprType = (Type) unaryExpr.getExpr().visit(this, arg);

        // getting the operation kind
        Kind opKind = unaryExpr.getOp();

        // inferring unary type
        Type inferredType = inferUnaryExprType(exprType, opKind);

        unaryExpr.setType(inferredType);

        return inferredType;
    }

    // from PowerPoint/Slack
    @Override
    public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws PLCCompilerException {
        writeStatement.getExpr().visit(this, arg);
        return writeStatement;
    }

    @Override
    public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws PLCCompilerException {
        booleanLitExpr.setType(Type.BOOLEAN);
        return Type.BOOLEAN;
    }

    // -----helper functions based on HW 3 tables----- //
    // binary helper
    private Type inferBinaryType(Type leftType, Kind opKind, Type rightType) {
        switch (opKind) {
            // pixel ops
            case BITAND:
            case BITOR:
                if (leftType == Type.PIXEL && rightType == Type.PIXEL) {
                    return Type.PIXEL;
                }
                break;

            // boolean ops
            case AND:
            case OR:
                if (leftType == Type.BOOLEAN && rightType == Type.BOOLEAN) {
                    return Type.BOOLEAN;
                }
                break;

            // comparison ops
            case LT:
            case GT:
            case LE:
            case GE:
                if (leftType == Type.INT && rightType == Type.INT) {
                    return Type.BOOLEAN;
                }
                break;

            // eq
            case EQ:
                if (leftType == rightType) {
                    return Type.BOOLEAN;
                }
                break;

            // exponents
            case EXP:
                if (leftType == Type.INT && rightType == Type.INT) {
                    return Type.INT;
                } else if (leftType == Type.PIXEL && rightType == Type.INT) {
                    return Type.PIXEL;
                }
                break;


            case PLUS:
                if (leftType == rightType) {
                    return leftType;
                }
                break;

            // arithmetic ops
            case MINUS:
            case TIMES:
            case DIV:
            case MOD:
                if ((leftType == Type.INT || leftType == Type.PIXEL || leftType == Type.IMAGE) && leftType == rightType) {
                    return leftType;
                } else if ((leftType == Type.PIXEL || leftType == Type.IMAGE) && rightType == Type.INT) {
                    return leftType;
                }
                break;

            default:

        }
        return Type.INT;

    }

    // assignment helper
    private boolean AssignmentCompatible(Type lValueType, Type exprType) {
        if (lValueType == exprType) {
            return true;
        }
        switch (lValueType) {
            case PIXEL:
                return exprType == Type.INT;
            case IMAGE:
                return exprType == Type.PIXEL || exprType == Type.INT || exprType == Type.STRING;
            default:
                return false;
        }
    }

    // LValue helper
    private Type inferLValueType(Type varType, PixelSelector pixelSelector, ChannelSelector channelSelector) throws PLCCompilerException {
        if (pixelSelector == null && channelSelector == null) {
            return varType;
        } else if (varType == Type.IMAGE) {
            if (pixelSelector != null && channelSelector == null) {
                return Type.PIXEL;
            } else if (pixelSelector != null && channelSelector != null) {
                return Type.INT;
            } else if (pixelSelector == null && channelSelector != null) {
                return Type.IMAGE;
            }
        } else if (varType == Type.PIXEL && channelSelector != null) {
            return Type.INT;
        }

        throw new TypeCheckException("Invalid LValue configuration.");
    }

    // postfixexpr helper
    private Type inferPostfixExprType(Type exprType, PixelSelector pixelSelector, ChannelSelector channelSelector) throws PLCCompilerException {
        if (pixelSelector == null && channelSelector == null) {
            return exprType;
        } else if (exprType == Type.IMAGE) {
            if (pixelSelector != null && channelSelector == null) {
                return Type.PIXEL;
            } else if (pixelSelector != null && channelSelector != null) {
                return Type.INT;
            } else if (pixelSelector == null && channelSelector != null) {
                return Type.IMAGE;
            }
        } else if (exprType == Type.PIXEL && channelSelector != null) {
            return Type.INT;
        }

        throw new TypeCheckException("Invalid PostfixExpr configuration.");
    }

    // unary helper
    private Type inferUnaryExprType(Type exprType, Kind op) throws PLCCompilerException {
        if (exprType == Type.BOOLEAN && op == Kind.BANG) {
            return Type.BOOLEAN;
        } else if (exprType == Type.INT && op == Kind.MINUS) {
            return Type.INT;
        } else if (exprType == Type.IMAGE && (op == Kind.RES_width || op == Kind.RES_height)) {
            return Type.INT;
        }

        throw new TypeCheckException("Invalid UnaryExpr configuration for type: " + exprType + " and operation: " + op);
    }

    private String generateUniqueJavaName(String baseName) {

        String javaName = baseName + "$" + st.currentScopeID;

        // Check if the generated JavaName is already in use, incrementing the suffix if necessary
        while (st.lookup(javaName) != null) {

            javaName = baseName + "$" + st.currentScopeID;
        }

        return javaName;
    }
}
