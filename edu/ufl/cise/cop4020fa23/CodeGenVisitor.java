package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.ast.*;
import edu.ufl.cise.cop4020fa23.exceptions.CodeGenException;
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
        Type a=assignmentStatement.getE().getType();
        if(a==Type.IMAGE){
           javaCode.append( "ImageOps.copyInto(").append(assignmentStatement.getlValue().getNameDef().getJavaName()).append("=").append(expressionResult).append(";\n");
        }
        else if(a==Type.PIXEL){
            javaCode.append("ImageOps.setAllPixels(");//Continue here, need add paras
        }

        javaCode.append("  ").append(assignmentStatement.getlValue().getNameDef().getJavaName())
                .append(" = ").append(expressionResult)
                .append(";\n");

        return javaCode.toString();
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws PLCCompilerException {
        String left = binaryExpr.getLeftExpr().visit(this, arg).toString();
        String operator = binaryExpr.getOp().text();
        String right = binaryExpr.getRightExpr().visit(this, arg).toString();

        if(binaryExpr.getOpKind()==Kind.EXP){
            return "((int)Math.round(Math.pow("+left+","+right+")))";
        }
        else{
            return "(" + left + " " + operator + " " + right + ")";
        }
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
       String blockaaa= statementBlock.getBlock().visit(this,arg).toString();

        return blockaaa;
    }

    @Override
    public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws PLCCompilerException {
        return booleanLitExpr.getText().toLowerCase();
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
        String aa=constExpr.getName();
        if(aa=="Z"){return 255;}
        else {
            return "0x"+"Integer.toHexString(Color."+aa+".getRGB())";
        }
    }

    @Override
    public Object visitDeclaration(Declaration declaration, Object arg) throws PLCCompilerException {
        String declarationType = typetostring(declaration.getNameDef().getType());
        String declarationName = declaration.getNameDef().getJavaName();
        String aaa= declaration.getInitializer().toString();
        if(declarationType!= "IMAGE"){

        javaCode.append("  ").append(declarationType).append(" ").append(declarationName);
        if (aaa!= null) {
            javaCode.append(" = ");
            Object initializerResult = declaration.getInitializer().visit(this, arg);
            javaCode.append(initializerResult);
        }
        javaCode.append(";\n");
        return javaCode.toString();}
        else {
            if(aaa==null){
                String aaaaa=declaration.getNameDef().getDimension().visit(this,arg).toString();
                if(aaaaa!=null){
                    javaCode.append("final BufferedImage").append(aaaaa).append(declarationName).append("=").append("ImageOps.makeImage(").append(aaaaa).append(")");}
                else {throw new CodeGenException("no dim from decl1");}
            }
            else{
                String aaa1=declaration.getNameDef().getDimension().visit(this,arg).toString();
                if(aaa1!=null){

                }
            }
            if(declaration.getInitializer()!=null){
               Expr x=declaration.getInitializer();
               if(x.getType()==Type.STRING){
                    javaCode.append("=").append(x.toString());
               }

            }

        }
    }

    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws PLCCompilerException {
        return dimension.getWidth().toString()+","+dimension.getHeight().toString();
    }

    @Override
    public Object visitDoStatement(DoStatement doStatement, Object arg) throws PLCCompilerException {
        return "Do";
    }

    @Override
    public Object visitExpandedPixelExpr(ExpandedPixelExpr expandedPixelExpr, Object arg) throws PLCCompilerException {
        return "PixelOps.pack("+expandedPixelExpr.getRed()+","+expandedPixelExpr.getGreen()+","+expandedPixelExpr.getBlue()+")";
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
        String aa=lValue.getChannelSelector().visit(this,arg).toString();
        String bb=lValue.getPixelSelector().visit(this,arg).toString();
        String cc=lValue.getNameDef().getJavaName().toString();
        return "L Value";
    }

    @Override
    public Object visitNameDef(NameDef nameDef, Object arg) throws PLCCompilerException {
        String f = typetostring(nameDef.getType());
        Object aaa=nameDef.getDimension().visit(this,arg);
        String name = nameDef.getName();
        return f + " " + name;
    }

    @Override
    public Object visitNumLitExpr(NumLitExpr numLitExpr, Object arg) throws PLCCompilerException {
        return numLitExpr.getText();
    }

    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws PLCCompilerException {
        String x=pixelSelector.xExpr().toString();
        String y=pixelSelector.yExpr().toString();
        return javaCode.append(x).append(",").append(y);
    }

    @Override
    public Object visitPostfixExpr(PostfixExpr postfixExpr, Object arg) throws PLCCompilerException {
        if(postfixExpr.getType()==Type.PIXEL){
            String aa=postfixExpr.channel().visit(this,arg).toString();
             javaCode.append(aa);

        }else if(postfixExpr.getType()==Type.IMAGE){
           String  aaa= postfixExpr.channel().visit(this,arg).toString();
           String bbb=postfixExpr.pixel().visit(this,arg).toString();
            if(aaa == null&&bbb!=null){
                 javaCode.append("ImageOps.getRGB( ").append(postfixExpr.primary().toString()).append(",").append(bbb).append(")");
            }
            else if(aaa!=null&&bbb!=null){javaCode.append(aaa).append("(ImageOps.getRGB(").append(postfixExpr.primary().toString()).append(",").append(bbb).append("))");
            }
            else if(aaa!=null&&bbb!=null){
                javaCode.append("ImageOps.extractRed( ").append(postfixExpr.primary().toString()).append(")");
            }
        }
        return javaCode;
    }

    @Override
    public Object visitProgram(Program program, Object arg) throws PLCCompilerException {
        // write package name
        if (packageName != null) {
            javaCode.append("package ").append(packageToDirectory(packageName)).append(";\n\n");
        }

        javaCode.append("import edu.ufl.cise.cop4020fa23.runtime.*;\n");
        javaCode.append("public class ").append(program.getName()).append("{\n").append("\tpublic static ").append(typetostring(program.getType())).append(" apply(");

        // visit params (if any)
        boolean firstParam = true;
        for (NameDef param : program.getParams()) {
            if (!firstParam) {
                javaCode.append(", ");
            }
            String paramName = param.getJavaName(); // Ensure unique names for parameters
            javaCode.append(typetostring(param.getType())).append(" ").append(paramName);
            firstParam = false;
        }
        javaCode.append("){\n");

        // visit block
        program.getBlock().visit(this, arg);
        System.out.println(javaCode);

        // close class
        javaCode.append("\t}\n}\n");

        // return in string
        return getGeneratedCode();
    }

    @Override
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws PLCCompilerException {
        Object j=returnStatement.getE().visit(this,arg).toString();
        javaCode.append("\t\treturn ").append(j).append(";\n");
        return javaCode.toString();

    }

    @Override
    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws PLCCompilerException {
        return stringLitExpr.getText();
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws PLCCompilerException {
        String operator = getOperatorString(unaryExpr.getOp());
        String operand = unaryExpr.getExpr().visit(this, arg).toString();
        if(operator=="RES_width"){
            return "("+ operand + ".getWidth())";
        }
        if(operator=="RES_height"){
           return  "("+ operand + ".getHeight())";
        }
    }

    @Override
    public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws PLCCompilerException {
        String aa = writeStatement.getExpr().visit(this, arg).toString();
        javaCode.append("\tConsoleIO.write(").append(aa).append(");\n");
        return javaCode.toString();
    }

    public String getGeneratedCode() {
        return javaCode.toString();
    }

    public String typetostring(Type type){
        switch (type){
            case IMAGE,PIXEL->{
                return "BufferedImage";
            }
            case INT,BOOLEAN,VOID->{return type.toString().toLowerCase();}
            case STRING -> {return "String";}
            default -> {
                return null;
            }

        }
    }

    // unaryexpr helper
    private String getOperatorString(Kind operatorKind) {
        switch (operatorKind) {
            case MINUS:
                return "-";
            case BANG:
                return "!";
            default:
                throw new IllegalArgumentException("Unknown operator: " + operatorKind);
        }
    }
}
