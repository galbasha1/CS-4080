import java.util.ArrayList;
import java.util.List;


public class Parser {
    interface Expr { Object evaluate(Interpreter interpreter); }
    interface Stmt { void execute(Interpreter interpreter); }

    static class Literal implements Expr {
        final Object value;
        Literal(Object value) { this.value = value; }
        public Object evaluate(Interpreter i) { return value; }
    }
    static class Variable implements Expr {
        final Scanner.Token name;
        Variable(Scanner.Token name) { this.name = name; }
        public Object evaluate(Interpreter i) { return i.environment.get(name); }
    }
    static class Assign implements Expr {
        final Scanner.Token name; final Expr value;
        Assign(Scanner.Token name, Expr value) { this.name = name; this.value = value; }
        public Object evaluate(Interpreter i) {
            Object result = value.evaluate(i); i.environment.assign(name, result); return result;
        }
    }
    static class Unary implements Expr {
        final Scanner.Token operator; final Expr right;
        Unary(Scanner.Token operator, Expr right) { this.operator = operator; this.right = right; }
        public Object evaluate(Interpreter i) { return i.unary(operator, right.evaluate(i)); }
    }
    static class Binary implements Expr {
        final Expr left, right; final Scanner.Token operator;
        Binary(Expr left, Scanner.Token operator, Expr right) {
            this.left = left; this.operator = operator; this.right = right;
        }
        public Object evaluate(Interpreter i) {
            return i.binary(operator, left.evaluate(i), right.evaluate(i));
        }
    }
    static class Logical implements Expr {
        final Expr left, right; final Scanner.Token operator;
        Logical(Expr left, Scanner.Token operator, Expr right) {
            this.left = left; this.operator = operator; this.right = right;
        }
        public Object evaluate(Interpreter i) {
            Object value = left.evaluate(i);
            if (operator.type == Scanner.TokenType.OR ? Interpreter.truthy(value) : !Interpreter.truthy(value))
                return value;
            return right.evaluate(i);
        }
    }
    static class Call implements Expr {
        final Expr callee; final Scanner.Token paren; final List<Expr> arguments;
        Call(Expr callee, Scanner.Token paren, List<Expr> arguments) {
            this.callee = callee; this.paren = paren; this.arguments = arguments;
        }
        public Object evaluate(Interpreter i) {
            Object target = callee.evaluate(i);
            List<Object> values = new ArrayList<>();
            for (Expr argument : arguments) values.add(argument.evaluate(i));
            return i.call(target, paren, values);
        }
    }

    static class Function implements Expr {
        final List<Scanner.Token> parameters; final List<Stmt> body;
        Function(List<Scanner.Token> parameters, List<Stmt> body) {
            this.parameters = parameters; this.body = body;
        }
        public Object evaluate(Interpreter i) { return new Interpreter.LoxFunction(null, this, i.environment); }
    }
    static class FunctionDeclaration implements Stmt {
        final Scanner.Token name; final Function function;
        FunctionDeclaration(Scanner.Token name, Function function) { this.name = name; this.function = function; }
        public void execute(Interpreter i) {
            i.environment.define(name.lexeme, new Interpreter.LoxFunction(name.lexeme, function, i.environment));
        }
    }
    static class Expression implements Stmt {
        final Expr expression;
        Expression(Expr expression) { this.expression = expression; }
        public void execute(Interpreter i) { expression.evaluate(i); }
    }
    static class Print implements Stmt {
        final Expr expression;
        Print(Expr expression) { this.expression = expression; }
        public void execute(Interpreter i) { System.out.println(Interpreter.stringify(expression.evaluate(i))); }
    }
    static class Var implements Stmt {
        final Scanner.Token name; final Expr initializer;
        Var(Scanner.Token name, Expr initializer) { this.name = name; this.initializer = initializer; }
        public void execute(Interpreter i) {
            i.environment.define(name.lexeme, initializer == null ? null : initializer.evaluate(i));
        }
    }
    static class Block implements Stmt {
        final List<Stmt> statements;
        Block(List<Stmt> statements) { this.statements = statements; }
        public void execute(Interpreter i) { i.executeBlock(statements, new Interpreter.Environment(i.environment)); }
    }
    static class If implements Stmt {
        final Expr condition; final Stmt thenBranch, elseBranch;
        If(Expr condition, Stmt thenBranch, Stmt elseBranch) {
            this.condition = condition; this.thenBranch = thenBranch; this.elseBranch = elseBranch;
        }
        public void execute(Interpreter i) {
            if (Interpreter.truthy(condition.evaluate(i))) thenBranch.execute(i);
            else if (elseBranch != null) elseBranch.execute(i);
        }
    }
    static class While implements Stmt {
        final Expr condition; final Stmt body;
        While(Expr condition, Stmt body) { this.condition = condition; this.body = body; }
        public void execute(Interpreter i) { while (Interpreter.truthy(condition.evaluate(i))) body.execute(i); }
    }
    static class Return implements Stmt {
        final Expr value;
        Return(Expr value) { this.value = value; }
        public void execute(Interpreter i) { throw new Interpreter.Return(value == null ? null : value.evaluate(i)); }
    }

