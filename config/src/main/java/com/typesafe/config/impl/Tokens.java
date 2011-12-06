/**
 *   Copyright (C) 2011 Typesafe Inc. <http://typesafe.com>
 */
package com.typesafe.config.impl;

import java.util.List;

import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigOrigin;
import com.typesafe.config.ConfigValueType;

/* FIXME the way the subclasses of Token are private with static isFoo and accessors is kind of ridiculous. */
final class Tokens {
    static private class Value extends Token {

        final private AbstractConfigValue value;

        Value(AbstractConfigValue value) {
            super(TokenType.VALUE);
            this.value = value;
        }

        AbstractConfigValue value() {
            return value;
        }

        @Override
        public String toString() {
            return "'" + value().unwrapped() + "' (" + value.valueType().name() + ")";
        }

        @Override
        protected boolean canEqual(Object other) {
            return other instanceof Value;
        }

        @Override
        public boolean equals(Object other) {
            return super.equals(other) && ((Value) other).value.equals(value);
        }

        @Override
        public int hashCode() {
            return 41 * (41 + super.hashCode()) + value.hashCode();
        }
    }

    static private class Line extends Token {
        final private int lineNumber;

        Line(int lineNumber) {
            super(TokenType.NEWLINE);
            this.lineNumber = lineNumber;
        }

        int lineNumber() {
            return lineNumber;
        }

        @Override
        public String toString() {
            return "'\n'@" + lineNumber;
        }

        @Override
        protected boolean canEqual(Object other) {
            return other instanceof Line;
        }

        @Override
        public boolean equals(Object other) {
            return super.equals(other)
                    && ((Line) other).lineNumber == lineNumber;
        }

        @Override
        public int hashCode() {
            return 41 * (41 + super.hashCode()) + lineNumber;
        }
    }

    // This is not a Value, because it requires special processing
    static private class UnquotedText extends Token {
        final private ConfigOrigin origin;
        final private String value;

        UnquotedText(ConfigOrigin origin, String s) {
            super(TokenType.UNQUOTED_TEXT);
            this.origin = origin;
            this.value = s;
        }

        ConfigOrigin origin() {
            return origin;
        }

        String value() {
            return value;
        }

        @Override
        public String toString() {
            return "'" + value + "'";
        }

        @Override
        protected boolean canEqual(Object other) {
            return other instanceof UnquotedText;
        }

        @Override
        public boolean equals(Object other) {
            return super.equals(other)
                    && ((UnquotedText) other).value.equals(value);
        }

        @Override
        public int hashCode() {
            return 41 * (41 + super.hashCode()) + value.hashCode();
        }
    }

    static private class Problem extends Token {
        final private ConfigOrigin origin;
        final private String what;
        final private String message;
        final private boolean suggestQuotes;
        final private Throwable cause;

        Problem(ConfigOrigin origin, String what, String message, boolean suggestQuotes,
                Throwable cause) {
            super(TokenType.PROBLEM);
            this.origin = origin;
            this.what = what;
            this.message = message;
            this.suggestQuotes = suggestQuotes;
            this.cause = cause;
        }

        ConfigOrigin origin() {
            return origin;
        }

        String message() {
            return message;
        }

        boolean suggestQuotes() {
            return suggestQuotes;
        }

        Throwable cause() {
            return cause;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append('\'');
            sb.append(what);
            sb.append('\'');
            return sb.toString();
        }

        @Override
        protected boolean canEqual(Object other) {
            return other instanceof Problem;
        }

        @Override
        public boolean equals(Object other) {
            return super.equals(other) && ((Problem) other).what.equals(what)
                    && ((Problem) other).message.equals(message)
                    && ((Problem) other).suggestQuotes == suggestQuotes
                    && ConfigUtil.equalsHandlingNull(((Problem) other).cause, cause);
        }

        @Override
        public int hashCode() {
            int h = 41 * (41 + super.hashCode());
            h = 41 * (h + what.hashCode());
            h = 41 * (h + message.hashCode());
            h = 41 * (h + Boolean.valueOf(suggestQuotes).hashCode());
            if (cause != null)
                h = 41 * (h + cause.hashCode());
            return h;
        }
    }

    // This is not a Value, because it requires special processing
    static private class Substitution extends Token {
        final private ConfigOrigin origin;
        final private boolean optional;
        final private List<Token> value;

        Substitution(ConfigOrigin origin, boolean optional, List<Token> expression) {
            super(TokenType.SUBSTITUTION);
            this.origin = origin;
            this.optional = optional;
            this.value = expression;
        }

        ConfigOrigin origin() {
            return origin;
        }

        boolean optional() {
            return optional;
        }

        List<Token> value() {
            return value;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (Token t : value) {
                sb.append(t.toString());
            }
            return "'${" + sb.toString() + "}'";
        }

        @Override
        protected boolean canEqual(Object other) {
            return other instanceof Substitution;
        }

        @Override
        public boolean equals(Object other) {
            return super.equals(other)
                    && ((Substitution) other).value.equals(value);
        }

        @Override
        public int hashCode() {
            return 41 * (41 + super.hashCode()) + value.hashCode();
        }
    }

    static boolean isValue(Token token) {
        return token instanceof Value;
    }

