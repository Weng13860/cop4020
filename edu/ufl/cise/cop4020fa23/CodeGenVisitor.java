package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.ast.*;
import edu.ufl.cise.cop4020fa23.ast.Dimension;
import edu.ufl.cise.cop4020fa23.exceptions.CodeGenException;
import edu.ufl.cise.cop4020fa23.exceptions.PLCCompilerException;

import java.awt.*;
import java.util.LinkedList;
import java.util.List;

public class CodeGenVisitor implements ASTVisitor {

    private boolean isWithinIfStatement;
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
        StringBuilder assignmentStatementCode = new StringBuilder();
        Object expressionResult = assignmentStatement.getE().visit(this, arg);
        Type assignmentExpr = assignmentStatement.getE().getType();
        LValue lValue = assignmentStatement.getlValue();
        Type lvt=lValue.getType();
        Type a=assignmentStatement.getlValue().getNameDef().getType();



        if((assignmentExpr==Type.PIXEL || assignmentExpr == Type.INT)&&a==Type.IMAGE&&assignmentStatement.getlValue().getPixelSelector()!=null&&assignmentStatement.getlValue().getChannelSelector()==null){
            Object xExpr = lValue.getPixelSelector().xExpr();
            String xName = ((IdentExpr) xExpr).getName();
            Expr yExpr = lValue.getPixelSelector().yExpr();
            String yName = ((IdentExpr) yExpr).getName();

            if(((IdentExpr) xExpr).getNameDef() instanceof SyntheticNameDef){
                assignmentStatementCode.append("\t\tfor(")
                        .append(typetostring(lValue.getPixelSelector().xExpr().getType()))
                        .append(" ").append(xName).append("$4 = 0; ").append(xName).append("$4 < ")
                        .append(lValue.getNameDef().getJavaName())
                        .append(".getWidth(); ").append(xName).append("$4++){\n").append("\t\t\tfor(")
                        .append(typetostring(lValue.getPixelSelector().xExpr().getType()))
                        .append(" ").append(yName).append("$4 = 0; ").append(yName).append("$4 < ")
                        .append(lValue.getNameDef().getJavaName())
                        .append(".getHeight(); ").append(yName).append("$4++){\n")
                        .append("\t\t\t\tImageOps.setRGB(")
                        .append(lValue.getNameDef().getJavaName())
                        .append(",")
                        .append(xName)
                        .append("$4,")
                        .append(yName)
                        .append("$4,")
                        .append(expressionResult)
                        .append(");\n")
                        .append("\n}\n};\n");
            }
            else{
                assignmentStatementCode.append("\t\tImageOps.setRGB(")
                        .append(lValue.getNameDef().getJavaName())
                        .append(",")
                        .append(xName)
                        .append("$4,")
                        .append(yName)
                        .append("$4,")
                        .append(expressionResult)
                        .append(");\n");
            }
        }
        else if(lvt==Type.IMAGE){
            if(lValue.getChannelSelector()==null&&lValue.getPixelSelector()==null){
                if(assignmentExpr==Type.IMAGE){
                    assignmentStatementCode.append("\t\tImageOps.copyInto(")
                            .append(expressionResult)
                            .append(",")
                            .append(lValue.getNameDef().getJavaName())
                            .append(");\n");

                }
                else if(assignmentExpr==Type.PIXEL){
                    assignmentStatementCode.append("ImageOps.setAllPixels(")
                            .append(lValue.getNameDef().getJavaName())
                            .append(",")
                            .append(expressionResult)
                            .append(");\n");

                }
                else if(assignmentExpr==Type.STRING){
                    assignmentStatementCode.append("ImageOps.copyInto(FileURLIO.readImage(")
                            .append(expressionResult)
                            .append("),")
                            .append(lValue.getNameDef().getJavaName())
                            .append(");\n");
                }
            }
            else if(lValue.getChannelSelector()!=null){
                throw new UnsupportedOperationException("channel selector is not null");
            }
            // SyntheticNameDef
            else if(lValue.getChannelSelector()==null&&lValue.getPixelSelector()!=null){

            }

        }
        else if(lvt == Type.PIXEL && lValue.getChannelSelector() == null){
            if(assignmentExpr == Type.INT && assignmentStatement.firstToken().kind() != Kind.CONST){
                    assignmentStatementCode.append("\t\t").append(lValue.getNameDef().getJavaName()).append(" = ")
                            .append("PixelOps.pack(").append(expressionResult).append(", ")
                            .append(expressionResult).append(", ")
                            .append(expressionResult).append(");\n");
            }
            else{
                assignmentStatementCode.append("\t\t")
                        .append(lValue.getNameDef().getJavaName())
                        .append(" = ")
                        .append(expressionResult)
                        .append(";\n");
            }
        }
        else if(lValue.getChannelSelector()!=null){

            String channel = lValue.getChannelSelector().visit(this, arg).toString();
            String color=channel.substring(0,1)+channel.substring(1);
            if(assignmentExpr!=null){
            assignmentStatementCode.append(lValue.getNameDef().getJavaName())
                    .append(" = PixelOps.set")
                    .append(getColorString(color))
                    .append("(")
                    .append(lValue.getNameDef().getJavaName())
                    .append(", ")
                    .append(expressionResult)
                    .append(");\n");}
            else {
                assignmentStatementCode.append(lValue.getNameDef().getJavaName())
                        .append("= PixelOps.")
                        .append(getColorString(color).toLowerCase())
                        .append("(")
                        .append(lValue.getNameDef().getJavaName())
                        .append(");");
            }

        }
        else {
            assignmentStatementCode.append("\t\t")
                    .append(lValue.getNameDef().getJavaName())
                    .append(" = ")
                    .append(expressionResult)
                    .append(";\n");
            //return assignmentStatementCode.toString();
        }
//copy code to conditions.

