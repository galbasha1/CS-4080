import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Chapter8Challenge1 {

    static Map<String, Object> variables = new HashMap<>();

    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);

        System.out.println("Lox REPL - type exit to quit.");

        while (true) {
            System.out.print("> ");

            if (!input.hasNextLine()) {
                break;
            }

            String line = input.nextLine().trim();

            if (line.equals("exit")) {
                break;
            }

            if (line.isEmpty()) {
                continue;
            }

            try {
                runLine(line);
            } catch (RuntimeException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }

        input.close();
    }

    static void runLine(String line) {

        if (line.startsWith("var ")) {
            variableDeclaration(line);
            return;
        }

        if (line.startsWith("print ")) {
            printStatement(line);
            return;
        }

        if (line.endsWith(";")) {
            String expression =
                    line.substring(0, line.length() - 1);

            ExpressionParser parser =
                    new ExpressionParser(expression);

            parser.parse();
            return;
        }

        ExpressionParser parser =
                new ExpressionParser(line);

        Object result = parser.parse();

        System.out.println(stringify(result));
    }

    static void variableDeclaration(String line) {

        if (!line.endsWith(";")) {
            throw new RuntimeException(
                    "Expected ';' after variable declaration.");
        }

        line = line.substring(4, line.length() - 1).trim();

        int equal = line.indexOf('=');

        if (equal == -1) {
            variables.put(line, null);
            return;
        }

        String name = line.substring(0, equal).trim();
        String expression = line.substring(equal + 1).trim();

        ExpressionParser parser =
                new ExpressionParser(expression);

        Object value = parser.parse();

        variables.put(name, value);
    }

    static void printStatement(String line) {

        if (!line.endsWith(";")) {
            throw new RuntimeException(
                    "Expected ';' after print statement.");
        }

        String expression =
                line.substring(6, line.length() - 1).trim();

        ExpressionParser parser =
                new ExpressionParser(expression);

        Object result = parser.parse();

        System.out.println(stringify(result));
    }

    static String stringify(Object value) {

        if (value == null) {
            return "nil";
        }

        if (value instanceof Double) {
            String text = value.toString();

            if (text.endsWith(".0")) {
                text = text.substring(
                        0, text.length() - 2);
            }

            return text;
        }

        return value.toString();
    }


    static class ExpressionParser {

        private final String source;
        private int current = 0;

        ExpressionParser(String source) {
            this.source = source;
        }

        Object parse() {
            Object value = assignment();

            skipSpaces();

            if (!isAtEnd()) {
                throw new RuntimeException(
                        "Unexpected character.");
            }

            return value;
        }

        private Object assignment() {

            int savedPosition = current;

            skipSpaces();

            String name = readIdentifier();

            if (name != null) {
                skipSpaces();

                if (match('=')) {

                    if (!variables.containsKey(name)) {
                        throw new RuntimeException(
                                "Undefined variable '" +
                                name + "'.");
                    }

                    Object value = assignment();
                    variables.put(name, value);

                    return value;
                }
            }

            current = savedPosition;

            return addition();
        }

        private Object addition() {

            Object value = multiplication();

            while (true) {

                skipSpaces();

                if (match('+')) {

                    Object right = multiplication();

                    if (value instanceof Double &&
                            right instanceof Double) {

                        value = (double) value +
                                (double) right;

                    } else if (value instanceof String &&
                            right instanceof String) {

                        value = (String) value +
                                (String) right;

                    } else {
                        throw new RuntimeException(
                                "Operands must be two numbers " +
                                "or two strings.");
                    }

                } else if (match('-')) {

                    Object right = multiplication();

                    checkNumbers(value, right);

                    value = (double) value -
                            (double) right;

                } else {
                    break;
                }
            }

            return value;
        }

        private Object multiplication() {

            Object value = unary();

            while (true) {

                skipSpaces();

                if (match('*')) {

                    Object right = unary();

                    checkNumbers(value, right);

                    value = (double) value *
                            (double) right;

                } else if (match('/')) {

                    Object right = unary();

                    checkNumbers(value, right);

                    value = (double) value /
                            (double) right;

                } else {
                    break;
                }
            }

            return value;
        }

        private Object unary() {

            skipSpaces();

            if (match('-')) {

                Object right = unary();

                if (!(right instanceof Double)) {
                    throw new RuntimeException(
                            "Operand must be a number.");
                }

                return -(double) right;
            }

            return primary();
        }

        private Object primary() {

            skipSpaces();

            if (match('(')) {

                Object value = assignment();

                skipSpaces();

                if (!match(')')) {
                    throw new RuntimeException(
                            "Expected ')'.");
                }

                return value;
            }

            if (peek() == '"') {
                return string();
            }

            if (Character.isDigit(peek())) {
                return number();
            }

            String name = readIdentifier();

            if (name != null) {

                if (name.equals("true")) {
                    return true;
                }

                if (name.equals("false")) {
                    return false;
                }

                if (name.equals("nil")) {
                    return null;
                }

                if (!variables.containsKey(name)) {
                    throw new RuntimeException(
                            "Undefined variable '" +
                            name + "'.");
                }

                return variables.get(name);
            }

            throw new RuntimeException(
                    "Expected expression.");
        }

        private Object number() {

            int start = current;

            while (Character.isDigit(peek())) {
                current++;
            }

            if (peek() == '.' &&
                    Character.isDigit(peekNext())) {

                current++;

                while (Character.isDigit(peek())) {
                    current++;
                }
            }

            return Double.parseDouble(
                    source.substring(start, current));
        }

        private String string() {

            current++;

            int start = current;

            while (!isAtEnd() && peek() != '"') {
                current++;
            }

            if (isAtEnd()) {
                throw new RuntimeException(
                        "Unterminated string.");
            }

            String value =
                    source.substring(start, current);

            current++;

            return value;
        }

        private String readIdentifier() {

            skipSpaces();

            if (!Character.isLetter(peek()) &&
                    peek() != '_') {
                return null;
            }

            int start = current;
            current++;

            while (Character.isLetterOrDigit(peek()) ||
                    peek() == '_') {
                current++;
            }

            return source.substring(start, current);
        }

        private void checkNumbers(
                Object left, Object right) {

            if (!(left instanceof Double) ||
                    !(right instanceof Double)) {

                throw new RuntimeException(
                        "Operands must be numbers.");
            }
        }

        private boolean match(char expected) {

            if (peek() != expected) {
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

        private void skipSpaces() {

            while (!isAtEnd() &&
                    Character.isWhitespace(peek())) {
                current++;
            }
        }

        private boolean isAtEnd() {
            return current >= source.length();
        }
    }
}