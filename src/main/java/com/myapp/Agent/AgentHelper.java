package com.myapp.agent;

import com.myapp.agent.tool.Tool;
import com.myapp.agent.tool.ToolRegistry;
import com.myapp.model.SessionEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class AgentHelper {

    private final ToolRegistry toolRegistry;

    public List<Tool> getAvailableTools(SessionEntity session) {
        if (session.getEnabledTools() == null || session.getEnabledTools().isBlank()) {
            return List.of();
        }
        List<String> enabledNames = Arrays.stream(session.getEnabledTools().split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .toList();

        return toolRegistry.getAllTools().stream()
                .filter(tool -> enabledNames.contains(tool.getName().toLowerCase()))
                .toList();
    }

    public String formatToolsDescription(List<Tool> tools) {
        if (tools.isEmpty()) {
            return "No tools available.";
        }
        StringBuilder sb = new StringBuilder();
        for (Tool tool : tools) {
            sb.append("- ").append(tool.getName()).append(": ").append(tool.getDescription()).append("\n");
            sb.append("  Parameter schema: ").append(tool.getParameterSchema()).append("\n");
        }
        return sb.toString();
    }

    public String buildReActSystemPrompt(String customSystemPrompt, String toolsDescription) {
        String basePrompt = """
                You are a ReAct AI chat agent. You solve problems by thinking step-by-step and using tools.
                
                You have access to the following tools:
                %s
                
                To use a tool, you MUST use the following format:
                Thought: Do I need to use a tool? Yes.
                Action: tool_name[tool_input]
                
                After the tool is executed, you will receive an Observation.
                
                When you have the final answer, or if you do not need to use a tool, you MUST use the following format:
                Thought: Do I need to use a tool? No.
                Final Answer: the final answer to the user's query
                
                You must output exactly one Thought and one Action, OR one Thought and one Final Answer in each turn.
                Do not output anything else.
                """;

        String formattedBase = String.format(basePrompt, toolsDescription);
        if (customSystemPrompt != null && !customSystemPrompt.isBlank()) {
            return customSystemPrompt + "\n\n" + formattedBase;
        }
        return formattedBase;
    }

    public String parseThought(String text) {
        Pattern pattern = Pattern.compile("Thought:\\s*(.*?)(?=\\nAction:|\\nFinal Answer:|$)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    public String parseAction(String text) {
        Pattern pattern = Pattern.compile("Action:\\s*(\\w+)\\[", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    public String parseActionInput(String text) {
        Pattern pattern = Pattern.compile("Action:\\s*\\w+\\[(.*?)\\]", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    public String parseFinalAnswer(String text) {
        Pattern pattern = Pattern.compile("Final Answer:\\s*(.*)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }
}
