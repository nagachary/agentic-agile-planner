# Orchestrator Agent

The Orchestrator Agent is the AI brain of the Agentic Agile Planner system. It acts as an A2A Client that autonomously drives the full sprint planning workflow by discovering, communicating with, and coordinating specialised agents over the official Agent2Agent (A2A) protocol.

---

## Purpose

The Orchestrator Agent abstracts the entire sprint planning pipeline behind a single REST endpoint. A user sends a plain-text software requirement and a Jira project key — the Orchestrator takes over from there, autonomously analysing, refining, and approving user stories before creating Jira tickets and storing acceptance criteria. The user receives a structured summary without interacting with any downstream agent directly.

---

## Responsibilities

- Discover registered A2A agents on startup via their AgentCard
- Register discovered agent skills into an in-memory registry
- Route messages to the correct agent based on skill identifier
- Maintain session state across multi-turn conversations using contextId
- Drive the full sprint planning workflow autonomously using a local LLM
- Provide human-in-the-loop endpoints as an alternative to autonomous mode
- Clean up sessions after workflow completion

---

## Autonomous Workflow

The Orchestrator LLM drives the workflow through three tool calls:

```
1. startRequirement
       ↓ sends requirement to Requirement Analysis Agent
       ↓ receives contextId + generated user stories

2. refineRequirement (optional — called if stories are incomplete)
       ↓ sends targeted feedback to agent
       ↓ receives refined stories
       ↓ may loop until stories are satisfactory

3. approveRequirement
       ↓ triggers Epic creation in Jira
       ↓ triggers Story ticket creation linked to Epic
       ↓ triggers acceptance criteria storage in VectorStore
       ↓ session cleaned up
```

The LLM decides when to refine and when to approve — no hardcoded decision logic.

---

## A2A Agent Integration

### Agent Discovery

On startup, `AgentDiscoveryService` fetches the AgentCard from each configured agent URL using `A2ACardResolver`. Each skill declared in the AgentCard is registered into `AgentRegistry` keyed by its skill ID. This means new agents are discovered automatically — no code changes required.

### Skills Consumed

The Orchestrator currently consumes skills from the **Requirement Analysis Agent**:

| Skill ID              | Used By Tool         | Purpose                                                         |
|-----------------------|----------------------|-----------------------------------------------------------------|
| `analyze-requirement` | `startRequirement`   | Send requirement, receive user stories with acceptance criteria |
| `refine-stories`      | `refineRequirement`  | Send feedback, receive refined stories                          |
| `approve-and-store`   | `approveRequirement` | Approve stories, trigger Jira Epic and Story creation           |

### Message Protocol

All messages are sent using `JSONRPCTransport` in the official A2A JSON-RPC format:

- **New session** — message sent without `contextId`, server generates one and returns it in the Task response
- **Follow-up** — message includes `contextId` from previous response, server continues the same conversation
- **Approval** — message includes `contextId` and the text `Approved projectKey:X`, server detects the approval signal

### Session Management

`AgentSessionStore` maps each `contextId` to the `AgentCard` that owns that session. This ensures follow-up messages always reach the correct agent without the client needing to track which agent started the conversation.

### Response Parsing

Responses from agents are returned as A2A `Task` objects containing `contextId` and `artifacts`. The `A2AResponseParser` extracts the contextId for session continuity and the text content from artifacts for passing back to the LLM.

---

## Component Overview

| Component                | Responsibility                                                           |
|--------------------------|--------------------------------------------------------------------------|
| `OrchestratorController` | Exposes REST API — autonomous `/plan` and manual endpoints               |
| `OrchestratorService`    | LLM-powered orchestration with `@Tool` annotated workflow methods        |
| `A2AAgentClient`         | Sends A2A messages using `JSONRPCTransport` — generic for any agent      |
| `A2AMessageBuilder`      | Builds official A2A `Message` objects — new session, follow-up, approval |
| `A2AResponseParser`      | Extracts contextId and text from `ClientEvent` subtypes                  |
| `AgentDiscoveryService`  | Fetches AgentCards on startup and registers skills                       |
| `AgentRegistry`          | In-memory skill-to-AgentCard map for routing                             |
| `AgentSessionStore`      | Thread-safe contextId-to-AgentCard session map                           |
| `AgentProperties`        | Binds `agents.urls` list from `application.properties`                   |
| `ChatClientConfig`       | Wires Spring AI `ChatClient` with Ollama model                           |

