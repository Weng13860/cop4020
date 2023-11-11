package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.ast.*;
import edu.ufl.cise.cop4020fa23.exceptions.PLCCompilerException;

public class CodeGenVisitor implements ASTVisitor {

    private StringBuilder javaCode;
    private String packageName;

    public CodeGenVisitor(String packageName) {
        this.packageName = packageName;
        this.javaCode = new StringBuilder();
    }

    private String packageToDirectory(String pack) {
        if (pack != null) {
            return pack.replace('/', '.');
        }
        else {
            return "";
        }
    }

    @Override
    public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws PLCCompilerException {
        Object expressionResult = assignmentStatement.getE().visit(this, arg);

        javaCode.append("  ").append(assignmentStatement.getlValue().getName())
                .append(" = ").append(expressionResult)
                .append(";\n");

        return javaCode.toString();
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws PLCCompilerException {
        String left = binaryExpr.getLeftExpr().visit(this, arg).toString();
        String operator = binaryExpr.getOpKind().toString();
        String right = binaryExpr.getRightExpr().visit(this, arg).toString();
        return "(" + left + " " + operator + " " + right + ")";
    }

    @Override
    public Object visitBlock(Block block, Object arg) throws PLCCompilerException {
        for (Block.BlockElem elem : block.getElems()) {
            elem.visit(this, arg);
        }
        return javaCode.toString();
    }

    @Override
    public Object visitBlockStatement(StatementBlock statementBlock, Object arg) throws PLCCompilerException {
        return "Block Statement";
    }

    @Override
    public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws PLCCompilerException {
        return booleanLitExpr.getText();
    }

    @Override
    public Object visitChannelSelector(ChannelSelector channelSelector, Object arg) throws PLCCompilerException {
        return "Channel Selector";
    }

    @Override
    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws PLCCompilerException {
        String condition = conditionalExpr.getGuardExpr().visit(this, arg).toString();
        String trueExpr = conditionalExpr.getTrueExpr().visit(this, arg).toString();
        String falseExpr = conditionalExpr.getFalseExpr().visit(this, arg).toString();
        return "(" + condition + " ? " + trueExpr + " : " + falseExpr + ")";
    }

    @Override
    public Object visitConstExpr(ConstExpr constExpr, Object arg) throws PLCCompilerException {
        return "Const Expr";
    }

    @Override
    public Object visitDeclaration(Declaration declaration, Object arg) throws PLCCompilerException {
        String declarationType = declaration.getNameDef().getType().toString();
        String declarationName = declaration.getNameDef().getJavaName();

        javaCode.append("  ").append(declarationType).append(" ").append(declarationName);

        if (declaration.getInitializer() != null) {
            javaCode.append(" = ");
            Object initializerResult = declaration.getInitializer().visit(this, arg);
            javaCode.append(initializerResult);
        }

        javaCode.append(";\n");

        return javaCode.toString();
    }

    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws PLCCompilerException {
        return "Dimension";
    }

    @Override
    public Object visitDoStatement(DoStatement doStatement, Object arg) throws PLCCompilerException {
        return "Do";
    }

    @Override
    public Object visitExpandedPixelExpr(ExpandedPixelExpr expandedPixelExpr, Object arg) throws PLCCompilerException {
        return "Expanded Pixel";
    }

    @Override
    public Object visitGuardedBlock(GuardedBlock guardedBlock, Object arg) throws PLCCompilerException {
        return "Guarded Block";
    }

    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws PLCCompilerException {
        return identExpr.getNameDef().getJavaName();
    }

    @Override
    public Object visitIfStatement(IfStatement ifStatement, Object arg) throws PLCCompilerException {
        return "If";
    }

    @Override
    public Object visitLValue(LValue lValue, Object arg) throws PLCCompilerException {
        return "L Value";
    }

    @Override
    public Object visitNameDef(NameDef nameDef, Object arg) throws PLCCompilerException {
        return nameDef.getJavaName();
    }

    @Override
    public Object visitNumLitExpr(NumLitExpr numLitExpr, Object arg) throws PLCCompilerException {
        return numLitExpr.getText();
    }

    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws PLCCompilerException {
        return "Pixel Selector";
    }

    @Override
    public Object visitPostfixExpr(PostfixExpr postfixExpr, Object arg) throws PLCCompilerException {
        return "Postfix";
    }

    @Override
    public Object visitProgram(Program program, Object arg) throws PLCCompilerException {
        // write package name
        if (packageName != null) {
            javaCode.append("import ").append(packageToDirectory(packageName)).append(".*;\n\n");
        }

        System.out.println(packageName);
        javaCode.append("public class ").append(program.getName());

        // visit params (if any)
        for (NameDef param : program.getParams()) {
            param.visit(this, arg);
        }
        javaCode.append("{\n");

        // visit block
        program.getBlock().visit(this, arg);

        // close class
        javaCode.append("}\n");

        // return in string
        return getGeneratedCode();
    }

    @Override
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws PLCCompilerException {
        return "Return";
    }

    @Override
    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws PLCCompilerException {
        return "\"" + stringLitExpr.getText() + "\"";
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws PLCCompilerException {
        String operator = unaryExpr.getOp().toString();
        String operand = unaryExpr.getExpr().visit(this, arg).toString();
        return "(" + operator + operand + ")";
    }

    @Override
    public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws PLCCompilerException {
        return "Write";
    }

    public String getGeneratedCode() {
        return javaCode.toString();
    }

}
