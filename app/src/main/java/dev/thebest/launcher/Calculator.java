package dev.thebest.launcher;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

final class Calculator {
    private static final DecimalFormat FORMAT = new DecimalFormat("0.########", DecimalFormatSymbols.getInstance(Locale.US));

    String evaluate(String rawExpression) {
        String expression = rawExpression.trim();
        if (expression.startsWith("=")) expression = expression.substring(1).trim();
        if (expression.isEmpty()) throw new IllegalArgumentException("Empty expression");
        Parser parser = new Parser(expression);
        double value = parser.parseExpression();
        parser.requireEnd();
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Invalid result");
        return FORMAT.format(value);
    }

    private static final class Parser {
        private final String input;
        private int position;

        Parser(String input) {
            this.input = input;
        }

        double parseExpression() {
            double value = parseTerm();
            while (true) {
                skipSpaces();
                if (match('+')) value += parseTerm();
                else if (match('-')) value -= parseTerm();
                else return value;
            }
        }

        private double parseTerm() {
            double value = parseFactor();
            while (true) {
                skipSpaces();
                if (match('*')) value *= parseFactor();
                else if (match('/')) value /= parseFactor();
                else return value;
            }
        }

        private double parseFactor() {
            skipSpaces();
            if (match('+')) return parseFactor();
            if (match('-')) return -parseFactor();
            if (match('(')) {
                double value = parseExpression();
                if (!match(')')) throw new IllegalArgumentException("Missing closing parenthesis");
                return value;
            }
            return parseNumber();
        }

        private double parseNumber() {
            skipSpaces();
            int start = position;
            boolean dotSeen = false;
            while (position < input.length()) {
                char current = input.charAt(position);
                if (Character.isDigit(current)) {
                    position++;
                } else if (current == '.' && !dotSeen) {
                    dotSeen = true;
                    position++;
                } else {
                    break;
                }
            }
            if (start == position) throw new IllegalArgumentException("Expected number");
            return Double.parseDouble(input.substring(start, position));
        }

        void requireEnd() {
            skipSpaces();
            if (position != input.length()) throw new IllegalArgumentException("Unexpected input");
        }

        private boolean match(char expected) {
            skipSpaces();
            if (position < input.length() && input.charAt(position) == expected) {
                position++;
                return true;
            }
            return false;
        }

        private void skipSpaces() {
            while (position < input.length() && Character.isWhitespace(input.charAt(position))) position++;
        }
    }
}
