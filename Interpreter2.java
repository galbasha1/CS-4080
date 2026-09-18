public class Interpreter implements Interpreter.Expr.Visitor<Object> {

    enum TokenType {
        MINUS, PLUS, SLASH, STAR,
        BANG,
        GREATER, GREATER_EQUAL,
        LESS, LESS_EQUAL,
        BANG_EQUAL, EQUAL_EQUAL
    }

    static class Token {
        final TokenType type;
        final String lexeme;

        Token(TokenType type, String lexeme) {
            this.type = type;
            this.lexeme = lexeme;
        }
    }

    static abstract class Expr {

        interface Visitor<R> {
            R visitBinaryExpr(Binary expr);
            R visitGroupingExpr(Grouping expr);
            R visitLiteralExpr(Literal expr);
            R visitUnaryExpr(Unary expr);
        }

        abstract <R> R accept(Visitor<R> visitor);

        static class Binary extends Expr {
            final Expr left;
            final Token operator;
            final Expr right;

            Binary(Expr left, Token operator, Expr right) {
                this.left = left;
                this.operator = operator;
                this.right = right;
            }

            @Override
            <R> R accept(Visitor<R> visitor) {
                return visitor.visitBinaryExpr(this);
            }
        }

        static class Grouping extends Expr {
            final Expr expression;

            Grouping(Expr expression) {
                this.expression = expression;
            }

            @Override
            <R> R accept(Visitor<R> visitor) {
                return visitor.visitGroupingExpr(this);
            }
        }

        static class Literal extends Expr {
            final Object value;

            Literal(Object value) {
                this.value = value;
            }

            @Override
            <R> R accept(Visitor<R> visitor) {
                return visitor.visitLiteralExpr(this);
            }
        }

        static class Unary extends Expr {
            final Token operator;
            final Expr right;

            Unary(Token operator, Expr right) {
                this.operator = operator;
                this.right = right;
            }

            @Override
            <R> R accept(Visitor<R> visitor) {
                return visitor.visitUnaryExpr(this);
            }
        }
    }

    static class RuntimeError extends RuntimeException {
        final Token token;

        RuntimeError(Token token, String message) {
            super(message);
            this.token = token;
        }
    }

    void interpret(Expr expression) {
        try {
            Object value = evaluate(expression);
            System.out.println(stringify(value));
        } catch (RuntimeError error) {
            System.out.println("Runtime error: " + error.getMessage());
        }
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double) right;

            case BANG:
                return !isTruthy(right);

            default:
                return null;
        }
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {

        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {

            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double) left - (double) right;

            case SLASH:
                checkNumberOperands(expr.operator, left, right);

                if ((double) right == 0.0) {
                    throw new RuntimeError(
                            expr.operator,
                            "Cannot divide by zero.");
                }

                return (double) left / (double) right;

            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double) left * (double) right;

            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double) left + (double) right;
                }

                if (left instanceof String && right instanceof String) {
                    return (String) left + (String) right;
                }

                throw new RuntimeError(
                        expr.operator,
                        "Operands must be two numbers or two strings.");

            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (double) left > (double) right;

            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double) left >= (double) right;

            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double) left < (double) right;

            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double) left <= (double) right;

            case BANG_EQUAL:
                return !isEqual(left, right);

            case EQUAL_EQUAL:
                return isEqual(left, right);

            default:
                return null;
        }
    }

    private boolean isTruthy(Object object) {
        if (object == null) {
            return false;
        }

        if (object instanceof Boolean) {
            return (boolean) object;
        }

        return true;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) {
            return true;
        }

        if (a == null) {
            return false;
        }

        return a.equals(b);
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) {
            return;
        }

        throw new RuntimeError(
                operator,
                "Operand must be a number.");
    }

    private void checkNumberOperands(
            Token operator, Object left, Object right) {

        if (left instanceof Double && right instanceof Double) {
            return;
        }

        throw new RuntimeError(
                operator,
                "Operands must be numbers.");
    }

    private String stringify(Object object) {

        if (object == null) {
            return "nil";
        }

        if (object instanceof Double) {
            String text = object.toString();

            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }

            return text;
        }

        return object.toString();
    }

    public static void main(String[] args) {

        Interpreter interpreter = new Interpreter();

        Token divide = new Token(TokenType.SLASH, "/");

        Expr normalDivision = new Expr.Binary(
                new Expr.Literal(10.0),
                divide,
                new Expr.Literal(2.0));

        Expr divideByZero = new Expr.Binary(
                new Expr.Literal(10.0),
                divide,
                new Expr.Literal(0.0));

        System.out.println("10 / 2:");
        interpreter.interpret(normalDivision);

        System.out.println("\n10 / 0:");
        interpreter.interpret(divideByZero);
    }
}