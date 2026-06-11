# Plan - ReAct AI Chat Agent Implementation

## Overview
We are building a ReAct AI chat agent with 3 main objects:
1. **Model**: Registered by users with custom link, API key, and format (OpenAI-compatible, Anthropic, Google Gemini).
2. **Session**: Chat scope. Created per chat with tools, model, custom system prompt, and conversation history.
3. **Agent**: Query scope. Orchestrates the ReAct loop (reasoning, tool calling, step counting, final response). The final response is saved to the conversation history.

We will also implement a Tool Registry where tools can be registered and limited per Session/Agent. We will replace the current `ExampleEntity` and migrations with new entities for `Model`, `Session`, `ChatMessage`, and `Tool` (or similar), and write unit/integration tests to verify the ReAct loop and tool execution.

## Phase 1: Database Schema & Entities
- Create Flyway migrations to drop `examples` table and create tables for `models`, `sessions`, `chat_messages`, and `tools` (or tool configurations).
- Define JPA Entities:
  - `ModelEntity`: fields for name, apiFormat (enum: OPENAI, ANTHROPIC, GEMINI), apiKey, url, contextLimit, owner (UserEntity).
  - `SessionEntity`: fields for name, systemPrompt, model (ModelEntity), owner (UserEntity), enabledTools (many-to-many or list).
  - `ChatMessageEntity`: fields for session, role (enum: USER, ASSISTANT, SYSTEM, TOOL), content, toolName, toolCallId, stepNumber, createdAt.
- Update repositories.

## Phase 2: LLM Clients & Tool Registry
- Add dependencies for OpenAI, Anthropic, and Gemini clients (or implement lightweight HTTP clients using Spring's `RestClient` or `WebClient` to support custom URLs easily).
- Implement `LlmClient` interface and implementations for OpenAI, Anthropic, and Gemini.
- Implement `Tool` interface and a `ToolRegistry`.
- Implement a few sample tools (e.g., `CalculatorTool`, `WeatherTool`) for testing.

## Phase 3: ReAct Agent Loop
- Implement the ReAct loop in `AgentService` or `Agent` class.
- The loop should:
  1. Take the user query.
  2. Format the prompt (system prompt + history + ReAct instructions).
  3. Call the LLM.
  4. Parse the response for Thought/Action/Final Answer.
  5. If Action: log tool usage, execute the tool, append tool response to history, increment step count, and repeat.
  6. If Final Answer: save to history and return.
  7. Handle max step limits.

## Phase 4: REST Controllers & DTOs
- Create controllers for managing Models, Sessions, and sending messages.
- Create DTOs for requests and responses.

## Phase 5: Verification & Testing
- Write unit tests for the ReAct loop using mocked LLM clients.
- Write integration tests for the API endpoints.
- Verify tool execution logging.
