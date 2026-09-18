import java.util.ArrayList;
import java.util.List;

public class Parser {

    enum TokenType {
        LEFT_PAREN, RIGHT_PAREN,
        COMMA,
        MINUS, PLUS, SLASH, STAR,

        BANG, BANG_EQUAL,
        EQUAL_EQUAL,
        GREATER, GREATER_EQUAL,
        LESS, LESS_EQUAL,

        NUMBER,
        EOF
    }

    static class Token {
        TokenType type;
        String lexeme;
        Object literal;

        Token(TokenType type, String lexeme, Object literal) {
            this.type = type;
            this.lexeme = lexeme;
            this.literal = literal;
        }
    }

    static abstract class Expr {
        abstract String print();
    }

    static class Binary extends Expr {
        Expr left;
        Token operator;
        Expr right;

        Binary(Expr left, Token operator, Expr right) {
            this.left = left;
            this.operator = operator;
            this.right = right;
        }

        @Override
        String print() {
            return "(" + operator.lexeme + " "
                    + left.print() + " "
                    + right.print() + ")";
        }
    }

    static class Unary extends Expr {
        Token operator;
        Expr right;

        Unary(Token operator, Expr right) {
            this.operator = operator;
            this.right = right;
        }

        @Override
        String print() {
            return "(" + operator.lexeme + " " + right.print() + ")";
        }
    }

    static class Grouping extends Expr {
        Expr expression;

        Grouping(Expr expression) {
            this.expression = expression;
        }

        @Override
        String print() {
            return "(group " + expression.print() + ")";
        }
    }

    static class Literal extends Expr {
        Object value;

        Literal(Object value) {
            this.value = value;
        }

        @Override
        String print() {
            if (value == null) {
                return "nil";
            }

            if (value instanceof Double) {
                double number = (Double) value;

                if (number == (long) number) {
                    return Long.toString((long) number);
                }
            }

            return value.toString();
        }
    }

    static class Lexer {

        private final String source;
        private final List<Token> tokens = new ArrayList<>();

        private int current = 0;

        Lexer(String source) {
            this.source = source;
        }

        List<Token> scanTokens() {
            while (!isAtEnd()) {
                scanToken();
            }

            tokens.add(new Token(TokenType.EOF, "", null));
            return tokens;
        }

        private void scanToken() {
            char c = advance();

            switch (c) {
                case '(':
                    addToken(TokenType.LEFT_PAREN);
                    break;

                case ')':
                    addToken(TokenType.RIGHT_PAREN);
                    break;

                case ',':
                    addToken(TokenType.COMMA);
                    break;

                case '-':
                    addToken(TokenType.MINUS);
                    break;

                case '+':
                    addToken(TokenType.PLUS);
                    break;

                case '/':
                    addToken(TokenType.SLASH);
                    break;

                case '*':
                    addToken(TokenType.STAR);
                    break;

                case '!':
                    if (match('=')) {
                        tokens.add(new Token(
                                TokenType.BANG_EQUAL, "!=", null));
                    } else {
                        addToken(TokenType.BANG);
                    }
                    break;

                case '=':
                    if (match('=')) {
                        tokens.add(new Token(
                                TokenType.EQUAL_EQUAL, "==", null));
                    }
                    break;

                case '>':
                    if (match('=')) {
                        tokens.add(new Token(
                                TokenType.GREATER_EQUAL, ">=", null));
                    } else {
                        addToken(TokenType.GREATER);
                    }
                    break;

                case '<':
                    if (match('=')) {
                        tokens.add(new Token(
                                TokenType.LESS_EQUAL, "<=", null));
                    } else {
                        addToken(TokenType.LESS);
                    }
                    break;

                case ' ':
                case '\r':
                case '\t':
                case '\n':
                    break;

                default:
                    if (isDigit(c)) {
                        number(c);
                    } else {
                        System.out.println("Unexpected character: " + c);
                    }

                    break;
            }
        }

