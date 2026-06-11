package com.myapp.agent.llm;

public record ToolCall(
    String name,
    String arguments
) {}
