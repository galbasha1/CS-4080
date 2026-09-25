import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Chapter9Challenge3 {

    public static void main(String[] args) {
        String source = """
            var i = 0;

            while (true) {
                i = i + 1;

                if (i == 5) {
                    break;
                }

                print i;
            }

            print "done";
            """;

        run(source);
    }
    

    static void run(String source) {
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.scanTokens();

        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();

        if (parser.hadError) return;

        Interpreter interpreter = new Interpreter();
        interpreter.interpret(statements);
    }

    enum TokenType {
        LEFT_PAREN, RIGHT_PAREN,
        LEFT_BRACE, RIGHT_BRACE,
        MINUS, PLUS, SLASH, STAR,
        SEMICOLON,

        BANG, BANG_EQUAL,
        EQUAL, EQUAL_EQUAL,
        GREATER, GREATER_EQUAL,
        LESS, LESS_EQUAL,

        IDENTIFIER, STRING, NUMBER,

        BREAK, ELSE, FALSE, FOR, IF,
        NIL, PRINT, TRUE, VAR, WHILE,

        EOF
    }

    static class Token {

        final TokenType type;
        final String lexeme;
        final Object literal;
        final int line;

        Token(TokenType type, String lexeme, Object literal, int line) {
            this.type = type;
            this.lexeme = lexeme;
            this.literal = literal;
            this.line = line;
        }
    }

    static class Lexer {

        private final String source;
        private final List<Token> tokens = new ArrayList<>();

        private int start = 0;
        private int current = 0;
        private int line = 1;

        private static final Map<String, TokenType> keywords = new HashMap<>();

        static {

            keywords.put("break", TokenType.BREAK);
            keywords.put("else", TokenType.ELSE);
            keywords.put("false", TokenType.FALSE);
            keywords.put("for", TokenType.FOR);
            keywords.put("if", TokenType.IF);
            keywords.put("nil", TokenType.NIL);
            keywords.put("print", TokenType.PRINT);
            keywords.put("true", TokenType.TRUE);
            keywords.put("var", TokenType.VAR);
            keywords.put("while", TokenType.WHILE);
        }



        Lexer(String source) {
            this.source = source;
        }

        List<Token> scanTokens() {
            while (!isAtEnd()) {
                start = current;
                scanToken();
            }

            tokens.add(new Token(TokenType.EOF, "", null, line));
            return tokens;
        }

        private void scanToken() {
            char c = advance();


            switch (c) {
                case '(' -> addToken(TokenType.LEFT_PAREN);
                case ')' -> addToken(TokenType.RIGHT_PAREN);
                case '{' -> addToken(TokenType.LEFT_BRACE);
                case '}' -> addToken(TokenType.RIGHT_BRACE);
                case '-' -> addToken(TokenType.MINUS);
                case '+' -> addToken(TokenType.PLUS);
                case ';' -> addToken(TokenType.SEMICOLON);
                case '*' -> addToken(TokenType.STAR);

                case '!' -> addToken(match('=') ? TokenType.BANG_EQUAL : TokenType.BANG);
                case '=' -> addToken(match('=') ? TokenType.EQUAL_EQUAL : TokenType.EQUAL);
                case '<' -> addToken(match('=') ? TokenType.LESS_EQUAL : TokenType.LESS);
                case '>' -> addToken(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER);

                case '/' -> {
                    if (match('/')) {
                        while (peek() != '\n' && !isAtEnd()) advance();
                    } else {
                        addToken(TokenType.SLASH);
                    }
                }

                case ' ', '\r', '\t' -> {
                }

                case '\n' -> line++;

                case '"' -> string();

                default -> {
                    if (isDigit(c)) {
                        number();
                    } else if (isAlpha(c)) {
                        identifier();
                    } else {
                        System.err.println("[line " + line + "] Unexpected character.");
                    }
                }
            }
        }


        private void identifier() {
            while (isAlphaNumeric(peek())) advance();

            String text = source.substring(start, current);
            TokenType type = keywords.get(text);

            if (type == null) type = TokenType.IDENTIFIER;
            addToken(type);
        }


        private void number() {
            while (isDigit(peek())) advance();

            if (peek() == '.' && isDigit(peekNext())) {
                advance();

                while (isDigit(peek())) advance();
            }

            addToken(
                TokenType.NUMBER,
                Double.parseDouble(source.substring(start, current))
            );
        }

        private void string() {
            while (peek() != '"' && !isAtEnd()) {
                if (peek() == '\n') line++;
                advance();

            }

            if (isAtEnd()) {
                System.err.println("[line " + line + "] Unterminated string.");
                return;
            }


            advance();

            String value = source.substring(start + 1, current - 1);
            addToken(TokenType.STRING, value);
        }

        private boolean match(char expected) {
            if (isAtEnd()) return false;

            if (source.charAt(current) != expected) return false;

            current++;
            return true;
        }

        private char peek() {
            if (isAtEnd()) return '\0';
            return source.charAt(current);
        }


        private char peekNext() {
            if (current + 1 >= source.length()) return '\0';
            return source.charAt(current + 1);
        }

        private boolean isAlpha(char c) {
            return (c >= 'a' && c <= 'z') ||
                   (c >= 'A' && c <= 'Z') ||
                   c == '_';
        }


        private boolean isAlphaNumeric(char c) {
            return isAlpha(c) || isDigit(c);
        }

        private boolean isDigit(char c) {
            return c >= '0' && c <= '9';
        }


        private boolean isAtEnd() {
            return current >= source.length();
        }

        private char advance() {
            return source.charAt(current++);
        }

        private void addToken(TokenType type) {
            addToken(type, null);
        }


        private void addToken(TokenType type, Object literal) {
            String text = source.substring(start, current);
            tokens.add(new Token(type, text, literal, line));
        }
    }

    interface Expr {}

    static class Literal implements Expr {
        final Object value;


        Literal(Object value) {
            this.value = value;
        }
    }

    static class Grouping implements Expr {
        final Expr expression;

        Grouping(Expr expression) {
            this.expression = expression;

        }
    }


    static class Unary implements Expr {
        final Token operator;
        final Expr right;

        Unary(Token operator, Expr right) {

            this.operator = operator;
            this.right = right;
        }
    }

    static class Binary implements Expr {

        final Expr left;
        final Token operator;
        final Expr right;

        Binary(Expr left, Token operator, Expr right) {
            this.left = left;
            this.operator = operator;
            this.right = right;
        }
    }

    static class Variable implements Expr {
        final Token name;

        Variable(Token name) {
            this.name = name;
        }
    }


    static class Assign implements Expr {
        final Token name;
        final Expr value;

        Assign(Token name, Expr value) {
            this.name = name;
            this.value = value;
        }
    }

    
    interface Stmt {}

    static class ExpressionStmt implements Stmt {
        final Expr expression;

        ExpressionStmt(Expr expression) {
            this.expression = expression;
        }
    }



    static class PrintStmt implements Stmt {
        final Expr expression;

        PrintStmt(Expr expression) {
            this.expression = expression;
        }
    }

    static class VarStmt implements Stmt {
        final Token name;
        final Expr initializer;


        VarStmt(Token name, Expr initializer) {
            this.name = name;
            this.initializer = initializer;
        }
    }

    static class BlockStmt implements Stmt {
        final List<Stmt> statements;

        BlockStmt(List<Stmt> statements) {
            this.statements = statements;
        }

    }

    static class IfStmt implements Stmt {
        final Expr condition;
        final Stmt thenBranch;
        final Stmt elseBranch;


        IfStmt(Expr condition, Stmt thenBranch, Stmt elseBranch) {
            this.condition = condition;
            this.thenBranch = thenBranch;
            this.elseBranch = elseBranch;
        }
    }

    static class WhileStmt implements Stmt {
        final Expr condition;
        final Stmt body;

        WhileStmt(Expr condition, Stmt body) {
            this.condition = condition;
            this.body = body;
        }
    }


    static class BreakStmt implements Stmt {
        final Token keyword;

        BreakStmt(Token keyword) {
            this.keyword = keyword;
        }
    }




    static class Parser {
        private static class ParseError extends RuntimeException {}

        private final List<Token> tokens;
        private int current = 0;
        private int loopDepth = 0;


        boolean hadError = false;

        Parser(List<Token> tokens) {
            this.tokens = tokens;
        }

        List<Stmt> parse() {
            List<Stmt> statements = new ArrayList<>();


            while (!isAtEnd()) {
                Stmt statement = declaration();
                if (statement != null) statements.add(statement);
            }

            return statements;
        }


        private Stmt declaration() {
            try {
                if (match(TokenType.VAR)) return varDeclaration();
                return statement();
            } catch (ParseError error) {
                synchronize();
                return null;
            }
        }



        private Stmt varDeclaration() {
            Token name = consume(TokenType.IDENTIFIER, "Expect variable name.");

            Expr initializer = null;
            if (match(TokenType.EQUAL)) {
                initializer = expression();
            }

            consume(TokenType.SEMICOLON, "Expect ';' after variable declaration.");
            return new VarStmt(name, initializer);
        }





        private Stmt statement() {
            if (match(TokenType.FOR)) return forStatement();
            if (match(TokenType.IF)) return ifStatement();
            if (match(TokenType.PRINT)) return printStatement();
            if (match(TokenType.WHILE)) return whileStatement();
            if (match(TokenType.BREAK)) return breakStatement();

            if (match(TokenType.LEFT_BRACE)) {
                return new BlockStmt(block());
            }

            return expressionStatement();
        }




        private Stmt breakStatement() {
            Token keyword = previous();

            if (loopDepth == 0) {
                throw error(keyword, "Can't use 'break' outside of a loop.");
            }

            consume(TokenType.SEMICOLON, "Expect ';' after 'break'.");
            return new BreakStmt(keyword);
        }




        private Stmt whileStatement() {
            consume(TokenType.LEFT_PAREN, "Expect '(' after 'while'.");
            Expr condition = expression();
            consume(TokenType.RIGHT_PAREN, "Expect ')' after condition.");

            loopDepth++;

            Stmt body;
            try {
                body = statement();
            } finally {
                loopDepth--;
            }

            return new WhileStmt(condition, body);
        }



        private Stmt forStatement() {

            consume(TokenType.LEFT_PAREN, "Expect '(' after 'for'.");

            Stmt initializer;

            if (match(TokenType.SEMICOLON)) {
                initializer = null;

            } else if (match(TokenType.VAR)) {
                initializer = varDeclaration();
            } else {
                initializer = expressionStatement();
            }

            Expr condition = null;

            if (!check(TokenType.SEMICOLON)) {
                condition = expression();
            }

            consume(TokenType.SEMICOLON, "Expect ';' after loop condition.");

            Expr increment = null;



            if (!check(TokenType.RIGHT_PAREN)) {
                increment = expression();
            }

            consume(TokenType.RIGHT_PAREN, "Expect ')' after for clauses.");

            loopDepth++;


            Stmt body;
            try {
                body = statement();
            } finally {
                loopDepth--;
            }

            if (increment != null) {
                List<Stmt> statements = new ArrayList<>();
                statements.add(body);
                statements.add(new ExpressionStmt(increment));
                body = new BlockStmt(statements);
            }

            if (condition == null) {
                condition = new Literal(true);
            }

            body = new WhileStmt(condition, body);

            if (initializer != null) {
                List<Stmt> statements = new ArrayList<>();
                statements.add(initializer);

                statements.add(body);
                body = new BlockStmt(statements);
            }

            return body;
        }

        private Stmt ifStatement() {
            consume(TokenType.LEFT_PAREN, "Expect '(' after 'if'.");
            Expr condition = expression();
            consume(TokenType.RIGHT_PAREN, "Expect ')' after if condition.");

            Stmt thenBranch = statement();
            Stmt elseBranch = null;

            if (match(TokenType.ELSE)) {
                elseBranch = statement();

            }

            return new IfStmt(condition, thenBranch, elseBranch);
        }

        private Stmt printStatement() {
            Expr value = expression();
            consume(TokenType.SEMICOLON, "Expect ';' after value.");
            return new PrintStmt(value);
        }


        private Stmt expressionStatement() {

            Expr expr = expression();
            consume(TokenType.SEMICOLON, "Expect ';' after expression.");
            return new ExpressionStmt(expr);
        }

        private List<Stmt> block() {

            List<Stmt> statements = new ArrayList<>();

            while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
                Stmt statement = declaration();
                if (statement != null) statements.add(statement);
            }

            consume(TokenType.RIGHT_BRACE, "Expect '}' after block.");
            return statements;
        }


        private Expr expression() {
            return assignment();
        }

        private Expr assignment() {
            Expr expr = equality();

            if (match(TokenType.EQUAL)) {
                Token equals = previous();
                Expr value = assignment();

                if (expr instanceof Variable variable) {
                    return new Assign(variable.name, value);
                }



                error(equals, "Invalid assignment target.");
            }

            return expr;
        }




        private Expr equality() {
            Expr expr = comparison();


            while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
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

            while (match(TokenType.MINUS, TokenType.PLUS)) {
                Token operator = previous();
                Expr right = factor();

                expr = new Binary(expr, operator, right);
            }


            return expr;
        }

        private Expr factor() {
            Expr expr = unary();


            while (match(TokenType.SLASH, TokenType.STAR)) {
                Token operator = previous();
                Expr right = unary();
                expr = new Binary(expr, operator, right);
            }


            return expr;
        }

        private Expr unary() {


            if (match(TokenType.BANG, TokenType.MINUS)) {
                Token operator = previous();
                Expr right = unary();
                return new Unary(operator, right);
            }

            return primary();

        }

        private Expr primary() {
            if (match(TokenType.FALSE)) return new Literal(false);
            if (match(TokenType.TRUE)) return new Literal(true);
            if (match(TokenType.NIL)) return new Literal(null);

            if (match(TokenType.NUMBER, TokenType.STRING)) {
                return new Literal(previous().literal);
            }



            if (match(TokenType.IDENTIFIER)) {
                return new Variable(previous());
            }

            if (match(TokenType.LEFT_PAREN)) {
                Expr expr = expression();
                consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.");
                return new Grouping(expr);

            }

            throw error(peek(), "Expect expression.");
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

        private Token consume(TokenType type, String message) {
            if (check(type)) return advance();

            throw error(peek(), message);
        }


        private boolean check(TokenType type) {
            if (isAtEnd()) return type == TokenType.EOF;
            return peek().type == type;
        }


        private Token advance() {
            if (!isAtEnd()) current++;
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


        private ParseError error(Token token, String message) {
            hadError = true;



            if (token.type == TokenType.EOF) {
                System.err.println(
                    "[line " + token.line + "] Error at end: " + message
                );
            } else {

                System.err.println(
                    "[line " + token.line + "] Error at '" +
                    token.lexeme + "': " + message
                );
            }


            return new ParseError();
        }

        private void synchronize() {
            advance();


            while (!isAtEnd()) {
                if (previous().type == TokenType.SEMICOLON) return;


                switch (peek().type) {
                    case BREAK, FOR, IF, PRINT, VAR, WHILE -> {
                        return;
                    }

                    default -> {
                    }
                }


                advance();
            }
        }
    }

    static class Environment {
        final Environment enclosing;


        private final Map<String, Object> values = new HashMap<>();

        Environment() {


            enclosing = null;
        }

        Environment(Environment enclosing) {
            this.enclosing = enclosing;
        }

        void define(String name, Object value) {
            values.put(name, value);
        }

        Object get(Token name) {

            if (values.containsKey(name.lexeme)) {
                return values.get(name.lexeme);
            }


            if (enclosing != null) {
                return enclosing.get(name);
            }


            throw new RuntimeException(
                "Undefined variable '" + name.lexeme + "'."
            );
        }



        void assign(Token name, Object value) {
            if (values.containsKey(name.lexeme)) {
                values.put(name.lexeme, value);
                return;
            }


            if (enclosing != null) {
                enclosing.assign(name, value);

                return;
            }

            throw new RuntimeException(
                "Undefined variable '" + name.lexeme + "'."
            );
        }
    }



    static class BreakSignal extends RuntimeException {
        BreakSignal() {
            super(null, null, false, false);
        }
    }


    static class Interpreter {
        private Environment environment = new Environment();

        void interpret(List<Stmt> statements) {
            try {
                for (Stmt statement : statements) {
                    execute(statement);
                }
            } catch (RuntimeException error) {
                System.err.println("Runtime error: " + error.getMessage());
            }


        }

        private void execute(Stmt stmt) {
            if (stmt instanceof ExpressionStmt s) {
                evaluate(s.expression);


            } else if (stmt instanceof PrintStmt s) {
                Object value = evaluate(s.expression);
                System.out.println(stringify(value));

            } else if (stmt instanceof VarStmt s) {
                Object value = null;


                if (s.initializer != null) {
                    value = evaluate(s.initializer);
                }

                environment.define(s.name.lexeme, value);

            } else if (stmt instanceof BlockStmt s) {
                executeBlock(

                    s.statements,
                    new Environment(environment)
                );

            } else if (stmt instanceof IfStmt s) {
                if (isTruthy(evaluate(s.condition))) {
                    execute(s.thenBranch);
                } else if (s.elseBranch != null) {
                    execute(s.elseBranch);

                }

            } else if (stmt instanceof WhileStmt s) {
                try {
                    while (isTruthy(evaluate(s.condition))) {
                        execute(s.body);
                    }
                } catch (BreakSignal signal) {

                }

            } else if (stmt instanceof BreakStmt) {
                throw new BreakSignal();
            }
        }


        private void executeBlock(
                List<Stmt> statements,
                Environment newEnvironment) {

            Environment previous = environment;

            try {
                environment = newEnvironment;


                for (Stmt statement : statements) {
                    execute(statement);
                }

            } finally {

                environment = previous;
            }
        }


        private Object evaluate(Expr expr) {
            if (expr instanceof Literal e) {
                return e.value;

            } else if (expr instanceof Grouping e) {
                return evaluate(e.expression);

            } else if (expr instanceof Variable e) {
                return environment.get(e.name);


            } else if (expr instanceof Assign e) {
                Object value = evaluate(e.value);
                environment.assign(e.name, value);
                return value;

            } else if (expr instanceof Unary e) {
                Object right = evaluate(e.right);

                return switch (e.operator.type) {
                    case MINUS ->
                        -asNumber(e.operator, right);

                    case BANG ->
                        !isTruthy(right);

                    default ->
                        null;
                };

            } else if (expr instanceof Binary e) {
                Object left = evaluate(e.left);
                Object right = evaluate(e.right);

                return switch (e.operator.type) {

                    case MINUS ->
                        asNumber(e.operator, left)
                        - asNumber(e.operator, right);

                    case SLASH ->
                        asNumber(e.operator, left)
                        / asNumber(e.operator, right);

                    case STAR ->
                        asNumber(e.operator, left)
                        * asNumber(e.operator, right);

                    case PLUS ->
                        plus(e.operator, left, right);

                    case GREATER ->
                        asNumber(e.operator, left)
                        > asNumber(e.operator, right);

                    case GREATER_EQUAL ->
                        asNumber(e.operator, left)
                        >= asNumber(e.operator, right);

                    case LESS ->
                        asNumber(e.operator, left)
                        < asNumber(e.operator, right);

                    case LESS_EQUAL ->
                        asNumber(e.operator, left)
                        <= asNumber(e.operator, right);

                    case BANG_EQUAL ->
                        !isEqual(left, right);

                    case EQUAL_EQUAL ->
                        isEqual(left, right);

                    default ->
                        null;
                };
            }

            return null;
        }

        private Object plus(
                Token operator,
                Object left,
                Object right) {



            if (left instanceof Double &&
                right instanceof Double) {

                return (double) left + (double) right;
            }

            if (left instanceof String &&
                right instanceof String) {

                return (String) left + (String) right;
            }


            throw new RuntimeException(
                "Operands for '" + operator.lexeme +
                "' must be two numbers or two strings."
            );
        }


        private double asNumber(
                Token operator,
                Object value) {

            if (value instanceof Double number) {
                return number;
            }

            throw new RuntimeException(
                "Operand for '" + operator.lexeme +
                "' must be a number."
            );
        }



        private boolean isTruthy(Object value) {
            if (value == null) return false;
            if (value instanceof Boolean bool) return bool;
            return true;
        }



        private boolean isEqual(Object a, Object b) {
            if (a == null && b == null) return true;
            if (a == null) return false;

            return a.equals(b);
        }


        private String stringify(Object value) {
            if (value == null) return "nil";

            if (value instanceof Double number) {
                String text = number.toString();

                if (text.endsWith(".0")) {
                    text = text.substring(
                        0,
                        text.length() - 2
                    );
                }

                return text;
            }

            return value.toString();
        }
    }
}