        private void number(char first) {
            StringBuilder value = new StringBuilder();
            value.append(first);

            while (isDigit(peek())) {
                value.append(advance());
            }

            if (peek() == '.' && isDigit(peekNext())) {
                value.append(advance());

                while (isDigit(peek())) {
                    value.append(advance());
                }
            }

            String text = value.toString();

            tokens.add(new Token(
                    TokenType.NUMBER,
                    text,
                    Double.parseDouble(text)));
        }

        private boolean match(char expected) {
            if (isAtEnd()) {
                return false;
            }

            if (source.charAt(current) != expected) {
                return false;
            }

            current++;
            return true;
        }

        private char peek() {
            if (isAtEnd()) {
                return '\0';
            }

            return source.charAt(current);
        }

        private char peekNext() {
            if (current + 1 >= source.length()) {
                return '\0';
            }

            return source.charAt(current + 1);
        }

        private char advance() {
            return source.charAt(current++);
        }

        private boolean isDigit(char c) {
            return c >= '0' && c <= '9';
        }

        private boolean isAtEnd() {
            return current >= source.length();
        }

        private void addToken(TokenType type) {
            String text;

            switch (type) {
                case LEFT_PAREN:
                    text = "(";
                    break;
                case RIGHT_PAREN:
                    text = ")";
                    break;
                case COMMA:
                    text = ",";
                    break;
                case MINUS:
                    text = "-";
                    break;
                case PLUS:
                    text = "+";
                    break;
                case SLASH:
                    text = "/";
                    break;
                case STAR:
                    text = "*";
                    break;
                case BANG:
                    text = "!";
                    break;
                case GREATER:
                    text = ">";
                    break;
                case LESS:
                    text = "<";
                    break;
                default:
                    text = "";
            }

            tokens.add(new Token(type, text, null));
        }
    }

    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    Expr parse() {
        return expression();
    }


    private Expr expression() {
        return comma();
    }

    private Expr comma() {
        Expr expr = equality();

        while (match(TokenType.COMMA)) {
            Token operator = previous();
            Expr right = equality();

            expr = new Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr equality() {
        Expr expr = comparison();

        while (match(
                TokenType.BANG_EQUAL,
                TokenType.EQUAL_EQUAL)) {

            Token operator = previous();
            Expr right = comparison();

            expr = new Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr comparison() {
        Expr expr = term();

        while (match(
                TokenType.GREATER,
                TokenType.GREATER_EQUAL,
                TokenType.LESS,
                TokenType.LESS_EQUAL)) {

            Token operator = previous();
            Expr right = term();

            expr = new Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr term() {
        Expr expr = factor();

        while (match(
                TokenType.MINUS,
                TokenType.PLUS)) {

            Token operator = previous();
            Expr right = factor();

            expr = new Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr factor() {
        Expr expr = unary();

        while (match(
                TokenType.SLASH,
                TokenType.STAR)) {

            Token operator = previous();
            Expr right = unary();

            expr = new Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr unary() {
        if (match(
                TokenType.BANG,
                TokenType.MINUS)) {

            Token operator = previous();
            Expr right = unary();

            return new Unary(operator, right);
        }

        return primary();
    }

    private Expr primary() {
        if (match(TokenType.NUMBER)) {
            return new Literal(previous().literal);
        }

        if (match(TokenType.LEFT_PAREN)) {
            Expr expr = expression();

            if (!match(TokenType.RIGHT_PAREN)) {
                throw new RuntimeException(
                        "Expected ')' after expression.");
            }

            return new Grouping(expr);
        }

        throw new RuntimeException("Expected expression.");
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }

        return false;
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) {
            return type == TokenType.EOF;
        }

        return peek().type == type;
    }

    private Token advance() {
        if (!isAtEnd()) {
            current++;
        }

        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    public static void main(String[] args) {

        String source = "1, 2 + 3 * 4, 5";

        Lexer lexer = new Lexer(source);

        List<Token> tokens = lexer.scanTokens();

        Parser parser = new Parser(tokens);

        Expr expression = parser.parse();

        System.out.println("Input:");
        System.out.println(source);

        System.out.println("\nParsed expression:");
        System.out.println(expression.print());
    }
}