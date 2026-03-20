# Agentic Agile Planner

An AI-powered sprint planning system that autonomously analyses software requirements, generates user stories with acceptance criteria, and creates Jira Epics and Stories — all driven by the Agent2Agent (A2A) protocol.

---

## Purpose

Agile sprint planning is time-consuming and requires consistent quality in breaking down requirements into well-defined user stories. This project automates the full sprint planning workflow using specialised AI agents that collaborate over the official A2A protocol. A developer or product owner provides a plain-text requirement and the system autonomously produces ready-to-use Jira tickets with acceptance criteria stored for downstream agents.

---

## How It Works

The system follows a three-step autonomous workflow:

**Step 1 — Requirement Analysis**  
The user sends a requirement to the Orchestrator Agent. The Orchestrator calls the Requirement Analysis Agent via A2A, which uses a local LLM (qwen2.5 via Ollama) to generate structured user stories with acceptance criteria.

**Step 2 — Iterative Refinement**  
The Orchestrator LLM reviews the generated stories. If they are incomplete or unclear, it automatically sends targeted feedback to the Requirement Analysis Agent for refinement. This loop continues until the stories meet quality standards.

**Step 3 — Approval and Jira Creation**  
Once satisfied, the Orchestrator triggers approval. The Requirement Analysis Agent creates an Epic in Jira, creates individual Story tickets linked to the Epic, and stores the acceptance criteria in a PGVector database for use by downstream agents such as Sprint Planner and Estimation agents.

---

## Architecture

```
User
 └── Orchestrator Agent (port 8080)
       ├── ChatClient (qwen2.5 via Ollama)
       ├── A2A Client (JSONRPCTransport)
       └── Requirement Analysis Agent (port 8081)
             ├── ChatClient (qwen2.5 via Ollama)
             ├── Jira REST API
             └── PGVector Store
```

The Orchestrator acts as an A2A Client that discovers and communicates with registered agents. Each agent exposes an AgentCard at `/.well-known/agent-card.json` describing its capabilities and skills. The Orchestrator discovers agents on startup and routes requests based on skill identifiers.

---

## Modules

| Module                       | Port | Role                                                     |
|------------------------------|------|----------------------------------------------------------|
| `common-utility`             | —    | Shared Jira client, DTOs, WebClient config               |
| `requirement-analysis-agent` | 8081 | A2A Server — analyses requirements, creates Jira tickets |
| `orchestrator-agent`         | 8080 | A2A Client — LLM-driven workflow orchestration           |

---

## A2A Agent Skills

The Requirement Analysis Agent exposes three skills via its AgentCard:

| Skill ID              | Skill Name          | Description                                                                                                |
|-----------------------|---------------------|------------------------------------------------------------------------------------------------------------|
| `analyze-requirement` | Analyze Requirement | Accepts plain-text requirement and generates user stories with acceptance criteria using a local LLM       |
| `refine-stories`      | Refine Stories      | Refines previously generated stories based on feedback using conversation history for context              |
| `approve-and-store`   | Approve and Store   | Creates Epic and Story tickets in Jira and stores acceptance criteria in VectorStore for downstream agents |

---

## A2A Integration Design

**Agent Discovery**  
The Orchestrator Agent uses `A2ACardResolver` on startup to fetch the AgentCard from each registered agent URL. Skills are registered into an in-memory `AgentRegistry` keyed by skill ID, enabling skill-based routing.

**Message Flow**  
The Orchestrator sends messages using `JSONRPCTransport` which serialises requests in the official A2A JSON-RPC format. The server receives the `message/send` method call, routes by `contextId` presence, processes the request, and returns a JSON-RPC response containing a Task object with artifacts.

**Session Management**  
The `AgentSessionStore` maps `contextId` to `AgentCard` so follow-up messages in a multi-turn conversation are always routed to the correct agent. Sessions are cleaned up after approval completes.

**Human-in-the-Loop**  
The system supports both fully autonomous mode and manual human-in-the-loop mode. Developers can use the `/plan` endpoint for full autonomy or call individual endpoints to manually control refinement and approval steps.

---

## Tech Stack

| Layer              | Technology                  |
|--------------------|-----------------------------|
| Language           | Java 21                     |
| Framework          | Spring Boot 3.5.11          |
| AI Framework       | Spring AI 1.0.1             |
| LLM                | qwen2.5 via Ollama (local)  |
| Embeddings         | nomic-embed-text via Ollama |
| Agent Protocol     | A2A Java SDK 1.0.0.Alpha1   |
| Vector Store       | PGVector (PostgreSQL 16)    |
| Project Management | Jira Cloud REST API v3      |
| Build              | Maven (multi-module)        |
| Runtime            | Docker (Ollama, PostgreSQL) |

---

## Infrastructure Requirements

| Service    | Version       | Purpose                                            |
|------------|---------------|----------------------------------------------------|
| Ollama     | Latest        | Local LLM inference — qwen2.5 and nomic-embed-text |
| PostgreSQL | 16 (pgvector) | Vector store for acceptance criteria               |
| Jira Cloud | —             | Epic and Story ticket creation                     |

---

## Key Design Decisions

**Why A2A Protocol?**  
A2A provides a vendor-neutral standard for agent communication. New agents such as Sprint Planner and Estimation can be added by registering their URL in `application.properties` — zero code changes to the Orchestrator.

**Why Local LLM?**  
Using qwen2.5 via Ollama keeps all data on-premise, avoids API costs, and enables deterministic output with temperature set to zero for consistent story generation across runs.

**Why PGVector?**  
Acceptance criteria stored in PGVector enables semantic search by downstream agents. The Sprint Planner and Estimation agents can retrieve relevant historical stories for planning and estimation without re-generating them.

**Why Spring AI?**  
Spring AI provides the `@Tool` annotation and `ChatClient` abstraction that allows the Orchestrator LLM to autonomously decide when to call each workflow step without hardcoded decision logic.

---

## Future Agents

The architecture is designed for extensibility. The following agents are planned:

| Agent                  | Port | Responsibility                                                    |
|------------------------|------|-------------------------------------------------------------------|
| `sprint-planner-agent` | 8082 | Organises stories into sprints based on velocity and priorities   |
| `estimation-agent`     | 8083 | Estimates story points using historical sprint data from PGVector |

Adding a new agent requires only registering its URL in `orchestrator-agent/application.properties`. The Orchestrator auto-discovers its skills on startup with zero code changes.

---

## API Reference

| Endpoint                                               | Method | Description                                                      |
|--------------------------------------------------------|--------|------------------------------------------------------------------|
| `/orchestrator-agent/orchestrator/plan`                | POST   | Fully autonomous sprint planning — single request, full workflow |
| `/orchestrator-agent/orchestrator/requirement/start`   | POST   | Start requirement analysis manually                              |
| `/orchestrator-agent/orchestrator/requirement/refine`  | POST   | Send refinement feedback manually                                |
| `/orchestrator-agent/orchestrator/requirement/approve` | POST   | Approve stories and create Jira tickets manually                 |
| `/orchestrator-agent/orchestrator/agents`              | GET    | List all discovered agents and their skills                      |

---

*Built with the A2A protocol — designed for extensible, autonomous, multi-agent sprint planning.*