---

## Tech Stack

| Layer          | Technology                                    |
|----------------|-----------------------------------------------|
| Language       | Java 21                                       |
| Framework      | Spring Boot 3.5.11                            |
| AI Framework   | Spring AI 1.0.1                               |
| LLM            | qwen2.5 via Ollama (local, temperature 0.0)   |
| Agent Protocol | A2A Java SDK 1.0.0.Alpha1                     |
| Transport      | `JSONRPCTransport` (JSON-RPC over HTTP)       |
| Build          | Maven (child module of agentic-agile-planner) |

---

## Configuration

All agent URLs are configured in `application.properties` — no code changes needed to add new agents:

| Property                                    | Description                                                           |
|---------------------------------------------|-----------------------------------------------------------------------|
| `server.port`                               | Orchestrator runs on port 8080                                        |
| `server.servlet.context-path`               | `/orchestrator-agent`                                                 |
| `agents.urls`                               | Comma-separated list of A2A agent base URLs                           |
| `spring.ai.ollama.base-url`                 | Ollama endpoint for LLM inference                                     |
| `spring.ai.ollama.chat.options.model`       | LLM model — `qwen2.5`                                                 |
| `spring.ai.ollama.chat.options.temperature` | Set to `0.0` for deterministic output                                 |
| `spring.main.lazy-initialization`           | Set to `true` — AgentDiscoveryService forced eager via `@Lazy(false)` |

---

## API Endpoints

### Autonomous Mode

| Endpoint                                | Method | Description                                             |
|-----------------------------------------|--------|---------------------------------------------------------|
| `/orchestrator-agent/orchestrator/plan` | POST   | Single endpoint — LLM drives full workflow autonomously |

**Request body:**
```
{
  "requirement": "Build a JWT authentication system",
  "projectKey": "NC018JIRA"
}
```

**Response:** Natural language summary from LLM including Epic key, story keys, and brief description.

---

### Human-in-the-Loop Mode

| Endpoint                                               | Method | Description                                                |
|--------------------------------------------------------|--------|------------------------------------------------------------|
| `/orchestrator-agent/orchestrator/requirement/start`   | POST   | Start requirement analysis — returns contextId and stories |
| `/orchestrator-agent/orchestrator/requirement/refine`  | POST   | Send refinement feedback — returns refined stories         |
| `/orchestrator-agent/orchestrator/requirement/approve` | POST   | Approve stories — creates Jira tickets                     |
| `/orchestrator-agent/orchestrator/agents`              | GET    | List all discovered agents and registered skills           |

---

## Extensibility

The Orchestrator is designed to support any number of agents without code changes:

- Register a new agent URL in `agents.urls`
- Orchestrator discovers it on next startup
- New skills are registered automatically
- Add a new `@Tool` method in `OrchestratorService` to expose the skill to the LLM
- `A2AAgentClient` works for any agent — no agent-specific code

**Planned future agents:**

| Agent                | Skill              | Purpose                                             |
|----------------------|--------------------|-----------------------------------------------------|
| Sprint Planner Agent | `create-sprint`    | Organise stories into sprints by velocity           |
| Estimation Agent     | `estimate-stories` | Estimate story points from historical PGVector data |

---

## Dependencies

| Artifact                                | Version        | Purpose                                  |
|-----------------------------------------|----------------|------------------------------------------|
| `spring-boot-starter-web`               | 3.5.11         | REST API                                 |
| `spring-boot-starter-webflux`           | 3.5.11         | Reactive HTTP client                     |
| `spring-ai-starter-model-ollama`        | 1.0.1          | ChatClient, @Tool, Ollama integration    |
| `a2a-java-sdk-client`                   | 1.0.0.Alpha1   | A2A Client and AgentCard resolution      |
| `a2a-java-sdk-spec`                     | 1.0.0.Alpha1   | Message, Task, AgentCard, TextPart types |
| `a2a-java-sdk-http-client`              | 1.0.0.Alpha1   | HTTP layer for A2A communication         |
| `a2a-java-sdk-client-transport-jsonrpc` | 1.0.0.Alpha1   | JSON-RPC transport for sending messages  |
| `common-utility`                        | 1.0.0-SNAPSHOT | Shared Jira client and DTOs              |

---
