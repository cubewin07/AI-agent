package com.myapp.agent.llm;

public record AgentDecision(
    String thought,
    ToolCall toolCall,
    String finalAnswer
) {}