    private final List<Scanner.Token> tokens;
    private int current;
    private int functionDepth;
    public Parser(List<Scanner.Token> tokens) { this.tokens = tokens; }
    public List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!check(Scanner.TokenType.EOF)) statements.add(declaration());
        return statements;
    }
    private Stmt declaration() {

        if (check(Scanner.TokenType.FUN) && checkNext(Scanner.TokenType.IDENTIFIER)) {
            advance();
            Scanner.Token name = consume(Scanner.TokenType.IDENTIFIER, "Expect function name.");
            return new FunctionDeclaration(name, function());
        }
        if (match(Scanner.TokenType.VAR)) return varDeclaration();
        return statement();
    }
    private Function function() {
        consume(Scanner.TokenType.LEFT_PAREN, "Expect '(' before parameters.");
        List<Scanner.Token> parameters = new ArrayList<>();
        if (!check(Scanner.TokenType.RIGHT_PAREN)) {
            do {
                if (parameters.size() >= 255) throw error(peek(), "Can't have more than 255 parameters.");
                parameters.add(consume(Scanner.TokenType.IDENTIFIER, "Expect parameter name."));
            } while (match(Scanner.TokenType.COMMA));
        }
        consume(Scanner.TokenType.RIGHT_PAREN, "Expect ')' after parameters.");
        consume(Scanner.TokenType.LEFT_BRACE, "Expect '{' before function body.");
        functionDepth++;
        try { return new Function(parameters, block()); }
        finally { functionDepth--; }
    }
    private Stmt varDeclaration() {
        Scanner.Token name = consume(Scanner.TokenType.IDENTIFIER, "Expect variable name.");
        Expr initializer = match(Scanner.TokenType.EQUAL) ? expression() : null;
        consume(Scanner.TokenType.SEMICOLON, "Expect ';' after variable declaration.");
        return new Var(name, initializer);
    }
    private Stmt statement() {
        if (match(Scanner.TokenType.FOR)) return forStatement();
        if (match(Scanner.TokenType.IF)) return ifStatement();
        if (match(Scanner.TokenType.WHILE)) return whileStatement();
        if (match(Scanner.TokenType.PRINT)) {
            Expr value = expression(); consume(Scanner.TokenType.SEMICOLON, "Expect ';' after value.");
            return new Print(value);
        }
        if (match(Scanner.TokenType.RETURN)) {
            if (functionDepth == 0) throw error(previous(), "Can't return from top-level code.");
            Expr value = check(Scanner.TokenType.SEMICOLON) ? null : expression();
            consume(Scanner.TokenType.SEMICOLON, "Expect ';' after return value.");
            return new Return(value);
        }
        if (match(Scanner.TokenType.LEFT_BRACE)) return new Block(block());
        return expressionStatement();
    }
    private Stmt expressionStatement() {
        Expr expression = expression();
        consume(Scanner.TokenType.SEMICOLON, "Expect ';' after expression.");
        return new Expression(expression);
    }
    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();
        while (!check(Scanner.TokenType.RIGHT_BRACE) && !check(Scanner.TokenType.EOF)) statements.add(declaration());
        consume(Scanner.TokenType.RIGHT_BRACE, "Expect '}' after block.");
        return statements;
    }
    private Stmt ifStatement() {
        consume(Scanner.TokenType.LEFT_PAREN, "Expect '(' after 'if'.");
        Expr condition = expression(); consume(Scanner.TokenType.RIGHT_PAREN, "Expect ')' after condition.");
        Stmt thenBranch = statement();
        return new If(condition, thenBranch, match(Scanner.TokenType.ELSE) ? statement() : null);
    }
    private Stmt whileStatement() {
        consume(Scanner.TokenType.LEFT_PAREN, "Expect '(' after 'while'.");
        Expr condition = expression(); consume(Scanner.TokenType.RIGHT_PAREN, "Expect ')' after condition.");
        return new While(condition, statement());
    }
    private Stmt forStatement() {
        consume(Scanner.TokenType.LEFT_PAREN, "Expect '(' after 'for'.");
        Stmt initializer = match(Scanner.TokenType.SEMICOLON) ? null
            : match(Scanner.TokenType.VAR) ? varDeclaration() : expressionStatement();
        Expr condition = check(Scanner.TokenType.SEMICOLON) ? new Literal(true) : expression();
        consume(Scanner.TokenType.SEMICOLON, "Expect ';' after loop condition.");
        Expr increment = check(Scanner.TokenType.RIGHT_PAREN) ? null : expression();
        consume(Scanner.TokenType.RIGHT_PAREN, "Expect ')' after for clauses.");
        Stmt body = statement();
        if (increment != null) body = new Block(java.util.Arrays.asList(body, new Expression(increment)));
        body = new While(condition, body);
        if (initializer != null) body = new Block(java.util.Arrays.asList(initializer, body));
        return body;
    }
    private Expr expression() { return assignment(); }
    private Expr assignment() {
        Expr expr = or();
        if (match(Scanner.TokenType.EQUAL)) {
            Scanner.Token equals = previous(); Expr value = assignment();
            if (expr instanceof Variable) return new Assign(((Variable) expr).name, value);
            throw error(equals, "Invalid assignment target.");
        }
        return expr;
    }
    private Expr or() {
        Expr expr = and();
        while (match(Scanner.TokenType.OR)) expr = new Logical(expr, previous(), and());
        return expr;
    }
    private Expr and() {
        Expr expr = equality();
        while (match(Scanner.TokenType.AND)) expr = new Logical(expr, previous(), equality());
        return expr;
    }
    private Expr equality() {
        Expr expr = comparison();
        while (match(Scanner.TokenType.BANG_EQUAL, Scanner.TokenType.EQUAL_EQUAL)) expr = new Binary(expr, previous(), comparison());
        return expr;
    }
    private Expr comparison() {
        Expr expr = term();
        while (match(Scanner.TokenType.GREATER, Scanner.TokenType.GREATER_EQUAL, Scanner.TokenType.LESS, Scanner.TokenType.LESS_EQUAL))
            expr = new Binary(expr, previous(), term());
        return expr;
    }
    private Expr term() {
        Expr expr = factor();
        while (match(Scanner.TokenType.MINUS, Scanner.TokenType.PLUS)) expr = new Binary(expr, previous(), factor());
        return expr;
    }
    private Expr factor() {
        Expr expr = unary();
        while (match(Scanner.TokenType.SLASH, Scanner.TokenType.STAR)) expr = new Binary(expr, previous(), unary());
        return expr;
    }
    private Expr unary() {
        if (match(Scanner.TokenType.BANG, Scanner.TokenType.MINUS)) return new Unary(previous(), unary());
        return call();
    }
    private Expr call() {
        Expr expr = primary();
        while (match(Scanner.TokenType.LEFT_PAREN)) {
            List<Expr> arguments = new ArrayList<>();
            if (!check(Scanner.TokenType.RIGHT_PAREN)) {
                do {
                    if (arguments.size() >= 255) throw error(peek(), "Can't have more than 255 arguments.");
                    arguments.add(expression());
                } while (match(Scanner.TokenType.COMMA));
            }
            expr = new Call(expr, consume(Scanner.TokenType.RIGHT_PAREN, "Expect ')' after arguments."), arguments);
        }
        return expr;
    }
    private Expr primary() {
        if (match(Scanner.TokenType.FALSE)) return new Literal(false);
        if (match(Scanner.TokenType.TRUE)) return new Literal(true);
        if (match(Scanner.TokenType.NIL)) return new Literal(null);
        if (match(Scanner.TokenType.NUMBER, Scanner.TokenType.STRING)) return new Literal(previous().literal);
        if (match(Scanner.TokenType.IDENTIFIER)) return new Variable(previous());
        if (match(Scanner.TokenType.FUN)) return function();
        if (match(Scanner.TokenType.LEFT_PAREN)) {
            Expr expr = expression(); consume(Scanner.TokenType.RIGHT_PAREN, "Expect ')' after expression."); return expr;
        }
        throw error(peek(), "Expect expression.");
    }
    private boolean match(Scanner.TokenType... types) {
        for (Scanner.TokenType type : types) if (check(type)) { advance(); return true; }
        return false;
    }
    private boolean check(Scanner.TokenType type) { return peek().type == type; }
    private boolean checkNext(Scanner.TokenType type) { return current + 1 < tokens.size() && tokens.get(current + 1).type == type; }
    private Scanner.Token advance() { if (!check(Scanner.TokenType.EOF)) current++; return previous(); }
    private Scanner.Token peek() { return tokens.get(current); }
    private Scanner.Token previous() { return tokens.get(current - 1); }
    private Scanner.Token consume(Scanner.TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }
    private RuntimeException error(Scanner.Token token, String message) {
        return new IllegalArgumentException("[line " + token.line + "] at '" + token.lexeme + "': " + message);
    }
}