    static AbstractConfigValue getValue(Token token) {
        if (token instanceof Value) {
            return ((Value) token).value();
        } else {
            throw new ConfigException.BugOrBroken(
                    "tried to get value of non-value token " + token);
        }
    }

    static boolean isValueWithType(Token t, ConfigValueType valueType) {
        return isValue(t) && getValue(t).valueType() == valueType;
    }

    static boolean isNewline(Token token) {
        return token instanceof Line;
    }

    static int getLineNumber(Token token) {
        if (token instanceof Line) {
            return ((Line) token).lineNumber();
        } else {
            throw new ConfigException.BugOrBroken(
                    "tried to get line number from non-newline " + token);
        }
    }

    static boolean isProblem(Token token) {
        return token instanceof Problem;
    }

    static ConfigOrigin getProblemOrigin(Token token) {
        if (token instanceof Problem) {
            return ((Problem) token).origin();
        } else {
            throw new ConfigException.BugOrBroken("tried to get problem origin from " + token);
        }
    }

    static String getProblemMessage(Token token) {
        if (token instanceof Problem) {
            return ((Problem) token).message();
        } else {
            throw new ConfigException.BugOrBroken("tried to get problem message from " + token);
        }
    }

    static boolean getProblemSuggestQuotes(Token token) {
        if (token instanceof Problem) {
            return ((Problem) token).suggestQuotes();
        } else {
            throw new ConfigException.BugOrBroken("tried to get problem suggestQuotes from "
                    + token);
        }
    }

    static Throwable getProblemCause(Token token) {
        if (token instanceof Problem) {
            return ((Problem) token).cause();
        } else {
            throw new ConfigException.BugOrBroken("tried to get problem cause from " + token);
        }
    }

    static boolean isUnquotedText(Token token) {
        return token instanceof UnquotedText;
    }

    static String getUnquotedText(Token token) {
        if (token instanceof UnquotedText) {
            return ((UnquotedText) token).value();
        } else {
            throw new ConfigException.BugOrBroken(
                    "tried to get unquoted text from " + token);
        }
    }

    static ConfigOrigin getUnquotedTextOrigin(Token token) {
        if (token instanceof UnquotedText) {
            return ((UnquotedText) token).origin();
        } else {
            throw new ConfigException.BugOrBroken(
                    "tried to get unquoted text from " + token);
        }
    }

    static boolean isSubstitution(Token token) {
        return token instanceof Substitution;
    }

    static List<Token> getSubstitutionPathExpression(Token token) {
        if (token instanceof Substitution) {
            return ((Substitution) token).value();
        } else {
            throw new ConfigException.BugOrBroken(
                    "tried to get substitution from " + token);
        }
    }

    static ConfigOrigin getSubstitutionOrigin(Token token) {
        if (token instanceof Substitution) {
            return ((Substitution) token).origin();
        } else {
            throw new ConfigException.BugOrBroken(
                    "tried to get substitution origin from " + token);
        }
    }

    static boolean getSubstitutionOptional(Token token) {
        if (token instanceof Substitution) {
            return ((Substitution) token).optional();
        } else {
            throw new ConfigException.BugOrBroken("tried to get substitution optionality from "
                    + token);
        }
    }

    final static Token START = new Token(TokenType.START, "start of file");
    final static Token END = new Token(TokenType.END, "end of file");
    final static Token COMMA = new Token(TokenType.COMMA, "','");
    final static Token EQUALS = new Token(TokenType.EQUALS, "'='");
    final static Token COLON = new Token(TokenType.COLON, "':'");
    final static Token OPEN_CURLY = new Token(TokenType.OPEN_CURLY, "'{'");
    final static Token CLOSE_CURLY = new Token(TokenType.CLOSE_CURLY, "'}'");
    final static Token OPEN_SQUARE = new Token(TokenType.OPEN_SQUARE, "'['");
    final static Token CLOSE_SQUARE = new Token(TokenType.CLOSE_SQUARE, "']'");

    static Token newLine(int lineNumberJustEnded) {
        return new Line(lineNumberJustEnded);
    }

    static Token newProblem(ConfigOrigin origin, String what, String message,
            boolean suggestQuotes, Throwable cause) {
        return new Problem(origin, what, message, suggestQuotes, cause);
    }

    static Token newUnquotedText(ConfigOrigin origin, String s) {
        return new UnquotedText(origin, s);
    }

    static Token newSubstitution(ConfigOrigin origin, boolean optional, List<Token> expression) {
        return new Substitution(origin, optional, expression);
    }

    static Token newValue(AbstractConfigValue value) {
        return new Value(value);
    }

    static Token newString(ConfigOrigin origin, String value) {
        return newValue(new ConfigString(origin, value));
    }

    static Token newInt(ConfigOrigin origin, int value, String originalText) {
        return newValue(ConfigNumber.newNumber(origin, value,
                originalText));
    }

    static Token newDouble(ConfigOrigin origin, double value,
            String originalText) {
        return newValue(ConfigNumber.newNumber(origin, value,
                originalText));
    }

    static Token newLong(ConfigOrigin origin, long value, String originalText) {
        return newValue(ConfigNumber.newNumber(origin, value,
                originalText));
    }

    static Token newNull(ConfigOrigin origin) {
        return newValue(new ConfigNull(origin));
    }

    static Token newBoolean(ConfigOrigin origin, boolean value) {
        return newValue(new ConfigBoolean(origin, value));
    }
}
