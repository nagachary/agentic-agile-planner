# Agentic Agile Planner — Issues and Fixes

## Overview

This document captures all the issues encountered during the development of the
`requirement-analysis-agent` module and the fixes applied to resolve them.

---

## Issue 1 — LLM Hallucination

### What happened
- The LLM generated a fake (hallucinated) tool results instead of calling actual tools
- The entire workflow produced fabricated results with no real side effects

### Root cause
- The `mistral` model has poor and unreliable tool calling support
- Default temperature setting was too high causing random unpredictable decisions

### Fixes applied
- Switched LLM model from `mistral` to `qwen2.5` which has significantly
  better tool calling reliability
- Set `temperature=0.0` to make model responses deterministic and instruction-following
- Added `top-p`, `top-k`, `repeat-penalty`, `num-predict` and `seed` properties
  to reduce randomness and hallucination risk
- Updated system prompt with strict rules:
  - Never invent data, session IDs or Epic keys
  - Always call real tools — never simulate tool calls
  - Only use values returned by actual tool executions

---

## Issue 2 — LLM Calling Tools in Parallel

### What happened
- The LLM fired two calls to `createEpicInJira` simultaneously
- First call had correct parameters and created a real Jira ticket.
- Second call had null parameters and threw a `NullPointerException`

### Root cause
- LLMs generate all tool calls before executing any of them — this is fundamental
  to how transformer models work and cannot be changed through prompting alone
- The approval message mentioned both `epicName` and `requirementDescription`
  as separate instructions which the model interpreted as two separate tool calls
- The second duplicate call was generated with null parameters because the model
  ran out of context while generating the duplicate

### Fixes applied
- Added null guard in `createEpicInJira` tool method to handle duplicate calls
  gracefully instead of throwing `NullPointerException`
- Added `sessionEpicKeys` map to track successful Epic keys — when a duplicate
  null call arrives the method returns the already-created Epic key instead of failing
- Updated `@Tool` description with explicit sequential execution rules:
  - `CALL EXACTLY ONCE per approval`
  - `NEVER call in parallel with other tools`
  - `SEQUENTIAL EXECUTION REQUIRED`
- Added null guard in `storeAcceptanceCriteria` to recover Epic key from
  `sessionEpicKeys` map if epicKey parameter arrives as null

---

## Issue 3 — Infinite Tool Loop

### What happened
- `analyzeRequirement` tool was being called in an infinite loop
- Application appeared to hang indefinitely with no user-visible output

### Root cause
- The tool return message contained instructions telling the LLM to generate
  user stories and present them to the user
- The LLM interpreted these instructions as a new task requiring another tool call
- This created a self-referencing loop — tool result triggered another tool call

### Fixes applied
- Changed tool return message from instructions to a simple confirmation
- The tool now returns a plain acknowledgement with no embedded instructions
- Added `RULE` to system prompt:
  - Call `analyzeRequirement` EXACTLY ONCE per request
  - After tool returns — generate stories directly in response
  - NEVER call `analyzeRequirement` in a loop or more than once per request

---

## Issue 4 — NullPointerException in buildTicketBody

### What happened
- `JiraClient.buildTicketBody()` threw `NullPointerException`
- Error occurred at `Map.of()` call inside the method
- Epic creation failed and LLM hallucinated a ticket key instead of reporting failure

### Root cause
- The LLM sometimes passed `null` as `requirementDescription` to `createEpicInJira`
- `Map.of()` in Java does not allow null values and throws `NullPointerException`
  immediately when any key or value is null
- No null guard existed before the `Map.of()` call

### Fixes applied
- Added null guard in `buildTicketBody()` with safe fallbacks for all parameters:
  - If `description` is null — fallback to `summary` value
  - If `summary` is null — fallback to `"No summary provided"`
  - If `issueType` is null — fallback to `"Epic"`
- Added null guard in `createEpicInJira` tool method:
  - If `epicName` is null — return `DUPLICATE_CALL_SKIPPED` message
  - If `requirementDescription` is null — fallback to `epicName` value

---

## Issue 5 — A2A Endpoint Returning Unknown Skill

### What happened
- Calling `POST /requirement-analysis-agent/requirement/` returned:
  `Unknown skill: . Supported: analyze-requirement, refine-stories, approve-and-store`
- The `skill` field in the response was always empty
- A2A message handling was unreachable with the correct JSON-RPC body

### Root cause
- The controller had `@RequestMapping("/requirement")` at class level
- This prefixed ALL endpoints in the class with `/requirement/`
- The A2A message endpoint `POST /` became `POST /requirement/`
- The JSON-RPC params were not being parsed because the wrong endpoint
  was receiving the request

### Fixes applied
- Removed `@RequestMapping("/requirement")` from the controller class level
- Kept `/requirement/` prefix only on individual REST convenience endpoint methods
- A2A endpoints now correctly map to:
  - `GET /.well-known/agent-card.json`
  - `POST /`
- REST convenience endpoints correctly map to:
  - `POST /requirement/analyze`
  - `POST /requirement/refine`
  - `POST /requirement/approve`

---

## Summary Table

| # | Issue                    | Root Cause                                  | Key Fix                               |
|---|--------------------------|---------------------------------------------|---------------------------------------|
| 1 | LLM Hallucination        | Weak model and high temperature             | Switch to qwen2.5 and temperature=0.0 |
| 2 | Parallel Tool Calls      | LLM generates all calls before executing    | Null guard and sessionEpicKeys map    |
| 3 | Infinite Tool Loop       | Tool returned instructions not confirmation | Simple return message and RULE 1      |
| 4 | NPE in buildTicketBody   | Null description passed to Map.of()         | Null guard with fallback values       |
| 5 | DB Connection on Startup | PGVector initializes schema on startup      | Docker and lazy-initialization=true   |
| 6 | Unknown Skill Error      | Class level @RequestMapping prefix          | Remove class level mapping            |

---

## Key Lessons Learned

- **Model choice is the most impactful decision** — a weak model cannot be fixed
  by prompt engineering alone. Always validate tool calling capability of the
  chosen model before building agents around it.

- **Temperature=0.0 is essential for agentic tool calling** — any randomness
  introduces hallucination risk in structured tool calling workflows.

- **LLMs generate all tool calls before executing any** — parallel tool calling
  is a fundamental behaviour of transformer models. Defensive null guards and
  state tracking are necessary rather than relying solely on prompt instructions.

- **Tool return messages must be results not instructions** — returning instructions
  from a tool causes the LLM to treat them as new tasks and call the tool again.

- **Fail fast on startup** — `@PostConstruct` validation in configuration classes
  catches misconfiguration immediately rather than letting it surface as a
  cryptic runtime error during a live demo.
