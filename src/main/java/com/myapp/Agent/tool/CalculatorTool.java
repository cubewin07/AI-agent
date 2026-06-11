package com.myapp.agent.tool;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CalculatorTool implements Tool {

    @Override
    public String getName() {
        return "calculator";
    }

    @Override
    public String getDescription() {
        return "Evaluates simple mathematical expressions. Input should be a mathematical expression like '2 + 2' or '15 * 3'.";
    }

    @Override
    public String getParameterSchema() {
        return "A mathematical expression string containing numbers and operators (+, -, *, /).";
    }

    @Override
    public String execute(String input) {
        if (input == null || input.isBlank()) {
            return "Error: Input is empty";
        }
        // Clean input
        String expr = input.replaceAll("\\s+", "");
        Pattern pattern = Pattern.compile("^(-?\\d+(?:\\.\\d+)?)([+\\-*/])(-?\\d+(?:\\.\\d+)?)$");
        Matcher matcher = pattern.matcher(expr);
        if (!matcher.matches()) {
            return "Error: Invalid expression format. Only simple binary operations are supported (e.g., '2 + 2').";
        }

        try {
            double num1 = Double.parseDouble(matcher.group(1));
            String op = matcher.group(2);
            double num2 = Double.parseDouble(matcher.group(3));

            double result = switch (op) {
                case "+" -> num1 + num2;
                case "-" -> num1 - num2;
                case "*" -> num1 * num2;
                case "/" -> {
                    if (num2 == 0) {
                        throw new ArithmeticException("Division by zero");
                    }
                    yield num1 / num2;
                }
                default -> throw new IllegalArgumentException("Unknown operator: " + op);
            };

            return String.valueOf(result);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
