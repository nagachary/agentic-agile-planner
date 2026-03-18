# Requirement Analysis Agent

## Overview
The Requirement Analysis Agent is an AI-powered microservice that transforms plain text
business requirements into structured user stories with Gherkin acceptance criteria.
It is part of the Agentic Agile Planner — a suite of collaborative AI agents that
automate end-to-end sprint planning workflows.

---

## Role in Sprint Planning Automation
This agent is the first in the sprint planning pipeline, responsible for requirement
intake and analysis. It feeds structured acceptance criteria downstream to the
Sprint Planner Agent and Estimation Agent, enabling fully automated sprint planning
from a single plain text requirement.

```
Plain Text Requirement
        ↓
Requirement Analysis Agent   ← this agent
        ↓
Sprint Planner Agent
        ↓
Estimation Agent
        ↓
Orchestrator Agent
```

---

## A2A Protocol and Exposed Skills
This agent is built on the Agent2Agent (A2A) protocol, making it discoverable and
callable by any A2A-compliant orchestrator. It exposes three skills that cover the
full requirement analysis lifecycle.

- **analyze-requirement** — accepts plain text requirement and generates user stories
- **refine-stories** — iteratively refines stories based on human feedback
- **approve-and-store** — creates Jira Epic and stores acceptance criteria in VectorStore

---

## Agent Card
The agent publishes its identity, capabilities and skill definitions via the standard
A2A discovery endpoint. Any A2A client can discover this agent without prior configuration.

- **Discovery endpoint** — `GET /.well-known/agent-card.json`
- **Message endpoint** — `POST /`
- **Agent URL** — `http://localhost:8081/requirement-analysis-agent`

---

## GAME Agentic Pattern
This agent is architected using the GAME pattern — Goal, Action, Memory, Environment —
ensuring clear separation of concerns across all layers of the agent.

- **Goal** — `AgentCardConfig` declares agent identity and skills to the A2A ecosystem
- **Action** — `RequirementTools` defines LLM-callable tools for Jira and VectorStore
- **Memory** — `ChatMemoryConfig` maintains conversation history per session using `MessageWindowChatMemory`
- **Environment** — `ChatClientConfig` and `A2AServerController` wire Ollama and A2A protocol

---

## Rule-Based Prompt Template
The agent uses a structured rule-based system prompt to guide LLM behaviour precisely
across all workflow phases. Rules are defined in `requirement-analysis-system.txt`.

- Ten explicit rules govern tool usage, execution order, approval signals and failure handling
- Separate workflow sections define step-by-step instructions for analysis, refinement and approval
- Prompt is loaded from an external `.txt` file — editable without recompiling the application

---

## Workflow and Human-in-the-Loop
The agent implements a human-in-the-loop approval pattern ensuring no Jira tickets are
created without explicit user confirmation. Stories are refined iteratively until the user
approves.

- User submits requirement → agent generates stories → user reviews and refines
- On explicit approval → agent creates Jira Epic → stores acceptance criteria in VectorStore
- Session memory is cleared after approval completing the workflow cleanly

---

## Jira Epic Creation
On user approval the agent automatically creates a Jira Epic in the configured project
linking the requirement to all downstream sprint stories. The Epic key is returned to
the user and stored in VectorStore metadata for downstream agents to reference.

- Creates Epic via Jira REST API v3 using Basic Auth
- Returns real Epic key e.g. `NC018JIRA-1` for Sprint Planner Agent to link stories
- Epic description contains the full approved requirement text

---

## Tool Performance and Model Optimisation
Significant effort was invested in optimising LLM tool calling reliability and preventing
hallucination. The agent uses carefully tuned model parameters and defensive tool design.

- Switched from `mistral` to `qwen2.5` for reliable tool calling support
- Set `temperature=0.0` for deterministic behaviour and strict instruction following
- Implemented null guards and session state tracking to handle parallel tool calls gracefully

---

## Technology Stack

| Technology         | Purpose                                          |
|--------------------|--------------------------------------------------|
| Spring Boot 3.5.11 | Application framework and REST server            |
| Spring AI 1.0.1    | LLM integration, tool calling and VectorStore    |
| Ollama             | Local LLM runtime — chat model `qwen2.5`         |
| nomic-embed-text   | Embedding model for VectorStore (768 dimensions) |
| PGVector           | Vector database for acceptance criteria storage  |
| PostgreSQL         | Underlying database for PGVector                 |
| A2A Java SDK       | A2A protocol server implementation               |
| Jira REST API v3   | Epic and ticket creation in Jira Cloud           |

---

## Running the Agent

**Prerequisites:**
- Docker running with `pgvector/pgvector:pg16` and `my-ollama` containers
- Models pulled: `ollama pull qwen2.5` and `ollama pull nomic-embed-text`
- IntelliJ environment variables set:

```
JIRA_EMAIL=your-email@example.com
JIRA_API_TOKEN=your-atlassian-api-token
JIRA_PROJECT_KEY=your-project-key
```

**Start the agent:**
```bash
cd requirement-analysis-agent
mvn spring-boot:run
```

**Verify agent is running:**
```
GET http://localhost:8081/requirement-analysis-agent/.well-known/agent-card.json
```

---

## REST Convenience Endpoints (Development Only)

| Method | Endpoint                       | Purpose                         |
|--------|--------------------------------|---------------------------------|
| GET    | `/.well-known/agent-card.json` | A2A agent discovery             |
| POST   | `/`                            | A2A JSON-RPC message handling   |
| POST   | `/requirement/analyze`         | Start new analysis session      |
| POST   | `/requirement/refine`          | Refine stories with feedback    |
| POST   | `/requirement/approve`         | Approve stories and create Epic |