        javaCode.append(assignmentStatementCode);
        return assignmentStatementCode.toString();
    }



    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws PLCCompilerException {

        String left = binaryExpr.getLeftExpr().visit(this, arg).toString();
        String operator = binaryExpr.getOp().text();
        String right = binaryExpr.getRightExpr().visit(this, arg).toString();

        if(binaryExpr.getOpKind()==Kind.EXP){
            if(binaryExpr.getLeftExpr().getType() == Type.IMAGE){
                return "ImageOps.setRGB(" + "((int)Math.round(Math.pow(" + "))))";
            }
            else{
                return "((int)Math.round(Math.pow(" + left + "," + right + ")))";
            }
        }
        else if(binaryExpr.getLeftExpr().getType() == Type.PIXEL){
            if(binaryExpr.getOpKind() == Kind.PLUS){
                return "(ImageOps.binaryPackedPixelPixelOp(ImageOps.OP.PLUS," + left+  "," + right + "))";
            }
            else if (binaryExpr.getOpKind() == Kind.TIMES || binaryExpr.getOpKind() == Kind.DIV){
                return "(ImageOps.binaryPackedPixelIntOp(ImageOps.OP." + binaryExpr.getOpKind() + "," + left +  "," + right + "))";
            }
            else if (binaryExpr.getOpKind() == Kind.EQ){
                return "(ImageOps.binaryPackedPixelBooleanOp(ImageOps.BoolOP.EQUALS," + left +  "," + right + "))";
            }
        }
        else if((binaryExpr.getOpKind() == Kind.DIV || binaryExpr.getOpKind() == Kind.TIMES) && binaryExpr.getLeftExpr().getType() == Type.IMAGE){
            return "(ImageOps.binaryImageScalarOp(ImageOps.OP." + binaryExpr.getOpKind() + "," + left+ "," + right + "))";
        }
        else if(binaryExpr.getLeftExpr().getType() == Type.IMAGE){
            return "(ImageOps.binaryImageImageOp(ImageOps.OP.PLUS," + left+  "," + right + "))";
        }

        else{
            return "(" + left + " " + operator + " " + right + ")";
        }
        throw new CodeGenException("invalid binary expr");
    }

    @Override
    public Object visitBlock(Block block, Object arg) throws PLCCompilerException {
        StringBuilder blockCode = new StringBuilder();
        for (Block.BlockElem elem : block.getElems()) {
            blockCode.append(elem.visit(this, arg));
        }
        return blockCode.toString();
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
        Object trueExpr = conditionalExpr.getTrueExpr().visit(this, arg);

        Object falseExpr = conditionalExpr.getFalseExpr().visit(this, arg);
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

        if (initializer == null) {
            if (declarationType != Type.IMAGE) {
                String aa = typetostring(declarationType);
                javaCode.append("\t\t").append(aa).append(" ").append(declarationName).append(";\n");
            } else {
                if (dimension != null) {
                    Object dimensionResult = declaration.getNameDef().getDimension().visit(this, arg);
                    javaCode.append("\t\tfinal BufferedImage ").append(declarationName)
                            .append(" = ImageOps.makeImage(").append(dimensionResult).append(");\n");
                } else {
                    throw new CodeGenException("No dimension found for declaration");
                }
            }
        } else {
            String aa = typetostring(declarationType);
            javaCode.append("\t\t").append(aa).append(" ").append(declarationName);
            if(declarationType==Type.IMAGE&&dimension!=null&&initializer.getType()==Type.STRING){
                Object initializerResult = initializer.visit(this, arg);
                String w = declaration.getNameDef().getDimension().getWidth().visit(this, arg).toString();
                String h = declaration.getNameDef().getDimension().getHeight().visit(this, arg).toString();

                javaCode.append(" = FileURLIO.readImage(")
                        .append(initializerResult).append(",")
                        .append(w).append(",")
                        .append(h)
                        .append(");\n");

            }


           else if (initializer.getType() == Type.STRING) {
                if (dimension != null) {
                    Object initializerResult = initializer.visit(this, arg);
                    String w = declaration.getNameDef().getDimension().getWidth().visit(this, arg).toString();
                    String h = declaration.getNameDef().getDimension().getHeight().visit(this, arg).toString();

                    javaCode.append(" = ImageOps.copyAndResize(")
                            .append(initializerResult).append(",")
                            .append(w).append(",")
                            .append(h)
                            .append(");\n");
                } else if(declarationType == Type.IMAGE){
                    Object initializerResult = initializer.visit(this, arg);
                    javaCode.append(" = FileURLIO.readImage(")
                            .append(initializerResult)
                            .append(");\n");
                }
                else{
                    javaCode.append("=").append(declaration.getInitializer().visit(this, arg).toString())
                            .append(";\n");
                }
            } else if (initializer.getType() == Type.IMAGE && dimension == null) {
                Object initializerResult = initializer.visit(this, arg);

                javaCode.append(" = ImageOps.cloneImage(")
                        .append(initializerResult)
                        .append(");\n");
            } else if (initializer.getType() == Type.IMAGE && dimension != null) {
                Object initializerResult = initializer.visit(this, arg);
                String w = declaration.getNameDef().getDimension().getWidth().visit(this, arg).toString();
                String h = declaration.getNameDef().getDimension().getHeight().visit(this, arg).toString();

                javaCode.append("= ImageOps.copyAndResize( ")
                        .append(initializerResult).append(",")
                        .append(w).append(",")
                        .append(h)
                        .append(");\n");
            } else {

                javaCode.append("=").append(declaration.getInitializer().visit(this, arg).toString())
                     .append(";\n");
            }
        }

        return null; // or any appropriate return value
    }



    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws PLCCompilerException {

        String width =dimension.getWidth().visit(this,arg).toString();
        String height =dimension.getHeight().visit(this,arg).toString();
        return width + "," + height;
    }

    @Override
    public Object visitDoStatement(DoStatement doStatement, Object arg) throws PLCCompilerException {
        StringBuilder doStatementCode = new StringBuilder();
        boolean hasTrueGuard = false;
        // iterate over guarded blocks
        doStatementCode.append("\t\t{ boolean continue$0=").append(hasTrueGuard).append(";\n");
        hasTrueGuard=!hasTrueGuard;
        doStatementCode.append("\t\twhile (");
        hasTrueGuard = true;
        doStatementCode.append("!continue$0){\n");
        doStatementCode.append("\t\tcontinue$0=true;\n");
        for (GuardedBlock guardedBlock : doStatement.getGuardedBlocks()) {
            isWithinIfStatement = true;
            // if & condition
            doStatementCode.append("\t\tif (");

            Object guard = guardedBlock.getGuard().visit(this, arg);
            doStatementCode.append(guard);

            doStatementCode.append(") {\n");
            doStatementCode.append("\t\tcontinue$0=false;{\n");

            // visit block
            Object block = guardedBlock.getBlock().visit(this, arg);
            doStatementCode.append("\t").append(block);

            doStatementCode.append("\t}}\n");
        }
        isWithinIfStatement = false;
        doStatementCode.append("}};\n");
        javaCode.append(doStatementCode);
        return doStatementCode.toString();
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
        StringBuilder guardedBlockCode = new StringBuilder();

        Object guard = guardedBlock.getGuard().visit(this, arg);
        Object block = guardedBlock.getBlock().visit(this, arg);

        guardedBlockCode.append("if(")
                .append(guard)
                .append(") {")
                .append(block)
                .append("}\n");

        //return guardedBlockCode.toString();
        return null;
    }


    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws PLCCompilerException {
        return identExpr.getNameDef().getJavaName();
    }

    @Override
    public Object visitIfStatement(IfStatement ifStatement, Object arg) throws PLCCompilerException {
        boolean hasTrueGuard = false;
        StringBuilder ifStatementCode = new StringBuilder();

        // iterate over guarded blocks
        int numBlocks = ifStatement.getGuardedBlocks().size();
        int blockIndex = 0;

        for (GuardedBlock guardedBlock : ifStatement.getGuardedBlocks()) {
            isWithinIfStatement = true;

            // if & condition
            if (!hasTrueGuard) {
                ifStatementCode.append("\t\tif (");
                hasTrueGuard = true;
            } else {
                ifStatementCode.append("\t\telse if (");
            }

            Object guard = guardedBlock.getGuard().visit(this, arg);
            ifStatementCode.append(guard);
            ifStatementCode.append(") {\n");

            Object block = guardedBlock.getBlock().visit(this, arg);
            ifStatementCode.append("\t").append(block);

            if (blockIndex < numBlocks - 1) {
                ifStatementCode.append("\t}\n");
            } else {
                // Last else if block, add a semicolon
                ifStatementCode.append("\t};\n");
            }

            blockIndex++;
        }

        isWithinIfStatement = false;

        javaCode.append(ifStatementCode);
        return ifStatementCode.toString();
    }


    @Override
    public Object visitLValue(LValue lValue, Object arg) throws PLCCompilerException {
        StringBuilder lValueCode = new StringBuilder();

        // Visit PixelSelector if present
        if (lValue.getPixelSelector() != null) {
            lValueCode.append(lValue.getPixelSelector().visit(this, arg)).append(".");
        }

        // Visit ChannelSelector if present
        if (lValue.getChannelSelector() != null) {
            lValueCode.append(lValue.getChannelSelector().visit(this, arg)).append(":");
        }

        lValueCode.append(lValue.getNameDef().getJavaName());
        javaCode.append(lValueCode);

        return lValueCode.toString();
    }



    @Override
    public Object visitNameDef(NameDef nameDef, Object arg) throws PLCCompilerException {
        String f = typetostring(nameDef.getType());
        if(nameDef.getDimension()!=null){

            Dimension dim=nameDef.getDimension();
            String name = nameDef.getName();
            return f +" "+dim+ " " + name;
        }else{
        String name = nameDef.getName();
        return f + " " + name;}
    }

    @Override
    public Object visitNumLitExpr(NumLitExpr numLitExpr, Object arg) throws PLCCompilerException {
        return numLitExpr.getText();
    }

    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws PLCCompilerException {
        String x = pixelSelector.xExpr().visit(this, arg).toString();
        String y = pixelSelector.yExpr().visit(this, arg).toString();
        return x + "," + y;
    }

    @Override
    public Object visitPostfixExpr(PostfixExpr postfixExpr, Object arg) throws PLCCompilerException {
        StringBuilder postfixExprCode = new StringBuilder();
        Object aa = postfixExpr.primary().visit(this, arg);

        if (postfixExpr.primary().getType() == Type.PIXEL) {
            String channelExpression = postfixExpr.channel().visit(this, arg).toString();
            postfixExprCode.append("PixelOps.")
                    .append(getColorString(channelExpression.toString()).toLowerCase())
                    .append("(")
                    .append(aa.toString())
                    .append(")");
        } else if (postfixExpr.primary().getType() == Type.IMAGE) {

            if (postfixExpr.channel() == null && postfixExpr.pixel() != null) {
                Object pixelExpression = postfixExpr.pixel().visit(this, arg);

                postfixExprCode.append("ImageOps.getRGB(")
                        .append(postfixExpr.primary().visit(this,arg))
                        .append(",")
                        .append(pixelExpression)
                        .append(")");
            } else if (postfixExpr.channel() != null && postfixExpr.pixel() != null) {
                Object pixelExpression = postfixExpr.pixel().visit(this, arg);
                Object channelExpression = postfixExpr.channel().visit(this, arg);

                postfixExprCode.append("PixelOps.").append(getColorString(channelExpression.toString()))
                        .append("(ImageOps.getRGB(")
                        .append(aa)
                        .append(",")
                        .append(pixelExpression)
                        .append("))");
            } else if (postfixExpr.channel() != null && postfixExpr.pixel() == null) {
                postfixExprCode.append("ImageOps.extract").append(getExtractColorString(postfixExpr.channel().color().toString())).append("(")
                        .append(aa)
                        .append(")");
            } else {
                throw new CodeGenException("no pixel or channel");
            }
        } else {
            // other cases?
        }

        return postfixExprCode.toString();
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

        // close class
        javaCode.append("\t}\n}\n");

        // return in string
        return javaCode.toString();
    }

    @Override
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws PLCCompilerException {
        Object j = returnStatement.getE().visit(this, arg);
        StringBuilder returnCode = new StringBuilder();

        returnCode.append("\t\treturn ").append(j.toString()).append(";\n");

        // Check if the return statement is not within an if statement
        if (!isWithinIfStatement) {
            javaCode.append(returnCode);
        }

        return returnCode.toString();
    }


    @Override
    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws PLCCompilerException {
        return stringLitExpr.getText();
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws PLCCompilerException {
        String operator = getOperatorString(unaryExpr.getOp());
        String operand = unaryExpr.getExpr().visit(this, arg).toString();
        if(operator.equals(".getWidth()")){
            return "("+ operand + ".getWidth())";
        }
        if(operator.equals(".getHeight()")){
           return  "("+ operand + ".getHeight())";
        }
        else{
            return "(" + operator + operand + ")";
        }
    }

    @Override
    public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws PLCCompilerException {
        StringBuilder writeStatementCode = new StringBuilder();
        if(writeStatement.getExpr().getType() == Type.PIXEL){
            Object writeExpr = writeStatement.getExpr().visit(this,arg);
            writeStatementCode.append("\t\tConsoleIO.writePixel(").append(writeExpr).append(");\n");
        }
        else{
            String writeExpr = writeStatement.getExpr().visit(this,arg).toString();
            //System.out.println(writeStatement);
            writeStatementCode.append("\t\tConsoleIO.write(").append(writeExpr).append(");\n");
        }
        javaCode.append(writeStatementCode);
        return writeStatementCode.toString();
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
            case RES_width:
                return ".getWidth()";
            case RES_height:
                return ".getHeight()";
            default:
                throw new IllegalArgumentException("Unknown operator: " + operatorKind);
        }
    }
    private String getColorString(String a) {
        switch (a) {
            case "RES_blue":
            return "blue";
            case "RES_red":
            return "red";
            case "RES_green":
            return "green";
            default:
                throw new IllegalArgumentException("Unknown color: " + a);}}

    private String getExtractColorString(String a) {
        switch (a) {
            case "RES_blue":
                return "Blue";
            case "RES_red":
                return "Red";
            case "RES_green":
                return "Green";
            default:
                throw new IllegalArgumentException("Unknown color: " + a);
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
