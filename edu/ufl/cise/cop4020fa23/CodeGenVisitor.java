package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.ast.*;
import edu.ufl.cise.cop4020fa23.ast.Dimension;
import edu.ufl.cise.cop4020fa23.exceptions.CodeGenException;
import edu.ufl.cise.cop4020fa23.exceptions.PLCCompilerException;

import java.awt.*;
import java.util.List;

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

        Type assignmentExpr = assignmentStatement.getE().getType();
        LValue lValue = assignmentStatement.getlValue();
        Type lvt=lValue.getType();
        Type a=assignmentStatement.getlValue().getNameDef().getType();

        if(assignmentExpr==Type.PIXEL&&a==Type.IMAGE&&assignmentStatement.getlValue().getPixelSelector()!=null&&assignmentStatement.getlValue().getChannelSelector()==null){

            javaCode.append("\t\tfor(")
                    .append(typetostring(lValue.getPixelSelector().xExpr().getType()))

                    .append(" ").append("x = 0; x < ").append(lValue.getNameDef().getJavaName()).append(".getWidth(); x++){").append("\n");
//continue here run and see what we need to do
        }
        else if (assignmentExpr == Type.IMAGE) {
            javaCode.append("ImageOps.copyInto(")
                    .append(lValue.getNameDef().getJavaName())
                    .append("=")
                    .append(expressionResult)
                    .append(");\n");
        }
        else if (assignmentExpr == Type.PIXEL) {
            if (lValue.getChannelSelector() != null) {
                String channel = lValue.getChannelSelector().visit(this, arg).toString();
                javaCode.append(lValue.getNameDef().getJavaName())
                        .append(" = PixelOps.set")
                        .append(channel.substring(0, 1).toUpperCase())
                        .append(channel.substring(1))
                        .append("(")
                        .append(lValue.getNameDef().getJavaName())
                        .append(", ")
                        .append(expressionResult)
                        .append(");\n");
            }
            else {
                System.out.println("null channel sele");
                javaCode.append("\t\t")
                        .append(lValue.getNameDef().getJavaName())
                        .append(" = ")
                        .append(expressionResult)
                        .append(";\n");
            }
        }
        else if (lValue.getType() == Type.PIXEL){
            javaCode.append("\t\t").append(lValue.getNameDef().getJavaName())
                    .append(" = PixelOps.pack(").append(expressionResult)
                    .append(",")
                    .append(expressionResult)
                    .append(",")
                    .append(expressionResult)
                    .append(");\n");
        }
        else {
            javaCode.append("\t\t")
                    .append(lValue.getNameDef().getJavaName())
                    .append(" = ")
                    .append(expressionResult)
                    .append(";\n");
        }

        return javaCode.toString();
    }



    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws PLCCompilerException {

        String left = binaryExpr.getLeftExpr().visit(this, arg).toString();
        String operator = binaryExpr.getOp().text();
        String right = binaryExpr.getRightExpr().visit(this, arg).toString();

        if(binaryExpr.getOpKind()==Kind.EXP){
            return "((int)Math.round(Math.pow(" + left + "," + right + ")))";
        }
        else if(binaryExpr.getLeftExpr().getType() == Type.PIXEL){
            return "ImageOps.binaryPackedPixelPixelOp(ImageOps.OP.PLUS," + left+  "," + right + ")";
        }
        else if(binaryExpr.getOpKind() == Kind.DIV && binaryExpr.getLeftExpr().getType() == Type.IMAGE){
            return "(ImageOps.binaryImageScalarOp(ImageOps.OP.DIV," + left+ "," + right + "))";
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
        javaCode.append("\t");
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
        return channelSelector.color().toString();
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
        String expressionName = constExpr.getName();
        return getRGB(expressionName);
    }

    @Override
    public Object visitDeclaration(Declaration declaration, Object arg) throws PLCCompilerException {

        Type declarationType = declaration.getNameDef().getType();
        String declarationName = declaration.getNameDef().getJavaName();
        Expr initializer = declaration.getInitializer();
        Dimension dimension = declaration.getNameDef().getDimension();
        String variableName = declaration.getNameDef().getJavaName();
        if(initializer==null){
            if(declarationType!=Type.IMAGE){
                String aa=typetostring(declarationType);
                javaCode.append("\t\t").append(aa).append(" ").append(declarationName).append(";");

            }
            else {
                javaCode.append("\t\t").append(declarationType).append(" ").append(declarationName);
                if(dimension!=null){
                    Object initializerResult = initializer.visit(this, arg);
                    String width = dimension.getWidth().firstToken.text();
                    String height = dimension.getHeight().firstToken.text();
                    javaCode.append(" = ImageOps.copyAndResize(")
                            .append(initializerResult).append(",")
                            .append(width).append(",")
                            .append(height)
                            .append(");");
                }
                else throw new CodeGenException("line179");
            }
        }else {

            String aa=typetostring(declarationType);

            javaCode.append("\t\t").append(aa).append(" ").append(declarationName);
                if (initializer.getType() == Type.STRING) {
                     if (dimension != null) {
                        Object initializerResult = initializer.visit(this, arg);
                        String width = dimension.getWidth().firstToken.text();
                        String height = dimension.getHeight().firstToken.text();
                        javaCode.append(" = ImageOps.copyAndResize(")
                                .append(initializerResult).append(",")
                                .append(width).append(",")
                                .append(height)
                                .append(");");
                    }
                     else{
                    Object initializerResult = initializer.visit(this, arg);
                    javaCode.append(" = FileURLIO.readImage(")
                            .append(initializerResult)
                            .append(");");
                     }
                }
                else if(initializer.getType()==Type.IMAGE&&dimension==null){
                    Object initializerResult = initializer.visit(this, arg);
                    javaCode.append(" = ImageOps.cloneImage(")
                            .append(initializerResult)
                            .append(");");
                }
                else if(initializer.getType()==Type.IMAGE&&dimension!=null){
                    Object initializerResult = initializer.visit(this, arg);
                    String width = dimension.getWidth().firstToken.text();
                    String height = dimension.getHeight().firstToken.text();
                    javaCode.append(" = ImageOps.copyAndResize(")
                            .append(initializerResult).append(",")
                            .append(width).append(",")
                            .append(height)
                            .append(");");
                }
                else {
                    javaCode.append("=").append(declaration.getInitializer().visit(this,arg).toString())
                            .append(";");
                }


            }







       /* if (declarationType.equals("BufferedImage") && initializer != null) {
            javaCode.append("\t\t").append(declarationType).append(" ").append(declarationName);

            if (initializer.getType() == Type.STRING) {
                Object initializerResult = initializer.visit(this, arg);
                javaCode.append(" = FileURLIO.readImage(")
                        .append(initializerResult)
                        .append(")");
            } else if (initializer.getType() == Type.IMAGE && dimension == null) {
                Object initializerResult = initializer.visit(this, arg);
                javaCode.append(" = ImageOps.cloneImage(")
                        .append(initializerResult)
                        .append(")");
            } else if (dimension != null && initializer.getType() == Type.IMAGE) {
                Object initializerResult = initializer.visit(this, arg);
                String width = dimension.getWidth().firstToken.text();
                String height = dimension.getHeight().firstToken.text();
                javaCode.append(" = ImageOps.copyAndResize(")
                        .append(initializerResult).append(",")
                        .append(width).append(",")
                        .append(height)
                        .append(")");
            }

            javaCode.append(";\n");
        } else if (initializer == null) {
            Dimension dimensionString = declaration.getNameDef().getDimension();
            String variableName = declaration.getNameDef().getJavaName();

            if (dimensionString != null) {
                Object dimensionResult = declaration.getNameDef().getDimension().visit(this, arg);
                javaCode.append("\t\tfinal BufferedImage ").append(variableName)
                        .append(" = ImageOps.makeImage(").append(dimensionResult).append(");\n");
            } else {
                throw new CodeGenException("No dimension found for declaration");
            }
        } else {
            javaCode.append(declarationType)
                    .append(" ")
                    .append(declarationName)
                    .append("=")
                    .append(initializer.visit(this, arg).toString())
                    .append(";");
            return javaCode.toString();
        }*/

        return new CodeGenException("Declaration error");
    }


    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws PLCCompilerException {

        String width =dimension.getWidth().visit(this,arg).toString();
        String height =dimension.getHeight().visit(this,arg).toString();
        return width + "," + height;
    }

    @Override
    public Object visitDoStatement(DoStatement doStatement, Object arg) throws PLCCompilerException {
        return "Do";
    }

    @Override
    public Object visitExpandedPixelExpr(ExpandedPixelExpr expandedPixelExpr, Object arg) throws PLCCompilerException {
        String redExpr = expandedPixelExpr.getRed().visit(this, arg).toString();
        String greenExpr = expandedPixelExpr.getGreen().visit(this, arg).toString();
        String blueExpr = expandedPixelExpr.getBlue().visit(this, arg).toString();

        return "PixelOps.pack(" + redExpr + ", " + greenExpr + ", " + blueExpr + ")";
    }

    @Override
    public Object visitGuardedBlock(GuardedBlock guardedBlock, Object arg) throws PLCCompilerException {
        Object guard = guardedBlock.getGuard().visit(this,arg);
        Object block = guardedBlock.getBlock().visit(this,arg);
        javaCode.append("if(")
                .append(guard)
                .append(")")
                .append("{")
                .append(block)
                .append("}");
        javaCode.append("else if(")
                .append(guard)
                .append(")")
                .append("{")
                .append(block)
                .append("}");

        return javaCode.toString();
    }

    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws PLCCompilerException {

        return identExpr.getNameDef().getJavaName();
    }

    @Override
    public Object visitIfStatement(IfStatement ifStatement, Object arg) throws PLCCompilerException {
        Object aa=ifStatement.visit(this,arg).toString();
        return "If";
    }

    @Override
    public Object visitLValue(LValue lValue, Object arg) throws PLCCompilerException {
        // Visit PixelSelector if present
        if (lValue.getPixelSelector() != null) {
            javaCode.append(lValue.getPixelSelector().visit(this, arg)).append(".");
        }

        // Visit ChannelSelector if present
        if (lValue.getChannelSelector() != null) {
            javaCode.append(lValue.getChannelSelector().visit(this, arg)).append(":");
        }

        javaCode.append(lValue.getNameDef().getJavaName());

        return javaCode.toString();
    }


    @Override
    public Object visitNameDef(NameDef nameDef, Object arg) throws PLCCompilerException {
        String f = typetostring(nameDef.getType());
        String name = nameDef.getName();
        return f + " " + name;
    }

    @Override
    public Object visitNumLitExpr(NumLitExpr numLitExpr, Object arg) throws PLCCompilerException {
        return numLitExpr.getText();
    }

    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws PLCCompilerException {

        String x=pixelSelector.xExpr().visit(this,arg).toString();
        String y=pixelSelector.yExpr().visit(this,arg).toString();
        return javaCode.append(x).append(",").append(y);
    }

    @Override
    public Object visitPostfixExpr(PostfixExpr postfixExpr, Object arg) throws PLCCompilerException {
        if (postfixExpr.getType() == Type.PIXEL) {
            String channelExpression = postfixExpr.channel().visit(this, arg).toString();
            javaCode.append(channelExpression);
        } else if (postfixExpr.getType() == Type.IMAGE) {
            Object channelExpression = postfixExpr.channel().visit(this, arg);
            Object pixelExpression = postfixExpr.pixel().visit(this, arg);

            if (channelExpression == null && pixelExpression != null) {
                javaCode.append("ImageOps.getRGB(").append(postfixExpr.primary().toString()).append(",").append(pixelExpression).append(")");
            } else if (channelExpression != null && pixelExpression != null) {
                javaCode.append(channelExpression).append("(ImageOps.getRGB(").append(postfixExpr.primary().toString()).append(",").append(pixelExpression).append("))");
            } else if (channelExpression != null && pixelExpression == null) {
                javaCode.append("ImageOps.extractRed(").append(postfixExpr.primary().toString()).append(")");
            } else {
                throw new CodeGenException("no pixel or channel");
            }
        } else {
            // any other cases?
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
        javaCode.append("import java.awt.image.BufferedImage;\n");
        //javaCode.append("import javax.imageio.ImageIO;\n");
        javaCode.append("import java.awt.Color;\n");
        javaCode.append("\npublic class ").append(program.getName()).append("{\n").append("\tpublic static ").append(typetostring(program.getType())).append(" apply(");

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
        return new CodeGenException("unary error");
    }

    @Override
    public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws PLCCompilerException {
        String writeExpr = writeStatement.getExpr().visit(this, arg).toString();
        if(writeStatement.getExpr().getType() == Type.PIXEL){
            javaCode.append("\t\tConsoleIO.writePixel(").append(writeExpr).append(");\n");
        }
        else{
            javaCode.append("\t\tConsoleIO.write(").append(writeExpr).append(");\n");
        }
        return javaCode.toString();
    }

    public String getGeneratedCode() {
        return javaCode.toString();
    }

    public String typetostring(Type type){
        switch (type){
            case IMAGE->{
                return "BufferedImage";
            }
            case PIXEL->{
                return "int";
            }
            case INT,BOOLEAN,VOID->{
                return type.toString().toLowerCase();
            }
            case STRING -> {
                return "String";
            }
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

    private String getRGB(String color) {
        switch (color) {
            case "BLUE" -> {
                return "0x" + Integer.toHexString(Color.BLUE.getRGB());
            }
            case "GREEN" -> {
                return "0x" + Integer.toHexString(Color.GREEN.getRGB());
            }
            case "BLACK" -> {
                return "0x" + Integer.toHexString(Color.BLACK.getRGB());
            }
            case "CYAN" -> {
                return "0x" + Integer.toHexString(Color.CYAN.getRGB());
            }
            case "DARK_GRAY" -> {
                return "0x" + Integer.toHexString(Color.DARK_GRAY.getRGB());
            }
            case "GRAY" -> {
                return "0x" + Integer.toHexString(Color.GRAY.getRGB());
            }
            case "LIGHT_GRAY" -> {
                return "0x" + Integer.toHexString(Color.LIGHT_GRAY.getRGB());
            }
            case "MAGENTA" -> {
                return "0x" + Integer.toHexString(Color.MAGENTA.getRGB());
            }
            case "ORANGE" -> {
                return "0x" + Integer.toHexString(Color.ORANGE.getRGB());
            }
            case "PINK" -> {
                return "0x" + Integer.toHexString(Color.PINK.getRGB());
            }
            case "RED" -> {
                return "0x" + Integer.toHexString(Color.RED.getRGB());
            }
            case "WHITE" -> {
                return "0x" + Integer.toHexString(Color.WHITE.getRGB());
            }
            case "YELLOW" -> {
                return "0x" + Integer.toHexString(Color.YELLOW.getRGB());
            }
            case "Z" -> {
                return "255";
            }
        }
        return null;
    }

}
