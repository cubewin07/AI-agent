# Plan v2 - ReAct AI Chat Agent Implementation

## Overview
We are building a ReAct AI chat agent with 3 main objects:
1. **Model**: Registered by users with custom link, API key, and format (OpenAI-compatible, Anthropic, Google Gemini).
2. **Session**: Chat scope. Created per chat with tools, model, custom system prompt, and conversation history.
3. **Agent**: Query scope. Orchestrates the ReAct loop (reasoning, tool calling, step counting, final response). The final response is saved to the conversation history.

In Plan v2, we introduce key improvements to the data structures, database storage strategy, and request payload handling to make the agent more robust, efficient, and client-driven.

---

## Key Improvements in Plan v2

### 1. Structured LLM Response Parsing (JSON-based)
- **Goal**: Replace fragile regex parsing of text responses with structured JSON parsing.
- **Design**:
  - Define a Java record/class representing the structured response from the LLM:
    ```java
    public record AgentDecision(
        String thought,
        ToolCall toolCall,
        String finalAnswer
    ) {}

    public record ToolCall(
        String name,
        String arguments // JSON string or raw input
    ) {}
    ```
  - Update the system prompt to instruct the LLM to output a single JSON object matching this schema.
  - Use Jackson (`ObjectMapper`) to parse the LLM response directly into `AgentDecision`.
  - Handle parsing errors gracefully (e.g., retry or fallback).

### 2. Optimized Chat Message Storage
- **Goal**: Avoid polluting the database with intermediate reasoning steps and tool execution logs.
- **Design**:
  - Only persist the user's initial query (`USER`) and the agent's final response (`ASSISTANT`) in the `chat_messages` table.
  - Keep intermediate thoughts, tool calls, and observations in-memory (e.g., in a local list or scratchpad) during the ReAct loop execution.
  - This keeps the database clean and ensures the history endpoint only returns user-facing messages.

### 3. Client-Driven Conversation History
- **Goal**: Allow the client to send the conversation history in the request payload instead of loading it from the database.
- **Design**:
  - Update `ChatRequest` to include both the current query and the conversation history:
    ```java
    public class ChatRequest {
        @NotBlank(message = "Query is required")
        private String query;
        
        private List<ChatMessageDto> history; // Optional: client-provided history
    }
    ```
  - In the backend, if the client provides `history` in the request, use it to construct the LLM prompt. If not provided, fallback to loading from the database or start with an empty history.
  - This gives the frontend full control over context management (e.g., trimming history, adding context, or client-side message editing).

---

## Phase 1: Database Schema & Entities
- Create Flyway migrations to drop `examples` table and create tables for `models`, `sessions`, `chat_messages`, and `tools`.
- Define JPA Entities:
  - `ModelEntity`: fields for name, apiFormat (enum: OPENAI, ANTHROPIC, GEMINI), apiKey, url, contextLimit, owner (UserEntity).
  - `SessionEntity`: fields for name, systemPrompt, model (ModelEntity), owner (UserEntity), enabledTools (many-to-many or list).
  - `ChatMessageEntity`: fields for session, role (enum: USER, ASSISTANT), content, createdAt. (Note: intermediate tool roles are removed from DB storage).
- Update repositories.

## Phase 2: LLM Clients & Tool Registry
- Add dependencies for OpenAI, Anthropic, and Gemini clients (or implement lightweight HTTP clients using Spring's `RestClient` or `WebClient` to support custom URLs easily).
- Implement `LlmClient` interface and implementations for OpenAI, Anthropic, and Gemini.
- Implement `Tool` interface and a `ToolRegistry`.
- Implement sample tools (e.g., `CalculatorTool`, `WeatherTool`) for testing.

## Phase 3: ReAct Agent Loop (v2)
- Implement the ReAct loop in `AgentService` or `Agent` class.
- The loop should:
  1. Take the user query and client-provided history.
  2. Format the prompt (system prompt + history + ReAct JSON instructions).
  3. Call the LLM.
  4. Parse the response as a JSON object into `AgentDecision`.
  5. If `toolCall` is present: execute the tool, append the observation to the in-memory scratchpad, increment step count, and repeat.
  6. If `finalAnswer` is present: save the user query and final answer to the database, and return.
  7. Handle max step limits.

## Phase 4: REST Controllers & DTOs
- Create controllers for managing Models, Sessions, and sending messages.
- Update `ChatRequest` DTO to accept `query` and `history`.
- Create DTOs for responses.

## Phase 5: Verification & Testing
- Write unit tests for the ReAct loop using mocked LLM clients and verifying JSON parsing.
- Write integration tests for the API endpoints.
- Verify that only user and final assistant messages are saved to the database.
