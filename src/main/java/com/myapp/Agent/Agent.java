package com.myapp.agent;

import com.myapp.agent.llm.AgentDecision;
import com.myapp.agent.llm.ChatMessageDto;
import com.myapp.agent.llm.LlmClient;
import com.myapp.agent.llm.LlmClientFactory;
import com.myapp.agent.tool.Tool;
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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class Agent {

    private final LlmClientFactory llmClientFactory;
    private final ChatMessageRepository chatMessageRepository;
    private final AgentHelper agentHelper;

    private static final int MAX_STEPS = 5;

    @Transactional
    public String processQuery(SessionEntity session, String query, List<ChatMessageDto> clientHistory) {
        log.info("Processing query for session {}: {}", session.getId(), query);

        // 1. Save user query to database
        ChatMessageEntity userMessage = ChatMessageEntity.builder()
                .session(session)
                .role(ChatRole.USER)
                .content(query)
                .createdAt(Instant.now())
                .build();
        chatMessageRepository.save(userMessage);

        // 2. Load conversation history (use clientHistory if provided, otherwise load from DB)
        List<ChatMessageDto> history;
        if (clientHistory != null) {
            history = new ArrayList<>(clientHistory);
        } else {
            List<ChatMessageEntity> historyEntities = chatMessageRepository.findBySessionOrderByCreatedAtAsc(session);
            history = historyEntities.stream()
                    .map(msg -> ChatMessageDto.builder()
                            .role(msg.getRole().name())
                            .content(msg.getContent())
                            .build())
                    .collect(Collectors.toList());
        }

        // 3. Get model details
        ModelEntity model = session.getModel();
        LlmClient llmClient = llmClientFactory.getClient(model.getApiFormat());

        // 4. Get available tools for this session
        List<Tool> availableTools = agentHelper.getAvailableTools(session);
        String toolsDescription = agentHelper.formatToolsDescription(availableTools);

        // 5. Build ReAct system prompt
        String reactSystemPrompt = agentHelper.buildReActSystemPrompt(session.getSystemPrompt(), toolsDescription);

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

            // Parse AgentDecision
            AgentDecision decision = agentHelper.parseDecision(llmResponse);
            String thought = decision.thought();
            var toolCall = decision.toolCall();
            String parsedFinalAnswer = decision.finalAnswer();

            if (toolCall != null && toolCall.name() != null) {
                String action = toolCall.name();
                String actionInput = toolCall.arguments();
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

                // Update scratchpad for the next iteration (intermediate steps are NOT saved to DB)
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
}
