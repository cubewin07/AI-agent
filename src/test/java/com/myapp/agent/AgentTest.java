package com.myapp.agent;

import com.myapp.agent.llm.LlmClient;
import com.myapp.agent.llm.LlmClientFactory;
import com.myapp.agent.tool.CalculatorTool;
import com.myapp.agent.tool.ToolRegistry;
import com.myapp.model.ChatMessageEntity;
import com.myapp.model.ModelEntity;
import com.myapp.model.SessionEntity;
import com.myapp.model.enums.ApiFormat;
import com.myapp.model.enums.ChatRole;
import com.myapp.repository.ChatMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentTest {

    @Mock
    private LlmClientFactory llmClientFactory;

    @Mock
    private LlmClient llmClient;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    private ToolRegistry toolRegistry;
    private AgentHelper agentHelper;
    private Agent agent;

    private SessionEntity session;
    private ModelEntity model;

    @BeforeEach
    void setUp() {
        CalculatorTool calculatorTool = new CalculatorTool();
        toolRegistry = new ToolRegistry(List.of(calculatorTool));
        toolRegistry.init();

        agentHelper = new AgentHelper(toolRegistry);
        agent = new Agent(llmClientFactory, chatMessageRepository, agentHelper);

        model = ModelEntity.builder()
                .id(UUID.randomUUID())
                .name("gpt-4")
                .apiFormat(ApiFormat.OPENAI)
                .apiKey("test-key")
                .url("https://api.openai.com/v1/chat/completions")
                .build();

        session = SessionEntity.builder()
                .id(UUID.randomUUID())
                .name("Test Session")
                .model(model)
                .enabledTools("calculator")
                .systemPrompt("You are a helpful assistant.")
                .build();
    }

    @Test
    void processQuery_withToolCall_shouldExecuteToolAndReturnFinalAnswer() {
        // Arrange
        when(llmClientFactory.getClient(ApiFormat.OPENAI)).thenReturn(llmClient);

        // First LLM call returns a tool action
        String llmResponse1 = "Thought: I need to calculate 2 + 2.\nAction: calculator[2 + 2]";
        // Second LLM call returns the final answer
        String llmResponse2 = "Thought: I have the result.\nFinal Answer: The answer is 4.0.";

        when(llmClient.generate(any(), any(), any(), any(), any()))
                .thenReturn(llmResponse1)
                .thenReturn(llmResponse2);

        // Act
        String result = agent.processQuery(session, "What is 2 + 2?");

        // Assert
        assertThat(result).isEqualTo("The answer is 4.0.");

        // Verify messages saved to DB
        ArgumentCaptor<ChatMessageEntity> messageCaptor = ArgumentCaptor.forClass(ChatMessageEntity.class);
        verify(chatMessageRepository, times(4)).save(messageCaptor.capture());

        List<ChatMessageEntity> savedMessages = messageCaptor.getAllValues();
        
        // 1. User query
        assertThat(savedMessages.get(0).getRole()).isEqualTo(ChatRole.USER);
        assertThat(savedMessages.get(0).getContent()).isEqualTo("What is 2 + 2?");

        // 2. Assistant thought + action
        assertThat(savedMessages.get(1).getRole()).isEqualTo(ChatRole.ASSISTANT);
        assertThat(savedMessages.get(1).getContent()).contains("Action: calculator[2 + 2]");
        assertThat(savedMessages.get(1).getToolName()).isEqualTo("calculator");
        assertThat(savedMessages.get(1).getStepNumber()).isEqualTo(1);

        // 3. Tool observation
        assertThat(savedMessages.get(2).getRole()).isEqualTo(ChatRole.TOOL);
        assertThat(savedMessages.get(2).getContent()).contains("Observation: 4.0");
        assertThat(savedMessages.get(2).getToolName()).isEqualTo("calculator");
        assertThat(savedMessages.get(2).getStepNumber()).isEqualTo(1);

        // 4. Assistant final answer
        assertThat(savedMessages.get(3).getRole()).isEqualTo(ChatRole.ASSISTANT);
        assertThat(savedMessages.get(3).getContent()).isEqualTo("The answer is 4.0.");
    }

    @Test
    void processQuery_withoutToolCall_shouldReturnFinalAnswerDirectly() {
        // Arrange
        when(llmClientFactory.getClient(ApiFormat.OPENAI)).thenReturn(llmClient);

        String llmResponse = "Thought: The user is greeting me.\nFinal Answer: Hello! How can I help you?";
        when(llmClient.generate(any(), any(), any(), any(), any())).thenReturn(llmResponse);

        // Act
        String result = agent.processQuery(session, "Hello");

        // Assert
        assertThat(result).isEqualTo("Hello! How can I help you?");

        ArgumentCaptor<ChatMessageEntity> messageCaptor = ArgumentCaptor.forClass(ChatMessageEntity.class);
        verify(chatMessageRepository, times(2)).save(messageCaptor.capture());

        List<ChatMessageEntity> savedMessages = messageCaptor.getAllValues();
        assertThat(savedMessages.get(0).getRole()).isEqualTo(ChatRole.USER);
        assertThat(savedMessages.get(1).getRole()).isEqualTo(ChatRole.ASSISTANT);
        assertThat(savedMessages.get(1).getContent()).isEqualTo("Hello! How can I help you?");
    }

    @Test
    void processQuery_withUnavailableTool_shouldReturnToolNotAvailableError() {
        // Arrange
        when(llmClientFactory.getClient(ApiFormat.OPENAI)).thenReturn(llmClient);

        // LLM tries to call 'weather' tool which is not enabled in session
        String llmResponse1 = "Thought: I need to check the weather.\nAction: weather[London]";
        String llmResponse2 = "Thought: The weather tool is not available.\nFinal Answer: I cannot check the weather because the tool is not enabled.";

        when(llmClient.generate(any(), any(), any(), any(), any()))
                .thenReturn(llmResponse1)
                .thenReturn(llmResponse2);

        // Act
        String result = agent.processQuery(session, "What is the weather in London?");

        // Assert
        assertThat(result).isEqualTo("I cannot check the weather because the tool is not enabled.");

        ArgumentCaptor<ChatMessageEntity> messageCaptor = ArgumentCaptor.forClass(ChatMessageEntity.class);
        verify(chatMessageRepository, times(4)).save(messageCaptor.capture());

        List<ChatMessageEntity> savedMessages = messageCaptor.getAllValues();
        
        // Tool observation should contain the error message
        assertThat(savedMessages.get(2).getRole()).isEqualTo(ChatRole.TOOL);
        assertThat(savedMessages.get(2).getContent()).contains("Error: Tool 'weather' is not available in this session.");
    }
}
