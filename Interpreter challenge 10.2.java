import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


public class Interpreter {
    static class Environment {
        final Environment enclosing;
        private final Map<String, Object> values = new HashMap<>();
        Environment(Environment enclosing) { this.enclosing = enclosing; }
        void define(String name, Object value) { values.put(name, value); }
        Object get(Scanner.Token name) {
            
            if (values.containsKey(name.lexeme)) return values.get(name.lexeme);
            if (enclosing != null) return enclosing.get(name);
            throw error(name, "Undefined variable '" + name.lexeme + "'.");
        }
        void assign(Scanner.Token name, Object value) {
            if (values.containsKey(name.lexeme)) { values.put(name.lexeme, value); return; }
            if (enclosing != null) { enclosing.assign(name, value); return; }
            throw error(name, "Undefined variable '" + name.lexeme + "'.");
        }
    }
    interface LoxCallable {
        int arity();
        Object call(Interpreter interpreter, List<Object> arguments);
    }
    static class Return extends RuntimeException {
        final Object value;
        Return(Object value) { super(null, null, false, false); this.value = value; }
    }
    static class LoxFunction implements LoxCallable {

        final String name;
        final Parser.Function declaration;
        final Environment closure;
        LoxFunction(String name, Parser.Function declaration, Environment closure) {
            this.name = name; this.declaration = declaration; this.closure = closure;
        }
        public int arity() { return declaration.parameters.size(); }
        public Object call(Interpreter interpreter, List<Object> arguments) {

            Environment local = new Environment(closure);
            for (int index = 0; index < arguments.size(); index++)

                local.define(declaration.parameters.get(index).lexeme, arguments.get(index));
            try { interpreter.executeBlock(declaration.body, local); }
            catch (Return result) { return result.value; }
            return null;
        }
        public String toString() { return name == null ? "<fn anonymous>" : "<fn " + name + ">"; }
    }
    final Environment globals = new Environment(null);
    Environment environment = globals;
    public Interpreter() {
        globals.define("clock", new LoxCallable() {
            public int arity() { return 0; }

            public Object call(Interpreter i, List<Object> arguments) { return System.currentTimeMillis() / 1000.0; }
            public String toString() { return "<native fn>"; }
        });
    }
    public void interpret(List<Parser.Stmt> statements) {
        for (Parser.Stmt statement : statements) statement.execute(this);
    }
    void executeBlock(List<Parser.Stmt> statements, Environment local) {
        Environment previous = environment;
        try { environment = local; interpret(statements); }
        finally { environment = previous; }
    }
    Object call(Object target, Scanner.Token paren, List<Object> arguments) {

        if (!(target instanceof LoxCallable)) throw error(paren, "Can only call functions.");

        LoxCallable function = (LoxCallable) target;
        if (arguments.size() != function.arity())

            throw error(paren, "Expected " + function.arity() + " arguments but got " + arguments.size() + ".");
        return function.call(this, arguments);
    }
    Object unary(Scanner.Token operator, Object right) {
        if (operator.type == Scanner.TokenType.BANG) return !truthy(right);

        number(operator, right); return -(Double) right;
    }
    Object binary(Scanner.Token operator, Object left, Object right) {
        switch (operator.type) {
            case EQUAL_EQUAL: return Objects.equals(left, right);
            case BANG_EQUAL: return !Objects.equals(left, right);
            case PLUS:
                if (left instanceof Double && right instanceof Double) return (Double) left + (Double) right;
                if (left instanceof String && right instanceof String) return (String) left + (String) right;
                throw error(operator, "Operands must be two numbers or two strings.");

            default: break;
        }
        number(operator, left); number(operator, right);
        double a = (Double) left, b = (Double) right;
        switch (operator.type) {
            case MINUS: return a - b;
            case STAR: return a * b;
            case SLASH: return a / b;
            case GREATER: return a > b;
            case GREATER_EQUAL: return a >= b;
            case LESS: return a < b;
            case LESS_EQUAL: return a <= b;
            default: throw error(operator, "Unknown operator.");
        }
    }
    private static void number(Scanner.Token token, Object value) {

        if (!(value instanceof Double)) throw error(token, "Operand must be a number.");

    }
    static boolean truthy(Object value) { return value != null && (!(value instanceof Boolean) || (Boolean) value); }
    static String stringify(Object value) {
        if (value == null) return "nil";
        String text = value.toString();
        if (value instanceof Double && text.endsWith(".0")) return text.substring(0, text.length() - 2);
        return text;
    }
    static RuntimeException error(Scanner.Token token, String message) {
        return new IllegalArgumentException("[line " + token.line + "] " + message);
    }
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: java Interpreter <script.lox>"); System.exit(64);

        }
        String source = new String(Files.readAllBytes(Paths.get(args[0])), StandardCharsets.UTF_8);
        try {
            List<Scanner.Token> tokens = new Scanner(source).scanTokens();
            new Interpreter().interpret(new Parser(tokens).parse());
        } catch (IllegalArgumentException error) {
            System.err.println(error.getMessage()); System.exit(1);
        }
    }
}
