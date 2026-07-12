package dev.thebest.launcher;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CalculatorTest {
    private final Calculator calculator = new Calculator();

    @Test public void respectsMultiplicationPrecedence() {
        assertEquals("76", calculator.evaluate("=19*4"));
        assertEquals("7", calculator.evaluate("=1+2*3"));
    }

    @Test public void supportsParenthesesAndWhitespace() {
        assertEquals("10", calculator.evaluate("= (12 + 8) / 2"));
    }

    @Test public void supportsUnaryMinusAndDecimals() {
        assertEquals("-2.5", calculator.evaluate("=-5 / 2"));
    }

    @Test(expected = IllegalArgumentException.class) public void rejectsUnexpectedInput() {
        calculator.evaluate("=2+bad");
    }
}
