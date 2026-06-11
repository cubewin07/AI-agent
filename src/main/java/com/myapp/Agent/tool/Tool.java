package com.myapp.agent.tool;

public interface Tool {
    String getName();
    String getDescription();
    String getParameterSchema(); // Description of parameters (e.g., "JSON object with 'expression' field")
    String execute(String input);
}
