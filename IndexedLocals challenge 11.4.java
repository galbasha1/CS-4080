import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;



public class Challenge4IndexedLocals {


    static class Token {
        final String lexeme;

        Token(String lexeme) {
            this.lexeme = lexeme;
        }
    }

    abstract static class Expr {
        interface Visitor<R> {
            R visitVariableExpr(Variable expr);
            R visitAssignExpr(Assign expr);
            R visitLiteralExpr(Literal expr);
        }

        abstract <R> R accept(Visitor<R> visitor);

        static class Variable extends Expr {
            final Token name;

            Variable(Token name) {
                this.name = name;
            }

            @Override
            <R> R accept(Visitor<R> visitor) {
                return visitor.visitVariableExpr(this);
            }
        }

        static class Assign extends Expr {
            final Token name;
            final Expr value;

            Assign(Token name, Expr value) {
                this.name = name;
                this.value = value;
            }

            @Override
            <R> R accept(Visitor<R> visitor) {
                return visitor.visitAssignExpr(this);
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
    }


    static class LocalInfo {
        final int distance;
        final int index;

        LocalInfo(int distance, int index) {
            this.distance = distance;
            this.index = index;
        }

        @Override
        public String toString() {
            return "(distance=" + distance + ", index=" + index + ")";
        }
    }

    static class Resolver {

        private static class Variable {
            final Token name;
            final int index;

            Variable(Token name, int index) {
                this.name = name;
                this.index = index;
            }
        }

        private final Stack<Map<String, Variable>> scopes = new Stack<>();
        private final Interpreter interpreter;

        Resolver(Interpreter interpreter) {
            this.interpreter = interpreter;
        }

        void beginScope() {
            scopes.push(new HashMap<>());
        }

        void endScope() {
            scopes.pop();
        }

        
        void declare(Token name) {
            if (scopes.isEmpty()) {
                return;
            }

            Map<String, Variable> scope = scopes.peek();

            if (scope.containsKey(name.lexeme)) {
                throw new RuntimeException(
                    "Already a variable with this name in this scope: " +
                    name.lexeme
                );
            }

            int index = scope.size();
            scope.put(name.lexeme, new Variable(name, index));
        }

        

        
        void resolveLocal(Expr expr, Token name) {
            for (int i = scopes.size() - 1; i >= 0; i--) {
                Map<String, Variable> scope = scopes.get(i);

                if (scope.containsKey(name.lexeme)) {
                    int distance = scopes.size() - 1 - i;
                    int index = scope.get(name.lexeme).index;

                    interpreter.resolve(expr, distance, index);
                    return;
                }
            }


        }
    }


    
    static class Environment {
        final Environment enclosing;

        
        private final List<Object> values = new ArrayList<>();

        Environment() {
            this(null);
        }

        Environment(Environment enclosing) {
            this.enclosing = enclosing;
        }

        
        int define(Object value) {
            values.add(value);
            return values.size() - 1;
        }

        Object getAt(int distance, int index) {
            return ancestor(distance).values.get(index);
        }

        void assignAt(int distance, int index, Object value) {
            ancestor(distance).values.set(index, value);
        }

        private Environment ancestor(int distance) {
            Environment environment = this;

            for (int i = 0; i < distance; i++) {
                environment = environment.enclosing;
            }

            return environment;
        }
    }

    
    static class Interpreter implements Expr.Visitor<Object> {


        
        private final Map<Expr, LocalInfo> locals = new HashMap<>();

        Environment globals = new Environment();
        Environment environment = globals;

        void resolve(Expr expr, int distance, int index) {
            locals.put(expr, new LocalInfo(distance, index));
        }

        @Override
        public Object visitVariableExpr(Expr.Variable expr) {
            LocalInfo local = locals.get(expr);

            if (local != null) {
                return environment.getAt(local.distance, local.index);
            }

            throw new RuntimeException(
                "Global lookup would be handled separately for: " +
                expr.name.lexeme
            );
        }

        @Override
        public Object visitAssignExpr(Expr.Assign expr) {
            Object value = expr.value.accept(this);
            LocalInfo local = locals.get(expr);

            if (local != null) {
                environment.assignAt(
                    local.distance,
                    local.index,
                    value
                );
                return value;
            }

            throw new RuntimeException(
                "Global assignment would be handled separately for: " +
                expr.name.lexeme
            );
        }

        @Override
        public Object visitLiteralExpr(Expr.Literal expr) {
            return expr.value;
        }
    }


    
    public static void main(String[] args) {

        Interpreter interpreter = new Interpreter();
        Resolver resolver = new Resolver(interpreter);


        

        resolver.beginScope();

        Token a = new Token("a");
        Token b = new Token("b");

        resolver.declare(a);
        resolver.declare(b);

        
        interpreter.environment = new Environment(interpreter.globals);
        interpreter.environment.define("first");
        interpreter.environment.define("second");

        Expr.Variable readB = new Expr.Variable(b);

        resolver.resolveLocal(readB, b);

        Object result = readB.accept(interpreter);

        System.out.println("Value of b: " + result);

        resolver.endScope();
    }
}
