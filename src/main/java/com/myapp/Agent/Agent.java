package com.myapp.agent;

import com.myapp.agent.llm.ChatMessageDto;
import com.myapp.agent.llm.LlmClient;
import com.myapp.agent.llm.LlmClientFactory;
import com.myapp.agent.tool.Tool;
import com.myapp.agent.tool.ToolRegistry;
import com.myapp.model.ChatMessageEntity;
import com.myapp.model.ModelEntity;
import com.myapp.model.SessionEntity;
import com.myapp.model.enums.ChatRole;
import com.myapp.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class Agent {

    private final LlmClientFactory llmClientFactory;
    private final ToolRegistry toolRegistry;
    private final ChatMessageRepository chatMessageRepository;

    private static final int MAX_STEPS = 5;

    @Transactional
    public String processQuery(SessionEntity session, String query) {
        log.info("Processing query for session {}: {}", session.getId(), query);

        // 1. Save user query to database
        ChatMessageEntity userMessage = ChatMessageEntity.builder()
                .session(session)
                .role(ChatRole.USER)
                .content(query)
                .createdAt(Instant.now())
                .build();
        chatMessageRepository.save(userMessage);

        // 2. Load conversation history
        List<ChatMessageEntity> historyEntities = chatMessageRepository.findBySessionOrderByCreatedAtAsc(session);
        
        // Convert history to DTOs for the LLM client
        List<ChatMessageDto> history = historyEntities.stream()
                .map(msg -> ChatMessageDto.builder()
                        .role(msg.getRole().name())
                        .content(msg.getContent())
                        .build())
                .collect(Collectors.toList());

        // 3. Get model details
        ModelEntity model = session.getModel();
        LlmClient llmClient = llmClientFactory.getClient(model.getApiFormat());

        // 4. Get available tools for this session
        List<Tool> availableTools = getAvailableTools(session);
        String toolsDescription = formatToolsDescription(availableTools);

        // 5. Build ReAct system prompt
        String reactSystemPrompt = buildReActSystemPrompt(session.getSystemPrompt(), toolsDescription);

        // 6. ReAct Loop
        int step = 0;
        String currentScratchpad = "";
        String finalAnswer = null;

        while (step < MAX_STEPS) {
            step++;
            log.info("ReAct Loop Step {}/{}", step, MAX_STEPS);

            // Prepare the prompt for this step
            List<ChatMessageDto> currentHistory = new ArrayList<>(history);
            if (!currentScratchpad.isEmpty()) {
                currentHistory.add(ChatMessageDto.builder()
                        .role("assistant")
                        .content("Here is my scratchpad of thoughts and tool executions so far:\n" + currentScratchpad)
                        .build());
            }

            // Call LLM
            String llmResponse;
            try {
                llmResponse = llmClient.generate(
                        reactSystemPrompt,
                        currentHistory,
                        model.getApiKey(),
                        model.getUrl(),
                        model.getName()
                );
            } catch (Exception e) {
                log.error("Error calling LLM in ReAct loop: {}", e.getMessage(), e);
                finalAnswer = "Error calling LLM: " + e.getMessage();
                break;
            }

            log.debug("LLM Response: {}", llmResponse);

            // Parse Thought, Action, and Final Answer
            String thought = parseThought(llmResponse);
            String action = parseAction(llmResponse);
            String actionInput = parseActionInput(llmResponse);
            String parsedFinalAnswer = parseFinalAnswer(llmResponse);

            if (action != null) {
                // LLM wants to use a tool
                log.info("LLM is using tool: {} with input: {}", action, actionInput);
                
                // Find the tool
                Optional<Tool> toolOpt = availableTools.stream()
                        .filter(t -> t.getName().equalsIgnoreCase(action))
                        .findFirst();

                String toolResult;
                if (toolOpt.isPresent()) {
                    Tool tool = toolOpt.get();
                    try {
                        toolResult = tool.execute(actionInput);
                    } catch (Exception e) {
                        toolResult = "Error executing tool: " + e.getMessage();
                    }
                } else {
                    toolResult = "Error: Tool '" + action + "' is not available in this session.";
                }

                log.info("Tool {} returned: {}", action, toolResult);

                // Save tool call and response to database as part of the step
                ChatMessageEntity stepMessage = ChatMessageEntity.builder()
                        .session(session)
                        .role(ChatRole.ASSISTANT)
                        .content("Thought: " + thought + "\nAction: " + action + "[" + actionInput + "]")
                        .toolName(action)
                        .stepNumber(step)
                        .createdAt(Instant.now())
                        .build();
                chatMessageRepository.save(stepMessage);

                ChatMessageEntity toolMessage = ChatMessageEntity.builder()
                        .session(session)
                        .role(ChatRole.TOOL)
                        .content("Observation: " + toolResult)
                        .toolName(action)
                        .stepNumber(step)
                        .createdAt(Instant.now())
                        .build();
                chatMessageRepository.save(toolMessage);

                // Update scratchpad for the next iteration
                currentScratchpad += "\nThought: " + thought + "\nAction: " + action + "[" + actionInput + "]\nObservation: " + toolResult;

            } else if (parsedFinalAnswer != null) {
                // LLM returned final answer
                finalAnswer = parsedFinalAnswer;
                break;
            } else {
                // Fallback if LLM didn't follow format but returned text
                if (llmResponse != null && !llmResponse.isBlank()) {
                    finalAnswer = llmResponse;
                } else {
                    finalAnswer = "No response generated by the model.";
                }
                break;
            }
        }

        if (finalAnswer == null) {
            finalAnswer = "ReAct loop exceeded maximum steps without finding a final answer.";
        }

        // Save final response to database
        ChatMessageEntity assistantMessage = ChatMessageEntity.builder()
                .session(session)
                .role(ChatRole.ASSISTANT)
                .content(finalAnswer)
                .createdAt(Instant.now())
                .build();
        chatMessageRepository.save(assistantMessage);

        return finalAnswer;
    }

    private List<Tool> getAvailableTools(SessionEntity session) {
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

    private String formatToolsDescription(List<Tool> tools) {
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

    private String buildReActSystemPrompt(String customSystemPrompt, String toolsDescription) {
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

    private String parseThought(String text) {
        Pattern pattern = Pattern.compile("Thought:\\s*(.*?)(?=\\nAction:|\\nFinal Answer:|$)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    private String parseAction(String text) {
        Pattern pattern = Pattern.compile("Action:\\s*(\\w+)\\[", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private String parseActionInput(String text) {
        Pattern pattern = Pattern.compile("Action:\\s*\\w+\\[(.*?)\\]", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    private String parseFinalAnswer(String text) {
        Pattern pattern = Pattern.compile("Final Answer:\\s*(.*)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }
}
