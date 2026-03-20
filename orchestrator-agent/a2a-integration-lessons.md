# A2A Client-Server Integration — Top 10 Issues & Fixes

## 1. `A2ACardResolver` Strips Context Path
**Issue:** `A2ACardResolver("http://localhost:8081/requirement-analysis-agent")` internally appends `/.well-known/agent-card.json` but drops the context path, resulting in a 404.  
**Fix:** Removed the Spring context path (`server.servlet.context-path`) from the agent server so the agent card is served at root level `http://localhost:8081/.well-known/agent-card.json`.  
**Lesson:** A2A protocol expects the agent card at the server root — never nest it under a context path.

---

## 2. `A2AClient` Does Not Exist in `1.0.0.Alpha1`
**Issue:** All documentation referenced `A2AClient` but the class does not exist in the `1.0.0.Alpha1` SDK jar — IntelliJ showed no such class in any package.  
**Fix:** Used `io.a2a.client.Client` with `Client.builder(agentCard).clientConfig(clientConfig).build()` — the correct class in `1.0.0.Alpha1`.  
**Lesson:** Always verify SDK classes via `javap` on the actual jar rather than trusting online documentation which may reference a different version.

---

## 3. `Message` and `Task` API Changed from Class to Record
**Issue:** IntelliJ resolved `Message` and `Task` from `0.2.5` jar showing `getContextId()`, `getParts()`, `getArtifacts()` — but `1.0.0.Alpha1` made them records with `contextId()`, `parts()`, `artifacts()`.  
**Fix:** Added explicit `version 1.0.0.Alpha1` to `orchestrator-agent/pom.xml` for all A2A artifacts, overriding parent POM's `0.2.5` — then updated all method calls to record accessor style.  
**Lesson:** Maven dependency version conflicts between parent and child POMs cause IntelliJ to show wrong API — always verify the actual resolved version with `mvn dependency:tree`.

---

## 4. `Client.sendMessage` BiConsumer Never Fires
**Issue:** `Client.sendMessage(message, consumers, errorHandler)` was called successfully but the `BiConsumer` never fired — `CompletableFuture` timed out every time.  
**Fix:** Bypassed `Client` entirely and used `JSONRPCTransport.sendMessage(params, null)` directly — which is synchronous and returns `EventKind` immediately.  
**Lesson:** The `Client` SDK requires explicit transport configuration via `withTransport()` — without it the BiConsumer is never invoked. Use `JSONRPCTransport` directly for simpler synchronous use cases.

---

## 5. `JSONRPCTransport` Uses Proto for Serialization
**Issue:** Server returned `"state": "completed"` (JSON spec format) but the `JSONRPCTransport` validated the response against the proto schema `a2a.v1.TaskState` which expects `"TASK_STATE_COMPLETED"`.  
**Fix:** Changed the server response to use `"TASK_STATE_COMPLETED"` in the status field by manually patching the serialized JSON after `JsonUtil.toJson(task)`.  
**Lesson:** `JSONRPCTransport` uses protobuf JSON format internally — the wire format differs from the human-readable A2A spec JSON format for enum values.

---

## 6. `JsonUtil.toJson(task)` Wraps Task in Extra Key
**Issue:** `JsonUtil.toJson(task)` produced `{"task": {"id":"...", ...}}` but initially we unwrapped it — causing the `StreamingEventKindTypeAdapter` to fail because it specifically looks for a `"task"` key in the result.  
**Fix:** Kept the `"task"` wrapper in the result — `StreamingEventKindTypeAdapter` checks `if (result.has("task"))` to deserialize as `Task`, so the wrapper is required.  
**Lesson:** The A2A SDK Gson deserializer uses discriminator keys (`"task"`, `"message"`) to determine the result type — never unwrap them.

---

## 7. `AgentCard` Lost `url()` — Replaced by `supportedInterfaces`
**Issue:** `agentCard.url()` existed in `0.2.5` but was removed in `1.0.0.Alpha1` — replaced by `agentCard.supportedInterfaces()` returning `List<AgentInterface>` with `protocolBinding` and `url`.  
**Fix:** Updated `AgentCardConfig` to use `AgentCard.builder().supportedInterfaces(List.of(new AgentInterface("JSONRPC", agentUrl)))` and `JSONRPCTransport` automatically selects the correct interface.  
**Lesson:** The `1.0.0.Alpha1` spec introduced `supportedInterfaces` to support multiple transports — agents must declare at least one interface for clients to connect.

---

## 8. `@ConfigurationProperties` Constructor Binding Ignored with `@Component`
**Issue:** `AgentProperties` used constructor injection `AgentProperties(List<String> urls)` with `@Component` and `@ConstructorBinding` — but Spring Boot 3.x ignores `@ConstructorBinding` when `@Component` is present, so `urls` was always null.  
**Fix:** Switched to mutable field with setter `setUrls(List<String> urls)` — Spring Boot binds `agents.urls` via the setter automatically when using `@Component @ConfigurationProperties`.  
**Lesson:** In Spring Boot 3.x, `@ConstructorBinding` only works without `@Component` — when using `@Component`, always use setter-based binding for `@ConfigurationProperties`.

---

## 9. Server Received Official A2A Format but Parsed Custom Format
**Issue:** The `JSONRPCTransport` sends the official A2A JSON-RPC format `{"method":"SendMessage","params":{"message":{...}}}` but the server controller tried to deserialize `@RequestBody Message message` — causing `role = null` NPE.  
**Fix:** Changed controller to `@RequestBody JsonNode request` and manually extracted `request.path("params").path("message")` — then routed by `contextId` presence instead of `skill` field.  
**Lesson:** A2A servers must accept the full JSON-RPC envelope and route by `contextId` — null contextId means new session, present contextId means follow-up or approval.

---

## 10. `kind` Field Valid in JSON Spec but Invalid in Proto Schema
**Issue:** The A2A JSON spec requires `"kind":"text"` in parts and `"kind":"task"` in results — but the proto schema `a2a.v1.Part` and `a2a.v1.Task` do not have a `kind` field, causing `InvalidParamsJsonMappingException`.  
**Fix:** Removed `kind` fields from parts and tasks in the response — proto uses oneof (`TEXT`, `FILE`, `DATA`) for part type discrimination instead of a `kind` discriminator field.  
**Lesson:** There is a divergence between the A2A JSON specification and the proto schema used by `JSONRPCTransport` — always validate field names against the actual proto schema when using `JSONRPCTransport`.

---
